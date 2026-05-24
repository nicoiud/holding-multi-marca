package blackjack.ui;

import blackjack.game.BlackjackGame;
import blackjack.game.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GamePanel extends JPanel {

    private final BlackjackGame game;

    // ── Hand panels ──────────────────────────────────────────────────────────
    private final HandPanel dealerPanel = new HandPanel("Crupier");
    private final HandPanel playerPanel = new HandPanel("Tu mano");

    // ── Controls ─────────────────────────────────────────────────────────────
    private final JLabel  messageLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel  chipsLabel   = new JLabel("", SwingConstants.CENTER);

    private final JButton hitButton    = createButton("Pedir carta");
    private final JButton standButton  = createButton("Plantarse");
    private final JButton dealButton   = createButton("Nueva ronda");

    private final JSpinner betSpinner;

    public GamePanel(BlackjackGame game) {
        this.game = game;

        SpinnerNumberModel betModel = new SpinnerNumberModel(25, 5, 500, 5);
        betSpinner = new JSpinner(betModel);
        betSpinner.setMaximumSize(new Dimension(80, 28));
        betSpinner.setPreferredSize(new Dimension(80, 28));

        buildLayout();
        wireActions();
        refresh();
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private void buildLayout() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(0, 100, 0));

        // ── Table area ───────────────────────────────────────────────────────
        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        table.setBorder(new EmptyBorder(20, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 0.5;
        table.add(dealerPanel, gbc);

        gbc.gridy = 1;
        table.add(playerPanel, gbc);

        add(table, BorderLayout.CENTER);

        // ── Message ──────────────────────────────────────────────────────────
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setBorder(new EmptyBorder(4, 0, 4, 0));

        chipsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chipsLabel.setForeground(new Color(255, 220, 80));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0, 70, 0));
        topBar.setBorder(new EmptyBorder(6, 16, 6, 16));
        topBar.add(messageLabel, BorderLayout.CENTER);
        topBar.add(chipsLabel,   BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Bottom controls ──────────────────────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        controls.setBackground(new Color(0, 70, 0));

        JLabel betLabel = new JLabel("Apuesta: $");
        betLabel.setForeground(Color.WHITE);
        betLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        controls.add(betLabel);
        controls.add(betSpinner);
        controls.add(dealButton);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(hitButton);
        controls.add(standButton);

        add(controls, BorderLayout.SOUTH);
    }

    private static JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(30, 130, 30));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 160, 60), 1),
                new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void wireActions() {
        dealButton.addActionListener((ActionEvent e) -> {
            if (game.getState() == GameState.BETTING) {
                int bet = (Integer) betSpinner.getValue();
                game.placeBet(bet);
            } else {
                game.newRound();
            }
            refresh();
        });

        hitButton.addActionListener((ActionEvent e) -> {
            game.hit();
            refresh();
        });

        standButton.addActionListener((ActionEvent e) -> {
            game.stand();
            refresh();
        });
    }

    // ── State sync ───────────────────────────────────────────────────────────

    public void refresh() {
        dealerPanel.setHand(game.getDealer().getHand());
        playerPanel.setHand(game.getPlayer().getHand());

        messageLabel.setText(game.getMessage());
        chipsLabel.setText("Fichas: $" + game.getPlayer().getChips()
                + "   Apuesta: $" + game.getPlayer().getCurrentBet());

        boolean playerTurn  = game.getState() == GameState.PLAYER_TURN;
        boolean roundOver   = game.getState() == GameState.ROUND_OVER;
        boolean betting     = game.getState() == GameState.BETTING;

        hitButton.setEnabled(playerTurn);
        standButton.setEnabled(playerTurn);
        betSpinner.setEnabled(betting);
        dealButton.setText(betting ? "Repartir" : "Nueva ronda");
        dealButton.setEnabled(betting || roundOver);

        // Highlight deal button when round over
        if (roundOver) {
            dealButton.setBackground(new Color(200, 150, 0));
        } else {
            dealButton.setBackground(new Color(30, 130, 30));
        }
    }
}
