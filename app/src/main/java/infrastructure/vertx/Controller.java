package infrastructure.vertx;

import infrastructure.config.Constants;
import infrastructure.http.GameHttpAdapter;
import io.vertx.core.http.HttpMethod;
import java.util.ArrayList;
import java.util.List;

/** This class is responsible for managing all the routes of the application. */
public class Controller implements IController {

	private final GameHttpAdapter entityService;
	private final List<IRouteResponse> routes = new ArrayList<>();

	public Controller(final GameHttpAdapter entityService) {
		this.entityService = entityService;
		this.addRoutes();
	}

	/** Add all Maraffa's routes */
	private void addRoutes() {
		this.routes
				.add(new RouteResponse(HttpMethod.POST, "/" + Constants.CREATE_GAME, this.entityService::createGame));
		this.routes.add(new RouteResponse(HttpMethod.PATCH, "/" + Constants.JOIN_GAME, this.entityService::joinGame));
		this.routes.add(new RouteResponse(HttpMethod.POST, "/" + Constants.PLAY_CARD, this.entityService::playCard));
		this.routes.add(new RouteResponse(HttpMethod.GET, "/" + Constants.CAN_START, this.entityService::canStart));
		this.routes.add(new RouteResponse(HttpMethod.PATCH, "/" + Constants.START_GAME, this.entityService::startGame));
		this.routes
				.add(new RouteResponse(HttpMethod.POST, "/" + Constants.CHOOSE_TRUMP, this.entityService::chooseTrump));
		this.routes.add(new RouteResponse(HttpMethod.PATCH, "/" + Constants.START_NEW_ROUND,
				this.entityService::startNewRound));
		this.routes
				.add(new RouteResponse(HttpMethod.PATCH, "/" + Constants.CHANGE_TEAM, this.entityService::changeTeam));
		this.routes.add(
				new RouteResponse(HttpMethod.GET, "/" + Constants.PLAYER_CARDS, this.entityService::getPlayerCard));
		this.routes.add(new RouteResponse(HttpMethod.GET, "/" + Constants.STATE, this.entityService::getState));
		this.routes.add(new RouteResponse(HttpMethod.GET, "/" + Constants.END_GAME, this.entityService::isGameEnded));
		this.routes.add(new RouteResponse(HttpMethod.POST, "/" + Constants.MAKE_CALL, this.entityService::makeCall));
		this.routes.add(new RouteResponse(HttpMethod.GET, "/" + Constants.GAMES, this.entityService::getGames));
		this.routes.add(new RouteResponse(HttpMethod.POST, "/" + Constants.NEW_GAME, this.entityService::newGame));
		this.routes.add(
				new RouteResponse(HttpMethod.PATCH, "/" + Constants.SET_PASSWORD, this.entityService::setPassword));
		this.routes
				.add(new RouteResponse(HttpMethod.PATCH, "/" + Constants.REMOVE_USER, this.entityService::removeUser));
		this.routes.add(new RouteResponse(HttpMethod.GET, "/" + Constants.GET_PLAYERS, this.entityService::getPlayers));
		this.routes.add(
				new RouteResponse(HttpMethod.GET, "/" + Constants.GET_TOTAL_GAMES, this.entityService::getCountGames));
		this.routes.add(new RouteResponse(HttpMethod.GET, "/" + Constants.GETGAME, this.entityService::getGame));
		this.routes.add(new RouteResponse(HttpMethod.DELETE, "/" + Constants.GETGAME, this.entityService::exitGame));
		this.routes.add(new RouteResponse(HttpMethod.POST, "/" + Constants.CHAT, this.entityService::chatMessage));
		this.routes.add(new RouteResponse(HttpMethod.POST, "/" + Constants.NOTIFY, this.entityService::notifyGame));
	}

	public List<IRouteResponse> getRoutes() {
		return this.routes;
	}
}
