package domain.aggregate;

import domain.valueobject.Call;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import java.util.List;
import java.util.Map;

/** Interface representing a trick in a card game. */
public interface Trick {
	void addCard(Card<CardValue, CardSuit> card, String username);

	boolean isCompleted();

	Map<String, String> getCardsAndUsers();

	List<String> getCards();

	void setCall(Call call, String username);

	Call getCall();
}
