package trabalhofinal.difusaocalor.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/**
 * Painel customizado Swing para visualização da malha de temperaturas.
 * 
 * Renderiza a matriz de temperaturas como um grid de células coloridas,
 * onde cada cor representa uma temperatura diferente:
 * - Azul → Verde: temperaturas baixas (20°C → 50°C)
 * - Verde → Vermelho: temperaturas altas (50°C → 100°C)
 * 
 * Otimização importante:
 * - Desenha tudo em uma única superfície (paintComponent override)
 * - Evita criar n² componentes Swing individuais (muito lento para malhas
 * grandes)
 * - Elimina gaps/linhas indesejadas entre células
 * 
 * Recursos configuráveis:
 * - Grade opcional (linhas divisórias entre células)
 * - Cor e espessura da grade
 * - Ajuste automático ao tamanho do container
 * - Distribuição uniforme de pixels extras quando dimensões não são múltiplas
 * exatas
 * 
 * Ideal para simulações interativas em tempo real.
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

        // Calcula tamanho base de cada célula
        int cellW = w / n;
        int cellH = h / n;

        // Pixels extras (quando dimensões não são múltiplas exatas de n)
        // Serão distribuídos entre as primeiras células para preencher completamente
        int remW = w - (cellW * n);
        int remH = h - (cellH * n);

        // Armazena largura/altura real de cada coluna/linha após distribuição
        int[] colW = new int[n];
        int[] rowH = new int[n];

        for (int i = 0; i < n; i++) {
            rowH[i] = cellH + (i < remH ? 1 : 0); // Primeiras remH linhas ganham +1 pixel
        }
        for (int j = 0; j < n; j++) {
            colW[j] = cellW + (j < remW ? 1 : 0); // Primeiras remW colunas ganham +1 pixel
        }

        // Desenha células coloridas
        int y = 0;
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

        // Desenha grade (linhas divisórias) se habilitado
        if (showGrid && n > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(gridColor);

            // Linhas verticais
            int cumX = 0;
            for (int j = 0; j < n - 1; j++) {
                cumX += colW[j];
                for (int t = 0; t < gridThickness; t++) {
                    g2.drawLine(cumX + t, 0, cumX + t, h);
                }
            }

            // Linhas horizontais
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

    /**
     * Mapeia temperatura para cor usando gradiente de calor.
     * 
     * Escala assumida: 20°C (mínimo) até 100°C (máximo)
     * - [20, 50]: Azul → Verde (frio → morno)
     * - [50, 100]: Verde → Vermelho (morno → quente)
     * 
     * Valores fora do intervalo são limitados (clamp) aos extremos.
     * 
     * @param temp temperatura em graus Celsius
     * @return cor RGB correspondente
     */
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
