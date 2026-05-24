package blackjack.ui;

import blackjack.game.BlackjackGame;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    public GameFrame() {
        super("Blackjack");

        // Ask for player name
        String name = JOptionPane.showInputDialog(
                null,
                "¡Bienvenido al Blackjack!\n\n¿Cuál es tu nombre?",
                "Blackjack",
                JOptionPane.QUESTION_MESSAGE);

        if (name == null || name.isBlank()) name = "Jugador";

        BlackjackGame game = new BlackjackGame(name.trim(), 1000);
        GamePanel panel = new GamePanel(game);

        setContentPane(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(720, 520));
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }
}
