package infrastructure.config;

import application.service.GameService;
import domain.port.outbound.IEventPublisher;
import domain.port.outbound.IGameFactory;
import domain.port.outbound.IGameRepository;
import domain.port.outbound.IRulesEnginePort;
import domain.port.outbound.IUserServicePort;
import infrastructure.event.HttpEventPublisher;
import infrastructure.http.GameHttpAdapter;
import infrastructure.http.RulesEngineHttpAdapter;
import infrastructure.http.UserServiceHttpAdapter;
import infrastructure.inmemory.InMemoryGameRepository;
import domain.port.outbound.IStatisticPort;
import infrastructure.mongo.MongoStatisticManager;
import infrastructure.vertx.GameVerticleFactory;
import infrastructure.vertx.RouterConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the server of the application. Reading
 * and using the enviroment variables and start the application.
 */
public class AppServer extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppServer.class);
	private final int port = Integer.parseInt(System.getenv().getOrDefault("MIDDLEWARE_PORT", "3003"));
	private HttpServer server;

	public AppServer() {
	}

	@Override
	public void start() throws Exception {
		final IStatisticPort mongoStatisticManager = new MongoStatisticManager(
				System.getenv().getOrDefault("MONGO_USER", "admin"),
				System.getenv().getOrDefault("MONGO_PASSWORD", "adminpassword"),
				System.getenv().getOrDefault("MONGO_HOST", "localhost"),
				Integer.parseInt(System.getenv().getOrDefault("MONGO_PORT", "27017")),
				System.getenv().getOrDefault("MONGO_DATABASE", "MaraffaStatisticsDB"));
		final String gatewayHost = System.getenv().getOrDefault("API_GATEWAY_HOST", "localhost");
		final int gatewayPort = Integer.parseInt(System.getenv().getOrDefault("API_GATEWAY_PORT", "8080"));
		final String notifHost = System.getenv().getOrDefault("NOTIFICATION_HOST", gatewayHost);
		final int notifPort = Integer.parseInt(System.getenv().getOrDefault("NOTIFICATION_PORT", String.valueOf(gatewayPort)));
		final IEventPublisher eventPublisher = new HttpEventPublisher(this.vertx, notifHost, notifPort);
		final String blHost = System.getenv().getOrDefault("BUSINESS_LOGIC_HOST", "localhost");
		final int blPort = Integer.parseInt(System.getenv().getOrDefault("BUSINESS_LOGIC_PORT", "3000"));
		final IRulesEnginePort rulesEngine = new RulesEngineHttpAdapter(this.vertx, blHost, blPort);
		final infrastructure.mongo.MongoGameRepository mongoGameRepo = new infrastructure.mongo.MongoGameRepository(
				System.getenv().getOrDefault("MONGO_USER", "admin"),
				System.getenv().getOrDefault("MONGO_PASSWORD", "adminpassword"),
				System.getenv().getOrDefault("MONGO_HOST", "localhost"),
				Integer.parseInt(System.getenv().getOrDefault("MONGO_PORT", "27017")),
				System.getenv().getOrDefault("MONGO_DATABASE", "MaraffaStatisticsDB"));
		final IGameRepository gameRepository = new InMemoryGameRepository(mongoGameRepo);
		final IUserServicePort userService = new UserServiceHttpAdapter(this.vertx, gatewayHost, gatewayPort);
		final IGameFactory gameFactory = new GameVerticleFactory(this.vertx, mongoStatisticManager, eventPublisher,
				rulesEngine, userService);
		final GameService gameService = new GameService(gameFactory, mongoStatisticManager, eventPublisher,
				gameRepository, rulesEngine, userService);
		final GameHttpAdapter gameHttpAdapter = new GameHttpAdapter(gameService);
		final RouterConfig routerConfig = new RouterConfig(this.port, gameHttpAdapter);
		this.server = this.vertx.createHttpServer(this.createOptions());
		this.server.requestHandler(routerConfig.configurationRouter(this.vertx));
		this.server.listen(res -> {
			{
				if (res.succeeded()) {
					LOGGER.info("Server is now listening!");
					LOGGER.info("PORT: " + this.port);
				} else {
					LOGGER.error("Failed to bind!");
				}
			}
		});
	}

	private HttpServerOptions createOptions() {
		final HttpServerOptions options = new HttpServerOptions();
		options.setPort(this.port);
		return options;
	}
}
