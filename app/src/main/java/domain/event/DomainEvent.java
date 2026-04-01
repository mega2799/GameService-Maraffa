package domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Base interface for all domain events. */
public interface DomainEvent {
	String eventType();

	UUID gameId();

	Instant timestamp();

	Map<String, Object> toMap();
}
