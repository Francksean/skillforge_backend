package org.example.skillforgeapi.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModuleRequest {
    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    private String description;
}
