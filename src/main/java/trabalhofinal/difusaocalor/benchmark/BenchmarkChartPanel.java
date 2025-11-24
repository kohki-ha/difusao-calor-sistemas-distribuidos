package trabalhofinal.difusaocalor.benchmark;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/**
 * Painel simples que desenha um gráfico de barras comparando tempos por
 * repetição
 * entre sequencial, paralelo e distribuído.
 */
public class BenchmarkChartPanel extends JPanel {

    private final List<Double> seqRuns;
    private final List<Double> parRuns;
    private final List<Double> distRuns;

    public BenchmarkChartPanel(List<Double> seqRuns, List<Double> parRuns, List<Double> distRuns) {
        this.seqRuns = seqRuns;
        this.parRuns = parRuns;
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
        if (parRuns != null) {
            for (double v : parRuns)
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

        int repeats = Math.max(seqRuns == null ? 0 : seqRuns.size(),
                Math.max(parRuns == null ? 0 : parRuns.size(), distRuns == null ? 0 : distRuns.size()));
        if (repeats == 0) {
            g2.setColor(Color.GRAY);
            g2.drawString("Sem dados de execução", margin, margin + 20);
            g2.dispose();
            return;
        }

        int groupWidth = availableW / repeats;
        int barWidth = Math.max(10, groupWidth / 5); // barras mais largas
        int barSpacing = Math.max(6, barWidth / 2); // mais espaçamento entre barras
        int totalBarsWidth = barWidth * 3 + barSpacing * 2;

        // draw y grid lines
        g2.setColor(new Color(220, 220, 220));
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int yy = margin + (int) (availableH * (1.0 - (double) i / gridLines));
            g2.drawLine(margin, yy, w - margin, yy);
            String label = String.format("%.4fs", max * i / gridLines);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(label, 4, yy + 4);
            g2.setColor(new Color(220, 220, 220));
        }

        // draw bars
        for (int i = 0; i < repeats; i++) {
            int gx = margin + i * groupWidth;
            int startX = gx + Math.max(0, (groupWidth - totalBarsWidth) / 2);

            // seq bar (left)
            if (seqRuns != null && i < seqRuns.size()) {
                double v = seqRuns.get(i);
                int bh = (int) (availableH * (v / max));
                int x = startX;
                int y = margin + (availableH - bh);
                g2.setColor(new Color(100, 149, 237)); // cornflower blue
                g2.fillRect(x, y, barWidth, bh);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("%.4f", v), x, y - 4);
            }

            // par bar (middle)
            if (parRuns != null && i < parRuns.size()) {
                double v = parRuns.get(i);
                int bh = (int) (availableH * (v / max));
                int x = startX + barWidth + barSpacing;
                int y = margin + (availableH - bh);
                g2.setColor(new Color(46, 204, 113)); // emerald
                g2.fillRect(x, y, barWidth, bh);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("%.4f", v), x, y - 4);
            }

            // dist bar (right)
            if (distRuns != null && i < distRuns.size()) {
                double v = distRuns.get(i);
                int bh = (int) (availableH * (v / max));
                int x = startX + 2 * (barWidth + barSpacing);
                int y = margin + (availableH - bh);
                g2.setColor(new Color(255, 165, 0)); // orange
                g2.fillRect(x, y, barWidth, bh);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("%.4f", v), x, y - 4);
            }

            // x label
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            String label = "#" + (i + 1);
            int lx = gx + groupWidth / 2 - 8;
            g2.drawString(label, lx, margin + availableH + 16);
        }

        // legend (canto inferior esquerdo)
        int lx = margin + 10;
        int ly = h - margin + 28;
        g2.setColor(new Color(100, 149, 237));
        g2.fillRect(lx, ly, 12, 12);
        g2.setColor(Color.BLACK);
        g2.drawString("Sequencial", lx + 16, ly + 12);

        g2.setColor(new Color(46, 204, 113));
        g2.fillRect(lx + 90, ly, 12, 12);
        g2.setColor(Color.BLACK);
        g2.drawString("Paralelo", lx + 106, ly + 12);

        g2.setColor(new Color(255, 165, 0));
        g2.fillRect(lx + 170, ly, 12, 12);
        g2.setColor(Color.BLACK);
        g2.drawString("Distribuído", lx + 186, ly + 12);

        g2.dispose();
    }
}
