package blackjack.model;

public class Card {

    public enum Suit {
        SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣");

        private final String symbol;

        Suit(String symbol) { this.symbol = symbol; }

        public String getSymbol() { return symbol; }

        public boolean isRed() { return this == HEARTS || this == DIAMONDS; }
    }

    public enum Rank {
        TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5),
        SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9),
        TEN("10", 10), JACK("J", 10), QUEEN("Q", 10), KING("K", 10),
        ACE("A", 11);

        private final String symbol;
        private final int value;

        Rank(String symbol, int value) {
            this.symbol = symbol;
            this.value = value;
        }

        public String getSymbol() { return symbol; }
        public int getValue()     { return value; }
    }

    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;

    public Card(Suit suit, Rank rank) {
        this.suit   = suit;
        this.rank   = rank;
        this.faceUp = true;
    }

    public Suit    getSuit()   { return suit; }
    public Rank    getRank()   { return rank; }
    public boolean isFaceUp()  { return faceUp; }
    public void    setFaceUp(boolean faceUp) { this.faceUp = faceUp; }

    @Override
    public String toString() {
        return rank.getSymbol() + suit.getSymbol();
    }
}
