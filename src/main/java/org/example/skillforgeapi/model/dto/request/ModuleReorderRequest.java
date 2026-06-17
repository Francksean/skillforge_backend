package org.example.skillforgeapi.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ModuleReorderRequest {
    @NotEmpty(message = "La liste des IDs ne peut pas être vide")
    private List<UUID> moduleIds;
}
