package blackjack.ui;

import blackjack.model.Card;
import blackjack.model.Hand;

import javax.swing.*;
import java.awt.*;

public class HandPanel extends JPanel {

    private Hand   hand;
    private String label;

    public HandPanel(String label) {
        this.label = label;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setHand(Hand hand) {
        this.hand = hand;
        refresh();
    }

    public void refresh() {
        removeAll();

        JLabel title = new JLabel(buildTitle());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setAlignmentX(CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));

        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        cardsRow.setOpaque(false);

        if (hand != null) {
            for (Card card : hand.getCards()) {
                cardsRow.add(new CardComponent(card));
            }
        }
        add(cardsRow);
        revalidate();
        repaint();
    }

    private String buildTitle() {
        if (hand == null || hand.size() == 0) return label;
        int val = hand.getValue();
        return label + "  (" + val + ")";
    }
}
