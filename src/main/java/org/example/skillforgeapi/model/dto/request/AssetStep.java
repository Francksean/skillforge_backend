package org.example.skillforgeapi.model.dto.request;

import lombok.Data;

@Data
public class AssetStep {
    private Integer stepId;
    private String instructionText;
    private String targetObjectName;
    private String expectedAction;
}
