package domain.valueobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Framework-agnostic list result for use case queries that return arrays.
 * Replaces io.vertx.core.json.JsonArray in domain boundaries.
 */
public class GameListResult {
	private final List<Map<String, Object>> items = new ArrayList<>();

	public GameListResult() {
	}

	public GameListResult add(final Map<String, Object> item) {
		this.items.add(item);
		return this;
	}

	public List<Map<String, Object>> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	public boolean isEmpty() {
		return this.items.isEmpty();
	}
}
