package domain.valueobject;

import java.util.List;

/**
 * Domain result for the Rules Engine startRound operation.
 * Framework-agnostic replacement for JsonObject in IRulesEnginePort.
 */
public record RoundStartResult(List<Integer> deck, int firstPlayer, String error) {
	public static RoundStartResult success(final List<Integer> deck, final int firstPlayer) {
		return new RoundStartResult(deck, firstPlayer, null);
	}

	public static RoundStartResult failure(final String error) {
		return new RoundStartResult(null, -1, error);
	}

	public boolean isSuccess() {
		return this.error == null;
	}
}
