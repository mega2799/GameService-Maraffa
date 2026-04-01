package domain.aggregate;

import static java.lang.Math.floor;

import domain.event.DomainEvent;
import domain.event.GameCreatedEvent;
import domain.event.GameEndedEvent;
import domain.event.PlayerJoinedEvent;
import domain.port.outbound.IGameAgent;
import domain.valueobject.Call;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.DomainConstants;
import domain.valueobject.GameMode;
import domain.valueobject.Pair;
import domain.valueobject.Status;
import domain.valueobject.User;
import domain.port.outbound.IStatisticPort;
import domain.valueobject.GameRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Domain aggregate containing all game state and business logic. This class is
 * framework-agnostic: no Vert.x, no WebSocket, no IEventPublisher. All domain
 * events are forwarded to the IGameAgent listener.
 */
public class GameAggregate {

	private final UUID id;
	private AtomicInteger currentState;
	private final int numberOfPlayers;
	private final Pair<Integer, Integer> currentScore;
	private final int expectedScore;
	private CardSuit trump = CardSuit.NONE;
	private Map<Integer, Trick> states = new ConcurrentHashMap<>();
	private List<User> users = new ArrayList<>();
	private final Map<User, List<Card<CardValue, CardSuit>>> userAndCards = new ConcurrentHashMap<>();
	private final GameRecord gameRecord;
	private IStatisticPort statisticPort;
	private Trick currentTrick;
	private final List<Trick> tricks = new ArrayList<>();
	private List<Team> teams = new ArrayList<>();
	private final String creatorName;
	private Boolean checkMaraffa = true;
	private Status status = Status.WAITING_PLAYERS;
	private final GameMode gameMode;
	private int turn = -1;
	private int initialTurn = -1;
	private List<Boolean> isSuitFinished = new ArrayList<>();
	private int elevenZeroTeam = -1;
	private int teamPos = 1;
	private final double numberOfTricksInRound;
	private boolean newGameCreated = false;
	private Optional<String> password = Optional.empty();
	private Optional<Team> teamAtTrick = Optional.empty();
	private final IGameAgent listener;
	private final List<DomainEvent> pendingEvents = new ArrayList<>();

	private static final Logger LOGGER = LoggerFactory.getLogger(GameAggregate.class);

	/** Primary constructor — corresponds to the 8-arg GameVerticle constructor. */
	public GameAggregate(final UUID id, final User user, final int numberOfPlayers, final int expectedScore,
			final GameMode gameMode, final IStatisticPort statisticPort, final IGameAgent listener) {
		this.id = id;
		this.gameMode = gameMode;
		this.expectedScore = expectedScore;
		this.currentScore = new Pair<>(0, 0);
		this.currentState = new AtomicInteger(0);
		this.numberOfPlayers = numberOfPlayers;
		this.numberOfTricksInRound = floor((float) DomainConstants.NUMBER_OF_CARDS / this.numberOfPlayers);
		this.creatorName = user.username();
		this.teams.add(new Team(List.of(user), "A", 0, 0));
		this.teams.add(new Team(List.of(), "B", 0, 0));
		this.users.add(user);
		this.gameRecord = new GameRecord(String.valueOf(id) + '-' + this.currentState.get() / 10, CardSuit.NONE);
		this.statisticPort = statisticPort;
		this.listener = listener;
		if (this.statisticPort != null) {
			this.statisticPort.createRecord(this.gameRecord);
		}
		this.pendingEvents.add(new GameCreatedEvent(id, user.username(), numberOfPlayers));
	}

	/**
	 * Secondary constructor — for compatibility, without statisticManager, with
	 * password.
	 */
	public GameAggregate(final UUID id, final User user, final int numberOfPlayers, final int expectedScore,
			final GameMode gameMode, final String password, final IGameAgent listener) {
		this.id = id;
		this.gameMode = gameMode;
		this.expectedScore = expectedScore;
		this.currentScore = new Pair<>(0, 0);
		this.currentState = new AtomicInteger(0);
		this.creatorName = user.username();
		this.numberOfPlayers = numberOfPlayers;
		this.numberOfTricksInRound = floor((float) DomainConstants.NUMBER_OF_CARDS / this.numberOfPlayers);
		this.teams.add(new Team(List.of(user), "A", 0, 0));
		this.teams.add(new Team(List.of(), "B", 0, 0));
		this.users.add(user);
		this.gameRecord = new GameRecord(String.valueOf(id) + '-' + this.currentState.get() / 10, CardSuit.NONE);
		this.password = Optional.of(password);
		this.listener = listener;
	}

	// -------------------------------------------------------------------------
	// Domain Events
	// -------------------------------------------------------------------------

	public List<DomainEvent> getPendingEvents() {
		return Collections.unmodifiableList(this.pendingEvents);
	}

	public void clearPendingEvents() {
		this.pendingEvents.clear();
	}

	// -------------------------------------------------------------------------
	// Domain methods
	// -------------------------------------------------------------------------

	/**
	 * @return true if the user is added
	 */
	public boolean addUser(final User user) {
		if (!this.users.stream().map(User::username).toList().contains(user.username())) {
			this.users.add(user);
			this.status = this.canStart() ? Status.STARTING : Status.WAITING_PLAYERS;
			final Team currentTeam = this.teams.get(this.teamPos % 2);
			final List<User> updatePlayers = new ArrayList<>(currentTeam.players());
			updatePlayers.add(user);
			this.teams.set(this.teamPos % 2,
					new Team(updatePlayers, currentTeam.nameOfTeam(), currentTeam.score(), currentTeam.currentScore()));
			LOGGER.info("GAME " + this.id + " joined: " + user.toString());
			this.teamPos += 1;
			this.pendingEvents.add(new PlayerJoinedEvent(this.id, user.username()));
			this.listener.onJoinGame(user);
			return true;
		}
		return false;
	}

	/**
	 * @param password
	 *            of the game
	 */
	public void setPassword(final String password) {
		this.password = Optional.of(password);
	}

	/**
	 * @return true if the password is correct
	 */
	public boolean checkPasword(final String pwd) {
		return !this.password.isPresent() || (this.password.isPresent() && this.password.get().equals(pwd));
	}

	/**
	 * Adds the card if the trick is not completed, otherwise it adds the card to a
	 * new trick and updates the current state.
	 *
	 * @param card
	 *            to be added to the trick
	 */
	public boolean addCard(final Card<CardValue, CardSuit> card, final String username, final Boolean isSuitAdded) {
		if (this.turn >= 0) {
			LOGGER.info("GAME " + this.id + " addCard: " + card.toString() + " by " + username + " suitFinished array: "
					+ this.isSuitFinished.toString());
			if (this.canStart() && this.users.get(this.turn).username().equals(username) && isSuitAdded) {
				if (this.currentTrick == null) {
					this.currentTrick = this.states.getOrDefault(this.currentState.get(),
							new TrickImpl(this.numberOfPlayers, this.trump));
					this.tricks.add(this.currentTrick);
					this.teamAtTrick = this.teams.stream().filter(t -> t.players().stream().map(User::username).toList()
							.contains(this.users.get(this.turn).username())).findFirst();
					LOGGER.info("[Duplicate] Start of a new trick means first player is:"
							+ this.users.get(this.turn).username() + " and the team is: " + this.teamAtTrick.get());
				}

				this.listener.onCheckMaraffa(card.cardSuit().value, card.cardValue().value, this.trump.value, username);

				if (this.currentTrick.getCardsAndUsers().containsKey(username)) {
					return false;
				}

				this.currentTrick.addCard(card, username);
				LOGGER.info("GAME " + this.id + " currentTrick: " + this.currentTrick.toString());
				this.turn = (this.turn + 1) % this.numberOfPlayers;
				this.removeFromHand(card, username);
				LOGGER.info("Checking trick is completed: " + this.currentTrick.isCompleted());
				if (this.currentTrick.isCompleted()) {
					LOGGER.info("Yes it is");
					this.getStates().put(this.getCurrentState().get(), this.getCurrentTrick());
				} else {
					LOGGER.info("NO it isnnnnn");
					this.listener.onPlayCard();
				}
				return true;
			}
		}
		return false;
	}

	private void removeFromHand(final Card<CardValue, CardSuit> card, final String username) {
		this.userAndCards.entrySet().stream().filter(e -> e.getKey().username().equals(username)).findFirst()
				.ifPresent(e -> {
					final List<Card<CardValue, CardSuit>> updateCards = new ArrayList<>(e.getValue());
					updateCards.remove(card);
					this.userAndCards.put(e.getKey(), Collections.unmodifiableList(updateCards));
				});
	}

	/**
	 * @return true if the teams are balanced: have the same number of players
	 */
	public boolean balancedTeams() {
		return this.teams.stream().mapToInt(team -> team.players().size()).distinct().count() == 1;
	}

	/**
	 * @return true if all players have joined the game and if the teams are
	 *         balanced
	 */
	public boolean canStart() {
		return this.users.size() == this.numberOfPlayers;
	}

	/**
	 * @param suit
	 *            the leading suit of the round
	 */
	public void chooseTrump(final CardSuit suit) {
		this.trump = suit;
		this.gameRecord.setTrump(suit);
		LOGGER.info("GAME " + this.id + " chose trump: " + suit.toString());
		this.listener.onNewRound();
		if (this.statisticPort != null) {
			this.statisticPort.updateSuit(this.gameRecord);
		}
	}

	/**
	 * @return true if all the players are in
	 */
	public boolean startGame() {
		if (this.canStart() && this.balancedTeams()) {
			final int maxPlayers = this.teams.stream().mapToInt(team -> team.players().size()).max().orElse(0);
			this.users = IntStream.range(0, maxPlayers).mapToObj(i -> this.teams.stream()
					.filter(team -> team.players().size() > i).map(team -> team.players().get(i)))
					.flatMap(Stream::distinct).collect(Collectors.toList());

			if (this.turn != -1) {
				this.teamAtTrick = this.teams.stream().filter(t -> t.players().stream().map(User::username).toList()
						.contains(this.users.get(this.turn).username())).findFirst();
				LOGGER.info("Start of a new trick means first player is:" + this.users.get(this.turn).username()
						+ " and the team is: " + this.teamAtTrick.get());
			}

			this.status = Status.PLAYING;
			this.listener.onStartGame();
			return true;
		}
		return false;
	}

	/** Reset the trump. */
	public void startNewRound() {
		this.chooseTrump(CardSuit.NONE);
		this.elevenZeroTeam = -1;
	}

	/**
	 * @param call
	 *            the call
	 * @param username
	 *            the user who makes the call
	 * @return true if the call is made correctly
	 */
	public boolean makeCall(final Call call, final String username) {
		if (this.currentTrick == null) {
			this.currentTrick = this.states.getOrDefault(this.currentState.get(),
					new TrickImpl(this.numberOfPlayers, this.trump));
		}
		LOGGER.info("this.users.stream().map(User::username).toList().get(this.turn).equals(username):  "
				+ this.users.stream().map(User::username).toList().get(this.turn).equals(username));
		if (this.users.stream().map(User::username).toList().get(this.turn).equals(username)) {
			this.currentTrick.setCall(call, username);
			this.listener.onMakeCall(call);
		}
		return !Call.NONE.equals(this.currentTrick.getCall());
	}

	/** Set the team who loses the round because of a mistake. */
	public void setScoreAfterMistake(final int score, final boolean isFirstTeam) {
		LOGGER.info("Calling: setScoreAfterMistake");
		final String beginTeam = this.teamAtTrick.get().nameOfTeam();
		final int index = isFirstTeam ? 0 : 1;
		final int invIndex = isFirstTeam ? 1 : 0;
		final Team currentTeam = "A".equals(beginTeam) ? this.teams.get(index) : this.teams.get(invIndex);
		final Team invTeam = "A".equals(beginTeam) ? this.teams.get(invIndex) : this.teams.get(index);
		do {
			this.incrementCurrentState("DO while");
			this.tricks.add(new TrickImpl(4, CardSuit.NONE));
			LOGGER.info("number of tricks: " + this.tricks.size());
		} while (this.currentState.get() % 9 != 0);

		this.incrementCurrentState("DO while SCRAUSO");
		LOGGER.info("number of tricks: " + this.tricks.size());
		LOGGER.info("Latest: " + this.getLatestTrick().toString());
		LOGGER.info("GAME " + this.id + " turn : " + this.currentState.get());
		LOGGER.info("[Score 11toZero] Score situation: " + this.teams.toString());
		LOGGER.info("[Score 11toZero] Begin team: " + this.teamAtTrick.toString());
		LOGGER.info("[Score 11toZero] MISTAKE made by team: " + currentTeam.nameOfTeam() + " so team :"
				+ invTeam.nameOfTeam() + " wins the round");
		LOGGER.info("mistake being made by anyone between: "
				+ currentTeam.players().stream().map(User::username).toList().toString());
		LOGGER.info("[Score 11toZero] Setting: " + this.teams.get("A".equals(beginTeam) ? index : invIndex).toString()
				+ " as winner");
		this.teams.set("A".equals(beginTeam) ? index : invIndex,
				new Team(currentTeam.players(), currentTeam.nameOfTeam(), currentTeam.score(), 0));

		LOGGER.info("[Score 11toZero] Setting: " + this.teams.get("A".equals(beginTeam) ? invIndex : index).toString()
				+ " as loser");
		this.teams.set("A".equals(beginTeam) ? invIndex : index,
				new Team(invTeam.players(), invTeam.nameOfTeam(), invTeam.score() + score, 0));
		LOGGER.info("[Score 11toZero] Score situation: " + this.teams.toString());
		if (this.isGameEnded()) {
			LOGGER.info("Game ended with eleven to zero, somebody is quite a noob...");
			this.pendingEvents
					.add(new GameEndedEvent(this.id, this.teams.get(0).score() / 3, this.teams.get(1).score() / 3));
			this.listener.onEndGame();
		}
	}

	/** Update the score of the teams. */
	public void setScore(final int score, final boolean isTeamA, final boolean increment) {
		final int index = isTeamA ? 0 : 1;
		final int invIndex = isTeamA ? 1 : 0;
		Team currentTeam = this.teams.get(index);
		LOGGER.info("GAME " + this.id + " turn : " + this.currentState.get());
		LOGGER.info("Score situation: " + this.teams.toString());
		LOGGER.info(
				"GAME " + this.id + " score before compute: " + currentTeam.nameOfTeam() + " : " + currentTeam.score());
		this.teams.set(index, new Team(currentTeam.players(), currentTeam.nameOfTeam(), currentTeam.score(),
				currentTeam.currentScore() + score));
		Team invTeam = this.teams.get(invIndex);
		currentTeam = this.teams.get(index);
		LOGGER.info("Score situation: " + this.teams.toString());
		if (increment)
			this.incrementCurrentState("NON e' finito");
		if (this.currentState.get() % (int) this.numberOfTricksInRound == 0) {
			LOGGER.info("Round ended — applying last trick bonus");
			this.teams.set(index, new Team(currentTeam.players(), currentTeam.nameOfTeam(), currentTeam.score(),
					(currentTeam.currentScore() - currentTeam.currentScore() % 3) + 3));
			this.teams.set(invIndex, new Team(invTeam.players(), invTeam.nameOfTeam(), invTeam.score(),
					(invTeam.currentScore() - invTeam.currentScore() % 3)));
			LOGGER.info("Score situation: " + this.teams.toString());
			currentTeam = this.teams.get(index);
			invTeam = this.teams.get(invIndex);
			this.teams.set(index, new Team(currentTeam.players(), currentTeam.nameOfTeam(),
					currentTeam.score() + currentTeam.currentScore(), 0));
			this.teams.set(invIndex,
					new Team(invTeam.players(), invTeam.nameOfTeam(), invTeam.score() + invTeam.currentScore(), 0));

			LOGGER.info("Round completed — next trump selector: " + this.users.get(this.initialTurn));
			LOGGER.info("Score situation: " + this.teams.toString());
			this.initialTurn += 1;
			this.setInitialTurn(this.initialTurn);
			this.setTurn(this.initialTurn);
			this.checkMaraffa = true;
			this.listener.onEndRound();
			this.startNewRound();
			this.listener.onStartGame();

			LOGGER.info("Next trump selector: " + this.users.get(this.initialTurn));
			LOGGER.info("GAME " + this.id + " teams : " + this.teams.toString());
		}
	}

	public boolean isCompleted() {
		return this.currentTrick.isCompleted();
	}

	public boolean isRoundEnded() {
		LOGGER.info("GAME " + this.id + " currentState : " + this.currentState.get() + " is round ended: "
				+ (this.currentState.get() % (int) this.numberOfTricksInRound == 0) + " turn making trumps:  index("
				+ this.initialTurn + ") -> user: " + this.users.get(this.initialTurn));
		final var res = this.currentState.get() % (int) this.numberOfTricksInRound == 0;
		this.incrementCurrentState("NON e' finito");
		return res;
	}

	public boolean isGameEnded() {
		LOGGER.info("[ isGameEnded ] teamA" + this.teams.get(0).score() / 3 + "teamB" + this.teams.get(1).score() / 3);
		LOGGER.info("Asking isGameEnded: " + (this.teams.get(0).score() / 3 >= this.expectedScore
				|| this.teams.get(1).score() / 3 >= this.expectedScore));
		return this.teams.get(0).score() / 3 >= this.expectedScore
				|| this.teams.get(1).score() / 3 >= this.expectedScore;
	}

	public void removeUser(final String username) {
		final List<User> usersToRemove = this.users.stream().filter(u -> u.username().equals(username))
				.collect(Collectors.toList());
		this.users.removeAll(usersToRemove);
		final List<Team> updatedTeams = this.teams.stream()
				.map(team -> new Team(team.players().stream().filter(user -> !username.equals(user.username()))
						.collect(Collectors.toList()), team.nameOfTeam(), team.score(), team.currentScore()))
				.collect(Collectors.toList());
		this.teams = updatedTeams;
		this.listener.onRemoveUser();
	}

	/** Framework-agnostic serialization. Infrastructure adapters convert this Map to JSON. */
	public Map<String, Object> toMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("gameID", this.id.toString());
		map.put("creator", this.creatorName);
		map.put("status", this.status.toString());
		map.put("score", this.expectedScore);
		map.put("firstPlayer", this.users.get(this.turn >= 0 ? this.turn : 0).username());
		map.put("playerTurn", this.users.get(this.turn >= 0 ? this.turn : 0).username());
		map.put("turn", this.turn);
		map.put("state", this.currentState.get());
		map.put("password", this.password.isPresent());
		map.put("trumpSelected", this.trump.toString());
		map.put("trumpSelectorUsername", this.users.get(this.initialTurn >= 0 ? this.initialTurn : 0).username());
		map.put("teamA", this.teams.get(0).toMap());
		map.put("teamB", this.teams.get(1).toMap());
		map.put("trick", this.currentTrick != null ? this.currentTrick.getCardsAndUsers() : null);
		map.put("teamAScore", (this.teams.get(0).score() + this.teams.get(0).currentScore()) / 3);
		map.put("teamBScore", (this.teams.get(1).score() + this.teams.get(1).currentScore()) / 3);
		map.put("teamAScoreCurrent", this.teams.get(0).currentScore() / 3);
		map.put("teamBScoreCurrent", this.teams.get(1).currentScore() / 3);
		map.put("mode", this.gameMode.toString());
		return map;
	}

	public void handOutCards(final List<Integer> list) {
		final int chunkSize = (list.size() + this.numberOfPlayers - 1) / this.numberOfPlayers;
		int index = 0;
		for (final var integer : IntStream.range(0, this.numberOfPlayers)
				.mapToObj(i -> list.subList(i * chunkSize, Math.min(list.size(), (i + 1) * chunkSize)))
				.collect(Collectors.toList())) {
			this.userAndCards.put(this.users.get(index++), integer.stream().map(Card::fromInteger).toList());
		}
	}

	public boolean changeTeam(final String username, final String team, final Integer pos) {
		if (this.status == Status.WAITING_PLAYERS || this.status == Status.STARTING) {
			final User deletedUser = this.teams.stream()
					.flatMap(tm -> tm.players().stream().filter(u -> u.username().equals(username))).findFirst()
					.orElseThrow();
			this.teams = this.teams.stream().map(t -> {
				final List<User> updatedPlayers = new ArrayList<>(t.players());
				updatedPlayers.remove(deletedUser);
				return new Team(updatedPlayers, t.nameOfTeam(), t.score(), t.currentScore());
			}).collect(Collectors.toList());
			final Team selectedteam = this.teams.stream().filter(t -> t.nameOfTeam().equals(team)).findFirst()
					.orElseThrow();
			try {
				final int teamIndex = this.teams.indexOf(selectedteam);
				final List<User> updatedPlayers = new ArrayList<>(selectedteam.players());
				updatedPlayers.add(pos, deletedUser);
				this.teams.set(teamIndex, new Team(updatedPlayers, selectedteam.nameOfTeam(), selectedteam.score(),
						selectedteam.currentScore()));
				LOGGER.info("The team has been changed" + this.teams.toString());
				this.listener.onChangeTeam();
				return true;
			} catch (final IndexOutOfBoundsException e) {
				throw new IndexOutOfBoundsException("Cannot add a user, the team is too small");
			}
		}
		return false;
	}

	public boolean isUserIn(final String user) {
		return this.users.stream().map(User::username).toList().contains(user);
	}

	public int getNumberOfPlayersIn() {
		return this.users.size();
	}

	public int getExpectedScore() {
		return this.expectedScore;
	}

	public boolean isNewGameCreated() {
		return this.newGameCreated;
	}

	public void setNewGameCreated() {
		this.newGameCreated = true;
	}

	public int getMaxNumberOfPlayers() {
		return this.numberOfPlayers;
	}

	public Optional<String> getPassword() {
		return this.password;
	}

	public void endRoundByMistake(final boolean firstTeam) {
		this.elevenZeroTeam = firstTeam ? 0 : 1;
		LOGGER.info("Eleven-to-zero mistake by team — next trump selector: " + this.users.get(this.initialTurn));
		LOGGER.info("Round ended by mistake — next trump selector: " + this.users.get(this.initialTurn));
		this.initialTurn += 1;
		this.setInitialTurn(this.initialTurn);
		this.setTurn(this.initialTurn);
		LOGGER.info("Next trump selector: " + this.users.get(this.initialTurn));
		this.elevenZeroTeam = -1;
		this.checkMaraffa = true;
	}

	public List<Card<CardValue, CardSuit>> getUserCards(final String username) {
		return this.userAndCards.entrySet().stream().filter(e -> e.getKey().username().equals(username)).findFirst()
				.map(Map.Entry::getValue)
				.map(cards -> cards.stream().sorted((o1, o2) -> o1.getCardValue().compareTo(o2.getCardValue()))
						.collect(Collectors.toList()))
				.orElse(Collections.emptyList());
	}

	public void setTurnWithUser(final String username) {
		this.turn = this.users.stream().map(User::username).toList().indexOf(username);
	}

	public boolean setIsSuitFinished(final Boolean value, final String username, final Boolean isValid) {
		LOGGER.info("Before:  setIsSuitFinished, " + this.isSuitFinished.toString());
		LOGGER.info("Is valid to set isSuitFinished: " + isValid);
		LOGGER.info("Current user: " + this.users.get((this.turn)).username());
		LOGGER.info("played by: " + username);
		if ((GameMode.ELEVEN2ZERO.equals(this.gameMode) || isValid)
				&& this.users.get((this.turn)).username().equals(username)) {
			LOGGER.info("Calling:  setIsSuitFinished, " + this.isSuitFinished.toString());
			if (this.isSuitFinished.size() == this.numberOfPlayers) {
				LOGGER.info("isSuitFinished is full, clearing it");
				this.isSuitFinished = new ArrayList<>();
			}
			this.isSuitFinished.add(value);
			LOGGER.info("After:  setIsSuitFinished, " + this.isSuitFinished.toString());
			return true;
		}
		return false;
	}

	public void clearIsSuitFinished() {
		this.isSuitFinished = new ArrayList<>();
	}

	// -------------------------------------------------------------------------
	// Getters and setters
	// -------------------------------------------------------------------------

	public UUID getId() {
		return this.id;
	}

	public Status getStatus() {
		return this.status;
	}

	public List<User> getUsers() {
		return this.users;
	}

	public List<Team> getTeams() {
		return this.teams;
	}

	public CardSuit getTrump() {
		return this.trump;
	}

	public GameMode getGameMode() {
		return this.gameMode;
	}

	public int getPositionByUsername(final String username) {
		return this.users.stream().map((Function<? super User, ? extends String>) User::username).toList()
				.indexOf(username);
	}

	public List<Boolean> getIsSuitFinished() {
		return this.isSuitFinished;
	}

	public Map<Integer, Trick> getStates() {
		return this.states;
	}

	public AtomicInteger getCurrentState() {
		return this.currentState;
	}

	public void setCurrentState(final int value) {
		this.currentState = new AtomicInteger(value);
	}

	public void incrementCurrentState(final String... from) {
		LOGGER.info("YOOO incrementing  currentState: from " + Arrays.toString(from));
		LOGGER.info("current state : " + this.currentState.incrementAndGet());
	}

	public int getInitialTurn() {
		return this.initialTurn;
	}

	public void setInitialTurn(final int initTurn) {
		this.initialTurn = initTurn % this.numberOfPlayers;
		this.turn = this.initialTurn;
	}

	public int getTurn() {
		return this.turn;
	}

	public void setTurn(final int turn) {
		this.turn = turn;
	}

	public Trick getCurrentTrick() {
		return this.currentTrick;
	}

	public void setCurrentTrick(final Trick trick) {
		this.currentTrick = trick;
		this.teamAtTrick = this.teams.stream().filter(
				t -> t.players().stream().map(User::username).toList().contains(this.users.get(this.turn).username()))
				.findFirst();
		LOGGER.info("[Set current trick] Start of a new trick means first player is:"
				+ this.users.get(this.turn).username() + " and the team is: " + this.teamAtTrick.get());
	}

	public Trick getLatestTrick() {
		final Trick latestTrick = this.tricks.get(this.tricks.size() > 0 ? this.tricks.size() - 1 : 0);
		return latestTrick;
	}

	public List<Trick> getTricks() {
		return this.tricks;
	}

	public GameRecord getGameRecord() {
		return this.gameRecord;
	}

	public IStatisticPort getStatisticPort() {
		return this.statisticPort;
	}
}
