package infrastructure.vertx;

import domain.aggregate.GameAggregate;
import domain.aggregate.Team;
import domain.aggregate.Trick;
import domain.aggregate.TrickImpl;
import domain.port.outbound.IEventPublisher;
import domain.port.outbound.IGame;
import domain.port.outbound.IGameAgent;
import domain.port.outbound.IRulesEnginePort;
import domain.port.outbound.IUserServicePort;
import domain.valueobject.Call;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.GameMode;
import domain.valueobject.RoundStartResult;
import domain.valueobject.ScoreComputeResult;
import domain.valueobject.Status;
import domain.valueobject.User;
import infrastructure.config.Constants;
import domain.port.outbound.IStatisticPort;
import domain.valueobject.GameRecord;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin Vert.x verticle wrapper around GameAggregate. Handles only event-bus
 * notifications and Vert.x lifecycle. All game state and business logic live in
 * GameAggregate.
 */
public class GameVerticle extends AbstractVerticle implements IGameAgent, IGame {

	private final GameAggregate aggregate;
	private IEventPublisher eventPublisher;
	private final IRulesEnginePort rulesEngine;
	private IUserServicePort userService;

	private static final Logger LOGGER = LoggerFactory.getLogger(GameVerticle.class);

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/** Primary constructor. */
	public GameVerticle(final UUID id, final User user, final int numberOfPlayers, final int expectedScore,
			final GameMode gameMode, final IStatisticPort statisticPort,
			final IEventPublisher eventPublisher, final IRulesEnginePort rulesEngine,
			final IUserServicePort userService) {
		this.eventPublisher = eventPublisher;
		this.rulesEngine = rulesEngine;
		this.userService = userService;
		this.aggregate = new GameAggregate(id, user, numberOfPlayers, expectedScore, gameMode, statisticPort, this);
	}

	/** Legacy constructor with password (6 args). */
	public GameVerticle(final UUID id, final User user, final int numberOfPlayers, final int expectedScore,
			final GameMode gameMode, final String password) {
		this.rulesEngine = null;
		this.aggregate = new GameAggregate(id, user, numberOfPlayers, expectedScore, gameMode, password, this);
	}

	// -------------------------------------------------------------------------
	// Vert.x lifecycle
	// -------------------------------------------------------------------------

	@Override
	public void start(final Promise<Void> startPromise) {
		startPromise.complete();
	}

	// -------------------------------------------------------------------------
	// Delegation — domain methods
	// -------------------------------------------------------------------------

	public boolean addUser(final User user) {
		return this.aggregate.addUser(user);
	}

	public void setPassword(final String password) {
		this.aggregate.setPassword(password);
	}

	public boolean checkPasword(final String pwd) {
		return this.aggregate.checkPasword(pwd);
	}

	public boolean addCard(final Card<CardValue, CardSuit> card, final String username, final Boolean isSuitAdded) {
		return this.aggregate.addCard(card, username, isSuitAdded);
	}

	public boolean balancedTeams() {
		return this.aggregate.balancedTeams();
	}

	public boolean canStart() {
		return this.aggregate.canStart();
	}

	public void chooseTrump(final CardSuit suit) {
		this.aggregate.chooseTrump(suit);
	}

	public boolean startGame() {
		return this.aggregate.startGame();
	}

	public void startNewRound() {
		this.aggregate.startNewRound();
	}

	public boolean makeCall(final Call call, final String username) {
		return this.aggregate.makeCall(call, username);
	}

	public void setScoreAfterMistake(final int score, final boolean isFirstTeam) {
		this.aggregate.setScoreAfterMistake(score, isFirstTeam);
	}

	public void setScore(final int score, final boolean isTeamA, final boolean increment) {
		this.aggregate.setScore(score, isTeamA, increment);
	}

	public boolean isCompleted() {
		return this.aggregate.isCompleted();
	}

	public boolean isRoundEnded() {
		return this.aggregate.isRoundEnded();
	}

	public boolean isGameEnded() {
		return this.aggregate.isGameEnded();
	}

	public void removeUser(final String username) {
		this.aggregate.removeUser(username);
	}

	public void handOutCards(final List<Integer> list) {
		this.aggregate.handOutCards(list);
	}

	public boolean changeTeam(final String username, final String team, final Integer pos) {
		return this.aggregate.changeTeam(username, team, pos);
	}

	public boolean isUserIn(final String user) {
		return this.aggregate.isUserIn(user);
	}

	public int getNumberOfPlayersIn() {
		return this.aggregate.getNumberOfPlayersIn();
	}

	public int getExpectedScore() {
		return this.aggregate.getExpectedScore();
	}

	public boolean isNewGameCreated() {
		return this.aggregate.isNewGameCreated();
	}

	public void setNewGameCreated() {
		this.aggregate.setNewGameCreated();
	}

	public int getMaxNumberOfPlayers() {
		return this.aggregate.getMaxNumberOfPlayers();
	}

	public Optional<String> getPassword() {
		return this.aggregate.getPassword();
	}

	public void endRoundByMistake(final boolean firstTeam) {
		this.aggregate.endRoundByMistake(firstTeam);
	}

	public List<Card<CardValue, CardSuit>> getUserCards(final String username) {
		return this.aggregate.getUserCards(username);
	}

	public void setTurnWithUser(final String username) {
		this.aggregate.setTurnWithUser(username);
	}

	public boolean setIsSuitFinished(final Boolean value, final String username, final Boolean isValid) {
		return this.aggregate.setIsSuitFinished(value, username, isValid);
	}

	public void clearIsSuitFinished() {
		this.aggregate.clearIsSuitFinished();
	}

	@Override
	public Map<String, Object> toMap() {
		return this.aggregate.toMap();
	}

	// Getters / setters
	public UUID getId() {
		return this.aggregate.getId();
	}

	public Status getStatus() {
		return this.aggregate.getStatus();
	}

	public List<User> getUsers() {
		return this.aggregate.getUsers();
	}

        @Override
	public List<Team> getTeams() {
		return this.aggregate.getTeams();
	}

	public CardSuit getTrump() {
		return this.aggregate.getTrump();
	}

	public GameMode getGameMode() {
		return this.aggregate.getGameMode();
	}

	public int getPositionByUsername(final String username) {
		return this.aggregate.getPositionByUsername(username);
	}

	public List<Boolean> getIsSuitFinished() {
		return this.aggregate.getIsSuitFinished();
	}

	public Map<Integer, Trick> getStates() {
		return this.aggregate.getStates();
	}

	public AtomicInteger getCurrentState() {
		return this.aggregate.getCurrentState();
	}

	public void setCurrentState(final int value) {
		this.aggregate.setCurrentState(value);
	}

	public void incrementCurrentState(final String... from) {
		this.aggregate.incrementCurrentState(from);
	}

	public int getInitialTurn() {
		return this.aggregate.getInitialTurn();
	}

	public void setInitialTurn(final int initTurn) {
		this.aggregate.setInitialTurn(initTurn);
	}

	public int getTurn() {
		return this.aggregate.getTurn();
	}

	public void setTurn(final int turn) {
		this.aggregate.setTurn(turn);
	}

	public Trick getCurrentTrick() {
		return this.aggregate.getCurrentTrick();
	}

	public void setCurrentTrick(final Trick trick) {
		this.aggregate.setCurrentTrick(trick);
	}

	public Trick getLatestTrick() {
		return this.aggregate.getLatestTrick();
	}

	public List<Trick> getTricks() {
		return this.aggregate.getTricks();
	}

	public GameRecord getGameRecord() {
		return this.aggregate.getGameRecord();
	}

	@Override
	public void addTrickToSchema(final Trick trick) {
		this.aggregate.getGameRecord().addTrick(trick);
	}

	// -------------------------------------------------------------------------
	// IGameAgent implementation — event-bus notifications via NotificationService
	// -------------------------------------------------------------------------

	@Override
	public void onCreateGame(final User user) {
		// intentionally empty
	}

	@Override
	public void onJoinGame(final User user) {
		if (this.eventPublisher != null) {
			final Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("gameID", this.getId().toString());
			payload.put("username", user.username());
			payload.put("status", this.getStatus().toString());
			payload.put("teamA", this.getTeams().get(0).toMap());
			payload.put("teamB", this.getTeams().get(1).toMap());
			this.eventPublisher.publish(java.util.List.of(), "userJoin", payload);
		}
	}

	@Override
	public void onStartGame() {
		if (this.rulesEngine == null) {
			LOGGER.warn("rulesEngine not set, skipping startRound");
			return;
		}
		this.rulesEngine.startRound(this.getId(), this.getMaxNumberOfPlayers()).whenComplete((result, error) -> {
			if (error != null || result.error() != null) {
				LOGGER.error("Failed to start round");
				return;
			}
			if (this.getInitialTurn() == -1) {
				this.setInitialTurn(result.firstPlayer());
			}
			this.handOutCards(result.deck());
			this.onNewRound();
			LOGGER.info("Round started, first player: " + result.firstPlayer());
			if (this.eventPublisher != null) {
				final List<String> targets = this.getUsers().stream().map(u -> u.username())
						.collect(Collectors.toList());
				this.eventPublisher.publish(targets, "startGame",
						Map.of("firstPlayer", this.getUsers().get(this.getTurn()).username(),
								"turn", this.getTurn(),
								"gameID", this.getId().toString()));
			}
		});
	}

	@Override
	public void onCheckMaraffa(final int suit, final int value, final int trump, final String username) {
		if (this.rulesEngine == null) {
			return;
		}
		final int userTurn = this.getTurn();
		final List<Card<CardValue, CardSuit>> userCardsTemp = this.getUserCards(username);
		userCardsTemp.add(new Card<>(CardValue.ONE, CardSuit.fromValue(suit)));
		final int[] userCards = userCardsTemp.stream().mapToInt(card -> card.getCardValue().intValue()).toArray();
		this.rulesEngine.checkMaraffa(userCards, suit, value, trump).whenComplete((isMaraffa, error) -> {
			if (error != null) {
				LOGGER.error("checkMaraffa failed");
				return;
			}
			if (isMaraffa) {
				this.setScore(Constants.MARAFFA_SCORE, userTurn % 2 == 0, false);
				LOGGER.info("Maraffa confirmed");
			}
		});
	}

	@Override
	public void onChangeTeam() {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "changeTeam",
					Map.of("gameID", this.getId().toString(),
							"teamA", this.getTeams().get(0).toMap(),
							"teamB", this.getTeams().get(1).toMap()));
		}
	}

	@Override
	public void onPlayCard() {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());

			// Build current trick (cards on table)
			final Trick current = this.getCurrentTrick();
			final Map<String, Object> trickMap = new LinkedHashMap<>();
			if (current != null) {
				final List<Integer> cardsArr = current.getCards().stream()
						.map(Integer::parseInt).collect(Collectors.toList());
				// Invert map: Java stores {username → cardStr}, frontend needs {cardStr → username}
				final Map<String, String> caU = new LinkedHashMap<>();
				current.getCardsAndUsers().forEach((username, cardStr) -> caU.put(cardStr, username));
				trickMap.put("cards", cardsArr);
				trickMap.put("cardsAndUsers", caU);
			} else {
				trickMap.put("cards", List.of());
				trickMap.put("cardsAndUsers", Map.of());
			}

			// Build latest (previous completed) trick
			final Map<String, Object> latestTrickMap = new LinkedHashMap<>();
			final List<Trick> allTricks = this.getTricks();
			if (allTricks.size() >= 2) {
				final Trick prev = allTricks.get(allTricks.size() - 2);
				final Map<String, String> prevCaU = new LinkedHashMap<>();
				prev.getCardsAndUsers().forEach((username, cardStr) -> prevCaU.put(cardStr, username));
				latestTrickMap.put("cardsAndUsers", prevCaU);
			} else {
				latestTrickMap.put("cardsAndUsers", Map.of());
			}

			final Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("gameID", this.getId().toString());
			payload.put("turn", this.getTurn());
			payload.put("userTurn", this.getUsers().get(this.getTurn()).username());
			payload.put("teamAScore", (this.getTeams().get(0).score() + this.getTeams().get(0).currentScore()) / 3);
			payload.put("teamBScore", (this.getTeams().get(1).score() + this.getTeams().get(1).currentScore()) / 3);
			payload.put("trick", trickMap);
			payload.put("latestTrick", latestTrickMap);
			this.eventPublisher.publish(targets, "userTurn", payload);
		}
	}

	@Override
	public CompletableFuture<Void> onTrickCompleted(final Trick latestTrick) {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		if (this.rulesEngine == null) {
			this.setCurrentTrick(new TrickImpl(this.getMaxNumberOfPlayers(), this.getTrump()));
			this.getTricks().add(this.getCurrentTrick());
			this.onPlayCard();
			future.complete(null);
			return future;
		}
		final int[] cardArray = latestTrick.getCards().stream().mapToInt(Integer::parseInt).toArray();
		final Map<String, String> userList = this.getCurrentTrick().getCardsAndUsers();
		final int[] teamACards = latestTrick
				.getCardsAndUsers().entrySet().stream().filter(e -> this.getTeams().get(0).players().stream()
						.map(User::username).toList().contains(e.getKey()))
				.map(Map.Entry::getValue).mapToInt(Integer::parseInt).toArray();

		this.rulesEngine.computeScore(cardArray, teamACards, userList, String.valueOf(this.getTrump().getValue()),
				this.getGameMode().toString(), this.getIsSuitFinished(), this.getId(),
				(this.getTurn() + 1) % this.getMaxNumberOfPlayers()).whenComplete((result, error) -> {
					if (error != null || result.error() != null) {
						LOGGER.error("computeScore failed");
						future.complete(null);
						return;
					}
					final int winningPosition = result.winningPosition();
					final boolean firstTeam = result.firstTeam();
					final int score = result.score();

					if (winningPosition == -1) {
						this.setScoreAfterMistake(score, firstTeam);
						this.endRoundByMistake(firstTeam);
						this.clearIsSuitFinished();
						this.onEndRound();
						this.startNewRound();
						this.onStartGame();
						future.complete(null);
					} else {
						final String winnerUsername = userList.entrySet().stream()
								.filter(e -> String.valueOf(cardArray[winningPosition]).equals(e.getValue()))
								.map(Map.Entry::getKey).findFirst().orElse(null);
						this.setTurnWithUser(winnerUsername);
						this.setScore(score, firstTeam, true);
						this.setCurrentTrick(new TrickImpl(this.getMaxNumberOfPlayers(), this.getTrump()));
						this.getTricks().add(this.getCurrentTrick());
						future.complete(null);
						this.onPlayCard();
						this.clearIsSuitFinished();
						if (this.isGameEnded()) {
							LOGGER.info("Game completed");
							this.onEndGame();
						}
					}
				});
		return future;
	}

	@Override
	public void onMessage() {
		throw new UnsupportedOperationException("Unimplemented method 'onMessage'");
	}

	@Override
	public void onEndRound() {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			final Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("gameID", this.getId().toString());
			payload.put("teamA",
					this.getTeams().get(0).players().stream().map(User::username).collect(Collectors.toList()));
			payload.put("teamB",
					this.getTeams().get(1).players().stream().map(User::username).collect(Collectors.toList()));
			payload.put("trumpSelectorUsername",
					this.getUsers().get(this.getInitialTurn() >= 0 ? this.getInitialTurn() : 0).username());
			payload.put("teamAScore", (this.getTeams().get(0).score() + this.getTeams().get(0).currentScore()) / 3);
			payload.put("teamBScore", (this.getTeams().get(1).score() + this.getTeams().get(1).currentScore()) / 3);
			this.eventPublisher.publish(targets, "endRound", payload);
		}
	}

	@Override
	public void onEndGame() {
		this.onExitGame();
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			final Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("gameID", this.getId().toString());
			payload.put("teamA",
					this.getTeams().get(0).players().stream().map(User::username).collect(Collectors.toList()));
			payload.put("teamB",
					this.getTeams().get(1).players().stream().map(User::username).collect(Collectors.toList()));
			payload.put("teamAScore", (this.getTeams().get(0).score() + this.getTeams().get(0).currentScore()) / 3);
			payload.put("teamBScore", (this.getTeams().get(1).score() + this.getTeams().get(1).currentScore()) / 3);
			this.eventPublisher.publish(targets, "endGame", payload);
		}
		if (this.userService != null) {
			final int teamATotal = this.getTeams().get(0).score() + this.getTeams().get(0).currentScore();
			final int teamBTotal = this.getTeams().get(1).score() + this.getTeams().get(1).currentScore();
			final List<String> teamAUsers = this.getTeams().get(0).players().stream().map(User::username)
					.collect(Collectors.toList());
			final List<String> teamBUsers = this.getTeams().get(1).players().stream().map(User::username)
					.collect(Collectors.toList());
			final List<String> winners = teamATotal >= teamBTotal ? teamAUsers : teamBUsers;
			final List<String> losers = teamATotal >= teamBTotal ? teamBUsers : teamAUsers;
			this.userService.updateStatistics(winners, losers);
		}
	}

	@Override
	public void onMakeCall(final Call call) {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "call",
					Map.of("gameID", this.getId().toString(),
							"username", this.getUsers().get(this.getTurn()).username(),
							"call", call.toString()));
		}
	}

	@Override
	public void onNewRound() {
		this.getGameRecord().setGameID(String.valueOf(this.getId()) + '-' + this.getCurrentState().get() / 10);
		this.getGameRecord().setTrump(CardSuit.NONE);
		if (this.aggregate.getStatisticPort() != null) {
			this.aggregate.getStatisticPort().createRecord(this.getGameRecord());
		}
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "trumpEvent",
					Map.of("username", this.getUsers().get(this.getInitialTurn()).username(),
							"trumpSelected", this.getTrump().toString()));
		}
	}

	@Override
	public void onNewGame(final String newGameID) {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "newGame",
					Map.of("gameID", this.getId().toString(), "newGameID", newGameID));
		}
	}

	@Override
	public void onRemoveUser() {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "userRemoved",
					Map.of("gameID", this.getId().toString()));
		}
	}

	// -------------------------------------------------------------------------
	// Helper methods
	// -------------------------------------------------------------------------

	public void messageReceived(final String msg, final String type, final UUID gameID, final String author) {
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "message",
					Map.of("message", msg, "author", author, "environment", type));
		}
	}

	public void onExitGame() {
		LOGGER.info("game " + this.getId() + " is exiting");
		if (this.eventPublisher != null) {
			final List<String> targets = this.getUsers().stream().map(u -> u.username()).collect(Collectors.toList());
			this.eventPublisher.publish(targets, "exitGame", Map.of("gameID", this.getId().toString()));
			this.eventPublisher.publish(java.util.List.of(), "gameRemoved", Map.of());
		}
		this.vertx.setTimer(5000, event -> {
			try {
				this.stop();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
	}
}
