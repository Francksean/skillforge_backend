package org.example.skillforgeapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.skillforgeapi.model.dto.request.ScenarioRequest;
import org.example.skillforgeapi.model.dto.request.ScenarioReorderRequest;
import org.example.skillforgeapi.model.dto.response.ScenarioResponse;
import org.example.skillforgeapi.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Scénarios", description = "Gestion des scénarios rattachés aux modules")
public class ScenarioController {

    private final ScenarioService scenarioService;

    // ---------- Endpoints liés à un Module ----------

    /**
     * GET /api/modules/{moduleId}/scenarios
     * Liste tous les scénarios d'un module (triés)
     * Accès : TRAINEE, TRAINER, ADMIN
     */
    @GetMapping("/modules/{moduleId}/scenarios")
    @PreAuthorize("hasAnyRole('TRAINEE', 'TRAINER', 'ADMIN')")
    @Operation(summary = "Lister les scénarios d'un module", description = "Liste tous les scénarios d'un module (triés). Accès : TRAINEE, TRAINER, ADMIN.")
    public ResponseEntity<List<ScenarioResponse>> getScenariosByModule(@PathVariable UUID moduleId) {
        List<ScenarioResponse> scenarios = scenarioService.getScenariosByModule(moduleId);
        return ResponseEntity.ok(scenarios);
    }

    /**
     * GET /api/modules/{moduleId}/scenarios/active
     * Liste uniquement les scénarios actifs d'un module (pour les stagiaires)
     * Accès : TRAINEE, TRAINER, ADMIN
     */
    @GetMapping("/modules/{moduleId}/scenarios/active")
    @PreAuthorize("hasAnyRole('TRAINEE', 'TRAINER', 'ADMIN')")
    @Operation(summary = "Lister les scénarios actifs d'un module", description = "Liste uniquement les scénarios actifs (pour les stagiaires). Accès : TRAINEE, TRAINER, ADMIN.")
    public ResponseEntity<List<ScenarioResponse>> getActiveScenariosByModule(@PathVariable UUID moduleId) {
        List<ScenarioResponse> scenarios = scenarioService.getActiveScenariosByModule(moduleId);
        return ResponseEntity.ok(scenarios);
    }

    /**
     * POST /api/modules/{moduleId}/scenarios
     * Crée un nouveau scénario dans le module
     * Accès : ADMIN, TRAINER
     */
    @PostMapping("/modules/{moduleId}/scenarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Créer un scénario", description = "Crée un nouveau scénario dans le module. Accès : ADMIN, TRAINER.")
    public ResponseEntity<ScenarioResponse> createScenario(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ScenarioRequest request) {
        ScenarioResponse created = scenarioService.createScenario(moduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/modules/{moduleId}/scenarios/reorder
     * Réorganise l'ordre des scénarios d'un module
     * Accès : ADMIN, TRAINER
     */
    @PatchMapping("/modules/{moduleId}/scenarios/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Réordonner les scénarios", description = "Réorganise l'ordre des scénarios d'un module. Accès : ADMIN, TRAINER.")
    public ResponseEntity<Void> reorderScenarios(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ScenarioReorderRequest request) {
        scenarioService.reorderScenarios(moduleId, request);
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoints dédiés à un Scénario (accès direct) ----------

    /**
     * GET /api/scenarios/{id}
     * Détails d'un scénario (avec ses assets)
     * Accès : TRAINEE, TRAINER, ADMIN
     */
    @GetMapping("/scenarios/{id}")
    @PreAuthorize("hasAnyRole('TRAINEE', 'TRAINER', 'ADMIN')")
    @Operation(summary = "Détails d'un scénario", description = "Retourne un scénario avec ses assets. Accès : TRAINEE, TRAINER, ADMIN.")
    public ResponseEntity<ScenarioResponse> getScenarioById(@PathVariable UUID id) {
        ScenarioResponse scenario = scenarioService.getScenarioById(id);
        return ResponseEntity.ok(scenario);
    }

    /**
     * PUT /api/scenarios/{id}
     * Met à jour un scénario
     * Accès : ADMIN, TRAINER
     */
    @PutMapping("/scenarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Mettre à jour un scénario", description = "Met à jour un scénario. Accès : ADMIN, TRAINER.")
    public ResponseEntity<ScenarioResponse> updateScenario(
            @PathVariable UUID id,
            @Valid @RequestBody ScenarioRequest request) {
        ScenarioResponse updated = scenarioService.updateScenario(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/scenarios/{id}
     * Supprime un scénario
     * Accès : ADMIN uniquement
     */
    @DeleteMapping("/scenarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un scénario", description = "Supprime un scénario. Accès : ADMIN.")
    public ResponseEntity<Void> deleteScenario(@PathVariable UUID id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/scenarios/{id}/toggle
     * Active / Désactive un scénario
     * Accès : ADMIN, TRAINER
     */
    @PatchMapping("/scenarios/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Activer / désactiver un scénario", description = "Bascule l'état actif d'un scénario. Accès : ADMIN, TRAINER.")
    public ResponseEntity<ScenarioResponse> toggleScenarioActive(@PathVariable UUID id) {
        ScenarioResponse updated = scenarioService.toggleScenarioActive(id);
        return ResponseEntity.ok(updated);
    }
}