package domain.aggregate;

import domain.valueobject.User;
import java.util.List;
import java.util.Map;

/** A record modelling the concept of "team" */
public record Team(List<User> players, String nameOfTeam, Integer score, Integer currentScore) {

	/** Framework-agnostic serialization for crossing boundaries. */
	public Map<String, Object> toMap() {
		return Map.of("players", this.players.stream().map(User::username).toList(), "nameOfTeam", this.nameOfTeam,
				"score", this.score, "currentScore", this.currentScore);
	}

	@Override
	public List<User> players() {
		return this.players;
	}

	@Override
	public String nameOfTeam() {
		return this.nameOfTeam;
	}

	@Override
	public Integer score() {
		return this.score;
	}

	@Override
	public Integer currentScore() {
		return this.currentScore;
	}

	@Override
	public String toString() {
		return "Team{" + "players=" + this.players.stream().map(User::username).toList() + ", nameOfTeam='"
				+ this.nameOfTeam + '\'' + ", score='" + this.score + '\'' + ", currentScore='" + this.currentScore
				+ '\'' + +'}';
	}
}
