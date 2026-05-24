package blackjack.model;

public class Dealer {

    private final Hand hand = new Hand();

    public Hand getHand() { return hand; }

    /** Returns true while the dealer must keep drawing (soft 17 rule). */
    public boolean shouldHit() {
        return hand.getValue() < 17;
    }

    /** Flips the face-down hole card. */
    public void revealHoleCard() {
        for (Card card : hand.getCards()) {
            card.setFaceUp(true);
        }
    }
}
