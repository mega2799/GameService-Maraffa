package domain.port.outbound;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Outbound port for user service communication. */
public interface IUserServicePort {
	CompletableFuture<Void> updateStatistics(List<String> winners, List<String> losers);
}
