package org.example.skillforgeapi.repository;

import org.example.skillforgeapi.model.entity.Asset;
import org.example.skillforgeapi.model.enums.AssetStatus;
import org.example.skillforgeapi.util.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    @Query("SELECT a FROM Asset a WHERE a.sftpPath LIKE %:token% AND a.status = :status")
    Optional<Asset> findBySftpPathContainingAndStatus(@Param("token") String token,
                                                      @Param("status") AssetStatus status);
    Optional<Asset> findByChecksumSha256(String checksumSha256);
    List<Asset> findByType(AssetType type);
    List<Asset> findByNameContainingIgnoreCase(String name);
}
