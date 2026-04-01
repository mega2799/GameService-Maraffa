package domain.port.outbound;

import java.util.List;
import java.util.Map;

/** Outbound port for publishing domain events. */
public interface IEventPublisher {
	void publish(List<String> targetUsers, String eventType, Map<String, Object> payload);
}
