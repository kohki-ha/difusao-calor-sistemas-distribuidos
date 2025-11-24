package trabalhofinal.difusaocalor.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementação do Worker que mantém a matriz em cache local.
 * Otimização: recebe matriz uma única vez, depois apenas blocos compactados.
 */
public class WorkerImpl extends UnicastRemoteObject implements Worker {

    private double[][] T; // Matriz mantida em cache no worker
    private int n;

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
        if (startRow > endRow || block == null || block.length == 0)
            return new double[0][];

        int rows = endRow - startRow + 1;
        double[][] result = new double[rows][n];

        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // block contém: [0] = linha startRow-1, [1..rows] = linhas a calcular, [rows+1]
        // = linha endRow+1
        for (int ii = 0; ii < rows; ii++) {
            int blockRowIdx = ii + 1;

            for (int j = 1; j < n - 1; j++) {
                double t = block[blockRowIdx][j];
                double tx = block[blockRowIdx + 1][j] - 2 * t + block[blockRowIdx - 1][j];
                double ty = block[blockRowIdx][j + 1] - 2 * t + block[blockRowIdx][j - 1];
                result[ii][j] = t + coefX * tx + coefY * ty;
            }

            result[ii][0] = block[blockRowIdx][0];
            result[ii][n - 1] = block[blockRowIdx][n - 1];
        }

        return result;
    }

    @Override
    public double[][] computeMultipleSteps(double[][] initialBlock, int startRow, int endRow, double alpha,
            double dx, double dy, double dt, int numSteps) throws RemoteException {
        if (startRow > endRow || initialBlock == null || initialBlock.length == 0 || numSteps <= 0)
            return new double[0][];

        int rows = endRow - startRow + 1;
        int blockRows = initialBlock.length;

        // Double buffering local
        double[][] current = new double[blockRows][n];
        double[][] next = new double[blockRows][n];

        // Copia bloco inicial
        for (int i = 0; i < blockRows; i++) {
            System.arraycopy(initialBlock[i], 0, current[i], 0, n);
        }

        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // Processa múltiplos steps
        for (int step = 0; step < numSteps; step++) {
            // Calcula apenas linhas interiores (índices 1 a blockRows-2 do bloco)
            for (int ii = 1; ii < blockRows - 1; ii++) {
                for (int j = 1; j < n - 1; j++) {
                    double t = current[ii][j];
                    double tx = current[ii + 1][j] - 2 * t + current[ii - 1][j];
                    double ty = current[ii][j + 1] - 2 * t + current[ii][j - 1];
                    next[ii][j] = t + coefX * tx + coefY * ty;
                }
                // Preserva bordas
                next[ii][0] = current[ii][0];
                next[ii][n - 1] = current[ii][n - 1];
            }

            // Swap: troca referências
            double[][] temp = current;
            current = next;
            next = temp;
        }

        // Retorna apenas linhas interiores (exclui vizinhanças)
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
