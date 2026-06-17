package org.example.skillforgeapi.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSummaryResponse {
    private UUID id;
    private String name;
    private String type;
    private String fileUrl;
}
