package domain.valueobject;

/**
 * Domain result for the Rules Engine computeScore operation.
 * Framework-agnostic replacement for JsonObject in IRulesEnginePort.
 */
public record ScoreComputeResult(int score, boolean firstTeam, int winningPosition, String error) {
	public static ScoreComputeResult success(final int score, final boolean firstTeam, final int winningPosition) {
		return new ScoreComputeResult(score, firstTeam, winningPosition, null);
	}

	public static ScoreComputeResult failure(final String error) {
		return new ScoreComputeResult(0, false, -1, error);
	}

	public boolean isSuccess() {
		return this.error == null;
	}
}
