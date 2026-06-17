package org.example.skillforgeapi.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AssetInitResponse {
    private UUID assetId;
    private String token;
    private String sftpPath;
    private String sftpUploadUrl;
}
