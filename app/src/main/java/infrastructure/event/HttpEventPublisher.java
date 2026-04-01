package infrastructure.event;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP adapter for publishing events to the notification service. */
public class HttpEventPublisher implements domain.port.outbound.IEventPublisher {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpEventPublisher.class);
	private final Vertx vertx;
	private final String notifHost;
	private final int notifPort;

	public HttpEventPublisher(final Vertx vertx, final String notifHost, final int notifPort) {
		this.vertx = vertx;
		this.notifHost = notifHost;
		this.notifPort = notifPort;
	}

	@Override
	public void publish(final List<String> targetUsers, final String eventType, final Map<String, Object> payload) {
		final JsonObject payloadJson = new JsonObject(payload);
		final JsonObject event = new JsonObject().put("type", eventType).put("payload", payloadJson);
		final JsonObject body = new JsonObject().put("targetUsers", new JsonArray(targetUsers)).put("event", event);

		WebClient.create(this.vertx).post(this.notifPort, this.notifHost, "/notify").sendJsonObject(body, res -> {
			if (res.failed()) {
				LOGGER.warn("[HttpEventPublisher] Failed to deliver event '{}' to notification-service: {}", eventType,
						res.cause().getMessage());
			}
		});
	}
}
