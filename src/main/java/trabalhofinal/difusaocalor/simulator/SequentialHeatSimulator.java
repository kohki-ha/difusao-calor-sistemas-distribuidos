package trabalhofinal.difusaocalor.simulator;

/**
 * Versão sequencial (single-thread) do simulador de difusão de calor.
 * 
 * Implementação de referência que processa cada célula da malha sequencialmente
 * em um único loop aninhado. É a versão mais simples e serve como baseline para
 * comparação de desempenho com as versões paralelizada e distribuída.
 * 
 * Complexidade: O(n²) por passo, onde n é a dimensão da malha.
 */
public class SequentialHeatSimulator extends AbstractHeatSimulator {

    public SequentialHeatSimulator(int n, double alpha) {
        super(n, alpha);
    }

    @Override
    protected void computeStep() {
        // Pré-calcula coeficientes do esquema de diferenças finitas
        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // Itera apenas sobre o interior da malha (1..n-2)
        // As bordas (i=0, i=n-1, j=0, j=n-1) são tratadas por applyBoundaries()
        for (int i = 1; i < n - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                // Esquema explícito de diferenças finitas 2D
                double t = T[i][j];
                double tx = T[i + 1][j] - 2 * t + T[i - 1][j]; // d²T/dx²
                double ty = T[i][j + 1] - 2 * t + T[i][j - 1]; // d²T/dy²
                newT[i][j] = t + coefX * tx + coefY * ty;
            }
        }
    }
}
