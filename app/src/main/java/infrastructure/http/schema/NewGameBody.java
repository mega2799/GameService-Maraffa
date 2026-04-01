package infrastructure.http.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import infrastructure.config.Constants;
import java.util.Objects;

/** Request body for new game. */
public class NewGameBody {
	@JsonProperty(Constants.GAME_ID)
	private String gameID;

	public String getGameID() {
		return gameID;
	}

	public void setGameID(String gameID) {
		this.gameID = gameID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NewGameBody)) {
			return false;
		}
		NewGameBody newGameBody = (NewGameBody) o;
		return gameID.equals(newGameBody.gameID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gameID);
	}
}
