package domain.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Domain event for when a game is created. */
public record GameCreatedEvent(UUID gameId, String creatorUsername, int numberOfPlayers,
		Instant timestamp) implements DomainEvent {

	public GameCreatedEvent(final UUID gameId, final String creatorUsername, final int numberOfPlayers) {
		this(gameId, creatorUsername, numberOfPlayers, Instant.now());
	}

	@Override
	public String eventType() {
		return "GameCreated";
	}

	@Override
	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("eventType", this.eventType());
		map.put("gameId", this.gameId.toString());
		map.put("creatorUsername", this.creatorUsername);
		map.put("numberOfPlayers", this.numberOfPlayers);
		map.put("timestamp", this.timestamp.toString());
		return map;
	}
}
