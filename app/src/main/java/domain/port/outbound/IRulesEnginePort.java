package domain.port.outbound;

import domain.valueobject.RoundStartResult;
import domain.valueobject.ScoreComputeResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Outbound port for the rules engine service. */
public interface IRulesEnginePort {
	CompletableFuture<RoundStartResult> startRound(UUID gameId, int numberOfPlayers);

	CompletableFuture<ScoreComputeResult> computeScore(int[] cards, int[] teamACards, Map<String, String> users,
			String trump, String mode, List<Boolean> isSuitFinished, UUID gameId, int turn);

	CompletableFuture<Boolean> checkMaraffa(int[] userCards, int suit, int value, int trump);

	CompletableFuture<Boolean> validateCard(int[] trick, int card, int[] userCards, boolean isCardTrump);
}
