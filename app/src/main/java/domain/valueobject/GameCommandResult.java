package domain.valueobject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Framework-agnostic result object for use case commands.
 * Replaces io.vertx.core.json.JsonObject in domain boundaries
 * to respect the Dependency Rule (domain must not know about infrastructure).
 */
public class GameCommandResult {
	private final Map<String, Object> data = new LinkedHashMap<>();

	public GameCommandResult() {
	}

	public GameCommandResult put(final String key, final Object value) {
		this.data.put(key, value);
		return this;
	}

	public Object get(final String key) {
		return this.data.get(key);
	}

	public String getString(final String key) {
		final Object v = this.data.get(key);
		return v != null ? v.toString() : null;
	}

	public Boolean getBoolean(final String key) {
		final Object v = this.data.get(key);
		return v instanceof Boolean ? (Boolean) v : null;
	}

	public boolean getBoolean(final String key, final boolean defaultValue) {
		final Object v = this.data.get(key);
		return v instanceof Boolean ? (Boolean) v : defaultValue;
	}

	public Integer getInteger(final String key) {
		final Object v = this.data.get(key);
		return v instanceof Number ? ((Number) v).intValue() : null;
	}

	public boolean containsKey(final String key) {
		return this.data.containsKey(key);
	}

	public Map<String, Object> toMap() {
		return Collections.unmodifiableMap(this.data);
	}
}
