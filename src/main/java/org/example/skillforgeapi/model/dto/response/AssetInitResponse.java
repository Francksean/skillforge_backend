package org.example.skillforgeapi.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.skillforgeapi.model.dto.request.AssetStep;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AssetInitResponse {
    private UUID assetId;
    private String token;
    private String sftpPath;
    private String sftpUploadUrl;
    private String equipmentId;
    private List<AssetStep> steps;
}
