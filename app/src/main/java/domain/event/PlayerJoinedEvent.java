package domain.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Domain event for when a player joins a game. */
public record PlayerJoinedEvent(UUID gameId, String username, Instant timestamp) implements DomainEvent {

	public PlayerJoinedEvent(final UUID gameId, final String username) {
		this(gameId, username, Instant.now());
	}

	@Override
	public String eventType() {
		return "PlayerJoined";
	}

	@Override
	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("eventType", this.eventType());
		map.put("gameId", this.gameId.toString());
		map.put("username", this.username);
		map.put("timestamp", this.timestamp.toString());
		return map;
	}
}
