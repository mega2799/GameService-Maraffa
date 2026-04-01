package infrastructure.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MongoDB-backed game repository. */
public class MongoGameRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(MongoGameRepository.class);
	private static final String GAMES_COLLECTION = "games";
	private final MongoCollection<Document> collection;

	public MongoGameRepository(final String user, final String password, final String host, final int port,
			final String database) {
		final String uri = "mongodb://" + user + ":" + password + "@" + host + ":" + port;
		MongoCollection<Document> col = null;
		try {
			final MongoClient mongoClient = MongoClients.create(uri);
			col = mongoClient.getDatabase(database).getCollection(GAMES_COLLECTION);
			LOGGER.info("MongoGameRepository connected to " + host + ":" + port);
		} catch (final Exception e) {
			LOGGER.error("MongoGameRepository init failed: " + e.getMessage());
		}
		this.collection = col;
	}

	public void upsert(final UUID id, final Map<String, Object> gameMap) {
		if (this.collection == null) {
			return;
		}
		try {
			final Document doc = new Document(gameMap);
			doc.put("_id", id.toString());
			this.collection.replaceOne(Filters.eq("_id", id.toString()), doc, new ReplaceOptions().upsert(true));
		} catch (final Exception e) {
			LOGGER.warn("Failed to upsert game " + id + ": " + e.getMessage());
		}
	}

	public void delete(final UUID id) {
		if (this.collection == null) {
			return;
		}
		try {
			this.collection.deleteOne(Filters.eq("_id", id.toString()));
		} catch (final Exception e) {
			LOGGER.warn("Failed to delete game " + id + ": " + e.getMessage());
		}
	}

	public long count() {
		if (this.collection == null) {
			return 0;
		}
		try {
			return this.collection.countDocuments();
		} catch (final Exception e) {
			return 0;
		}
	}
}
