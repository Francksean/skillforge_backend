package org.example.skillforgeapi.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScenarioRequest {
    @NotBlank(message = "Le nom du scénario est obligatoire")
    private String name;
    private String description;
    private String thumbnailUrl;
}
