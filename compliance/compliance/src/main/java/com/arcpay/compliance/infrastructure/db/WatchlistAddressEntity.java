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
@Table(name = "watchlist_address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
class WatchlistAddressEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @ToString.Include
    private UUID id;

    @Column(name = "address", nullable = false, unique = true, length = 64)
    private String address;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "added_by", nullable = false, length = 255)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;
}
