package com.arcpay.identity.agentidentity.domain.port;

import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GasUsageRepository {

    GasUsage save(GasUsage gasUsage);

    Page<GasUsage> findByOwnerId(UUID ownerId, Pageable pageable);
}
