package infrastructure.vertx;

import domain.port.outbound.IEventPublisher;
import domain.port.outbound.IGame;
import domain.port.outbound.IGameFactory;
import domain.port.outbound.IRulesEnginePort;
import domain.port.outbound.IUserServicePort;
import domain.valueobject.GameMode;
import domain.valueobject.User;
import domain.port.outbound.IStatisticPort;
import io.vertx.core.Vertx;
import java.util.UUID;

/**
 * Factory that creates GameVerticle instances and deploys them as Vert.x
 * verticles.
 */
public class GameVerticleFactory implements IGameFactory {
	private final Vertx vertx;
	private final IStatisticPort statisticManager;
	private final IEventPublisher eventPublisher;
	private final IRulesEnginePort rulesEngine;
	private final IUserServicePort userService;

	public GameVerticleFactory(final Vertx vertx, final IStatisticPort statisticManager,
			final IEventPublisher eventPublisher, final IRulesEnginePort rulesEngine,
			final IUserServicePort userService) {
		this.vertx = vertx;
		this.statisticManager = statisticManager;
		this.eventPublisher = eventPublisher;
		this.rulesEngine = rulesEngine;
		this.userService = userService;
	}

	@Override
	public IGame createGame(final UUID id, final User user, final int numberOfPlayers, final int expectedScore,
			final GameMode gameMode) {
		final GameVerticle verticle = new GameVerticle(id, user, numberOfPlayers, expectedScore, gameMode,
				this.statisticManager, this.eventPublisher, this.rulesEngine, this.userService);
		this.vertx.deployVerticle(verticle);
		return verticle;
	}
}
