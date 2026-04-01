package domain.aggregate;

import domain.valueobject.Call;
import domain.valueobject.Card;
import domain.valueobject.CardSuit;
import domain.valueobject.CardValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Implementation of the Trick interface. */
public class TrickImpl implements Trick {
	private transient final Map<String, Card<CardValue, CardSuit>> cards = new LinkedHashMap<>(); // username -> card

	private final AtomicInteger currentUser;

	private Call call = Call.NONE;
	private transient final int numberOfPlayers;

	private transient final CardSuit trump;

	public TrickImpl(final int numberOfPlayers, final CardSuit trump) {
		this.numberOfPlayers = numberOfPlayers;
		this.trump = trump;
		this.currentUser = new AtomicInteger(0);
	}

	@Override
	public void addCard(final Card<CardValue, CardSuit> card, final String username) {
		this.cards.put(username, card);
	}

	public Call getCall() {
		return this.call;
	}

	@Override
	public void setCall(final Call call, final String username) {
		this.call = call;
	}

	public List<String> getCards() {
		return this.cards.values().stream().map(card -> String.valueOf(card.getCardValue())).toList();
	}

	@Override
	public Map<String, String> getCardsAndUsers() {
		final Map<String, String> result = new java.util.LinkedHashMap<>();
		this.cards.forEach((username, card) -> result.put(username, String.valueOf(card.getCardValue())));
		return result;
	}

	public int getNumberOfPlayers() {
		return this.numberOfPlayers;
	}

	public CardSuit getTrump() {
		return this.trump;
	}

	@Override
	public boolean isCompleted() {
		return this.cards.keySet().size() == this.numberOfPlayers;
	}

	@Override
	public String toString() {
		return "Trick{" + "cards=" + this.cards + ", trump=" + this.trump + ", call=" + this.call + '}';
	}
}
