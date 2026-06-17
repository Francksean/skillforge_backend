package org.example.skillforgeapi.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.skillforgeapi.model.enums.AssetStatus;
import org.example.skillforgeapi.util.AssetType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "asset")
@Data
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AssetType type;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "sftp_path")
    private String sftpPath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.PENDING;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "assets")
    private Set<Scenario> scenarios = new HashSet<>();
}