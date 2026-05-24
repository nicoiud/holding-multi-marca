package blackjack.ui;

import blackjack.model.Card;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CardComponent extends JComponent {

    private static final int W = 80;
    private static final int H = 110;

    private final Card card;

    public CardComponent(Card card) {
        this.card = card;
        setPreferredSize(new Dimension(W, H));
        setMinimumSize(new Dimension(W, H));
        setMaximumSize(new Dimension(W, H));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        RoundRectangle2D rect = new RoundRectangle2D.Float(1, 1, W - 2, H - 2, 12, 12);

        if (!card.isFaceUp()) {
            drawBack(g2, rect);
        } else {
            drawFace(g2, rect);
        }
        g2.dispose();
    }

    private void drawFace(Graphics2D g2, RoundRectangle2D rect) {
        // White fill + shadow
        g2.setColor(new Color(30, 30, 30, 60));
        g2.fill(new RoundRectangle2D.Float(3, 3, W - 2, H - 2, 12, 12));

        g2.setColor(Color.WHITE);
        g2.fill(rect);
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(rect);

        Color textColor = card.getSuit().isRed()
                ? new Color(200, 20, 20)
                : new Color(20, 20, 20);

        String rankStr = card.getRank().getSymbol();
        String suitStr = card.getSuit().getSymbol();

        // Top-left corner
        g2.setColor(textColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(rankStr, 6, 16);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(suitStr, 6, 28);

        // Bottom-right corner (upside-down)
        g2.rotate(Math.PI, W / 2.0, H / 2.0);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(rankStr, 6, 16);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(suitStr, 6, 28);
        g2.rotate(-Math.PI, W / 2.0, H / 2.0);

        // Center suit
        g2.setFont(new Font("SansSerif", Font.PLAIN, 28));
        FontMetrics fm = g2.getFontMetrics();
        int cx = (W - fm.stringWidth(suitStr)) / 2;
        int cy = (H + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(suitStr, cx, cy);
    }

    private void drawBack(Graphics2D g2, RoundRectangle2D rect) {
        g2.setColor(new Color(30, 30, 30, 60));
        g2.fill(new RoundRectangle2D.Float(3, 3, W - 2, H - 2, 12, 12));

        // Background
        g2.setColor(new Color(15, 50, 120));
        g2.fill(rect);

        // Pattern
        g2.setColor(new Color(10, 35, 90));
        for (int i = -H; i < W + H; i += 8) {
            g2.drawLine(i, 0, i + H, H);
        }

        // Inner border
        g2.setColor(new Color(200, 170, 50));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(5, 5, W - 10, H - 10, 8, 8));

        // Outer border
        g2.setColor(new Color(180, 150, 30));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(rect);
    }
}
