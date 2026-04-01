package domain.port.outbound;

import domain.aggregate.Trick;
import domain.valueobject.Call;
import domain.valueobject.User;
import java.util.concurrent.CompletableFuture;

/** Callback interface for game events (outbound port). */
public interface IGameAgent {

	void onCreateGame(User user);

	void onNewRound();

	void onJoinGame(User user);

	void onStartGame();

	void onCheckMaraffa(int suit, int value, int trump, String username);

	void onPlayCard();

	CompletableFuture<Void> onTrickCompleted(Trick latestTrick);

	void onMessage();

	void onEndRound();

	void onEndGame();

	void onNewGame(String newGameID);

	void onChangeTeam();

	void onMakeCall(Call call);

	void onRemoveUser();
}
