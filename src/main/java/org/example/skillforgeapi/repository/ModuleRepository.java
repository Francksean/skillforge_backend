package org.example.skillforgeapi.repository;

import org.example.skillforgeapi.model.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {
    List<Module> findAllByOrderByOrderIndexAsc();
    boolean existsByTitle(String title);

    // Optionnel : pour les doublons lors d'une mise à jour (exclure l'ID courant)
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Module m WHERE m.title = :title AND m.id != :id")
    boolean existsByTitleAndIdNot(String title, UUID id);
}