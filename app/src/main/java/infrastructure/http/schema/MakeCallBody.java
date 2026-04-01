package infrastructure.http.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import infrastructure.config.Constants;

/** Request body for making a call. */
public class MakeCallBody {
	@JsonProperty(Constants.GAME_ID)
	private String gameID;

	@JsonProperty(Constants.CALL)
	private String call;

	@JsonProperty(Constants.USERNAME)
	private String username;
}
