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
@Table(name = "current_list_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
class CurrentListVersionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @ToString.Include
    private Short id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
