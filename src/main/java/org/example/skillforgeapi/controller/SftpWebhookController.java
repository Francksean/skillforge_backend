package org.example.skillforgeapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillforgeapi.model.dto.request.SftpGoWebhookPayload;
import org.example.skillforgeapi.service.AssetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/sftp")
@RequiredArgsConstructor
@Slf4j
public class SftpWebhookController {

    private final AssetService assetService;

    @Value("${app.sftp.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/upload")
    public ResponseEntity<Void> handleUpload(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody SftpGoWebhookPayload payload) {

        // 1. Vérification du secret
        if (webhookSecret == null || !webhookSecret.equals(secret)) {
            log.warn("Tentative de webhook non autorisée (secret invalide)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Ignorer les événements qui ne sont pas des uploads complets
        if (!"upload".equals(payload.getEvent()) || payload.getSize() == 0) {
            log.debug("Webhook ignoré : event={}, size={}", payload.getEvent(), payload.getSize());
            return ResponseEntity.ok().build();
        }

        // 3. Traiter la finalisation
        try {
            assetService.finalizeAssetFromSftpUpload(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erreur lors du traitement du webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}