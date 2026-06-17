package org.example.skillforgeapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillforgeapi.exception.DuplicateResourceException;
import org.example.skillforgeapi.exception.EntityNotFoundException;
import org.example.skillforgeapi.model.dto.request.ModuleReorderRequest;
import org.example.skillforgeapi.model.dto.request.ModuleRequest;
import org.example.skillforgeapi.model.dto.response.ModuleResponse;
import org.example.skillforgeapi.model.dto.response.ScenarioSummaryResponse;
import org.example.skillforgeapi.model.entity.Module;
import org.example.skillforgeapi.model.entity.Scenario;
import org.example.skillforgeapi.repository.ModuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleService {

    private final ModuleRepository moduleRepository;

    /**
     * Récupère tous les modules triés par ordre
     */
    public List<ModuleResponse> getAllModules() {
        List<Module> modules = moduleRepository.findAllByOrderByOrderIndexAsc();
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un module par son ID
     */
    public ModuleResponse getModuleById(UUID id) {
        Module module = getModuleEntity(id);
        return toDetailedResponse(module);
    }

    /**
     * Récupère l'entité Module (pour les services imbriqués)
     */
    public Module getModuleEntity(UUID id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Module non trouvé avec l'ID: " + id));
    }

    /**
     * Crée un nouveau module
     */
    @Transactional
    public ModuleResponse createModule(ModuleRequest request) {
        // Vérifier l'unicité du titre
        if (moduleRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Un module avec le titre '" + request.getTitle() + "' existe déjà.");
        }

        // Déterminer l'ordre (position à la fin)
        List<Module> existingModules = moduleRepository.findAllByOrderByOrderIndexAsc();
        int maxOrderIndex = existingModules.isEmpty() ? 0 : existingModules.get(existingModules.size() - 1).getOrderIndex();

        // Créer le module
        Module module = new Module();
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setOrderIndex(maxOrderIndex + 1);

        module = moduleRepository.save(module);
        log.info("Module créé : {} (ID: {}, ordre: {})", module.getTitle(), module.getId(), module.getOrderIndex());

        return toResponse(module);
    }

    /**
     * Met à jour un module existant
     */
    @Transactional
    public ModuleResponse updateModule(UUID id, ModuleRequest request) {
        Module module = getModuleEntity(id);

        // Vérifier l'unicité du titre (en excluant le module courant)
        if (!module.getTitle().equals(request.getTitle()) &&
                moduleRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            throw new DuplicateResourceException("Un autre module avec le titre '" + request.getTitle() + "' existe déjà.");
        }

        // Mettre à jour les champs
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());

        module = moduleRepository.save(module);
        log.info("Module mis à jour : {} (ID: {})", module.getTitle(), module.getId());

        return toResponse(module);
    }

    /**
     * Supprime un module (cascade sur les niveaux)
     */
    @Transactional
    public void deleteModule(UUID id) {
        Module module = getModuleEntity(id);

        // Vérifier si le module a des niveaux (optionnel, cascade gère déjà)
        if (!module.getScenarios().isEmpty()) {
            log.warn("Suppression du module '{}' avec {} niveaux associés", module.getTitle(), module.getScenarios().size());
        }

        moduleRepository.delete(module);
        log.info("Module supprimé : {} (ID: {})", module.getTitle(), id);

        // Réorganiser les ordres des modules restants
        reorganizeOrderAfterDeletion(id);
    }

    /**
     * Réorganise l'ordre des modules (réarrangement manuel)
     */
    @Transactional
    public void reorderModules(ModuleReorderRequest request) {
        List<UUID> newOrderIds = request.getModuleIds();

        // 1. Vérifier que tous les IDs existent
        List<Module> allModules = moduleRepository.findAllById(newOrderIds);
        if (allModules.size() != newOrderIds.size()) {
            throw new EntityNotFoundException("Certains modules spécifiés n'existent pas.");
        }

        // 2. Créer un map ID -> Module pour mise à jour rapide
        java.util.Map<UUID, Module> moduleMap = allModules.stream()
                .collect(Collectors.toMap(Module::getId, m -> m));

        // 3. Mettre à jour l'orderIndex pour chaque module dans le nouvel ordre
        for (int i = 0; i < newOrderIds.size(); i++) {
            UUID moduleId = newOrderIds.get(i);
            Module module = moduleMap.get(moduleId);
            module.setOrderIndex(i + 1);
            moduleRepository.save(module);
        }

        log.info("Modules réorganisés : nouveau nombre de positions = {}", newOrderIds.size());
    }

    /**
     * Réorganise les orderIndex après une suppression (pour éviter les trous)
     */
    private void reorganizeOrderAfterDeletion(UUID deletedId) {
        List<Module> remainingModules = moduleRepository.findAllByOrderByOrderIndexAsc();
        for (int i = 0; i < remainingModules.size(); i++) {
            Module module = remainingModules.get(i);
            int newOrder = i + 1;
            if (!module.getOrderIndex().equals(newOrder)) {
                module.setOrderIndex(newOrder);
                moduleRepository.save(module);
            }
        }
        log.info("Ordres réorganisés après suppression du module ID {}", deletedId);
    }

    // ---------- Méthodes de mapping ----------

    /**
     * Mapping simple (sans la liste des niveaux)
     */
    private ModuleResponse toResponse(Module module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .scenarioCount(module.getScenarios().size())
                .build();
    }

    /**
     * Mapping détaillé (avec la liste des niveaux)
     */
    private ModuleResponse toDetailedResponse(Module module) {
        ModuleResponse response = toResponse(module);

        // Ajouter les niveaux résumés
        List<ScenarioSummaryResponse> scenarioSummaries = module.getScenarios().stream()
                .map(this::toScenarioSummary)
                .collect(Collectors.toList());
        response.setScenarios(scenarioSummaries);

        return response;
    }

    private ScenarioSummaryResponse toScenarioSummary(Scenario scenario) {
        return ScenarioSummaryResponse.builder()
                .id(scenario.getId())
                .name(scenario.getName())
                .active(scenario.getActive())
                .build();
    }
}