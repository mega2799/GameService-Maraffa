package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.service.GameService;
import domain.port.outbound.IGameFactory;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.GameCommandResult;
import domain.valueobject.GameListResult;
import domain.valueobject.GameMode;
import domain.valueobject.User;
import infrastructure.config.Constants;
import infrastructure.inmemory.InMemoryGameRepository;
import infrastructure.vertx.GameVerticleFactory;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

/** This class tests the game service using vertx. */
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class GameTest {
	private static final User TEST_USER = new User("testUser", UUID.randomUUID(), false);
	private static final int FIRST_PLAYER = 0;
	private static final CardSuit TRUMP = CardSuit.COINS;
	private static final String PASSWORD = "1234";
	private static final String FAKE_TRUMP = "hammers";
	private static final String CALL = "busso";
	private static final String FAKE_CALL = "suono";
	private static final String FAKE_GAME_MODE = "tresette";
	private static final CardSuit UNDEFINED_TRUMP = CardSuit.NONE;
	private static final GameMode GAME_MODE = GameMode.CLASSIC;
	private static final int MARAFFA_PLAYERS = 4;
	private static final int UUID_SIZE = 36;
	private static final int EXPECTED_SCORE = 11;
	private static final int EXPECTED_POS = 1;
	private static final UUID FAKE_UUID = UUID.randomUUID();
	private static final Boolean IS_SUIT_FINISHED = true;
	private static final Card<CardValue, CardSuit> TEST_CARD = new Card<>(CardValue.HORSE, CardSuit.CLUBS);
	private static final List<Card<CardValue, CardSuit>> TEST_CARDS = List.of(new Card<>(CardValue.KING, CardSuit.CUPS),
			new Card<>(CardValue.KNAVE, CardSuit.COINS), new Card<>(CardValue.SEVEN, CardSuit.SWORDS), TEST_CARD);
	private Vertx vertx;
	private GameService gameService;
	static final Dotenv dotenv = Dotenv.configure().filename("env.example").load();

	/**
	 * Before executing our test, let's deploy our verticle. This method
	 * instantiates a new Vertx and deploy the verticle. Then, it waits until the
	 * verticle has successfully completed its start sequence (thanks to
	 * `context.asyncAssertSuccess`).
	 */
	@BeforeAll
	public void setUp() {
		this.vertx = Vertx.vertx();
		final IGameFactory gameFactory = new GameVerticleFactory(this.vertx, null, null, null, null);
		this.gameService = new GameService(gameFactory, new InMemoryGameRepository());
	}

	/**
	 * This method, called after our test, just cleanup everything by closing the
	 * vert.x instance
	 */
	@AfterAll
	public void tearDown() {
		this.vertx.close();
	}

	/**
	 * Create a new game (GameVerticle) and ensure that its UUID has been created
	 * correctly
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void createGameTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length()); // Assuming UUID is 36
		// characters long
		context.completeNow();
	}

	/**
	 * If no one creates {@code FAKE_UUID} game, join response should have not found
	 *
	 * @param context
	 *            vertx test context attribute
	 */
	@Test
	public void joinNotFoundGameTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.joinGame(FAKE_UUID, TEST_USER, PASSWORD);
		assertTrue(gameResponse.containsKey(Constants.NOT_FOUND));
		context.completeNow();
	}

	/**
	 * The join should add at maximum {@code MARAFFA_PLAYERS}
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void joinReachedLimitGameTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		final GameCommandResult joinResponse = this.gameService.joinGame(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				new User(TEST_USER.username() + TEST_USER.username(), TEST_USER.clientID(), false), PASSWORD);
		assertTrue(joinResponse.containsKey(Constants.FULL));
		context.completeNow();
	}

	/**
	 * The same user can't be added twice
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void joinWithSameUserTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		GameCommandResult joinResponse = this.gameService.joinGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				new User(TEST_USER.username() + TEST_USER.username(), TEST_USER.clientID(), false), PASSWORD);
		assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		joinResponse = this.gameService.joinGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER,
				PASSWORD);
		assertTrue(joinResponse.containsKey(Constants.ALREADY_JOINED));
		context.completeNow();
	}

	/**
	 * If all the players haven't joined, the game can't start
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void theGameCantStartTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		GameCommandResult startGameResponse = this.gameService
				.startGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			assertFalse(startGameResponse.getBoolean(Constants.START_ATTR));
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
			startGameResponse = this.gameService.startGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		}
		// GameCommandResult changeResponse = this.gameService.changeTeam(
		// UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
		// TEST_USER.username(), "B", 0);
		// assertTrue(changeResponse.getBoolean(Constants.TEAM));
		// changeResponse =
		// this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
		// TEST_USER.username() + 1, "B", EXPECTED_POS);
		startGameResponse = this.gameService.startGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		assertTrue(startGameResponse.getBoolean(Constants.START_ATTR));
		context.completeNow();
	}

	/**
	 * The round can't start if all players haven't joined it
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void waitAllPlayersTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult canStartResponse = this.gameService
					.canStart(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
			assertFalse(canStartResponse.getBoolean(Constants.START_ATTR));
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult changeResponse = this.gameService
				.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		final GameCommandResult canStartResponse = this.gameService
				.canStart(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		assertTrue(canStartResponse.getBoolean(Constants.START_ATTR));
		context.completeNow();
	}

	/**
	 * The trump is not a legal suit
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void chooseWrongTrumpTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult canStartResponse = this.gameService
					.canStart(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
			assertFalse(canStartResponse.getBoolean(Constants.START_ATTR));
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), FAKE_TRUMP,
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertFalse(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		assertTrue(chooseTrumpResponse.getBoolean(Constants.ILLEGAL_TRUMP));
		context.completeNow();
	}

	/**
	 * Reset the trump in order to start a new round
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void startNewRoundTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult canStartResponse = this.gameService
					.canStart(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
			assertFalse(canStartResponse.getBoolean(Constants.START_ATTR));
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		assertTrue(this.gameService.startNewRound(UUID.fromString(gameResponse.getString(Constants.GAME_ID))));
		Assertions.assertEquals(UNDEFINED_TRUMP, this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getTrump());
		context.completeNow();
	}

	/**
	 * The card can be played only when the game is started
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void playCardTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			assertThrows(IndexOutOfBoundsException.class,
					() -> this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
							TEST_USER.username(), TEST_CARD, IS_SUIT_FINISHED, true));

			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult changeResponse = this.gameService
				.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		assertTrue(this.gameService
				.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
								.getUsers().get(initialTurn).username(),
						TEST_CARD, IS_SUIT_FINISHED, true)
				.getBoolean(Constants.PLAY));
		context.completeNow();
	}

	/**
	 * Get a state
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void getStateTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			assertThrows(IndexOutOfBoundsException.class,
					() -> this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
							TEST_USER.username(), TEST_CARD, IS_SUIT_FINISHED, true));
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult stateResponse = this.gameService
				.getState(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		assertTrue(stateResponse.containsKey(Constants.NOT_FOUND));
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		GameCommandResult changeResponse = this.gameService
				.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		assertTrue(this.gameService
				.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
								.getUsers().get(initialTurn).username(),
						TEST_CARD, IS_SUIT_FINISHED, true)
				.getBoolean(Constants.PLAY));

		for (int i = 1; i < MARAFFA_PLAYERS; i++) {
			assertTrue(this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
							.getUsers().get((initialTurn + i) % MARAFFA_PLAYERS).username(),
					TEST_CARDS.get(i - 1), IS_SUIT_FINISHED, true).getBoolean(Constants.PLAY));
		}
		stateResponse = this.gameService.getState(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		assertFalse(stateResponse.containsKey(Constants.NOT_FOUND));
		context.completeNow();
	}

	/**
	 * Only the first player can make a call
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void onlyFirstPlayerCanMakeACallTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		// final CompletableFuture<GameCommandResult> future = this.businessLogicController
		// .getShuffledDeck(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
		// MARAFFA_PLAYERS);
		// try {
		// final int firstPlayer = future.get().getInteger("firstPlayer");
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		// assertEquals(firstPlayer, initialTurn);

		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));

		GameCommandResult callResponse = this.gameService.makeCall(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				CALL, this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
						.getUsers().get((initialTurn + 1) % MARAFFA_PLAYERS).username());
		assertFalse(callResponse.getBoolean(Constants.MESSAGE));
		callResponse = this.gameService.makeCall(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), CALL,
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(callResponse.getBoolean(Constants.MESSAGE));

		// } catch (final InterruptedException e) {
		// this.e.printStackTrace();
		// } catch (final ExecutionException e) {
		// this.e.printStackTrace();
		// }
		context.completeNow();
	}

	/**
	 * The call is not a legal call
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void chooseWrongCallTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		// final CompletableFuture<GameCommandResult> future = this.businessLogicController
		// .getShuffledDeck(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
		// MARAFFA_PLAYERS);
		// try {
		// final int firstPlayer = future.get().getInteger("firstPlayer");
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		// assertEquals(firstPlayer, initialTurn);

		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));

		final GameCommandResult callResponse = this.gameService.makeCall(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), FAKE_CALL,
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertFalse(callResponse.getBoolean(Constants.MESSAGE));

		// } catch (final InterruptedException e) {
		// e.printStackTrace();
		// } catch (final ExecutionException e) {
		// e.printStackTrace();
		// }
		context.completeNow();
	}

	/**
	 * Returns all the games created or not found if there aren't games
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void getGamesTest(final VertxTestContext context) {
		GameListResult gamesResponse = this.gameService.getGames();
		// assertTrue(gamesResponse.isEmpty());
		this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE, GAME_MODE.toString());
		this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE, GAME_MODE.toString());
		this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE, GAME_MODE.toString());
		gamesResponse = this.gameService.getGames();
		Assertions.assertFalse(gamesResponse.isEmpty());
		context.completeNow();
	}

	/**
	 * A player can play only one card in their turn
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void playOnlyOneCardTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			assertThrows(IndexOutOfBoundsException.class,
					() -> this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
							TEST_USER.username(), TEST_CARD, IS_SUIT_FINISHED, true));

			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult changeResponse = this.gameService.changeTeam(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username() + 0, "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		assertTrue(this.gameService
				.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
								.getUsers().get(initialTurn).username(),
						TEST_CARD, IS_SUIT_FINISHED, true)
				.getBoolean(Constants.PLAY));
		assertFalse(this.gameService
				.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
								.getUsers().get(initialTurn).username(),
						TEST_CARDS.get(1), IS_SUIT_FINISHED, true)
				.getBoolean(Constants.PLAY));
		context.completeNow();
	}

	/**
	 * Each player can play only in their turn
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void playOnlyInTheirTurnTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			assertThrows(IndexOutOfBoundsException.class,
					() -> this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
							TEST_USER.username(), TEST_CARD, IS_SUIT_FINISHED, true));

			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult changeResponse = this.gameService
				.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		final GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		final int turn = this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.getTurn();

		assertEquals(
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(turn).username());
		assertFalse(this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get((initialTurn + 1) % MARAFFA_PLAYERS).username(),
				TEST_CARD, IS_SUIT_FINISHED, true).getBoolean(Constants.PLAY));
		assertTrue(this.gameService
				.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
								.getUsers().get(initialTurn).username(),
						TEST_CARD, IS_SUIT_FINISHED, true)
				.getBoolean(Constants.PLAY));
		context.completeNow();
	}

	/**
	 * An invalid user can't choose the trump
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void invalidUserCantChooseTrumpTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			assertThrows(IndexOutOfBoundsException.class,
					() -> this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
							TEST_USER.username(), TEST_CARD, IS_SUIT_FINISHED, true));

			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult changeResponse = this.gameService
				.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get((initialTurn + 1) % MARAFFA_PLAYERS).username());
		assertFalse(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		chooseTrumpResponse = this.gameService.chooseTrump(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		final int turn = this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.getTurn();

		assertEquals(
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(turn).username());
		context.completeNow();
	}

	/**
	 * A player can't play if the system doesn't know who has the 4 of coins
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void dontPlayWithout4Coins(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			final GameCommandResult joinResponse = this.gameService.joinGame(
					UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
			assertTrue(joinResponse.containsKey(Constants.JOIN_ATTR));
		}
		GameCommandResult changeResponse = this.gameService
				.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "B", 0);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username() + 1, "B", EXPECTED_POS);
		assertThrows(IndexOutOfBoundsException.class,
				() -> this.gameService.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						TEST_USER.username(), TEST_CARD, IS_SUIT_FINISHED, true));

		GameCommandResult chooseTrumpResponse = this.gameService.chooseTrump(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TRUMP.name(), TEST_USER.username());
		assertFalse(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
				.setInitialTurn(FIRST_PLAYER);
		final int initialTurn = this.gameService.getGameMap()
				.get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getInitialTurn();
		chooseTrumpResponse = this.gameService.chooseTrump(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TRUMP.name(), TEST_USER.username());
		if (initialTurn != 0) {
			assertFalse(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		}

		chooseTrumpResponse = this.gameService.chooseTrump(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TRUMP.name(),
				this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID))).getUsers()
						.get(initialTurn).username());
		assertTrue(chooseTrumpResponse.getBoolean(Constants.TRUMP));
		assertTrue(this.gameService
				.doPlayCard(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
						this.gameService.getGameMap().get(UUID.fromString(gameResponse.getString(Constants.GAME_ID)))
								.getUsers().get(initialTurn).username(),
						TEST_CARD, IS_SUIT_FINISHED, true)
				.getBoolean(Constants.PLAY));
		context.completeNow();
	}

	/**
	 * The game mode is invalid, create returns "invalid" and getJsonGames "not
	 * found"
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void getGamesInvalidGameModeTest(final VertxTestContext context) {
		final GameCommandResult createResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				FAKE_GAME_MODE);
		final GameListResult gamesResponse = this.gameService.getGames();
		assertTrue(createResponse.containsKey(Constants.INVALID));
		// assertTrue(gamesResponse.isEmpty());
		context.completeNow();
	}

	/**
	 * A user is able to change the team
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void changeTeamTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			this.gameService.joinGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
		}
		final GameCommandResult changeResponse = this.gameService.changeTeam(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "A", EXPECTED_POS);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		context.completeNow();
	}

	/**
	 * A user can't change the team if the game has already started
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void changeNotAllowedWhilePlayingTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			this.gameService.joinGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
		}
		this.gameService.startGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		final GameCommandResult changeResponse = this.gameService.changeTeam(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "A",
				EXPECTED_POS - 1);
		assertFalse(changeResponse.getBoolean(Constants.TEAM));
		context.completeNow();
	}

	/**
	 * A user can't change the team if the position specified is invalid
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void wrongPositionTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		assertThrows(IndexOutOfBoundsException.class, () -> {
			this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					TEST_USER.username(), "A", EXPECTED_POS);
		});
		context.completeNow();
	}

	/**
	 * Users can change their position in the same team they are
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void changePositionSameTeamTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		for (int i = 0; i < MARAFFA_PLAYERS - 1; i++) {
			this.gameService.joinGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
					new User(TEST_USER.username() + i, TEST_USER.clientID(), false), PASSWORD);
		}
		GameCommandResult changeResponse = this.gameService.changeTeam(
				UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username(), "A", EXPECTED_POS);
		changeResponse = this.gameService.changeTeam(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				TEST_USER.username(), "A", EXPECTED_POS - 1);
		assertTrue(changeResponse.getBoolean(Constants.TEAM));
		context.completeNow();
	}

	/** Creator can set a password for the game */
	@Test
	public void setPasswordTest(final VertxTestContext context) {
		assertTrue(this.gameService.setPassword(FAKE_UUID, PASSWORD).containsKey(Constants.NOT_FOUND));
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		assertFalse(this.gameService.setPassword(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), PASSWORD)
				.containsKey(Constants.NOT_FOUND));
		context.completeNow();
	}

	/**
	 * Start another game with the same users, same game mode, same expected score
	 * of the previos one
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void newGameTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		final GameCommandResult newGameResponse = this.gameService
				.newGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)));
		this.gameService.getGameMap().get(UUID.fromString(newGameResponse.getString("newGameID")));
		assertTrue(newGameResponse.getBoolean(Constants.NEW_GAME_CREATION));
		context.completeNow();
	}

	/**
	 * Check if the user is removed from the game
	 *
	 * @param context
	 *            vertx test context
	 */
	@Test
	public void removeUserTest(final VertxTestContext context) {
		final GameCommandResult gameResponse = this.gameService.createGame(MARAFFA_PLAYERS, TEST_USER, EXPECTED_SCORE,
				GAME_MODE.toString());
		Assertions.assertEquals(UUID_SIZE, gameResponse.getString(Constants.GAME_ID).length());
		this.gameService.joinGame(UUID.fromString(gameResponse.getString(Constants.GAME_ID)),
				new User(TEST_USER.username() + 0, TEST_USER.clientID(), false), PASSWORD);
		final GameCommandResult removeResponse = this.gameService
				.removeUser(UUID.fromString(gameResponse.getString(Constants.GAME_ID)), TEST_USER.username() + 0);
		assertFalse(removeResponse.containsKey(Constants.NOT_FOUND));
		context.completeNow();
	}
}
