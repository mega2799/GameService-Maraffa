package infrastructure.http;

import domain.port.inbound.IGameUseCase;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import domain.valueobject.GameCommandResult;
import domain.valueobject.GameListResult;
import infrastructure.config.Constants;
import infrastructure.http.schema.ChangeTeamBody;
import infrastructure.http.schema.ChooseTrumpBody;
import infrastructure.http.schema.CreateGameBody;
import infrastructure.http.schema.JoinGameBody;
import infrastructure.http.schema.MakeCallBody;
import infrastructure.http.schema.NewGameBody;
import infrastructure.http.schema.PasswordBody;
import infrastructure.http.schema.PlayCardBody;
import infrastructure.http.schema.RemoveUserBody;
import infrastructure.http.schema.StartBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.UUID;

/** Primary adapter: converts HTTP requests to IGameUseCase calls and domain results to JSON responses. */
public class GameHttpAdapter {
	private final IGameUseCase gameUseCase;
	private static final Logger LOGGER = LoggerFactory.getLogger(GameHttpAdapter.class);

	public GameHttpAdapter(final IGameUseCase gameUseCase) {
		this.gameUseCase = gameUseCase;
	}

	/** Converts a framework-agnostic GameCommandResult into a Vert.x JsonObject for HTTP responses. */
	private static JsonObject toJson(final GameCommandResult result) {
		return new JsonObject(result.toMap());
	}

	/** Converts a GameListResult into a Vert.x JsonArray for HTTP responses. */
	private static JsonArray toJsonArray(final GameListResult result) {
		return new JsonArray(result.getItems());
	}

	@Operation(summary = "Create new game", method = Constants.CREATE_GAME_METHOD, operationId = Constants.CREATE_GAME, tags = {
			Constants.GAME_TAG}, requestBody = @RequestBody(description = "insert username and the number of players", required = true, content = @Content(mediaType = "application/json", encoding = @Encoding(contentType = "application/json"), schema = @Schema(implementation = CreateGameBody.class))), responses = {
					@ApiResponse(responseCode = "200", description = "OK"),
					@ApiResponse(responseCode = "417", description = "Invalid game mode."),
					@ApiResponse(responseCode = "500", description = "Internal Server Error.")})
	public void createGame(final RoutingContext context) {
		final String guiIdAsString = context.body().asJsonObject().getString(Constants.GUIID);
		final UUID guiId = UUID.fromString(guiIdAsString);
		final Integer numberOfPlayers = context.body().asJsonObject().getInteger(Constants.NUMBER_OF_PLAYERS);
		final String username = context.request().getHeader("x-user-id");
		final String gameMode = context.body().asJsonObject().getString(Constants.GAME_MODE);
		final boolean isGuest = context.body().asJsonObject().getBoolean(Constants.GUEST, false);
		final Integer expectedScore = context.body().asJsonObject().getInteger(Constants.EXPECTED_SCORE);
		final GameCommandResult result = this.gameUseCase.createGame(numberOfPlayers, username, guiId, isGuest,
				expectedScore, gameMode);
		if (result.containsKey(Constants.INVALID)) {
			context.response().setStatusCode(401).end("Invalid game mode");
		}
		context.response().end(toJson(result).toBuffer());
	}

	@Operation(summary = "Join a specific game", method = Constants.JOIN_GAME_METHOD, operationId = Constants.JOIN_GAME, tags = {
			Constants.GAME_TAG}, requestBody = @RequestBody(description = "username and id of the game are required", required = true, content = @Content(mediaType = "application/json", encoding = @Encoding(contentType = "application/json"), schema = @Schema(implementation = JoinGameBody.class))), responses = {
					@ApiResponse(responseCode = "200", description = "OK"),
					@ApiResponse(responseCode = "404", description = "Game not found."),
					@ApiResponse(responseCode = "417", description = "Reached the limit of maximum players in the game."),
					@ApiResponse(responseCode = "500", description = "Internal Server Error.")})
	public void joinGame(final RoutingContext context) {
		final String uuidAsString = context.body().asJsonObject().getString(Constants.GAME_ID);
		final String guiIdAsString = context.body().asJsonObject().getString(Constants.GUIID);
		final boolean isGuest = context.body().asJsonObject().getBoolean(Constants.GUEST, false);
		final UUID gameID = UUID.fromString(uuidAsString);
		final UUID guiId = UUID.fromString(guiIdAsString);
		final String pwd = context.body().asJsonObject().getString(Constants.PASSWORD);
		final String username = context.request().getHeader("x-user-id");
		final GameCommandResult result = this.gameUseCase.joinGame(gameID, username, guiId, isGuest, pwd);
		if (result.containsKey(Constants.NOT_FOUND)) {
			context.response().setStatusCode(404).end(result.getString(Constants.MESSAGE));
		} else if (result.containsKey(Constants.FULL)) {
			context.response().setStatusCode(401).end(result.getString(Constants.MESSAGE));
		} else {
			context.response().end(toJson(result).toBuffer());
		}
	}

	@Operation(summary = "Start a specific game", method = Constants.START_GAME_METHOD, operationId = Constants.START_GAME, tags = {
			Constants.GAME_TAG}, requestBody = @RequestBody(description = "id of the game is required", required = true, content = @Content(mediaType = "application/json", encoding = @Encoding(contentType = "application/json"), schema = @Schema(implementation = StartBody.class))), responses = {
					@ApiResponse(responseCode = "200", description = "OK"),
					@ApiResponse(responseCode = "404", description = "Game not found."),
					@ApiResponse(responseCode = "500", description = "Internal Server Error.")})
	public void startGame(final RoutingContext context) {
		final String uuidAsString = (String) context.body().asJsonObject().getValue(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final GameCommandResult result = this.gameUseCase.startGame(gameID);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void startNewRound(final RoutingContext context) {
		final String uuidAsString = context.body().asJsonObject().getString(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		if (this.gameUseCase.startNewRound(gameID)) {
			context.response().end("New round started");
		} else {
			context.response().setStatusCode(404).end("Game " + gameID + " not found");
		}
	}

	public void changeTeam(final RoutingContext context) {
		final String uuidAsString = context.body().asJsonObject().getString(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final String username = context.request().getHeader("x-user-id");
		final String team = context.body().asJsonObject().getString(Constants.TEAM);
		final Integer position = context.body().asJsonObject().getInteger(Constants.POSITION);
		final GameCommandResult result = this.gameUseCase.changeTeam(gameID, username, team, position);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			if (result.getBoolean(Constants.TEAM, false)) {
				context.response().end(new JsonObject().put(Constants.MESSAGE, "Team changed").toBuffer());
			} else {
				context.response().setStatusCode(417)
						.end(new JsonObject().put(Constants.MESSAGE, "The game is already started").toBuffer());
			}
		} else {
			context.response().setStatusCode(404)
					.end(new JsonObject().put(Constants.MESSAGE, "Game " + gameID + " not found").toBuffer());
		}
	}

	public void getPlayerCard(final RoutingContext context) {
		final UUID gameID = UUID.fromString(context.pathParam(Constants.GAME_ID));
		final String username = context.pathParam(Constants.USERNAME);
		final GameCommandResult result = this.gameUseCase.getPlayerCards(gameID, username);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end("Game " + gameID + " not found");
		}
	}

	public void canStart(final RoutingContext context) {
		final UUID gameID = UUID.fromString(context.pathParam(Constants.GAME_ID));
		final GameCommandResult result = this.gameUseCase.canStart(gameID);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void playCard(final RoutingContext context) {
		final JsonObject response = new JsonObject();
		final String uuidAsString = context.body().asJsonObject().getString(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final String cardValue = context.body().asJsonObject().getString(Constants.CARD_VALUE);
		final String cardSuit = context.body().asJsonObject().getString(Constants.CARD_SUIT);
		final Boolean isSuitFinishedByPlayer = context.body().asJsonObject().getBoolean(Constants.IS_SUIT_FINISHED);
		try {
			final Card<CardValue, CardSuit> card = new Card<>(CardValue.getName(cardValue), CardSuit.getName(cardSuit));
			final String username = context.request().getHeader("x-user-id");
			if (CardSuit.NONE.equals(card.cardSuit()) || CardValue.NONE.equals(card.cardValue())) {
				response.put(Constants.MESSAGE, "Invalid card " + card);
				context.response().setStatusCode(401).end(response.toBuffer());
				return;
			}
			this.gameUseCase.playCard(gameID, username, card, isSuitFinishedByPlayer).whenComplete((result, err) -> {
				final JsonObject resultJson = toJson(result);
				if (err != null) {
					response.put(Constants.ERROR, "Errore interno nel giocare la carta");
					context.response().setStatusCode(500).end(response.toBuffer());
				} else if (result.containsKey(Constants.NOT_FOUND)) {
					context.response().setStatusCode(404).end(resultJson.toBuffer());
				} else if (result.containsKey("invalidUser")) {
					response.put(Constants.MESSAGE, "Invalid user " + username);
					context.response().setStatusCode(401).end(response.toBuffer());
				} else if (result.containsKey("notTurn")) {
					context.response().setStatusCode(417).end(resultJson.toBuffer());
				} else if (result.containsKey("invalidCard")) {
					context.response().setStatusCode(500).end(resultJson.toBuffer());
				} else if (!result.getBoolean(Constants.PLAY, false)) {
					response.put(Constants.ERROR, "Non e' il turno di " + username
							+ " o non e' stata scelta la briscola o i team non sono bilanciati");
					response.put(Constants.MESSAGE, "Is not the turn of " + username);
					context.response().setStatusCode(417).end(response.toBuffer());
				} else {
					context.response().end(resultJson.toBuffer());
				}
			});
		} catch (final IllegalArgumentException e) {
			response.put(Constants.MESSAGE, "Errore nel giocare la carta: " + cardValue + " di " + cardSuit);
			context.response().setStatusCode(417).end(response.toBuffer());
		}
	}

	public void chooseTrump(final RoutingContext context) {
		final String uuidAsString = context.body().asJsonObject().getString(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final String cardSuit = context.body().asJsonObject().getString(Constants.CARD_SUIT);
		final String username = context.request().getHeader("x-user-id");
		final GameCommandResult result = this.gameUseCase.chooseTrump(gameID, cardSuit, username);
		if (!result.containsKey(Constants.NOT_FOUND) && !result.containsKey(Constants.ILLEGAL_TRUMP)
				&& !result.containsKey(Constants.NOT_ALLOWED)) {
			context.response().end(toJson(result).toBuffer());
		} else if (result.containsKey(Constants.ILLEGAL_TRUMP)) {
			context.response().setStatusCode(401).end(toJson(result).toBuffer());
		} else if (result.containsKey(Constants.NOT_ALLOWED)) {
			context.response().setStatusCode(417).end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void getState(final RoutingContext context) {
		final UUID gameID = UUID.fromString(context.pathParam(Constants.GAME_ID));
		final GameCommandResult stateResult = this.gameUseCase.getState(gameID);
		if (stateResult.containsKey(Constants.NOT_FOUND)) {
			context.response().setStatusCode(404).end(stateResult.getString(Constants.MESSAGE));
			return;
		}
		final GameCommandResult canStartResult = this.gameUseCase.canStart(gameID);
		if (canStartResult.getBoolean(Constants.START_ATTR, false)) {
			context.response().end(stateResult.getString(Constants.MESSAGE));
		} else {
			context.response().setStatusCode(404).end(stateResult.getString(Constants.MESSAGE));
		}
	}

	public void isGameEnded(final RoutingContext context) {
		final UUID gameID = UUID.fromString(context.pathParam(Constants.GAME_ID));
		final GameCommandResult result = this.gameUseCase.isGameEnded(gameID);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void makeCall(final RoutingContext context) {
		final String uuidAsString = (String) context.body().asJsonObject().getValue(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final String call = context.body().asJsonObject().getString(Constants.CALL);
		final String username = context.request().getHeader("x-user-id");
		final GameCommandResult result = this.gameUseCase.makeCall(gameID, call, username);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			if (Boolean.TRUE.equals(result.getBoolean(Constants.MESSAGE))) {
				context.response().end("Call " + call + " setted!");
			} else {
				context.response().setStatusCode(404).end("Invalid call");
			}
		} else {
			context.response().setStatusCode(404).end(result.getString(Constants.MESSAGE));
		}
	}

	public void getGames(final RoutingContext context) {
		final GameListResult result = this.gameUseCase.getGames();
		final JsonArray jsonArray = toJsonArray(result);
		if (!jsonArray.isEmpty()) {
			context.response().end(jsonArray.toBuffer());
		} else {
			context.response().setStatusCode(404).end(jsonArray.toBuffer());
		}
	}

	public void getPlayers(final RoutingContext context) {
		context.response().end(toJson(this.gameUseCase.getPlayers()).toBuffer());
	}

	public void getGame(final RoutingContext context) {
		final UUID gameID = UUID.fromString(context.pathParam(Constants.GAME_ID));
		final GameCommandResult result = this.gameUseCase.getGame(gameID);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void getCountGames(final RoutingContext context) {
		context.response()
				.end(new JsonObject().put(Constants.TOTAL, String.valueOf(this.gameUseCase.getGameCount())).toBuffer());
	}

	public void newGame(final RoutingContext context) {
		final String uuidAsString = (String) context.body().asJsonObject().getValue(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final GameCommandResult result = this.gameUseCase.newGame(gameID);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(result.getString(Constants.MESSAGE));
		} else {
			context.response().setStatusCode(404).end(result.getString(Constants.MESSAGE));
		}
	}

	public void setPassword(final RoutingContext context) {
		final String uuidAsString = (String) context.body().asJsonObject().getValue(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final String password = context.body().asJsonObject().getString(Constants.PASSWORD);
		final GameCommandResult result = this.gameUseCase.setPassword(gameID, password);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void exitGame(final RoutingContext context) {
		final UUID gameID = UUID.fromString(context.pathParam(Constants.GAME_ID));
		final GameCommandResult result = this.gameUseCase.exitGame(gameID);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end();
		} else {
			context.response().setStatusCode(404).end(result.getString(Constants.ERROR));
		}
	}

	public void chatMessage(final RoutingContext context) {
		final JsonObject body = context.body().asJsonObject();
		LOGGER.info("chatMessage body: " + (body != null ? body.encode() : "NULL"));
		if (body == null) {
			context.response().setStatusCode(400).end(new JsonObject().put(Constants.ERROR, "Body mancante").toBuffer());
			return;
		}
		final String gameIDStr = body.getString(Constants.GAME_ID);
		final UUID gameID = gameIDStr != null ? UUID.fromString(gameIDStr) : null;
		final String author = body.getString(Constants.AUTHOR);
		final String message = body.getString("message");
		final String environment = body.getString("environment", "game");
		final GameCommandResult result = this.gameUseCase.chatMessage(gameID, author, message, environment);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void notifyGame(final RoutingContext context) {
		final String gameIDStr = context.body().asJsonObject().getString(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(gameIDStr);
		final String message = context.body().asJsonObject().getString(Constants.MESSAGE);
		final GameCommandResult result = this.gameUseCase.notifyGame(gameID, message);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end(toJson(result).toBuffer());
		} else {
			context.response().setStatusCode(404).end(toJson(result).toBuffer());
		}
	}

	public void removeUser(final RoutingContext context) {
		final String uuidAsString = (String) context.body().asJsonObject().getValue(Constants.GAME_ID);
		final UUID gameID = UUID.fromString(uuidAsString);
		final String username = context.request().getHeader("x-user-id");
		final GameCommandResult result = this.gameUseCase.removeUser(gameID, username);
		if (!result.containsKey(Constants.NOT_FOUND)) {
			context.response().end();
		} else {
			context.response().setStatusCode(404).end(result.getString(Constants.ERROR));
		}
	}
}
