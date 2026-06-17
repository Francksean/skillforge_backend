package org.example.skillforgeapi.service;

import org.example.skillforgeapi.exception.DuplicateResourceException;
import org.example.skillforgeapi.exception.EntityNotFoundException;
import org.example.skillforgeapi.model.dto.request.ScenarioRequest;
import org.example.skillforgeapi.model.dto.request.ScenarioReorderRequest;
import org.example.skillforgeapi.model.dto.response.ScenarioResponse;
import org.example.skillforgeapi.model.dto.response.AssetSummaryResponse;
import org.example.skillforgeapi.model.dto.response.ScenarioSummaryResponse;
import org.example.skillforgeapi.model.entity.Module;
import org.example.skillforgeapi.model.entity.Scenario;
import org.example.skillforgeapi.model.entity.Asset;
import org.example.skillforgeapi.model.enums.AssetStatus;
import org.example.skillforgeapi.repository.AssetRepository;
import org.example.skillforgeapi.repository.ScenarioRepository;
import org.example.skillforgeapi.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ModuleRepository moduleRepository;
    private final AssetRepository assetRepository; // À injecter


    /**
     * Récupère tous les scénarios d'un module (triés)
     */
    public List<ScenarioResponse> getScenariosByModule(UUID moduleId) {
        // Vérifier que le module existe
        if (!moduleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException("Module non trouvé avec l'ID: " + moduleId);
        }
        List<Scenario> scenarios = scenarioRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        return scenarios.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les scénarios actifs d'un module (pour les stagiaires)
     */
    public List<ScenarioResponse> getActiveScenariosByModule(UUID moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException("Module non trouvé avec l'ID: " + moduleId);
        }
        List<Scenario> scenarios = scenarioRepository.findByModuleIdAndActiveTrueOrderByOrderIndexAsc(moduleId);
        return scenarios.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un scénario par son ID
     */
    public ScenarioResponse getScenarioById(UUID id) {
        Scenario scenario = getScenarioEntity(id);
        return toDetailedResponse(scenario);
    }

    /**
     * Récupère l'entité Scenario (pour les services imbriqués)
     */
    public Scenario getScenarioEntity(UUID id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Scénario non trouvé avec l'ID: " + id));
    }

    /**
     * Crée un scénario dans un module
     */
    @Transactional
    public ScenarioResponse createScenario(UUID moduleId, ScenarioRequest request) {
        // 1. Vérifier que le module existe
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("Module non trouvé avec l'ID: " + moduleId));

        // 2. Vérifier l'unicité du nom dans ce module
        if (scenarioRepository.existsByNameAndModuleId(request.getName(), moduleId)) {
            throw new DuplicateResourceException("Un scénario avec le nom '" + request.getName() + "' existe déjà dans ce module.");
        }

        // 3. Déterminer l'ordre (position à la fin)
        long count = scenarioRepository.countByModuleId(moduleId);
        int nextOrderIndex = (int) count + 1;

        // 4. Construire l'entité
        Scenario scenario = new Scenario();
        scenario.setName(request.getName());
        scenario.setDescription(request.getDescription());
        scenario.setThumbnailUrl(request.getThumbnailUrl());
        scenario.setActive(true); // Par défaut
        scenario.setOrderIndex(nextOrderIndex);
        scenario.setModule(module);

        scenario = scenarioRepository.save(scenario);
        log.info("Scénario créé : '{}' dans le module '{}' (ID: {}, ordre: {})",
                scenario.getName(), module.getTitle(), scenario.getId(), scenario.getOrderIndex());

        return toResponse(scenario);
    }

    /**
     * Met à jour un scénario
     */
    @Transactional
    public ScenarioResponse updateScenario(UUID id, ScenarioRequest request) {
        Scenario scenario = getScenarioEntity(id);

        // Vérifier l'unicité du nom dans le module (en excluant le scénario courant)
        if (!scenario.getName().equals(request.getName()) &&
                scenarioRepository.existsByNameAndModuleIdAndIdNot(
                        request.getName(), scenario.getModule().getId(), id)) {
            throw new DuplicateResourceException("Un autre scénario avec le nom '" + request.getName() +
                    "' existe déjà dans ce module.");
        }

        // Mettre à jour les champs
        scenario.setName(request.getName());
        scenario.setDescription(request.getDescription());
        scenario.setThumbnailUrl(request.getThumbnailUrl());

        scenario = scenarioRepository.save(scenario);
        log.info("Scénario mis à jour : '{}' (ID: {})", scenario.getName(), scenario.getId());

        return toResponse(scenario);
    }

    /**
     * Supprime un scénario (et le retire de la liste des assets associés)
     */
    @Transactional
    public void deleteScenario(UUID id) {
        Scenario scenario = getScenarioEntity(id);
        UUID moduleId = scenario.getModule().getId();
        String name = scenario.getName();

        // Nettoyer les relations Many-to-Many (pour éviter les orphelins)
        scenario.getAssets().clear();

        scenarioRepository.delete(scenario);
        log.info("Scénario supprimé : '{}' (ID: {})", name, id);

        // Réorganiser les ordres des scénarios restants dans le module
        reorganizeOrderAfterDeletion(moduleId);
    }

    /**
     * Active / Désactive un scénario
     */
    @Transactional
    public ScenarioResponse toggleScenarioActive(UUID id) {
        Scenario scenario = getScenarioEntity(id);
        scenario.setActive(!scenario.getActive());
        scenario = scenarioRepository.save(scenario);
        log.info("Scénario '{}' (ID: {}) : active = {}", scenario.getName(), scenario.getId(), scenario.getActive());
        return toResponse(scenario);
    }

    /**
     * Réorganise l'ordre des scénarios à l'intérieur d'un module
     */
    @Transactional
    public void reorderScenarios(UUID moduleId, ScenarioReorderRequest request) {
        // 1. Vérifier que le module existe
        if (!moduleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException("Module non trouvé avec l'ID: " + moduleId);
        }

        List<UUID> newOrderIds = request.getScenarioIds();

        // 2. Vérifier que tous les IDs existent et appartiennent bien au module
        List<Scenario> allScenarios = scenarioRepository.findAllById(newOrderIds);
        if (allScenarios.size() != newOrderIds.size()) {
            throw new EntityNotFoundException("Certains scénarios spécifiés n'existent pas.");
        }

        // Vérifier qu'aucun scénario ne provient d'un autre module
        for (Scenario scenario : allScenarios) {
            if (!scenario.getModule().getId().equals(moduleId)) {
                throw new IllegalArgumentException("Le scénario '" + scenario.getName() +
                        "' n'appartient pas au module ID " + moduleId);
            }
        }

        // 3. Mettre à jour l'orderIndex pour chaque scénario dans le nouvel ordre
        for (int i = 0; i < newOrderIds.size(); i++) {
            UUID scenarioId = newOrderIds.get(i);
            Scenario scenario = allScenarios.stream()
                    .filter(s -> s.getId().equals(scenarioId))
                    .findFirst()
                    .orElseThrow();
            scenario.setOrderIndex(i + 1);
            scenarioRepository.save(scenario);
        }

        log.info("Scénarios du module {} réorganisés : nouveau nombre de positions = {}", moduleId, newOrderIds.size());
    }

    /**
     * Réorganise les orderIndex après une suppression (pour éviter les trous)
     */
    private void reorganizeOrderAfterDeletion(UUID moduleId) {
        List<Scenario> remainingScenarios = scenarioRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        for (int i = 0; i < remainingScenarios.size(); i++) {
            Scenario scenario = remainingScenarios.get(i);
            int newOrder = i + 1;
            if (!scenario.getOrderIndex().equals(newOrder)) {
                scenario.setOrderIndex(newOrder);
                scenarioRepository.save(scenario);
            }
        }
        log.info("Ordres des scénarios réorganisés pour le module ID {}", moduleId);
    }

    @Transactional
    public void attachAssetToScenario(UUID scenarioId, UUID assetId) {
        Scenario scenario = getScenarioEntity(scenarioId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("Asset non trouvé avec l'ID: " + assetId));

        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new IllegalStateException("Impossible d'attacher un asset en statut PENDING ou DELETED.");
        }

        if (scenario.getAssets().contains(asset)) {
            throw new DuplicateResourceException("Cet asset est déjà attaché à ce scénario.");
        }

        scenario.getAssets().add(asset);
        scenarioRepository.save(scenario);
        log.info("Asset {} attaché au scénario {}", assetId, scenarioId);
    }

    /**
     * Détache un asset d'un scénario.
     */
    @Transactional
    public void detachAssetFromScenario(UUID scenarioId, UUID assetId) {
        Scenario scenario = getScenarioEntity(scenarioId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("Asset non trouvé avec l'ID: " + assetId));

        boolean removed = scenario.getAssets().remove(asset);
        if (!removed) {
            throw new EntityNotFoundException("Cet asset n'est pas attaché à ce scénario.");
        }
        scenarioRepository.save(scenario);
        log.info("Asset {} détaché du scénario {}", assetId, scenarioId);
    }

    /**
     * Liste les assets attachés à un scénario (uniquement ACTIVE).
     */
    public List<AssetSummaryResponse> getAssetsByScenario(UUID scenarioId) {
        Scenario scenario = getScenarioEntity(scenarioId);
        return scenario.getAssets().stream()
                .filter(asset -> asset.getStatus() == AssetStatus.ACTIVE)
                .map(this::toAssetSummary)
                .collect(Collectors.toList());
    }

    /**
     * Liste les scénarios qui utilisent un asset donné.
     */
    public List<ScenarioSummaryResponse> getScenariosByAsset(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("Asset non trouvé avec l'ID: " + assetId));
        return asset.getScenarios().stream()
                .map(this::toScenarioSummary)
                .collect(Collectors.toList());
    }


    // ---------- Méthodes de mapping ----------

    private ScenarioResponse toResponse(Scenario scenario) {
        return ScenarioResponse.builder()
                .id(scenario.getId())
                .name(scenario.getName())
                .description(scenario.getDescription())
                .thumbnailUrl(scenario.getThumbnailUrl())
                .active(scenario.getActive())
                .orderIndex(scenario.getOrderIndex())
                .moduleId(scenario.getModule().getId())
                .createdAt(scenario.getCreatedAt())
                .updatedAt(scenario.getUpdatedAt())
                .build();
    }

    private ScenarioResponse toDetailedResponse(Scenario scenario) {
        ScenarioResponse response = toResponse(scenario);

        // Ajouter la liste des assets associés (uniquement ceux qui sont ACTIVE)
        List<AssetSummaryResponse> assetSummaries = scenario.getAssets().stream()
                .filter(asset -> asset.getStatus() == AssetStatus.ACTIVE) // Ne montrer que les assets prêts
                .map(this::toAssetSummary)
                .collect(Collectors.toList());
        response.setAssets(assetSummaries);

        return response;
    }

    private AssetSummaryResponse toAssetSummary(Asset asset) {
        return AssetSummaryResponse.builder()
                .id(asset.getId())
                .name(asset.getName())
                .type(asset.getType() != null ? asset.getType().name() : null)
                .fileUrl(asset.getFileUrl())
                .build();
    }

    private ScenarioSummaryResponse toScenarioSummary(Scenario scenario) {
        return ScenarioSummaryResponse.builder()
                .id(scenario.getId())
                .name(scenario.getName())
                .moduleId(scenario.getModule().getId())
                .active(scenario.getActive())
                .build();
    }

}
