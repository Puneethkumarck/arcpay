package com.arcpay.compliance.infrastructure.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sanctions_list_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
class SanctionsListVersionEntity {

    @Id
    @Column(name = "version_id", nullable = false, updatable = false)
    @ToString.Include
    private UUID versionId;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;

    @Column(name = "downloaded_at", nullable = false)
    private Instant downloadedAt;

    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "status", nullable = false, length = 16)
    private String status;
}
