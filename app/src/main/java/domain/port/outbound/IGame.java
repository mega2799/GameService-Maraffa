package domain.port.outbound;

import domain.aggregate.Team;
import domain.aggregate.Trick;
import domain.valueobject.Call;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.GameMode;
import domain.valueobject.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/** Domain interface representing a game instance. */
public interface IGame {

	// Identity
	UUID getId();

	// User management
	boolean addUser(User user);

	void removeUser(String username);

	boolean isUserIn(String user);

	int getNumberOfPlayersIn();

	int getMaxNumberOfPlayers();

	List<User> getUsers();

	int getPositionByUsername(String username);

	// Password
	void setPassword(String password);

	boolean checkPasword(String pwd);

	// Game state
	boolean canStart();

	boolean startGame();

	void startNewRound();

	boolean isGameEnded();

	boolean isNewGameCreated();

	void setNewGameCreated();

	// Teams
	boolean changeTeam(String username, String team, Integer pos);

	List<Team> getTeams();

	// Trump
	CardSuit getTrump();

	void chooseTrump(CardSuit suit);

	// Cards and tricks
	boolean addCard(Card<CardValue, CardSuit> card, String username, Boolean isSuitAdded);

	List<Card<CardValue, CardSuit>> getUserCards(String username);

	boolean setIsSuitFinished(Boolean value, String username, Boolean isValid);

	Trick getCurrentTrick();

	void setCurrentTrick(Trick trick);

	Trick getLatestTrick();

	List<Trick> getTricks();

	void addTrickToSchema(Trick trick);

	AtomicInteger getCurrentState();

	// Turn
	int getTurn();

	void setInitialTurn(int initTurn);

	int getInitialTurn();

	// Game config
	int getExpectedScore();

	GameMode getGameMode();

	// Calls
	boolean makeCall(Call call, String username);

	// Lifecycle events
	void onCreateGame(User user);

	void onStartGame();

	CompletableFuture<Void> onTrickCompleted(Trick trick);

	void onPlayCard();

	void onNewGame(String newGameID);

	void onExitGame();

	// Serialization — framework-agnostic
	Map<String, Object> toMap();
}
