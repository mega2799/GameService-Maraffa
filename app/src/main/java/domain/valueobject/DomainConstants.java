package domain.valueobject;

/** Domain-specific constants used by aggregates and value objects. */
public final class DomainConstants {
	/** Total number of cards in the Maraffa deck. */
	public static final int NUMBER_OF_CARDS = 40;

	/** Score awarded for a Maraffa declaration. */
	public static final int MARAFFA_SCORE = 9;

	/** Score for an 11-to-0 round. */
	public static final int ELEVEN_ZERO_SCORE = 11;

	private DomainConstants() {
	}
}
