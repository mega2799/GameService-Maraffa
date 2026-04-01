package infrastructure.mongo;

import domain.valueobject.CardSuit;
import domain.valueobject.GameRecord;

/**
 * MongoDB-specific schema that extends the domain {@link GameRecord}.
 * Kept for backward compatibility with the MongoDB POJO codec.
 */
public class GameSchema extends GameRecord {

	public GameSchema() {
		super();
	}

	public GameSchema(final String identifier, final CardSuit leadingSuit) {
		super(identifier, leadingSuit);
	}
}
