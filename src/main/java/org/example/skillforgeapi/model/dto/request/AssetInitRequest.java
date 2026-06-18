package org.example.skillforgeapi.model.dto.request;

import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import org.example.skillforgeapi.util.AssetType;

import java.util.List;
import java.util.UUID;

@Data
public class AssetInitRequest {
    @NotNull
    private String name;

    @NotNull
    private AssetType type;

    // Optionnel : pré-associer à des niveaux
    private List<UUID> scenariosId;

    // Payload Unity : identifiant de l'équipement et étapes configurées
    private String equipmentId;
    private List<AssetStep> steps;
}
