package trabalhofinal.difusaocalor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/**
 * Painel simples que desenha um gráfico de barras comparando tempos por repetição
 * entre sequencial e distribuído.
 */
public class BenchmarkChartPanel extends JPanel {

    private final List<Double> seqRuns;
    private final List<Double> distRuns;

    public BenchmarkChartPanel(List<Double> seqRuns, List<Double> distRuns) {
        this.seqRuns = seqRuns;
        this.distRuns = distRuns;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int margin = 40;

        // compute max value
        double max = 0.0;
        if (seqRuns != null) {
            for (double v : seqRuns)
                max = Math.max(max, v);
        }
        if (distRuns != null) {
            for (double v : distRuns)
                max = Math.max(max, v);
        }
        if (max <= 0)
            max = 1.0;

        int availableW = w - 2 * margin;
        int availableH = h - 2 * margin;

        int repeats = Math.max(seqRuns == null ? 0 : seqRuns.size(), distRuns == null ? 0 : distRuns.size());
        if (repeats == 0) {
            g2.setColor(Color.GRAY);
            g2.drawString("Sem dados de execução", margin, margin + 20);
            g2.dispose();
            return;
        }

        int groupWidth = availableW / repeats;
        int barWidth = Math.max(6, groupWidth / 3);

        // draw y grid lines
        g2.setColor(new Color(220, 220, 220));
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int yy = margin + (int) (availableH * (1.0 - (double) i / gridLines));
            g2.drawLine(margin, yy, w - margin, yy);
            String label = String.format("%.2fs", max * i / gridLines);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(label, 4, yy + 4);
            g2.setColor(new Color(220, 220, 220));
        }

        // draw bars
        for (int i = 0; i < repeats; i++) {
            int gx = margin + i * groupWidth;
            // seq bar (left)
            if (seqRuns != null && i < seqRuns.size()) {
                double v = seqRuns.get(i);
                int bh = (int) (availableH * (v / max));
                int x = gx + (groupWidth / 2) - barWidth - 2;
                int y = margin + (availableH - bh);
                g2.setColor(new Color(100, 149, 237)); // cornflower blue
                g2.fillRect(x, y, barWidth, bh);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("%.2f", v), x, y - 4);
            }

            // dist bar (right)
            if (distRuns != null && i < distRuns.size()) {
                double v = distRuns.get(i);
                int bh = (int) (availableH * (v / max));
                int x = gx + (groupWidth / 2) + 2;
                int y = margin + (availableH - bh);
                g2.setColor(new Color(255, 165, 0)); // orange
                g2.fillRect(x, y, barWidth, bh);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("%.2f", v), x, y - 4);
            }

            // x label
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            String label = "#" + (i + 1);
            int lx = gx + groupWidth / 2 - 8;
            g2.drawString(label, lx, margin + availableH + 16);
        }

        // legend
        int lx = w - margin - 120;
        int ly = margin - 10;
        g2.setColor(new Color(100, 149, 237));
        g2.fillRect(lx, ly, 12, 12);
        g2.setColor(Color.BLACK);
        g2.drawString("Sequencial", lx + 16, ly + 12);

        g2.setColor(new Color(255, 165, 0));
        g2.fillRect(lx + 70, ly, 12, 12);
        g2.setColor(Color.BLACK);
        g2.drawString("Distribuído", lx + 88, ly + 12);

        g2.dispose();
    }
}
