package org.example.skillforgeapi.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioResponse {
    private UUID id;
    private String name;
    private String description;
    private String siteConcerning;
    private String thumbnailUrl;
    private Boolean active;
    private Integer orderIndex;
    private UUID moduleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Pour les détails : liste des assets associés (IDs ou objets résumés)
    private List<AssetSummaryResponse> assets;
}

