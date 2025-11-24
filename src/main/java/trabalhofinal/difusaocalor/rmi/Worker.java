package trabalhofinal.difusaocalor.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota para um trabalhador que calcula blocos de linhas da malha.
 * Otimização: passa matriz uma única vez, depois apenas blocos compactados.
 */
public interface Worker extends Remote {

    /**
     * Inicializa a matriz no worker (enviada uma única vez no início).
     */
    void initializeMatrix(double[][] T, int n) throws RemoteException;

    /**
     * Calcula as novas temperaturas para as linhas de índice startRow..endRow.
     * A matriz T já está no worker, recebe apenas o bloco de vizinhança.
     */
    double[][] computeBlock(double[][] block, int startRow, int endRow, double alpha, double dx, double dy, double dt)
            throws RemoteException;

    /**
     * Processa múltiplos steps de uma vez para reduzir overhead RMI.
     * Retorna apenas o resultado final após N iterações.
     */
    double[][] computeMultipleSteps(double[][] initialBlock, int startRow, int endRow, double alpha, double dx,
            double dy, double dt, int numSteps)
            throws RemoteException;

    /**
     * Atualiza a matriz do worker com novos valores (após broadcast dos
     * resultados).
     */
    void updateMatrix(double[][] newT) throws RemoteException;

    /**
     * Atualiza apenas linhas de fronteira (vizinhanças) para reduzir overhead.
     * Envia apenas as linhas necessárias para o próximo cálculo.
     */
    void updateBoundaryRows(int startRow, int endRow, double[] rowBeforeStart, double[] rowAfterEnd)
            throws RemoteException;
}
