package org.example.skillforgeapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.skillforgeapi.model.dto.request.AssetInitRequest;
import org.example.skillforgeapi.model.dto.response.AssetInitResponse;
import org.example.skillforgeapi.model.enums.AssetStatus;
import org.example.skillforgeapi.service.AssetService;
import org.example.skillforgeapi.service.SftpClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Upload (via SFTP) et téléchargement des ressources pédagogiques")
public class AssetController {

    private final AssetService assetService;
    private final SftpClientService sftpClientService;

    /**
     * Étape 1 : Initialisation de l'upload (admin/trainer)
     */
    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Initialiser un upload d'asset", description = "Étape 1 : crée l'asset et retourne les informations d'upload SFTP. Accès : ADMIN, TRAINER.")
    public ResponseEntity<AssetInitResponse> initAsset(@Valid @RequestBody AssetInitRequest request) {
        AssetInitResponse response = assetService.initializeAsset(request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Récupérer le statut d'un asset (pour polling)
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Statut d'un asset", description = "Retourne le statut d'un asset (utile pour le polling après upload). Accès : ADMIN, TRAINER.")
    public ResponseEntity<AssetStatus> getStatus(@PathVariable UUID id) {
        AssetStatus status = assetService.getAssetStatus(id);
        return ResponseEntity.ok(status);
    }

    /**
     * Téléchargement HTTP du fichier (pour les stagiaires)
     * - Vérifie que l'asset est ACTIF
     * - Soit proxy via SFTP, soit redirection vers une URL signée
     * Ici on fait un proxy classique avec stream (à améliorer si fichier > 200 Mo)
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('TRAINEE', 'ADMIN', 'TRAINER')")
    @Operation(summary = "Télécharger un asset", description = "Stream le fichier de l'asset (uniquement si ACTIVE). Accès : TRAINEE, ADMIN, TRAINER.")
    public void downloadAsset(@PathVariable UUID id, HttpServletResponse response) throws IOException {
        try {
            var asset = assetService.getAsset(id);
            if (asset.getStatus() != AssetStatus.ACTIVE) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Asset not active");
                return;
            }
            sftpClientService.streamFileToResponse(asset, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Download failed: " + e.getMessage());
        }
    }
}
