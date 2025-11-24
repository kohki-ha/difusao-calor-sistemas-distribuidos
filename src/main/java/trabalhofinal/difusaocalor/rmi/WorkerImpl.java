package trabalhofinal.difusaocalor.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementação concreta do worker RMI para cálculos distribuídos de difusão de
 * calor.
 * 
 * Esta classe mantém uma cópia em cache da matriz de temperaturas para
 * minimizar
 * a transferência de dados pela rede. Após a inicialização, apenas blocos
 * compactos
 * de linhas são trocados com o coordenador.
 * 
 * Características de implementação:
 * - Cache local da matriz completa (evita retransmissão)
 * - Suporte a processamento em lote com double buffering interno
 * - Preservação automática das condições de contorno (bordas)
 * - Tratamento seguro de casos extremos (blocos vazios, dimensões inválidas)
 */
public class WorkerImpl extends UnicastRemoteObject implements Worker {

    private double[][] T; // Cache local da matriz de temperaturas
    private int n; // Dimensão da malha quadrada

    protected WorkerImpl() throws RemoteException {
        super();
    }

    @Override
    public void initializeMatrix(double[][] matrix, int dimension) throws RemoteException {
        this.n = dimension;
        this.T = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, T[i], 0, n);
        }
    }

    @Override
    public double[][] computeBlock(double[][] block, int startRow, int endRow, double alpha, double dx,
            double dy, double dt) throws RemoteException {
        // Validação de entrada: retorna vazio se parâmetros inválidos
        if (startRow > endRow || block == null || block.length == 0)
            return new double[0][];

        int rows = endRow - startRow + 1;
        double[][] result = new double[rows][n];

        // Pré-calcula coeficientes do método de diferenças finitas (uma vez só)
        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // Estrutura do bloco recebido:
        // block[0] = linha startRow-1 (vizinhança superior)
        // block[1..rows] = linhas a calcular (startRow até endRow)
        // block[rows+1] = linha endRow+1 (vizinhança inferior)

        // Itera sobre cada linha interior do bloco
        for (int ii = 0; ii < rows; ii++) {
            int blockRowIdx = ii + 1; // Índice no bloco (pula a linha de vizinhança)

            // Itera sobre colunas interiores (exclui bordas em j=0 e j=n-1)
            for (int j = 1; j < n - 1; j++) {
                // Método de diferenças finitas explícito 2D:
                // T_novo = T_atual + alpha*dt * (d²T/dx² + d²T/dy²)
                double t = block[blockRowIdx][j];
                double tx = block[blockRowIdx + 1][j] - 2 * t + block[blockRowIdx - 1][j]; // Segunda derivada em X
                double ty = block[blockRowIdx][j + 1] - 2 * t + block[blockRowIdx][j - 1]; // Segunda derivada em Y
                result[ii][j] = t + coefX * tx + coefY * ty;
            }

            // Preserva condições de contorno (bordas esquerda e direita)
            result[ii][0] = block[blockRowIdx][0];
            result[ii][n - 1] = block[blockRowIdx][n - 1];
        }

        return result;
    }

    @Override
    public double[][] computeMultipleSteps(double[][] initialBlock, int startRow, int endRow, double alpha,
            double dx, double dy, double dt, int numSteps) throws RemoteException {
        // Validação de parâmetros
        if (startRow > endRow || initialBlock == null || initialBlock.length == 0 || numSteps <= 0)
            return new double[0][];

        int rows = endRow - startRow + 1;
        int blockRows = initialBlock.length;

        // Double buffering local: alterna entre current e next a cada iteração
        // Isso permite ler de 'current' e escrever em 'next' sem interferência
        double[][] current = new double[blockRows][n];
        double[][] next = new double[blockRows][n];

        // Copia o bloco inicial para o buffer 'current'
        for (int i = 0; i < blockRows; i++) {
            System.arraycopy(initialBlock[i], 0, current[i], 0, n);
        }

        // Pré-calcula coeficientes uma única vez
        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // Loop principal: processa numSteps iterações consecutivas localmente
        // Esta é a chave da otimização: N passos em 1 chamada RMI
        for (int step = 0; step < numSteps; step++) {
            // Calcula apenas linhas interiores do bloco (exclui vizinhanças)
            // Índices 0 e blockRows-1 são linhas de vizinhança, não são atualizadas
            for (int ii = 1; ii < blockRows - 1; ii++) {
                for (int j = 1; j < n - 1; j++) {
                    // Aplica diferenças finitas explícitas
                    double t = current[ii][j];
                    double tx = current[ii + 1][j] - 2 * t + current[ii - 1][j];
                    double ty = current[ii][j + 1] - 2 * t + current[ii][j - 1];
                    next[ii][j] = t + coefX * tx + coefY * ty;
                }
                // Preserva bordas laterais
                next[ii][0] = current[ii][0];
                next[ii][n - 1] = current[ii][n - 1];
            }

            // Swap: troca referências dos buffers (mais eficiente que copiar)
            // Após o swap, 'current' aponta para os dados atualizados
            double[][] temp = current;
            current = next;
            next = temp;
        }

        // Retorna apenas as linhas interiores (exclui vizinhanças nos índices 0 e
        // blockRows-1)
        double[][] result = new double[rows][n];
        for (int ii = 0; ii < rows; ii++) {
            System.arraycopy(current[ii + 1], 0, result[ii], 0, n);
        }

        return result;
    }

    @Override
    public void updateMatrix(double[][] newT) throws RemoteException {
        if (newT == null || newT.length != n)
            return;
        for (int i = 0; i < n; i++) {
            System.arraycopy(newT[i], 0, T[i], 0, n);
        }
    }

    @Override
    public void updateBoundaryRows(int startRow, int endRow, double[] rowBeforeStart, double[] rowAfterEnd)
            throws RemoteException {
        if (rowBeforeStart != null && startRow > 0) {
            System.arraycopy(rowBeforeStart, 0, T[startRow - 1], 0, Math.min(rowBeforeStart.length, n));
        }
        if (rowAfterEnd != null && endRow < n - 1) {
            System.arraycopy(rowAfterEnd, 0, T[endRow + 1], 0, Math.min(rowAfterEnd.length, n));
        }
    }
}
