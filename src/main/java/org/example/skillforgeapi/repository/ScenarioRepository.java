package org.example.skillforgeapi.repository;

import org.example.skillforgeapi.model.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {
    // Récupère les scénarios d'un module triés par ordre
    List<Scenario> findByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    // Récupère les scénarios actifs d'un module (pour les stagiaires)
    List<Scenario> findByModuleIdAndActiveTrueOrderByOrderIndexAsc(UUID moduleId);

    // Compte les scénarios dans un module (pour déterminer le prochain orderIndex)
    long countByModuleId(UUID moduleId);

    // Vérifie l'unicité du nom à l'intérieur d'un module (pour la création)
    boolean existsByNameAndModuleId(String name, UUID moduleId);

    // Vérifie l'unicité du nom dans un module en excluant un ID (pour la mise à jour)
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Scenario s " +
            "WHERE s.name = :name AND s.module.id = :moduleId AND s.id != :id")
    boolean existsByNameAndModuleIdAndIdNot(@Param("name") String name,
                                            @Param("moduleId") UUID moduleId,
                                            @Param("id") UUID id);
}