package blackjack.model;

public class Player {

    private final String name;
    private int chips;
    private int currentBet;
    private final Hand hand;

    public Player(String name, int startingChips) {
        this.name       = name;
        this.chips      = startingChips;
        this.currentBet = 0;
        this.hand       = new Hand();
    }

    public boolean placeBet(int amount) {
        if (amount <= 0 || amount > chips) return false;
        currentBet = amount;
        chips -= amount;
        return true;
    }

    public void winBet()     { chips += currentBet * 2; currentBet = 0; }
    public void pushBet()    { chips += currentBet;     currentBet = 0; }
    public void loseBet()    { currentBet = 0; }

    public void winBlackjack() {
        // 3:2 payout
        chips += currentBet + (int)(currentBet * 1.5);
        currentBet = 0;
    }

    public String getName()      { return name; }
    public int    getChips()     { return chips; }
    public int    getCurrentBet(){ return currentBet; }
    public Hand   getHand()      { return hand; }
}
