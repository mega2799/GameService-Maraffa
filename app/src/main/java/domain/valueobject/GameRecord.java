package domain.valueobject;

import domain.aggregate.Trick;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Domain value object representing the statistical record of a game round. */
public class GameRecord {
	private String gameID;
	private CardSuit leadingSuit;
	private Date date;
	private List<Trick> tricks;

	public GameRecord() {
	}

	public GameRecord(final String identifier, final CardSuit leadingSuit) {
		this.gameID = identifier;
		this.leadingSuit = leadingSuit;
		this.tricks = new ArrayList<>();
		this.date = new Date();
	}

	public String getGameID() {
		return this.gameID;
	}

	public void setGameID(final String gameID) {
		this.gameID = gameID;
	}

	public CardSuit getTrump() {
		return this.leadingSuit;
	}

	public void setTrump(final CardSuit leadingSuit) {
		this.leadingSuit = leadingSuit;
	}

	public Date getDate() {
		return this.date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public List<Trick> getTricks() {
		return this.tricks;
	}

	public void addTrick(final Trick trick) {
		this.tricks.add(trick);
	}

	@Override
	public String toString() {
		return "GameRecord [gameID=" + this.gameID + ", tricks=" + this.tricks + "]";
	}
}
