package domain.port.outbound;

import domain.valueobject.GameMode;
import domain.valueobject.User;
import java.util.UUID;

/** Factory for creating game instances. */
public interface IGameFactory {

	IGame createGame(UUID id, User user, int numberOfPlayers, int expectedScore, GameMode gameMode);
}
