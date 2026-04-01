package application.service;

import domain.aggregate.Trick;
import domain.aggregate.TrickImpl;
import domain.port.inbound.IGameUseCase;
import domain.port.outbound.IEventPublisher;
import domain.port.outbound.IGame;
import domain.port.outbound.IGameFactory;
import domain.port.outbound.IGameRepository;
import domain.port.outbound.IRulesEnginePort;
import domain.port.outbound.IUserServicePort;
import domain.valueobject.Call;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.GameCommandResult;
import domain.valueobject.GameListResult;
import domain.valueobject.GameMode;
import domain.valueobject.User;
import application.config.AppConstants;
import domain.port.outbound.IStatisticPort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The `GameService` class in Java manages game-related operations such as
 * creating games, joining games, playing cards, and handling game state.
 */
public class GameService implements IGameUseCase {
	private final IGameRepository gameRepository;
	private final IGameFactory gameFactory;
	private final IRulesEnginePort rulesEngine;
	private final IUserServicePort userService;
	private static final Boolean DEBUG = false;
	private IStatisticPort statisticPort;
	private IEventPublisher eventPublisher;

	public GameService(final IGameFactory gameFactory, final IGameRepository gameRepository) {
		this(gameFactory, null, null, gameRepository, null, null);
	}

	public GameService(final IGameFactory gameFactory, final IStatisticPort statisticPort,
			final IGameRepository gameRepository) {
		this(gameFactory, statisticPort, null, gameRepository, null, null);
	}

	public GameService(final IGameFactory gameFactory, final IStatisticPort statisticPort,
			final IEventPublisher eventPublisher, final IGameRepository gameRepository,
			final IRulesEnginePort rulesEngine) {
		this(gameFactory, statisticPort, eventPublisher, gameRepository, rulesEngine, null);
	}

	public GameService(final IGameFactory gameFactory, final IStatisticPort statisticPort,
			final IEventPublisher eventPublisher, final IGameRepository gameRepository,
			final IRulesEnginePort rulesEngine, final IUserServicePort userService) {
		this.gameFactory = gameFactory;
		this.statisticPort = statisticPort;
		this.eventPublisher = eventPublisher;
		this.gameRepository = gameRepository;
		this.rulesEngine = rulesEngine;
		this.userService = userService;
	}

	@Override
	public GameCommandResult createGame(final int numberOfPlayers, final String username, final UUID guiId,
			final boolean isGuest, final int expectedScore, final String gameMode) {
		return this.createGame(numberOfPlayers, new User(username, guiId, isGuest), expectedScore, gameMode);
	}

	public GameCommandResult createGame(final Integer numberOfPlayers, final User user, final int expectedScore,
			final String gameMode) {
		final GameCommandResult result = new GameCommandResult();
		final UUID newId = UUID.randomUUID();
		IGame currentGame;
		try {
			currentGame = this.gameFactory.createGame(newId, user, numberOfPlayers, expectedScore,
					GameMode.valueOf(gameMode.toUpperCase()));
		} catch (final IllegalArgumentException e) {
			result.put(AppConstants.ERROR, "Invalida modalità di gioco " + gameMode);
			return result.put(AppConstants.INVALID, gameMode);
		}
		this.gameRepository.save(newId, currentGame);
		currentGame.onCreateGame(user);
		if (this.eventPublisher != null) {
			final GameListResult gameList = this.getGames();
			this.eventPublisher.publish(java.util.List.of(), "gameList",
					Map.of(AppConstants.GAME, gameList.getItems()));
		}
		result.put(AppConstants.GAME_ID, String.valueOf(newId));
		return result;
	}

	@Override
	public GameCommandResult joinGame(final UUID gameID, final String username, final UUID guiId,
			final boolean isGuest, final String password) {
		return this.joinGame(gameID, new User(username, guiId, isGuest), password);
	}

	public GameCommandResult joinGame(final UUID gameID, final User user, final String pwd) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			if (!game.checkPasword(pwd)) {
				result.put(AppConstants.JOIN_ATTR, false);
				result.put(AppConstants.ERROR, "Password errata");
				return result.put(AppConstants.MESSAGE, "Wrong password");
			}
			if (game.getNumberOfPlayersIn() < game.getMaxNumberOfPlayers()) {
				if (game.addUser(user)) {
					result.put(AppConstants.JOIN_ATTR, true);
					return result.put(AppConstants.MESSAGE, "Game " + gameID + " joined by " + user.username());
				} else {
					result.put(AppConstants.ALREADY_JOINED, true);
					result.put(AppConstants.ERROR, user.username() + " già presente nella partita " + gameID);
					return result.put(AppConstants.MESSAGE,
							"Game " + gameID + " already joined by " + user.username());
				}
			}
			result.put(AppConstants.FULL, true);
			result.put(AppConstants.ERROR, "Raggiunto il limite massimo di giocatori nella partita " + gameID);
			return result.put(AppConstants.MESSAGE,
					"Reached the limit of maximum players in the game " + gameID);
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato ");
		result.put(AppConstants.MESSAGE, "Game " + gameID + " not found ");
		return result;
	}

	@Override
	public GameCommandResult startGame(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			if (game.startGame()) {
				try {
					result.put(AppConstants.START_ATTR, true);
					result.put(AppConstants.MESSAGE, "The game " + gameID + " can start");
				} catch (final Exception e) {
					result.put(AppConstants.START_ATTR, false);
					result.put(AppConstants.ERROR, "Errore nell'avvio del game");
					result.put(AppConstants.MESSAGE, "Error in starting the game");
				}
				return result;
			} else {
				result.put(AppConstants.START_ATTR, false);
				result.put(AppConstants.ERROR, "Team non bilanciati o non tutti i giocatori si sono uniti");
				return result.put(AppConstants.MESSAGE,
						"Not all the players are in or the teams are not balanced");
			}
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.START_ATTR, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult canStart(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			if (game.canStart()) {
				result.put(AppConstants.START_ATTR, true);
				return result.put(AppConstants.MESSAGE, "The game " + gameID + " can start");
			} else {
				result.put(AppConstants.START_ATTR, false);
				result.put(AppConstants.ERROR, "Il game " + gameID + " non può iniziare");
				return result.put(AppConstants.MESSAGE, "The game " + gameID + " can't start");
			}
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	public GameCommandResult canPlayCard(final UUID gameID, final String username,
			final Card<CardValue, CardSuit> card, final Boolean isSuitFinishedByPlayer) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent() && this.gameRepository.findById(gameID).get().canStart()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			if (CardSuit.NONE.equals(game.getTrump())) {
				result.put(AppConstants.PLAY, false);
				result.put(AppConstants.ERROR, "La briscola non è stata scelta");
				result.put(AppConstants.MESSAGE, "Trump not setted");
				return result;
			}
		} else {
			result.put(AppConstants.NOT_FOUND, false);
			result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
			return result.put(AppConstants.PLAY, false);
		}
		return result;
	}

	@Override
	public CompletableFuture<GameCommandResult> playCard(final UUID gameID, final String username,
			final Card<CardValue, CardSuit> card, final boolean isSuitFinished) {
		final GameCommandResult response = new GameCommandResult();
		final IGame game = this.gameRepository.findById(gameID).orElse(null);
		if (game == null) {
			response.put(AppConstants.NOT_FOUND, false);
			response.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
			response.put(AppConstants.PLAY, false);
			return CompletableFuture.completedFuture(response);
		}
		final int userPosition = game.getPositionByUsername(username);
		if (userPosition == -1) {
			response.put(AppConstants.ERROR, "Invalid user " + username);
			response.put("invalidUser", true);
			response.put(AppConstants.PLAY, false);
			return CompletableFuture.completedFuture(response);
		}
		final GameCommandResult canPlayResult = this.canPlayCard(gameID, username, card, isSuitFinished);
		if (canPlayResult.containsKey(AppConstants.NOT_FOUND)
				|| Boolean.FALSE.equals(canPlayResult.getBoolean(AppConstants.PLAY))) {
			return CompletableFuture.completedFuture(canPlayResult);
		}
		if (this.rulesEngine == null) {
			return CompletableFuture.completedFuture(this.doPlayCard(gameID, username, card, isSuitFinished, true));
		}
		final boolean isCardTrump = game.getTrump().equals(card.cardSuit());
		final int[] trick = game.getCurrentTrick() == null ? new int[0]
				: game.getCurrentTrick().getCards().stream().mapToInt(Integer::parseInt).toArray();
		final int[] userCards = game.getUserCards(username).stream()
				.mapToInt(userCard -> userCard.getCardValue().intValue()).toArray();
		return this.rulesEngine.validateCard(trick, card.getCardValue(), userCards, isCardTrump).thenApply(isValid -> {
			response.put("mode", game.getGameMode());
			if (GameMode.ELEVEN2ZERO.equals(game.getGameMode()) || isValid) {
				final GameCommandResult playResult = this.doPlayCard(gameID, username, card, isSuitFinished, isValid);
				if (!game.isUserIn(username) || !playResult.getBoolean(AppConstants.PLAY, false)) {
					response.put(AppConstants.ERROR, "Non e' il turno di " + username
							+ " o non e' stata scelta la briscola o i team non sono bilanciati");
					response.put(AppConstants.MESSAGE, "Is not the turn of " + username);
					response.put(AppConstants.PLAY, false);
					response.put("notTurn", true);
					return response;
				}
				response.put(AppConstants.PLAY, true);
				return response;
			} else {
				response.put(AppConstants.ERROR, "Carta non valida");
				response.put(AppConstants.MESSAGE, "Invalid card " + card);
				response.put(AppConstants.PLAY, false);
				response.put("invalidCard", true);
				return response;
			}
		});
	}

	public GameCommandResult doPlayCard(final UUID gameID, final String username,
			final Card<CardValue, CardSuit> card, final Boolean isSuitFinishedByPlayer, final Boolean isValid) {
		final GameCommandResult result = new GameCommandResult();
		final IGame game = this.gameRepository.findById(gameID).orElse(null);
		final var isSuitAdded = game.setIsSuitFinished(isSuitFinishedByPlayer, username, isValid);
		final Boolean play = game.addCard(card, username, isSuitAdded);
		result.put(AppConstants.PLAY, play);
		if (play && game.getLatestTrick().isCompleted()) {
			game.addTrickToSchema(game.getCurrentTrick());
			if (this.statisticPort != null) {
				this.statisticPort.updateRecordWithTrick(
						String.valueOf(gameID) + '-' + game.getCurrentState().get() / 10, game.getCurrentTrick());
			}
			try {
				game.onTrickCompleted(game.getCurrentTrick());
			} catch (final Exception e) {
				result.put(AppConstants.PLAY, false);
				result.put(AppConstants.ERROR, "La presa non è stata completata correttamente");
				result.put(AppConstants.MESSAGE, "Failed to complete the trick");
				return result;
			}
		}
		return result;
	}

	@Override
	public GameCommandResult chooseTrump(final UUID gameID, final String cardSuit, final String username) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			if (game.getPositionByUsername(username) == game.getTurn() || DEBUG) {
				CardSuit trump;
				try {
					trump = CardSuit.valueOf(cardSuit);
				} catch (final IllegalArgumentException e) {
					trump = CardSuit.NONE;
				}
				game.chooseTrump(trump);
				result.put(AppConstants.MESSAGE, trump + " setted as trump");
				result.put(AppConstants.VALUE, trump.toString());
				if (CardSuit.NONE.equals(trump)) {
					result.put(AppConstants.TRUMP, false);
					result.put(AppConstants.ERROR, "Briscola non valida: " + trump);
					result.put(AppConstants.ILLEGAL_TRUMP, true);
					return result;
				}
				result.put(AppConstants.TRUMP, true);
				return result;
			} else {
				result.put(AppConstants.TRUMP, false);
				result.put(AppConstants.NOT_ALLOWED, true);
				result.put(AppConstants.ERROR,
						"Il giocatore " + username + " non è autorizzato a scegliere la briscola");
				return result.put(AppConstants.MESSAGE,
						"The user " + username + " is not allowed to choose the trump");
			}
		} else {
			result.put(AppConstants.TRUMP, false);
			result.put(AppConstants.NOT_FOUND, false);
			result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
			return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
		}
	}

	@Override
	public boolean startNewRound(final UUID gameID) {
		if (this.gameRepository.findById(gameID).isPresent()) {
			this.gameRepository.findById(gameID).get().startNewRound();
			return true;
		}
		return false;
	}

	@Override
	public GameCommandResult changeTeam(final UUID gameID, final String username, final String team,
			final int position) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			result.put(AppConstants.TEAM,
					this.gameRepository.findById(gameID).get().changeTeam(username, team, position));
			return result;
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult getState(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final Trick currentTrick = this.gameRepository.findById(gameID).get().getCurrentTrick();
			if (currentTrick == null) {
				result.put(AppConstants.NOT_FOUND, false);
				result.put(AppConstants.ERROR, "Stato sconosciuto: presa non trovata");
				return result.put(AppConstants.MESSAGE, "Trick not found");
			}
			result.put(AppConstants.MESSAGE, currentTrick.toString());
			return result;
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult isGameEnded(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final Boolean isEnded = this.gameRepository.findById(gameID).get().isGameEnded();
			result.put(AppConstants.ENDED, isEnded);
			if (!isEnded) {
				result.put(AppConstants.ERROR, "Il game " + gameID + " non è concluso");
			}
			return result;
		}
		result.put(AppConstants.ENDED, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult makeCall(final UUID gameID, final String call, final String username) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final boolean success = this.gameRepository.findById(gameID).get()
					.makeCall(Call.fromUppercaseString(call.toUpperCase()), username);
			if (!success) {
				result.put(AppConstants.ERROR, "La chiamata " + call + " non è andata a buon fine");
			}
			return result.put(AppConstants.MESSAGE, success);
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult newGame(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame previousGame = this.gameRepository.findById(gameID).get();
			if (!previousGame.isNewGameCreated()) {
				previousGame.setNewGameCreated();
				final GameCommandResult newGameResult = this.createGame(previousGame.getNumberOfPlayersIn(),
						previousGame.getUsers().get(0), previousGame.getExpectedScore(),
						previousGame.getGameMode().name());
				final String newGameID = newGameResult.getString(AppConstants.GAME_ID);
				final IGame newGame = this.gameRepository.findById(UUID.fromString(newGameID)).get();
				previousGame.getUsers().stream()
						.filter(user -> !user.username().equals(previousGame.getUsers().get(0).username()))
						.forEach(newGame::addUser);
				newGame.onStartGame();
				previousGame.onNewGame(newGameID);
				result.put(AppConstants.MESSAGE, "New game created");
				result.put("newGameID", newGameID);
				return result.put(AppConstants.NEW_GAME_CREATION, true);
			}
			result.put(AppConstants.NEW_GAME_CREATION, false);
			result.put(AppConstants.ERROR, "Nuovo game già creato");
			return result.put(AppConstants.MESSAGE, "New game already created");
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult setPassword(final UUID gameID, final String password) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			this.gameRepository.findById(gameID).get().setPassword(password);
			result.put(AppConstants.PASSWORD, "Password impostata");
			return result;
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Gioco non trovato");
		return result;
	}

	public Map<UUID, IGame> getGameMap() {
		return this.gameRepository.getAll();
	}

	@Override
	public GameListResult getGames() {
		final GameListResult games = new GameListResult();
		this.gameRepository.getAll().values().stream().map(IGame::toMap).forEach(games::add);
		return games;
	}

	@Override
	public GameCommandResult getPlayers() {
		final GameCommandResult result = new GameCommandResult();
		result.put("inGamePlayers", this.gameRepository.getAll().values().stream().map(IGame::getUsers)
						.flatMap(List::stream).map(User::username).collect(Collectors.toList()))
				.put("connected", List.of());
		return result;
	}

	@Override
	public GameCommandResult exitGame(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			this.gameRepository.findById(gameID).get().onExitGame();
			this.gameRepository.remove(gameID);
			result.put(AppConstants.CLOSED, true);
			return result.put(AppConstants.MESSAGE, "Game " + gameID + " exited correctly");
		}
		result.put(AppConstants.CLOSED, false);
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult removeUser(final UUID gameID, final String username) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			this.gameRepository.findById(gameID).get().removeUser(username);
			return result.put(AppConstants.MESSAGE, "Username " + username + " removed from the game");
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result.put(AppConstants.MESSAGE, "Game " + gameID + " not found");
	}

	@Override
	public GameCommandResult getGame(final UUID gameID) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final Map<String, Object> gameMap = this.gameRepository.findById(gameID).get().toMap();
			gameMap.forEach(result::put);
			return result;
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result;
	}

	@Override
	public GameCommandResult getPlayerCards(final UUID gameID, final String username) {
		final GameCommandResult result = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			result.put("cards", this.gameRepository.findById(gameID).get().getUserCards(username));
			return result;
		}
		result.put(AppConstants.NOT_FOUND, false);
		result.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return result;
	}

	@Override
	public int getGameCount() {
		return this.gameRepository.count();
	}

	@Override
	public GameCommandResult notifyGame(final UUID gameID, final String message) {
		final GameCommandResult response = new GameCommandResult();
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			final List<String> targets = game.getUsers().stream().map(domain.valueobject.User::username)
					.collect(Collectors.toList());
			if (this.eventPublisher != null) {
				this.eventPublisher.publish(targets, "notification", Map.of("message", message));
			}
			response.put("notified", true);
			return response;
		}
		response.put(AppConstants.NOT_FOUND, false);
		response.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return response;
	}

	@Override
	public GameCommandResult chatMessage(final UUID gameID, final String author, final String message,
			final String environment) {
		final GameCommandResult response = new GameCommandResult();
		if (gameID == null) {
			// Chat globale: broadcast a tutti gli utenti connessi
			if (this.eventPublisher != null) {
				this.eventPublisher.publish(java.util.List.of(), "message",
						Map.of("message", message, "author", author, "environment", environment));
			}
			response.put("sent", true);
			return response;
		}
		if (this.gameRepository.findById(gameID).isPresent()) {
			final IGame game = this.gameRepository.findById(gameID).get();
			// FIX: no more instanceof — route through eventPublisher using game's user list
			final List<String> targets = game.getUsers().stream().map(User::username).collect(Collectors.toList());
			if (this.eventPublisher != null) {
				this.eventPublisher.publish(targets, "message",
						Map.of("message", message, "author", author, "environment", environment));
			}
			response.put("sent", true);
			return response;
		}
		response.put(AppConstants.NOT_FOUND, false);
		response.put(AppConstants.ERROR, "Game " + gameID + " non trovato");
		return response;
	}
}
