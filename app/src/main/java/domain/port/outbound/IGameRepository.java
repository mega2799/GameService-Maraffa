package domain.port.outbound;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for game persistence. */
public interface IGameRepository {
	Optional<IGame> findById(UUID id);

	void save(UUID id, IGame game);

	Map<UUID, IGame> getAll();

	int count();

	void remove(UUID id);
}
