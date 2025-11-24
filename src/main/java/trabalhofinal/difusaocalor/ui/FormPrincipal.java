/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package trabalhofinal.difusaocalor.ui;

// import javax.swing.border.LineBorder; (não usado após remoção das bordas das células)
import java.awt.BorderLayout;
// import java.awt.Color; (não usado aqui)
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

import trabalhofinal.difusaocalor.simulator.*;
import trabalhofinal.difusaocalor.benchmark.BenchmarkUtil;
import trabalhofinal.difusaocalor.benchmark.BenchmarkChartPanel;

/**
 *
 * @author Kaneko
 */
public class FormPrincipal extends javax.swing.JFrame {

    private static final int MAX_RECORDED_FRAMES = 300;
    private static final int MAX_DISPLAY_SIZE = 200;
    private static final int DEFAULT_PARALLEL_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());

    private HeatGridPanel heatPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel lblStatus;

    // processos de worker iniciados pela UI (nome -> Process)
    private Map<String, Process> workerProcesses = new ConcurrentHashMap<>();
    // URLs dos workers iniciados pela UI (nome -> rmi://...)
    private Map<String, String> workerUrls = new ConcurrentHashMap<>();
    // última porta usada para worker (auto-incremento)
    private int lastWorkerPort = 1098;

    /**
     * Creates new form FormPrincipal
     */
    public FormPrincipal() {
        initComponents();
        // configura listeners adicionais que não são gerados pelo NetBeans
        setupListeners();
    }

    private void setupListeners() {
        javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateEnviarEnabled();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateEnviarEnabled();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateEnviarEnabled();
            }
        };
        txtAltura.getDocument().addDocumentListener(docListener);
        txtCoeficienteMaterial.getDocument().addDocumentListener(docListener);
        txtTempo.getDocument().addDocumentListener(docListener);
        // listener para campo de URLs dos workers (modo distribuído)
        txtWorkerUrls.getDocument().addDocumentListener(docListener);
        // listener para threads do modo paralelo
        txtParallelThreads.getDocument().addDocumentListener(docListener);

        // checkboxes already have action listeners generated -> reuse them to update
        // state
        // ensure initial state is correct
        updateEnviarEnabled();
    }

    private void updateEnviarEnabled() {
        boolean modeSelected = rbSequencial.isSelected() || rbParalelo.isSelected() || rbDistribuido.isSelected();
        boolean inputsFilled = !txtAltura.getText().trim().isEmpty()
                && !txtCoeficienteMaterial.getText().trim().isEmpty()
                && !txtTempo.getText().trim().isEmpty();
        // se modo distribuído selecionado, requerer URLs
        if (rbDistribuido.isSelected()) {
            inputsFilled = inputsFilled && txtWorkerUrls != null && !txtWorkerUrls.getText().trim().isEmpty();
        }
        if (rbParalelo.isSelected()) {
            inputsFilled = inputsFilled && isParallelThreadInputValid();
        }
        boolean positionSelected = cbCima.isSelected() || cbBaixo.isSelected() || cbDireita.isSelected()
                || cbEsquerda.isSelected();
        btnEnviar.setEnabled(modeSelected && inputsFilled && positionSelected);
    }

    private boolean isParallelThreadInputValid() {
        if (txtParallelThreads == null)
            return false;
        String text = txtParallelThreads.getText().trim();
        if (text.isEmpty())
            return false;
        try {
            return Integer.parseInt(text) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Integer resolveParallelThreadInput(boolean showError) {
        if (txtParallelThreads == null)
            return DEFAULT_PARALLEL_THREADS;
        String text = txtParallelThreads.getText().trim();
        try {
            int value = Integer.parseInt(text);
            if (value <= 0)
                throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            if (showError) {
                JOptionPane.showMessageDialog(this,
                        "Informe um número inteiro positivo de threads para o modo paralelo.", "Entrada inválida",
                        JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        lblDimensao = new javax.swing.JLabel();
        txtAltura = new javax.swing.JTextField();
        cbCima = new javax.swing.JCheckBox();
        cbBaixo = new javax.swing.JCheckBox();
        cbEsquerda = new javax.swing.JCheckBox();
        cbDireita = new javax.swing.JCheckBox();
        lblPosicaoCalor = new javax.swing.JLabel();
        lblCoeficienteMaterial = new javax.swing.JLabel();
        txtCoeficienteMaterial = new javax.swing.JTextField();
        lblTempo = new javax.swing.JLabel();
        txtTempo = new javax.swing.JTextField();
        btnEnviar = new javax.swing.JButton();
        btnLimpar = new javax.swing.JToggleButton();
        btnResultados = new javax.swing.JButton();
        pnMalha = new javax.swing.JPanel();
        rbParalelo = new javax.swing.JRadioButton();
        rbDistribuido = new javax.swing.JRadioButton();
        rbSequencial = new javax.swing.JRadioButton();
        lblParallelThreads = new javax.swing.JLabel();
        txtParallelThreads = new javax.swing.JTextField();
        lblWorkerUrls = new javax.swing.JLabel();
        scrollWorkerUrls = new javax.swing.JScrollPane();
        txtWorkerUrls = new javax.swing.JTextArea();
        progressBarBenchmark = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblDimensao.setText("Dimensão da Malha (X * Y)");

        txtAltura.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAlturaActionPerformed(evt);
            }
        });

        cbCima.setText("Cima");
        cbCima.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCimaActionPerformed(evt);
            }
        });

        cbBaixo.setText("Baixo");
        cbBaixo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbBaixoActionPerformed(evt);
            }
        });

        cbEsquerda.setText("Esquerda");
        cbEsquerda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbEsquerdaActionPerformed(evt);
            }
        });

        cbDireita.setText("Direita");
        cbDireita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbDireitaActionPerformed(evt);
            }
        });

        lblPosicaoCalor.setText("Posição do calor");

        lblCoeficienteMaterial.setText("Coeficiente do Material ");

        txtCoeficienteMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCoeficienteMaterialActionPerformed(evt);
            }
        });

        lblTempo.setText("Tempo (s)");

        txtTempo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTempoActionPerformed(evt);
            }
        });

        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        btnLimpar.setText("Limpar");
        btnLimpar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimparActionPerformed(evt);
            }
        });

        btnResultados.setText("Resultados");
        btnResultados.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResultadosActionPerformed(evt);
            }
        });

        // Botões para gerenciar workers locais
        btnStartWorker = new javax.swing.JButton();
        btnStopWorker = new javax.swing.JButton();
        btnStartWorker.setText("Iniciar Worker");
        btnStartWorker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartWorkerActionPerformed(evt);
            }
        });
        btnStopWorker.setText("Parar Worker");
        btnStopWorker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopWorkerActionPerformed(evt);
            }
        });

        progressBarBenchmark.setStringPainted(true);
        progressBarBenchmark.setString("Pronto");
        progressBarBenchmark.setVisible(false);

        // remover borda externa para que a malha comece na borda do painel
        pnMalha.setBorder(null);
        pnMalha.setToolTipText("");
        pnMalha.setName(""); // NOI18N
        pnMalha.setPreferredSize(new java.awt.Dimension(500, 500));
        // usamos BorderLayout como container para o painel customizado de desenho
        pnMalha.setLayout(new BorderLayout());

        // status (barra de progresso) será adicionado ao sul de pnMalha quando a malha
        // for criada

        buttonGroup1.add(rbParalelo);
        rbParalelo.setText("Paralelo");
        rbParalelo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbParaleloActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbDistribuido);
        rbDistribuido.setText("Distribuído");
        rbDistribuido.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbDistribuidoActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbSequencial);
        rbSequencial.setText("Sequêncial");
        rbSequencial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbSequencialActionPerformed(evt);
            }
        });

        lblParallelThreads.setText("Threads (paralelo)");

        txtParallelThreads.setColumns(4);
        txtParallelThreads.setText("4");

        lblWorkerUrls.setText("Workers RMI (vírgula separado)");

        txtWorkerUrls.setColumns(20);
        txtWorkerUrls.setRows(2);
        txtWorkerUrls.setLineWrap(true);
        txtWorkerUrls.setWrapStyleWord(true);
        txtWorkerUrls.setText("");
        scrollWorkerUrls.setViewportView(txtWorkerUrls);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(39, 39, 39)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(txtAltura, javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(lblDimensao, javax.swing.GroupLayout.Alignment.LEADING,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(lblCoeficienteMaterial,
                                                        javax.swing.GroupLayout.Alignment.LEADING,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(txtCoeficienteMaterial,
                                                        javax.swing.GroupLayout.Alignment.LEADING))
                                        .addComponent(lblParallelThreads)
                                        .addComponent(txtParallelThreads, javax.swing.GroupLayout.PREFERRED_SIZE, 120,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblWorkerUrls)
                                        .addComponent(scrollWorkerUrls, javax.swing.GroupLayout.PREFERRED_SIZE, 220,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnStartWorker, javax.swing.GroupLayout.PREFERRED_SIZE, 111,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnStopWorker, javax.swing.GroupLayout.PREFERRED_SIZE, 111,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtTempo, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblTempo))
                                .addGap(101, 101, 101)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(rbSequencial, javax.swing.GroupLayout.PREFERRED_SIZE, 98,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(rbParalelo, javax.swing.GroupLayout.PREFERRED_SIZE, 98,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(rbDistribuido, javax.swing.GroupLayout.PREFERRED_SIZE, 98,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cbCima, javax.swing.GroupLayout.PREFERRED_SIZE, 85,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cbDireita, javax.swing.GroupLayout.PREFERRED_SIZE, 85,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cbBaixo, javax.swing.GroupLayout.PREFERRED_SIZE, 85,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cbEsquerda, javax.swing.GroupLayout.PREFERRED_SIZE, 85,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblPosicaoCalor)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(btnLimpar, javax.swing.GroupLayout.PREFERRED_SIZE, 111,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 111,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnResultados, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(progressBarBenchmark,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        111, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(57, 57, 57)
                                .addComponent(pnMalha, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(50, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(53, 53, 53)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(lblPosicaoCalor)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbCima)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbBaixo)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbDireita)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbEsquerda)
                                                .addGap(18, 18, 18)
                                                .addComponent(rbSequencial)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(rbParalelo)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(rbDistribuido)
                                                .addGap(18, 18, 18)
                                                .addComponent(btnEnviar)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btnLimpar)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnResultados)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(progressBarBenchmark))
                                        .addComponent(pnMalha, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(1, 1, 1)
                                                .addComponent(lblDimensao)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtAltura, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(lblCoeficienteMaterial)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtCoeficienteMaterial,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(lblTempo)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtTempo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(lblParallelThreads)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtParallelThreads,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(lblWorkerUrls)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(scrollWorkerUrls, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnStartWorker)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnStopWorker)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cbCimaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbCimaActionPerformed
        // estado do checkbox lido quando necessário pela simulação
        updateEnviarEnabled();
    }// GEN-LAST:event_cbCimaActionPerformed

    private void cbBaixoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbBaixoActionPerformed
        // handler left intentionally simple; state read where needed
        updateEnviarEnabled();
    }// GEN-LAST:event_cbBaixoActionPerformed

    private void cbEsquerdaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbEsquerdaActionPerformed
        // handler left intentionally simple; state read where needed
        updateEnviarEnabled();
    }// GEN-LAST:event_cbEsquerdaActionPerformed

    private void cbDireitaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbDireitaActionPerformed
        // handler left intentionally simple; state read where needed
        updateEnviarEnabled();
    }// GEN-LAST:event_cbDireitaActionPerformed

    private void txtAlturaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtAlturaActionPerformed

    }// GEN-LAST:event_txtAlturaActionPerformed

    private void txtTempoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtTempoActionPerformed
        try {
            Integer.parseInt(txtTempo.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido para tempo. Digite um inteiro.", "Entrada inválida",
                    JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_txtTempoActionPerformed

    private void txtCoeficienteMaterialActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtCoeficienteMaterialActionPerformed
        try {
            Double.parseDouble(txtCoeficienteMaterial.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido para coeficiente. Digite um número.",
                    "Entrada inválida", JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_txtCoeficienteMaterialActionPerformed

    private void btnLimparActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnLimparActionPerformed
        limpar();
    }// GEN-LAST:event_btnLimparActionPerformed

    private void btnResultadosActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnResultadosActionPerformed
        // lê parâmetros da UI
        int n;
        double alpha;
        int steps;
        int repeats = 5; // padrão
        try {
            n = Integer.parseInt(txtAltura.getText());
            alpha = Double.parseDouble(txtCoeficienteMaterial.getText());
            steps = Integer.parseInt(txtTempo.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Verifique as entradas: dimensão, coeficiente e tempo devem ser números.", "Entrada inválida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int fn = n;
        final double falpha = alpha;
        final int fsteps = steps;
        final int frepeats = repeats;
        final Integer fParallelThreads = resolveParallelThreadInput(true);
        if (fParallelThreads == null) {
            return;
        }

        // pega URLs (opcional)
        final java.util.List<String> urls = new java.util.ArrayList<>();
        String urlsText = txtWorkerUrls.getText().trim();
        if (!urlsText.isEmpty()) {
            for (String p : urlsText.split(",")) {
                String u = p.trim();
                if (!u.isEmpty())
                    urls.add(u);
            }
        }

        // executa em background para não travar a UI
        btnResultados.setEnabled(false);
        btnEnviar.setEnabled(false);
        btnLimpar.setEnabled(false);
        progressBarBenchmark.setVisible(true);
        progressBarBenchmark.setValue(0);
        progressBarBenchmark.setString("Executando benchmark... 0%");

        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<>() {
            private BenchmarkUtil.Stats seqStats;
            private BenchmarkUtil.Stats parStats;
            private BenchmarkUtil.Stats distStats;

            @Override
            protected Void doInBackground() throws Exception {
                progressBarBenchmark.setValue(10);
                progressBarBenchmark.setString("Modo sequencial... 10%");
                seqStats = BenchmarkUtil.runSequential(fn, falpha, fsteps, frepeats);

                progressBarBenchmark.setValue(40);
                progressBarBenchmark.setString("Modo paralelo... 40%");
                parStats = BenchmarkUtil.runParallel(fn, falpha, fsteps, frepeats, fParallelThreads);

                if (!urls.isEmpty()) {
                    progressBarBenchmark.setValue(70);
                    progressBarBenchmark.setString("Modo distribuído... 70%");
                    distStats = BenchmarkUtil.runDistributed(fn, falpha, fsteps, frepeats, urls);
                }
                progressBarBenchmark.setValue(100);
                progressBarBenchmark.setString("Concluído! 100%");
                return null;
            }

            @Override
            protected void done() {
                btnResultados.setEnabled(true);
                btnEnviar.setEnabled(true);
                btnLimpar.setEnabled(true);
                progressBarBenchmark.setVisible(false);

                String header = String.format(
                        "Benchmark - Dimensão: %d | Coeficiente: %.2f | Tempo: %d | Threads: %d | WorkersRMI: %d",
                        fn, falpha, fsteps, fParallelThreads, urls.size());

                // cria diálogo com texto e gráfico
                javax.swing.JDialog dlg = new javax.swing.JDialog(FormPrincipal.this, "Resultados do Benchmark", true);
                dlg.setLayout(new java.awt.BorderLayout());

                BestModeResult best = determineBestMode(seqStats, parStats, distStats);
                String bestModeText = "";
                if (best != null) {
                    bestModeText = String.format("\nModo com menor média: %s (%.6fs)", best.label, best.mean);
                }

                javax.swing.JTextArea ta = new javax.swing.JTextArea(header + bestModeText);
                ta.setEditable(false);
                ta.setBackground(dlg.getBackground());
                ta.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
                dlg.add(ta, java.awt.BorderLayout.NORTH);

                BenchmarkChartPanel chart = new BenchmarkChartPanel(seqStats == null ? null : seqStats.runs,
                        parStats == null ? null : parStats.runs, distStats == null ? null : distStats.runs);
                chart.setPreferredSize(new java.awt.Dimension(800, 350));
                dlg.add(chart, java.awt.BorderLayout.CENTER);

                // cria tabela comparativa de tempos
                javax.swing.JPanel tablePanel = new javax.swing.JPanel(new java.awt.BorderLayout());
                tablePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

                int maxRuns = Math.max(seqStats != null ? seqStats.runs.size() : 0,
                        Math.max(parStats != null ? parStats.runs.size() : 0,
                                distStats != null ? distStats.runs.size() : 0));

                String[] columnNames = { "Execução", "Sequencial (s)", "Paralelo (s)", "Distribuído (s)" };
                Object[][] tableData = new Object[maxRuns + 1][4]; // +1 para linha de média

                for (int i = 0; i < maxRuns; i++) {
                    tableData[i][0] = "#" + (i + 1);
                    tableData[i][1] = seqStats != null && i < seqStats.runs.size()
                            ? String.format("%.6f", seqStats.runs.get(i))
                            : "-";
                    tableData[i][2] = parStats != null && i < parStats.runs.size()
                            ? String.format("%.6f", parStats.runs.get(i))
                            : "-";
                    tableData[i][3] = distStats != null && i < distStats.runs.size()
                            ? String.format("%.6f", distStats.runs.get(i))
                            : "-";
                }

                // adiciona linha de média
                tableData[maxRuns][0] = "Média";
                tableData[maxRuns][1] = seqStats != null && !Double.isNaN(seqStats.mean)
                        ? String.format("%.6f", seqStats.mean)
                        : "-";
                tableData[maxRuns][2] = parStats != null && !Double.isNaN(parStats.mean)
                        ? String.format("%.6f", parStats.mean)
                        : "-";
                tableData[maxRuns][3] = distStats != null && !Double.isNaN(distStats.mean)
                        ? String.format("%.6f", distStats.mean)
                        : "-";

                javax.swing.JTable table = new javax.swing.JTable(tableData, columnNames);
                table.setEnabled(false);
                table.getTableHeader().setReorderingAllowed(false);
                javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);
                scrollPane.setPreferredSize(new java.awt.Dimension(800, 150));
                tablePanel.add(scrollPane, java.awt.BorderLayout.CENTER);

                javax.swing.JButton close = new javax.swing.JButton("Fechar");
                close.addActionListener(ae -> dlg.dispose());
                javax.swing.JPanel bottom = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
                bottom.add(close);
                tablePanel.add(bottom, java.awt.BorderLayout.SOUTH);

                dlg.add(tablePanel, java.awt.BorderLayout.SOUTH);

                dlg.pack();
                dlg.setLocationRelativeTo(FormPrincipal.this);
                dlg.setVisible(true);
            }
        };

        worker.execute();
    }// GEN-LAST:event_btnResultadosActionPerformed

    private void btnStartWorkerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnStartWorkerActionPerformed
        String name = JOptionPane.showInputDialog(this, "Nome do worker (ex: Worker1):");
        if (name == null || name.trim().isEmpty())
            return;
        // incrementa a porta automaticamente
        lastWorkerPort++;
        String portStr = JOptionPane.showInputDialog(this, "Porta (ex: 1099):", String.valueOf(lastWorkerPort));
        if (portStr == null)
            return;
        try {
            int port = Integer.parseInt(portStr.trim());
            if (workerProcesses.containsKey(name)) {
                JOptionPane.showMessageDialog(this, "Worker com este nome já foi iniciado pela UI.", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "target\\classes",
                    "trabalhofinal.difusaocalor.rmi.WorkerServer", name, String.valueOf(port));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            workerProcesses.put(name, p);
            // registra URL do worker e adiciona ao campo de URLs (evita duplicatas)
            String url = String.format("rmi://localhost:%d/%s", port, name);
            workerUrls.put(name, url);
            String existing = txtWorkerUrls.getText().trim();
            if (existing.isEmpty()) {
                txtWorkerUrls.setText(url);
            } else if (!existing.contains(url)) {
                txtWorkerUrls.setText(existing + "," + url);
            }
            new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[" + name + "] " + line);
                    }
                } catch (IOException ex) {
                    System.out.println("Erro ao ler saída de " + name + ": " + ex.getMessage());
                }
            }, "WorkerLog-" + name).start();
            JOptionPane.showMessageDialog(this, "Worker iniciado: " + name, "Iniciado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Falha ao iniciar processo: " + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_btnStartWorkerActionPerformed

    private void btnStopWorkerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnStopWorkerActionPerformed
        if (workerProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum worker iniciado pela UI.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object[] names = workerProcesses.keySet().toArray();
        String sel = (String) JOptionPane.showInputDialog(this, "Escolha worker para parar:", "Parar Worker",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (sel == null)
            return;
        Process p = workerProcesses.remove(sel);
        if (p != null) {
            p.destroy();
            try {
                if (!p.waitFor(2, TimeUnit.SECONDS))
                    p.destroyForcibly();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            // remove URL do campo caso tenha sido adicionada pela UI
            String removedUrl = workerUrls.remove(sel);
            if (removedUrl != null) {
                String existing = txtWorkerUrls.getText();
                String[] parts = existing.split(",");
                StringBuilder sb = new StringBuilder();
                for (String purl : parts) {
                    String t = purl.trim();
                    if (t.isEmpty() || t.equals(removedUrl))
                        continue;
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(t);
                }
                txtWorkerUrls.setText(sb.toString());
            }
            JOptionPane.showMessageDialog(this, "Worker parado: " + sel, "Parado",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Worker não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_btnStopWorkerActionPerformed

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnEnviarActionPerformed
        gerarMalha();
        simularDifusao();
    }// GEN-LAST:event_btnEnviarActionPerformed

    private void rbParaleloActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_rbParaleloActionPerformed
        // seleção alterada — atualiza estado do botão Enviar
        updateEnviarEnabled();
    }// GEN-LAST:event_rbParaleloActionPerformed

    private void rbSequencialActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_rbSequencialActionPerformed
        // seleção alterada — atualiza estado do botão Enviar
        updateEnviarEnabled();
    }// GEN-LAST:event_rbSequencialActionPerformed

    private void rbDistribuidoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_rbDistribuidoActionPerformed
        // distribuído selecionado - funcionalidade não implementada ainda
        updateEnviarEnabled();
    }// GEN-LAST:event_rbDistribuidoActionPerformed

    private void limpar() {
        txtAltura.setText("");
        txtCoeficienteMaterial.setText("");
        txtTempo.setText("");
        pnMalha.removeAll();
        pnMalha.revalidate();
        pnMalha.repaint();
        cbCima.setSelected(false);
        cbBaixo.setSelected(false);
        cbDireita.setSelected(false);
        cbEsquerda.setSelected(false);
        txtParallelThreads.setText(String.valueOf(DEFAULT_PARALLEL_THREADS));
        buttonGroup1.clearSelection();
        // desabilita Enviar até nova seleção e atualiza estado
        updateEnviarEnabled();
    }

    private void gerarMalha() {
        int altura = 1;
        try {
            altura = Integer.parseInt(txtAltura.getText());
            if (altura <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Dimensão inválida. Digite um inteiro positivo.", "Entrada inválida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        pnMalha.removeAll();

        // cria um painel customizado que desenha toda a malha em uma só superfície
        heatPanel = new HeatGridPanel(altura);
        pnMalha.add(heatPanel, BorderLayout.CENTER);

        // cria painel de status (barra de progresso + label) e adiciona ao sul da área
        // da malha
        progressBar = new javax.swing.JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        lblStatus = new javax.swing.JLabel("Pronto");
        javax.swing.JPanel statusPanel = new javax.swing.JPanel(new BorderLayout());
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(lblStatus, BorderLayout.EAST);
        pnMalha.add(statusPanel, BorderLayout.SOUTH);

        pnMalha.revalidate();
        pnMalha.repaint();
    }

    // ---------------- DIFUSÃO DE CALOR ----------------
    private void simularDifusao() {
        int n;
        double alpha;
        int tempoIteracoes;

        try {
            n = Integer.parseInt(txtAltura.getText());
            alpha = Double.parseDouble(txtCoeficienteMaterial.getText());
            tempoIteracoes = Integer.parseInt(txtTempo.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Verifique as entradas: dimensão, coeficiente e tempo devem ser números.", "Entrada inválida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ExecutionMode mode;
        if (rbSequencial.isSelected()) {
            mode = ExecutionMode.SEQUENCIAL;
        } else if (rbParalelo.isSelected()) {
            mode = ExecutionMode.PARALELO;
        } else if (rbDistribuido.isSelected()) {
            mode = ExecutionMode.DISTRIBUIDO;
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um modo de execução (Sequencial/Paralelo/Distribuído).",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int parallelThreadsValue = DEFAULT_PARALLEL_THREADS;
        if (mode == ExecutionMode.PARALELO) {
            Integer parsed = resolveParallelThreadInput(true);
            if (parsed == null) {
                return;
            }
            parallelThreadsValue = parsed;
        }

        List<String> workerUrls = new ArrayList<>();
        if (mode == ExecutionMode.DISTRIBUIDO) {
            String urlsText = txtWorkerUrls.getText().trim();
            if (urlsText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Informe ao menos um URL de worker para o modo distribuído.",
                        "Entrada inválida", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (String raw : urlsText.split(",")) {
                String cleaned = raw.trim();
                if (!cleaned.isEmpty())
                    workerUrls.add(cleaned);
            }
            if (workerUrls.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Informe ao menos um URL de worker válido.", "Entrada inválida",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        final boolean cima = cbCima.isSelected();
        final boolean baixo = cbBaixo.isSelected();
        final boolean esquerda = cbEsquerda.isSelected();
        final boolean direita = cbDireita.isSelected();

        runMeasuredSimulation(mode, n, alpha, tempoIteracoes, workerUrls, cima, baixo, esquerda, direita,
                parallelThreadsValue);
    }

    private enum ExecutionMode {
        SEQUENCIAL,
        PARALELO,
        DISTRIBUIDO
    }

    private static class SimulationFrame {
        final int step;
        final double[][] snapshot;

        SimulationFrame(int step, double[][] snapshot) {
            this.step = step;
            this.snapshot = snapshot;
        }
    }

    private static class SimulationPlaybackData {
        final double elapsedSeconds;
        final double computeSeconds;
        final List<SimulationFrame> frames;
        final int totalSteps;
        final double[][] finalStateFull;

        SimulationPlaybackData(double elapsedSeconds, double computeSeconds, List<SimulationFrame> frames,
                int totalSteps, double[][] finalStateFull) {
            this.elapsedSeconds = elapsedSeconds;
            this.computeSeconds = computeSeconds;
            this.frames = Collections.unmodifiableList(new ArrayList<>(frames));
            this.totalSteps = totalSteps;
            this.finalStateFull = finalStateFull;
        }
    }

    private AbstractHeatSimulator buildSimulator(ExecutionMode mode, int n, double alpha, List<String> workerUrls,
            int parallelThreads) {
        switch (mode) {
            case DISTRIBUIDO:
                return new DistributedHeatSimulator(n, alpha, workerUrls);
            case PARALELO:
                int threads = parallelThreads <= 0 ? DEFAULT_PARALLEL_THREADS : parallelThreads;
                return new ParallelHeatSimulator(n, alpha, threads);
            case SEQUENCIAL:
            default:
                return new SequentialHeatSimulator(n, alpha);
        }
    }

    private void cleanupSimulator(AbstractHeatSimulator simulator) {
        if (simulator instanceof DistributedHeatSimulator) {
            try {
                ((DistributedHeatSimulator) simulator).shutdown();
            } catch (Exception ignore) {
            }
        } else if (simulator instanceof ParallelHeatSimulator) {
            try {
                ((ParallelHeatSimulator) simulator).shutdown();
            } catch (Exception ignore) {
            }
        }
    }

    private void runMeasuredSimulation(ExecutionMode mode, int n, double alpha, int totalSteps,
            List<String> workerUrls, boolean cima, boolean baixo, boolean esquerda, boolean direita,
            int parallelThreads) {
        final ExecutionMode chosenMode = mode;
        final List<String> urlsCopy = new ArrayList<>(workerUrls);
        final int threadCount = Math.max(1, parallelThreads);

        if (progressBar != null) {
            progressBar.setIndeterminate(true);
            progressBar.setValue(0);
        }
        if (lblStatus != null) {
            lblStatus.setText("Calculando simulação real...");
        }
        btnEnviar.setEnabled(false);
        btnLimpar.setEnabled(false);

        javax.swing.SwingWorker<SimulationPlaybackData, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected SimulationPlaybackData doInBackground() throws Exception {
                double computeSeconds = measurePureCompute(chosenMode, n, alpha, totalSteps, urlsCopy, cima, baixo,
                        esquerda, direita, threadCount);

                AbstractHeatSimulator sim = buildSimulator(chosenMode, n, alpha, urlsCopy, threadCount);
                sim.setBoundaryFlags(cima, baixo, esquerda, direita);

                List<SimulationFrame> frames = new ArrayList<>();
                frames.add(new SimulationFrame(0, downsampleMatrix(sim.getTemperatureCopy())));

                int stride = Math.max(1, (int) Math.ceil((double) totalSteps / MAX_RECORDED_FRAMES));
                long start = System.nanoTime();
                double[][] finalState = null;

                for (int step = 1; step <= totalSteps; step++) {
                    sim.step();
                    if (step % stride == 0 || step == totalSteps) {
                        double[][] snapshot = sim.getTemperatureCopy();
                        if (step == totalSteps) {
                            finalState = snapshot;
                        }
                        frames.add(new SimulationFrame(step, downsampleMatrix(snapshot)));
                    }
                }

                long end = System.nanoTime();
                double elapsed = (end - start) / 1_000_000_000.0;
                cleanupSimulator(sim);
                return new SimulationPlaybackData(elapsed, computeSeconds, frames, totalSteps, finalState);
            }

            @Override
            protected void done() {
                if (progressBar != null) {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                }
                SimulationPlaybackData data;
                try {
                    data = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(FormPrincipal.this,
                            "Falha ao executar a simulação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    finalizePlaybackFailure();
                    return;
                }
                playbackSimulation(data);
            }
        };

        worker.execute();
    }

    private double measurePureCompute(ExecutionMode mode, int n, double alpha, int totalSteps,
            List<String> workerUrls, boolean cima, boolean baixo, boolean esquerda, boolean direita,
            int parallelThreads) {
        if (totalSteps <= 0) {
            return 0.0;
        }
        AbstractHeatSimulator sim = buildSimulator(mode, n, alpha, workerUrls, parallelThreads);
        sim.setBoundaryFlags(cima, baixo, esquerda, direita);
        long start = System.nanoTime();
        for (int step = 0; step < totalSteps; step++) {
            sim.step();
        }
        long end = System.nanoTime();
        cleanupSimulator(sim);
        return (end - start) / 1_000_000_000.0;
    }

    private void playbackSimulation(SimulationPlaybackData data) {
        if (heatPanel == null || data.frames.isEmpty()) {
            finalizePlaybackSuccess(data);
            return;
        }

        if (progressBar != null) {
            progressBar.setValue(0);
        }
        if (lblStatus != null) {
            lblStatus.setText(String.format("Reproduzindo simulação — tempo real %.3fs", data.elapsedSeconds));
        }

        final int[] index = { 0 };
        javax.swing.Timer timer = new javax.swing.Timer(50, null);
        timer.addActionListener(e -> {
            if (index[0] >= data.frames.size()) {
                ((javax.swing.Timer) e.getSource()).stop();
                finalizePlaybackSuccess(data);
                return;
            }
            SimulationFrame frame = data.frames.get(index[0]);
            heatPanel.setTemperature(frame.snapshot);
            updatePlaybackStatus(frame.step, data.totalSteps, data.elapsedSeconds);
            index[0]++;
        });
        timer.start();
    }

    private void updatePlaybackStatus(int step, int totalSteps, double elapsedSeconds) {
        int pct = totalSteps == 0 ? 100 : Math.min(100, (int) ((step * 100L) / totalSteps));
        if (progressBar != null) {
            progressBar.setValue(pct);
        }
        if (lblStatus != null) {
            lblStatus.setText(String.format("It: %d/%d — tempo real %.3fs", step, totalSteps, elapsedSeconds));
        }
        this.setTitle(String.format("Difusão de Calor — %d%%", pct));
    }

    private void finalizePlaybackSuccess(SimulationPlaybackData data) {
        if (heatPanel != null && data.finalStateFull != null) {
            heatPanel.setTemperature(downsampleMatrix(data.finalStateFull));
        }
        if (progressBar != null) {
            progressBar.setValue(100);
            progressBar.setIndeterminate(false);
        }
        if (lblStatus != null) {
            lblStatus.setText(String.format("Concluído — cálculo puro: %.5fs | total com animação: %.5fs",
                    data.computeSeconds, data.elapsedSeconds));
        }
        btnEnviar.setEnabled(true);
        btnLimpar.setEnabled(true);
        this.setTitle("Difusão de Calor");
    }

    private void finalizePlaybackFailure() {
        if (progressBar != null) {
            progressBar.setValue(0);
            progressBar.setIndeterminate(false);
        }
        if (lblStatus != null) {
            lblStatus.setText("Simulação não iniciada");
        }
        btnEnviar.setEnabled(true);
        btnLimpar.setEnabled(true);
        this.setTitle("Difusão de Calor");
    }

    private boolean hasStats(BenchmarkUtil.Stats stats) {
        return stats != null && stats.runs != null && !stats.runs.isEmpty() && !Double.isNaN(stats.mean);
    }

    private BestModeResult determineBestMode(BenchmarkUtil.Stats seq, BenchmarkUtil.Stats par,
            BenchmarkUtil.Stats dist) {
        BestModeResult best = null;
        best = considerMode(best, "Sequencial", seq);
        best = considerMode(best, "Paralelo", par);
        best = considerMode(best, "Distribuído", dist);
        return best;
    }

    private BestModeResult considerMode(BestModeResult current, String label, BenchmarkUtil.Stats stats) {
        if (!hasStats(stats)) {
            return current;
        }
        if (current == null || stats.mean < current.mean) {
            return new BestModeResult(label, stats.mean);
        }
        return current;
    }

    private static class BestModeResult {
        final String label;
        final double mean;

        BestModeResult(String label, double mean) {
            this.label = label;
            this.mean = mean;
        }
    }

    private double[][] downsampleMatrix(double[][] source) {
        if (source == null) {
            return null;
        }
        int n = source.length;
        if (n == 0) {
            return new double[0][0];
        }
        if (n <= MAX_DISPLAY_SIZE) {
            double[][] copy = new double[n][n];
            for (int i = 0; i < n; i++) {
                System.arraycopy(source[i], 0, copy[i], 0, n);
            }
            return copy;
        }

        int factor = (int) Math.ceil((double) n / MAX_DISPLAY_SIZE);
        int target = (int) Math.ceil((double) n / factor);
        double[][] result = new double[target][target];
        for (int i = 0; i < target; i++) {
            for (int j = 0; j < target; j++) {
                double sum = 0.0;
                int count = 0;
                int startI = i * factor;
                int startJ = j * factor;
                for (int si = startI; si < Math.min(n, startI + factor); si++) {
                    for (int sj = startJ; sj < Math.min(n, startJ + factor); sj++) {
                        sum += source[si][sj];
                        count++;
                    }
                }
                result[i][j] = count == 0 ? 0.0 : sum / count;
            }
        }
        return result;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FormPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FormPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FormPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FormPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FormPrincipal().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEnviar;
    private javax.swing.JToggleButton btnLimpar;
    private javax.swing.JButton btnResultados;
    private javax.swing.JButton btnStartWorker;
    private javax.swing.JButton btnStopWorker;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox cbBaixo;
    private javax.swing.JCheckBox cbCima;
    private javax.swing.JCheckBox cbDireita;
    private javax.swing.JCheckBox cbEsquerda;
    private javax.swing.JLabel lblCoeficienteMaterial;
    private javax.swing.JLabel lblDimensao;
    private javax.swing.JLabel lblParallelThreads;
    private javax.swing.JLabel lblPosicaoCalor;
    private javax.swing.JLabel lblTempo;
    private javax.swing.JLabel lblWorkerUrls;
    private javax.swing.JPanel pnMalha;
    private javax.swing.JProgressBar progressBarBenchmark;
    private javax.swing.JRadioButton rbDistribuido;
    private javax.swing.JRadioButton rbParalelo;
    private javax.swing.JRadioButton rbSequencial;
    private javax.swing.JTextField txtAltura;
    private javax.swing.JTextField txtCoeficienteMaterial;
    private javax.swing.JTextField txtParallelThreads;
    private javax.swing.JTextField txtTempo;
    private javax.swing.JScrollPane scrollWorkerUrls;
    private javax.swing.JTextArea txtWorkerUrls;
}
