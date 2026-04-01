package infrastructure.http;

import com.google.common.primitives.Booleans;
import domain.port.outbound.IRulesEnginePort;
import domain.valueobject.RoundStartResult;
import domain.valueobject.ScoreComputeResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP adapter for the rules engine service. */
public class RulesEngineHttpAdapter implements IRulesEnginePort {
	private static final Logger LOGGER = LoggerFactory.getLogger(RulesEngineHttpAdapter.class);
	private final Vertx vertx;
	private final String host;
	private final int port;

	public RulesEngineHttpAdapter(final Vertx vertx, final String host, final int port) {
		this.vertx = vertx;
		this.host = host;
		this.port = port;
	}

	@Override
	public CompletableFuture<RoundStartResult> startRound(final UUID gameId, final int numberOfPlayers) {
		final CompletableFuture<RoundStartResult> future = new CompletableFuture<>();
		WebClient.create(this.vertx).get(this.port, this.host, "/games/startRound")
				.putHeader("Accept", "application/json").as(BodyCodec.jsonObject()).send(handler -> {
					if (handler.succeeded()) {
						final JsonObject body = handler.result().body();
						final var deck = body.getJsonArray("deck").stream().map(el -> (Integer) el).toList();
						final int firstPlayer = body.getInteger("firstPlayer");
						future.complete(RoundStartResult.success(deck, firstPlayer));
					} else {
						LOGGER.error("startRound failed: " + handler.cause().getMessage());
						future.complete(RoundStartResult.failure(handler.cause().getMessage()));
					}
				});
		return future;
	}

	@Override
	public CompletableFuture<ScoreComputeResult> computeScore(final int[] cards, final int[] teamACards,
			final Map<String, String> users, final String trump, final String mode,
			final List<Boolean> isSuitFinishedList, final UUID gameId, final int turn) {
		final boolean[] isSuitFinished = Booleans.toArray(isSuitFinishedList);
		final JsonObject body = new JsonObject().put("trick", cards).put("trump", Integer.parseInt(trump))
				.put("teamACards", teamACards).put("mode", mode).put("isSuitFinished", isSuitFinished);
		final CompletableFuture<ScoreComputeResult> future = new CompletableFuture<>();
		WebClient.create(this.vertx).post(this.port, this.host, "/games/computeScore")
				.putHeader("Accept", "application/json").as(BodyCodec.jsonObject()).sendJsonObject(body, handler -> {
					if (handler.succeeded()) {
						final JsonObject res = handler.result().body();
						future.complete(ScoreComputeResult.success(res.getInteger("score"),
								res.getBoolean("firstTeam"), res.getInteger("winningPosition")));
					} else {
						LOGGER.error("computeScore failed: " + handler.cause().getMessage());
						future.complete(ScoreComputeResult.failure(handler.cause().getMessage()));
					}
				});
		return future;
	}

	@Override
	public CompletableFuture<Boolean> checkMaraffa(final int[] userCards, final int suit, final int value,
			final int trump) {
		final JsonObject body = new JsonObject().put("trump", trump).put("deck", userCards).put("value", value)
				.put("suit", suit);
		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		WebClient.create(this.vertx).post(this.port, this.host, "/games/checkMaraffa")
				.putHeader("Accept", "application/json").as(BodyCodec.jsonObject()).sendJsonObject(body, handler -> {
					if (handler.succeeded()) {
						future.complete(handler.result().body().getBoolean("maraffa", false));
					} else {
						LOGGER.error("checkMaraffa failed: " + handler.cause().getMessage());
						future.complete(false);
					}
				});
		return future;
	}

	@Override
	public CompletableFuture<Boolean> validateCard(final int[] trick, final int card, final int[] userCards,
			final boolean isCardTrump) {
		final JsonObject body = new JsonObject().put("trick", trick).put("card", card).put("userCards", userCards)
				.put("cardIsTrump", isCardTrump);
		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		WebClient.create(this.vertx).post(this.port, this.host, "/games/playCard-validation")
				.putHeader("Accept", "application/json").as(BodyCodec.jsonObject()).sendJsonObject(body, handler -> {
					if (handler.succeeded()) {
						future.complete(handler.result().body().getBoolean("valid", false));
					} else {
						LOGGER.error("validateCard failed: " + handler.cause().getMessage());
						future.complete(false);
					}
				});
		return future;
	}
}
