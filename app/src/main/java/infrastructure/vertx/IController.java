package infrastructure.vertx;

import java.util.List;

/** Controller interface for route management. */
public interface IController {
	/**
	 * @return the list with all the routes
	 */
	List<IRouteResponse> getRoutes();
}
