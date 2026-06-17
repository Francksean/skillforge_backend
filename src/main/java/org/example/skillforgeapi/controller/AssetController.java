package org.example.skillforgeapi.controller;

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
public class AssetController {

    private final AssetService assetService;
    private final SftpClientService sftpClientService;

    /**
     * Étape 1 : Initialisation de l'upload (admin/trainer)
     */
    @PostMapping("/init")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<AssetInitResponse> initAsset(@Valid @RequestBody AssetInitRequest request) {
        AssetInitResponse response = assetService.initializeAsset(request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Récupérer le statut d'un asset (pour polling)
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
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
