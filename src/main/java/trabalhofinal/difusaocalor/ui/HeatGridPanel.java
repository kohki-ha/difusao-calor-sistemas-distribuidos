package trabalhofinal.difusaocalor.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/**
 * Painel que desenha a malha de temperaturas em uma única superfície.
 * Isso evita criar N*N componentes e elimina gaps/linhas indesejadas.
 */
public class HeatGridPanel extends JPanel {

    private double[][] temperature;
    private boolean showGrid = true;
    private Color gridColor = new Color(50, 50, 50); // linha fina escura
    private int gridThickness = 1; // pixels

    public HeatGridPanel(int n) {
        if (n <= 0)
            n = 1;
        this.temperature = new double[n][n];
        setOpaque(true);
    }

    public void setTemperature(double[][] temperature) {
        this.temperature = temperature;
        repaint();
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        repaint();
    }

    public void setGridColor(Color c) {
        this.gridColor = c;
        repaint();
    }

    public void setGridThickness(int t) {
        this.gridThickness = Math.max(1, t);
        repaint();
    }

    public int getGridSize() {
        return temperature == null ? 0 : temperature.length;
    }

    @Override
    public Dimension getPreferredSize() {
        // preferimos preencher o container; fallback razoável
        return new Dimension(500, 500);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (temperature == null)
            return;

        int n = temperature.length;
        if (n == 0)
            return;

        int w = getWidth();
        int h = getHeight();

        // calcula tamanho de cada célula (pode haver restos; distribuímos os pixels
        // extras)
        int cellW = w / n;
        int cellH = h / n;
        int remW = w - (cellW * n);
        int remH = h - (cellH * n);

        int y = 0;
        // store column widths and row heights to draw grid lines precisely
        int[] colW = new int[n];
        int[] rowH = new int[n];

        for (int i = 0; i < n; i++) {
            rowH[i] = cellH + (i < remH ? 1 : 0);
        }
        for (int j = 0; j < n; j++) {
            colW[j] = cellW + (j < remW ? 1 : 0);
        }

        for (int i = 0; i < n; i++) {
            int thisCellH = rowH[i];
            int x = 0;
            for (int j = 0; j < n; j++) {
                int thisCellW = colW[j];
                Color c = getColorFromTemperature(temperature[i][j]);
                g.setColor(c);
                g.fillRect(x, y, thisCellW, thisCellH);
                x += thisCellW;
            }
            y += thisCellH;
        }

        // desenha linhas de grade sobre as células, se solicitado
        if (showGrid && n > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(gridColor);

            // vertical lines
            int cumX = 0;
            for (int j = 0; j < n - 1; j++) {
                cumX += colW[j];
                for (int t = 0; t < gridThickness; t++) {
                    g2.drawLine(cumX + t, 0, cumX + t, h);
                }
            }

            // horizontal lines
            int cumY = 0;
            for (int i2 = 0; i2 < n - 1; i2++) {
                cumY += rowH[i2];
                for (int t = 0; t < gridThickness; t++) {
                    g2.drawLine(0, cumY + t, w, cumY + t);
                }
            }

            g2.dispose();
        }
    }

    private Color getColorFromTemperature(double temp) {
        temp = Math.max(20, Math.min(100, temp));

        if (temp < 50) {
            double ratio = (temp - 20) / 30.0;
            int r = 0;
            int g = (int) (255 * ratio);
            int b = (int) (255 * (1 - ratio));
            return new Color(r, g, b);
        } else {
            double ratio = (temp - 50) / 50.0;
            int r = 255;
            int g = (int) (255 * (1 - ratio));
            int b = 0;
            return new Color(r, g, b);
        }
    }
}
