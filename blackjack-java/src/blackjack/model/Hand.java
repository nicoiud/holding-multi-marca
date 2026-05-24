package blackjack.model;

import java.util.ArrayList;
import java.util.List;

public class Hand {

    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
    }

    public void clear() {
        cards.clear();
    }

    public List<Card> getCards() {
        return cards;
    }

    public int getValue() {
        int total = 0;
        int aces  = 0;

        for (Card card : cards) {
            if (!card.isFaceUp()) continue;
            total += card.getRank().getValue();
            if (card.getRank() == Card.Rank.ACE) aces++;
        }

        // Reduce aces from 11 to 1 while busted
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public int getTotalValue() {
        int total = 0;
        int aces  = 0;

        for (Card card : cards) {
            total += card.getRank().getValue();
            if (card.getRank() == Card.Rank.ACE) aces++;
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public boolean isBusted()    { return getValue() > 21; }
    public boolean isBlackjack() { return cards.size() == 2 && getTotalValue() == 21; }
    public int     size()        { return cards.size(); }
}
