package infrastructure.http;

import domain.port.outbound.IUserServicePort;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP adapter that updates user statistics via the API Gateway. */
public class UserServiceHttpAdapter implements IUserServicePort {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceHttpAdapter.class);
	private final Vertx vertx;
	private final String host;
	private final int port;

	public UserServiceHttpAdapter(final Vertx vertx, final String host, final int port) {
		this.vertx = vertx;
		this.host = host;
		this.port = port;
	}

	@Override
	public CompletableFuture<Void> updateStatistics(final List<String> winners, final List<String> losers) {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		final JsonArray body = new JsonArray();
		for (final String nickname : winners) {
			body.add(new JsonObject().put("nickname", nickname).put("win", true).put("cricca", 0));
		}
		for (final String nickname : losers) {
			body.add(new JsonObject().put("nickname", nickname).put("win", false).put("cricca", 0));
		}
		WebClient.create(this.vertx).post(this.port, this.host, "/statistic/bulk")
				.putHeader("Content-Type", "application/json")
				.sendBuffer(body.toBuffer(), res -> {
					if (res.failed()) {
						LOGGER.warn("Failed to update statistics via gateway: {}", res.cause().getMessage());
					}
					future.complete(null);
				});
		return future;
	}
}
