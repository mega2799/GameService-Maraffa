package domain.port.inbound;

import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.GameCommandResult;
import domain.valueobject.GameListResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Inbound port defining all game operations. */
public interface IGameUseCase {
	GameCommandResult createGame(int numberOfPlayers, String username, UUID guiId, boolean isGuest, int expectedScore,
			String gameMode);

	GameCommandResult joinGame(UUID gameID, String username, UUID guiId, boolean isGuest, String password);

	GameCommandResult startGame(UUID gameID);

	CompletableFuture<GameCommandResult> playCard(UUID gameID, String username, Card<CardValue, CardSuit> card,
			boolean isSuitFinished);

	GameCommandResult chooseTrump(UUID gameID, String cardSuit, String username);

	GameCommandResult makeCall(UUID gameID, String call, String username);

	GameCommandResult changeTeam(UUID gameID, String username, String team, int position);

	GameCommandResult removeUser(UUID gameID, String username);

	GameCommandResult exitGame(UUID gameID);

	GameCommandResult newGame(UUID gameID);

	GameCommandResult canStart(UUID gameID);

	boolean startNewRound(UUID gameID);

	GameCommandResult setPassword(UUID gameID, String password);

	GameCommandResult getState(UUID gameID);

	GameCommandResult isGameEnded(UUID gameID);

	GameListResult getGames();

	GameCommandResult getPlayers();

	GameCommandResult getGame(UUID gameID);

	GameCommandResult getPlayerCards(UUID gameID, String username);

	int getGameCount();

	GameCommandResult chatMessage(UUID gameID, String author, String message, String environment);

	GameCommandResult notifyGame(UUID gameID, String message);
}
