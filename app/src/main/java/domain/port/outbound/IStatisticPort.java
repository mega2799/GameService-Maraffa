package domain.port.outbound;

import domain.aggregate.Trick;
import domain.valueobject.GameRecord;

/**
 * Outbound port for managing game statistics persistence.
 * Infrastructure adapters (e.g. MongoDB) implement this interface.
 */
public interface IStatisticPort {
	void createRecord(GameRecord record);

	void updateRecordWithTrick(String recordID, Trick trick);

	void updateSuit(GameRecord record);

	long getGamesCompleted();
}
