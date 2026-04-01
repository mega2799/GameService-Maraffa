package infrastructure.inmemory;

import domain.port.outbound.IGame;
import domain.port.outbound.IGameRepository;
import infrastructure.mongo.MongoGameRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory game repository with optional MongoDB backing. */
public class InMemoryGameRepository implements IGameRepository {
	private final Map<UUID, IGame> games = new ConcurrentHashMap<>();
	private final MongoGameRepository mongoRepo;

	public InMemoryGameRepository() {
		this.mongoRepo = null;
	}

	public InMemoryGameRepository(final MongoGameRepository mongoRepo) {
		this.mongoRepo = mongoRepo;
	}

	@Override
	public Optional<IGame> findById(final UUID id) {
		return Optional.ofNullable(this.games.get(id));
	}

	@Override
	public void save(final UUID id, final IGame game) {
		this.games.put(id, game);
		if (this.mongoRepo != null) {
			final var snapshot = game.toMap();
			new Thread(() -> this.mongoRepo.upsert(id, snapshot)).start();
		}
	}

	@Override
	public Map<UUID, IGame> getAll() {
		return this.games;
	}

	@Override
	public int count() {
		return this.games.size();
	}

	@Override
	public void remove(final UUID id) {
		this.games.remove(id);
		if (this.mongoRepo != null) {
			new Thread(() -> this.mongoRepo.delete(id)).start();
		}
	}
}
