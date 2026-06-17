package org.example.skillforgeapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.skillforgeapi.model.dto.request.ModuleReorderRequest;
import org.example.skillforgeapi.model.dto.request.ModuleRequest;
import org.example.skillforgeapi.model.dto.response.ModuleResponse;
import org.example.skillforgeapi.service.ModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    /**
     * GET /api/modules
     * Liste tous les modules (triés par ordre)
     * Accès : TRAINEE, TRAINER, ADMIN
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TRAINEE', 'TRAINER', 'ADMIN')")
    public ResponseEntity<List<ModuleResponse>> getAllModules() {
        List<ModuleResponse> modules = moduleService.getAllModules();
        return ResponseEntity.ok(modules);
    }

    /**
     * GET /api/modules/{id}
     * Détails d'un module (avec ses niveaux)
     * Accès : TRAINEE, TRAINER, ADMIN
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINEE', 'TRAINER', 'ADMIN')")
    public ResponseEntity<ModuleResponse> getModuleById(@PathVariable UUID id) {
        ModuleResponse module = moduleService.getModuleById(id);
        return ResponseEntity.ok(module);
    }

    /**
     * POST /api/modules
     * Crée un nouveau module
     * Accès : ADMIN, TRAINER
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<ModuleResponse> createModule(@Valid @RequestBody ModuleRequest request) {
        ModuleResponse created = moduleService.createModule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/modules/{id}
     * Met à jour un module existant
     * Accès : ADMIN, TRAINER
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleRequest request) {
        ModuleResponse updated = moduleService.updateModule(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/modules/{id}
     * Supprime un module (avec ses niveaux en cascade)
     * Accès : ADMIN uniquement (car destruction de données)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModule(@PathVariable UUID id) {
        moduleService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/modules/reorder
     * Réorganise l'ordre des modules
     * Accès : ADMIN, TRAINER
     */
    @PatchMapping("/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<Void> reorderModules(@Valid @RequestBody ModuleReorderRequest request) {
        moduleService.reorderModules(request);
        return ResponseEntity.ok().build();
    }
}