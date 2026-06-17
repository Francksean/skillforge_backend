package org.example.skillforgeapi.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillforgeapi.model.dto.request.AssetInitRequest;
import org.example.skillforgeapi.model.dto.request.SftpGoWebhookPayload;
import org.example.skillforgeapi.model.dto.response.AssetInitResponse;
import org.example.skillforgeapi.model.entity.Asset;
import org.example.skillforgeapi.model.entity.Scenario;
import org.example.skillforgeapi.model.enums.AssetStatus;
import org.example.skillforgeapi.repository.AssetRepository;
import org.example.skillforgeapi.repository.ScenarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final AssetRepository assetRepository;
    private final ScenarioRepository scenarioRepository;

    @Value("${app.sftp.base-url}")
    private String sftpBaseUrl; // ex: sftp://sftpgo:2022

    @Value("${app.sftp.local-base-path}")
    private String localBasePath;

    @Value("${app.sftp.base-virtual-path}")
    private String baseVirtualPath; // par défaut "/"

    @Value("${app.base-url}")
    private String appBaseUrl;

    /**
     * Étape 1 : Initialisation de l'upload (création d'un asset PENDING)
     */
    @Transactional
    public AssetInitResponse initializeAsset(AssetInitRequest request) {
        // Générer un token unique
        String token = UUID.randomUUID().toString().replace("-", "");

        // Construire le chemin virtuel relatif (par rapport à la racine du compte SFTP)
        // Exemple : /MODEL/a1b2c3d4/
        String virtualPath = baseVirtualPath + "/" + request.getType().name() + "/" + token + "/";
        // Nettoyer les doubles slashes éventuels
        virtualPath = virtualPath.replaceAll("/+", "/");

        // Construire le chemin physique absolu
        Path physicalPath = Paths.get(localBasePath, request.getType().name(), token);

        try {
            // Créer le dossier (et ses parents) s'il n'existe pas
            Files.createDirectories(physicalPath);
            log.info("Dossier physique créé : {}", physicalPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Impossible de créer le dossier physique pour l'asset", e);
            throw new RuntimeException("Erreur lors de la création du répertoire de stockage", e);
        }

        // Créer l'entité Asset en base
        Asset asset = new Asset();
        asset.setName(request.getName());
        asset.setType(request.getType());
        asset.setSftpPath(virtualPath); // stocke le chemin virtuel (sans nom de fichier)
        asset.setStatus(AssetStatus.PENDING);
        asset = assetRepository.save(asset);

        // Pré-associer aux scénarios si demandé
        if (request.getScenariosId() != null && !request.getScenariosId().isEmpty()) {
            List<Scenario> scenarios = scenarioRepository.findAllById(request.getScenariosId());
            asset.setScenarios(new HashSet<>(scenarios));
            asset = assetRepository.save(asset);
        }

        // Construire la réponse : le chemin virtuel relatif (pour que le client FTP s'y place)
        // On envoie le chemin sans le premier '/' pour certains clients, mais on peut l'envoyer avec.
        // Nous envoyons le chemin complet virtuel (commençant par /) pour être explicite.
        String fullSftpUrl = sftpBaseUrl + virtualPath;

        return new AssetInitResponse(
                asset.getId(),
                token,
                virtualPath,      // chemin relatif virtuel (ex: /MODEL/a1b2c3/)
                fullSftpUrl       // URL complète sftp://... (optionnel)
        );
    }


    /**
     * Étape 2 : Finalisation par webhook SFTPGo
     */
    @Transactional
    public void finalizeAssetFromSftpUpload(SftpGoWebhookPayload payload) {
        // Extraire le token du chemin complet
        String path = payload.getPath(); // ex: /uploads/a1b2c3/model.gltf
        String[] parts = path.split("/");
        if (parts.length < 3) {
            log.error("Chemin invalide dans le webhook: {}", path);
            return;
        }
        String token = parts[2]; // le token est le 3ème élément (index 2)

        // Chercher l'asset PENDING avec ce token dans son sftpPath
        Asset asset = assetRepository.findBySftpPathContainingAndStatus(token, AssetStatus.PENDING)
                .orElseThrow(() -> new EntityNotFoundException("Asset avec token " + token + " non trouvé (ou déjà finalisé)"));

        // Mettre à jour les métadonnées
        asset.setFileSize(payload.getSize());
        if (payload.getChecksum() != null && payload.getChecksum().containsKey("sha256")) {
            asset.setChecksumSha256(payload.getChecksum().get("sha256"));
        }
        // Enregistrer le chemin complet (avec le nom du fichier)
        asset.setSftpPath(payload.getPath());

        // URL publique (pour téléchargement HTTP)
        asset.setFileUrl(appBaseUrl + "/assets/" + asset.getId() + "/download");

        // Changer le statut
        asset.setStatus(AssetStatus.ACTIVE);

        assetRepository.save(asset);
        log.info("Asset {} finalisé avec succès (taille: {} bytes)", asset.getId(), asset.getFileSize());
    }

    /**
     * Récupérer un asset par son ID (avec vérification existence)
     */
    public Asset getAsset(UUID id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Asset non trouvé: " + id));
    }

    /**
     * Vérifier le statut d'un asset (utilisé par le frontend pour le polling)
     */
    public AssetStatus getAssetStatus(UUID id) {
        Asset asset = getAsset(id);
        return asset.getStatus();
    }

    /**
     * Générer l'URL de téléchargement (via SFTPGo ou direct)
     * Pour l'instant, on retourne simplement l'URL publique HTTP.
     * On peut aussi rediriger vers un endpoint HTTP qui fera proxy.
     */
    public String getDownloadUrl(UUID assetId) {
        Asset asset = getAsset(assetId);
        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new IllegalStateException("Le fichier n'est pas encore disponible (statut: " + asset.getStatus() + ")");
        }
        // Retourne l'URL stockée (générée lors de la finalisation)
        return asset.getFileUrl();
    }
}