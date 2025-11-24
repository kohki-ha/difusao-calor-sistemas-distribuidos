package trabalhofinal.difusaocalor.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota RMI para um trabalhador distribuído de simulação de difusão
 * de calor.
 * 
 * Cada worker é responsável por calcular uma faixa de linhas da malha de
 * temperaturas.
 * Para reduzir o overhead de comunicação RMI:
 * - A matriz inicial é enviada uma única vez (initializeMatrix)
 * - Apenas blocos compactos são trocados nas operações subsequentes
 * - Suporta processamento em lote (computeMultipleSteps) para reduzir chamadas
 * RMI
 * 
 * Esta interface permite distribuir o cálculo computacional intensivo entre
 * múltiplos
 * processos ou máquinas através de Java RMI.
 */
public interface Worker extends Remote {

        /**
         * Inicializa a matriz de temperaturas no worker.
         * Este método é chamado uma única vez no início da simulação para
         * evitar o envio repetido da matriz completa em cada passo.
         * 
         * @param T matriz de temperaturas n×n
         * @param n dimensão da matriz quadrada
         * @throws RemoteException se houver falha na comunicação RMI
         */
        void initializeMatrix(double[][] T, int n) throws RemoteException;

        /**
         * Calcula um passo da simulação para um bloco específico de linhas.
         * 
         * Aplica o método de diferenças finitas para atualizar as temperaturas
         * nas linhas do intervalo [startRow, endRow]. O bloco recebido inclui
         * as linhas vizinhas necessárias para o cálculo (startRow-1 e endRow+1).
         * 
         * @param block    bloco compacto contendo as linhas a calcular mais vizinhanças
         * @param startRow índice inicial da faixa de linhas a calcular
         * @param endRow   índice final da faixa de linhas a calcular
         * @param alpha    coeficiente de difusividade térmica do material
         * @param dx       espaçamento da malha no eixo X
         * @param dy       espaçamento da malha no eixo Y
         * @param dt       passo de tempo da simulação
         * @return matriz com as novas temperaturas calculadas para as linhas [startRow,
         *         endRow]
         * @throws RemoteException se houver falha na comunicação RMI
         */
        double[][] computeBlock(double[][] block, int startRow, int endRow, double alpha, double dx, double dy,
                        double dt)
                        throws RemoteException;

        /**
         * Processa múltiplos passos da simulação em lote (batching).
         * 
         * Esta otimização reduz drasticamente o overhead de comunicação RMI ao
         * processar
         * vários passos consecutivos localmente no worker antes de retornar o
         * resultado.
         * Em vez de N chamadas RMI (uma por passo), faz-se apenas 1 chamada para N
         * passos.
         * 
         * O worker executa internamente um loop com double buffering local,
         * retornando apenas o estado final após todas as iterações.
         * 
         * @param initialBlock bloco inicial com linhas a calcular mais vizinhanças
         * @param startRow     índice inicial da faixa de linhas
         * @param endRow       índice final da faixa de linhas
         * @param alpha        coeficiente de difusividade térmica
         * @param dx           espaçamento no eixo X
         * @param dy           espaçamento no eixo Y
         * @param dt           passo de tempo
         * @param numSteps     número de iterações a processar em lote
         * @return resultado final após numSteps iterações
         * @throws RemoteException se houver falha na comunicação RMI
         */
        double[][] computeMultipleSteps(double[][] initialBlock, int startRow, int endRow, double alpha, double dx,
                        double dy, double dt, int numSteps)
                        throws RemoteException;

        /**
         * Atualiza completamente a matriz mantida em cache no worker.
         * 
         * Usado após broadcast dos resultados de todos os workers para sincronizar
         * o estado global da simulação antes do próximo passo.
         * 
         * @param newT nova matriz completa de temperaturas
         * @throws RemoteException se houver falha na comunicação RMI
         */
        void updateMatrix(double[][] newT) throws RemoteException;

        /**
         * Atualiza apenas as linhas de fronteira (vizinhanças) da matriz em cache.
         * 
         * Otimização que envia apenas as linhas adjacentes necessárias para o próximo
         * cálculo, em vez de transmitir a matriz completa. Reduz significativamente
         * o tráfego de rede quando a malha é grande.
         * 
         * @param startRow       índice inicial da faixa calculada por este worker
         * @param endRow         índice final da faixa calculada por este worker
         * @param rowBeforeStart linha imediatamente anterior a startRow (vizinhança
         *                       superior)
         * @param rowAfterEnd    linha imediatamente posterior a endRow (vizinhança
         *                       inferior)
         * @throws RemoteException se houver falha na comunicação RMI
         */
        void updateBoundaryRows(int startRow, int endRow, double[] rowBeforeStart, double[] rowAfterEnd)
                        throws RemoteException;
}
