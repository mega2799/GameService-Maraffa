package domain.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Domain event for when a game ends. */
public record GameEndedEvent(UUID gameId, int teamAScore, int teamBScore, Instant timestamp) implements DomainEvent {

	public GameEndedEvent(final UUID gameId, final int teamAScore, final int teamBScore) {
		this(gameId, teamAScore, teamBScore, Instant.now());
	}

	@Override
	public String eventType() {
		return "GameEnded";
	}

	@Override
	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("eventType", this.eventType());
		map.put("gameId", this.gameId.toString());
		map.put("teamAScore", this.teamAScore);
		map.put("teamBScore", this.teamBScore);
		map.put("timestamp", this.timestamp.toString());
		return map;
	}
}
