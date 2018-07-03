/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufg.es.jogovelha;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Random;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author willi
 */
public class MinhaConexao extends SwingWorker<Boolean, String> {

    private Socket socket;
    private NewJFrame mainFrame;
    private DefaultListModel mensagens;

    // leitura dos dados
    private InputStream entrada;
    private InputStreamReader inr;
    private BufferedReader bfr;

    // envio dos dados
    private OutputStream saida;
    private OutputStreamWriter outw;
    private BufferedWriter bfw;
    
    private int vez;

    public MinhaConexao(NewJFrame mainFrame, Socket socket, boolean convidado) throws SocketException {
        this.mainFrame = mainFrame;
        this.socket = socket;
        //this.mensagens = mensagens;

        Random random = new Random();
        
        try {
            entrada = socket.getInputStream();
            inr = new InputStreamReader(entrada);
            bfr = new BufferedReader(inr);

            saida = this.socket.getOutputStream();
            outw = new OutputStreamWriter(saida);
            bfw = new BufferedWriter(outw);
        } catch (IOException e) {
            publish("Erro na conexão com o servidor");
            publish(e.getMessage());
        }

        publish("Servidor conectado na porta " + socket.getLocalPort());
        
        if(convidado){
            vez = 1;
        } else {
            vez = 2;
            int comeco = random.nextInt(2) + 1;
            String mensagem = "07006" + comeco;
            enviaMensagem(mensagem);
            
            if(comeco == 2){
                this.mainFrame.bloqueiaTabuleiro(false);
            }
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        String incommingMessage;
        while (true) {
            try {
                incommingMessage = (String) bfr.readLine();
                publish(incommingMessage);
                // Recebeu uma mensagem?

            } catch (IOException e) {
                // mostra mensagem de erro
                JOptionPane.showMessageDialog(mainFrame,
                        "Erro na leitura\n" + e.getMessage(),
                        "Servidor", JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
    }

    @Override
    protected void process(List<String> msg) {
        for (int i = 0; i < msg.size(); i++) {
            try {
                if (msg.get(i) != null) {
                    String mensagem = "Recebida: " + msg.get(i);
                    publish(mensagem);

                    switch (msg.get(i).substring(0, 2)) {
                        case "07":
                            // my turn? enable the board; 
                            if (Integer.parseInt(msg.get(i).substring(5)) == vez) {
                                mainFrame.bloqueiaTabuleiro(false);
                                mainFrame.setJogadorVez("X");
                            // it's not? Well, then disable it
                            } else {
                                mainFrame.bloqueiaTabuleiro(true);
                                mainFrame.setJogadorVez("O");
                            }
                            break;

                        case "08":
                            mainFrame.bloqueiaTabuleiro(false);
                            mainFrame.jogadaRecebida(Integer.parseInt(msg.get(i).substring(msg.get(i).length() - 1)));
                            mainFrame.setJogadorVez("X");
                            break;

                        case "09":
                            // clear board, add 1 victory
                            mainFrame.esvaziaTabuleiro();
                            mainFrame.setJogadorVez("O");
                            
                            if(!mainFrame.isFull()){
                                mainFrame.incrementaVitoria();
                            }
                            break;

                        case "10":
                        // terminate connection
                            JOptionPane.showMessageDialog(mainFrame, "O adversário desistiu!");
                    }
                    // lidar com a mensagem recebida
                } else {
                    publish("Conexão foi encerrada.");

                    // encerra atributos de comunicação
                    bfr.close();
                    inr.close();
                    entrada.close();
                    bfw.close();
                    outw.close();
                    saida.close();
                    socket.close();

                    mainFrame.encerraServer();

                    Thread.currentThread().stop();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean enviaMensagem(String msg) {
        try {
            outw.write(msg + "\n");
            outw.flush();
            System.out.println("Mensagem enviada: " + msg);
            return true;
        } catch (IOException ex) {
            publish("Erro ao enviar mensagem.");
            return false;
        }
    }

}
