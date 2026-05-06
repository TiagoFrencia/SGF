package com.sgf.modules.integrations.anmat.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnmatTraceabilityEventRepository extends JpaRepository<AnmatTraceabilityEvent, UUID> {
    Optional<AnmatTraceabilityEvent> findByEventTypeAndGtinAndSerialNumber(AnmatEventType eventType, String gtin, String serialNumber);
    List<AnmatTraceabilityEvent> findTop50ByOrderByOccurredAtDesc();
    List<AnmatTraceabilityEvent> findByGtinAndSerialNumberOrderByOccurredAtAsc(String gtin, String serialNumber);
    List<AnmatTraceabilityEvent> findTop100ByGtinOrderByOccurredAtDesc(String gtin);
    List<AnmatTraceabilityEvent> findTop100ByGtinAndLotNumberOrderByOccurredAtDesc(String gtin, String lotNumber);
}
