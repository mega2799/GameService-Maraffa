package infrastructure.http.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import infrastructure.config.Constants;
import java.util.Objects;

/** Request body for setting password. */
public class PasswordBody {
	@JsonProperty(Constants.GAME_ID)
	private String gameID;

	@JsonProperty(Constants.PASSWORD)
	private String password;

	public String getGameID() {
		return gameID;
	}

	public void setGameID(String gameID) {
		this.gameID = gameID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PasswordBody))
			return false;
		PasswordBody pwdBody = (PasswordBody) o;
		return gameID.equals(pwdBody.gameID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gameID);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
