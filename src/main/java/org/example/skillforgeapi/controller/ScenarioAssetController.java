package org.example.skillforgeapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.skillforgeapi.model.dto.response.AssetSummaryResponse;
import org.example.skillforgeapi.model.dto.response.ScenarioSummaryResponse;
import org.example.skillforgeapi.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Scénarios ↔ Assets", description = "Association entre scénarios et assets")
public class ScenarioAssetController {

    private final ScenarioService scenarioService;

    /**
     * POST /api/scenarios/{scenarioId}/assets/{assetId}
     * Attache un asset à un scénario.
     */
    @PostMapping("/scenarios/{scenarioId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Attacher un asset à un scénario", description = "Associe un asset existant à un scénario. Accès : ADMIN, TRAINER.")
    public ResponseEntity<Void> attachAssetToScenario(
            @PathVariable UUID scenarioId,
            @PathVariable UUID assetId) {
        scenarioService.attachAssetToScenario(scenarioId, assetId);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/scenarios/{scenarioId}/assets/{assetId}
     * Détache un asset d'un scénario.
     */
    @DeleteMapping("/scenarios/{scenarioId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Détacher un asset d'un scénario", description = "Retire l'association entre un asset et un scénario. Accès : ADMIN, TRAINER.")
    public ResponseEntity<Void> detachAssetFromScenario(
            @PathVariable UUID scenarioId,
            @PathVariable UUID assetId) {
        scenarioService.detachAssetFromScenario(scenarioId, assetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/scenarios/{scenarioId}/assets
     * Liste les assets d'un scénario (uniquement ACTIVE).
     */
    @GetMapping("/scenarios/{scenarioId}/assets")
    @PreAuthorize("hasAnyRole('TRAINEE', 'TRAINER', 'ADMIN')")
    @Operation(summary = "Lister les assets d'un scénario", description = "Liste les assets ACTIVE d'un scénario. Accès : TRAINEE, TRAINER, ADMIN.")
    public ResponseEntity<List<AssetSummaryResponse>> getAssetsByScenario(
            @PathVariable UUID scenarioId) {
        List<AssetSummaryResponse> assets = scenarioService.getAssetsByScenario(scenarioId);
        return ResponseEntity.ok(assets);
    }

    /**
     * GET /api/assets/{assetId}/scenarios
     * Liste les scénarios qui utilisent un asset donné.
     */
    @GetMapping("/assets/{assetId}/scenarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Lister les scénarios utilisant un asset", description = "Liste les scénarios qui référencent un asset donné. Accès : ADMIN, TRAINER.")
    public ResponseEntity<List<ScenarioSummaryResponse>> getScenariosByAsset(
            @PathVariable UUID assetId) {
        List<ScenarioSummaryResponse> scenarios = scenarioService.getScenariosByAsset(assetId);
        return ResponseEntity.ok(scenarios);
    }
}
