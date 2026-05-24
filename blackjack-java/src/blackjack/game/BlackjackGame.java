package blackjack.game;

import blackjack.model.Card;
import blackjack.model.Dealer;
import blackjack.model.Deck;
import blackjack.model.Player;

public class BlackjackGame {

    private final Deck   deck   = new Deck();
    private final Player player;
    private final Dealer dealer = new Dealer();
    private GameState    state  = GameState.BETTING;
    private String       message = "Coloca tu apuesta para comenzar.";

    public BlackjackGame(String playerName, int startingChips) {
        this.player = new Player(playerName, startingChips);
    }

    // ── Betting ──────────────────────────────────────────────────────────────

    public boolean placeBet(int amount) {
        if (state != GameState.BETTING) return false;
        if (!player.placeBet(amount)) {
            message = "Apuesta inválida. Tienes $" + player.getChips() + ".";
            return false;
        }
        dealInitialCards();
        return true;
    }

    private void dealInitialCards() {
        player.getHand().clear();
        dealer.getHand().clear();

        player.getHand().addCard(deck.deal());
        dealer.getHand().addCard(deck.deal());
        player.getHand().addCard(deck.deal());

        Card holeCard = deck.deal();
        holeCard.setFaceUp(false);
        dealer.getHand().addCard(holeCard);

        state = GameState.PLAYER_TURN;

        if (player.getHand().isBlackjack()) {
            dealer.revealHoleCard();
            if (dealer.getHand().isBlackjack()) {
                message = "¡Empate! Los dos tienen Blackjack.";
                player.pushBet();
            } else {
                message = "¡BLACKJACK! ¡Ganaste 3 a 2!";
                player.winBlackjack();
            }
            state = GameState.ROUND_OVER;
        } else {
            message = "Tu turno: pide carta o plántate.";
        }
    }

    // ── Player actions ───────────────────────────────────────────────────────

    public void hit() {
        if (state != GameState.PLAYER_TURN) return;
        player.getHand().addCard(deck.deal());

        if (player.getHand().isBusted()) {
            dealer.revealHoleCard();
            message = "¡Te pasaste! Perdiste $" + player.getCurrentBet() + ".";
            player.loseBet();
            state = GameState.ROUND_OVER;
        } else if (player.getHand().getValue() == 21) {
            stand();
        } else {
            message = "Tu valor: " + player.getHand().getValue() + ". ¿Pides carta o te plantas?";
        }
    }

    public void stand() {
        if (state != GameState.PLAYER_TURN) return;
        state = GameState.DEALER_TURN;
        dealer.revealHoleCard();
        playDealerTurn();
    }

    private void playDealerTurn() {
        while (dealer.shouldHit()) {
            dealer.getHand().addCard(deck.deal());
        }
        resolveRound();
    }

    private void resolveRound() {
        int playerVal = player.getHand().getValue();
        int dealerVal = dealer.getHand().getValue();

        if (dealer.getHand().isBusted()) {
            message = "¡El crupier se pasó! ¡Ganaste $" + player.getCurrentBet() + "!";
            player.winBet();
        } else if (playerVal > dealerVal) {
            message = "¡Ganaste! " + playerVal + " vs " + dealerVal + ".";
            player.winBet();
        } else if (playerVal < dealerVal) {
            message = "Perdiste. " + playerVal + " vs " + dealerVal + ".";
            player.loseBet();
        } else {
            message = "¡Empate! " + playerVal + " vs " + dealerVal + ".";
            player.pushBet();
        }
        state = GameState.ROUND_OVER;
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    public void newRound() {
        if (player.getChips() == 0) {
            message = "¡Sin fichas! Iniciando nueva partida...";
            // Reset chips but keep player name
        }
        state   = GameState.BETTING;
        message = "Coloca tu apuesta para comenzar.";
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Player    getPlayer()  { return player; }
    public Dealer    getDealer()  { return dealer; }
    public GameState getState()   { return state; }
    public String    getMessage() { return message; }
}
