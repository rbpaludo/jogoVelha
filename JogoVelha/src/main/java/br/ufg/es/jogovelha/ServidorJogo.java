/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufg.es.jogovelha;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Random;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author rafae
 */
public class ServidorJogo extends SwingWorker<Boolean, String> {

    private NewJFrame mainFrame;
    private ServerSocket server;
    private boolean inGame;
    private DatagramSocket socketBroadcast;
    private DatagramPacket packet;
    private String apelido;
    private DefaultListModel onlineUsers;
    private Random random = new Random();

    public ServidorJogo(NewJFrame mainFrame, ServerSocket server,
            String apelido, DefaultListModel onlineUsers) {

        try {
            this.onlineUsers = onlineUsers;
            this.mainFrame = mainFrame;
            this.server = server;
            this.apelido = apelido;
            socketBroadcast = new DatagramSocket(20181);
            byte[] buf = new byte[512];
            packet = new DatagramPacket(buf, buf.length);
        } catch (SocketException ex) {
            System.out.println("Erro: " + ex.getMessage());
        }
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        try {

            String msgEnviar, msgReceb, apelidoOnline;
            int porta;

            // escutando na porta tal
            while (true) {
                if (inGame) {
                    // espera conexão
                    Socket connection = server.accept();

                    // cria conexão com cliente
                    MinhaConexao novaConexao = new MinhaConexao(mainFrame, connection);
                    // processa as comunicações com o cliente
                    novaConexao.execute();
                } else {
                    socketBroadcast.receive(packet);
                    msgReceb = packet.getData().toString();

                    switch (msgReceb.substring(0, 2)) {
                        case "01":
                            // responder com mensagem 2 para o remetente
                            msgEnviar = "02";
                            msgEnviar += String.format("%03d",
                                    apelido.length() + 5);
                            msgEnviar += apelido;
                            enviarMsg(msgEnviar,
                                    packet.getAddress(),
                                    20181);

                            onlineUsers.clear();
                            break;
                        case "02":
                            // adicionar o remetente à lista de usuários ativos
                            apelidoOnline = msgReceb.substring(5,
                                    msgReceb.length()) + "-" + packet.getAddress();
                            onlineUsers.addElement(apelidoOnline);
                            break;
                        case "03":
                            // remover o remetente`da lista de usuários ativos
                            apelidoOnline = msgReceb.substring(5,
                                    msgReceb.length()) + "-" + packet.getAddress();
                            onlineUsers.removeElement(apelidoOnline);
                            break;
                        case "04":
                            // responder com mensagem 5 para o remetente
                            int resposta = JOptionPane.showConfirmDialog(null,
                                    msgReceb.substring(5, msgReceb.length())
                                    + " quer jogar. Você aceita?",
                                    "Convite para jogar", JOptionPane.YES_NO_OPTION);
                            msgEnviar = "05";
                            if (resposta == 0) {
                                do{
                                    porta = random.nextInt(65536);
                                }while(porta == 20181 || porta == 0);
                                msgEnviar += String.format("%03d", 
                                        apelido.length() + 6 + 
                                                String.valueOf(porta).length());
                                msgEnviar += apelido;
                                msgEnviar += "|" + porta;
                                enviarMsg(msgEnviar, packet.getAddress(), 20181);
                            } else {
                                msgEnviar += String.format("%03d", apelido.length() + 7);
                                msgEnviar += apelido;
                                msgEnviar += "|0";
                                enviarMsg(msgEnviar, packet.getAddress(), 20181);
                            }
                            break;
                        case "05":
                            // criar conexão TCP e responder com mensagem 6 para o remetente
                            if(!msgReceb.contains("|")){
                                msgEnviar = "06007OK";
                                enviarMsg(msgEnviar, packet.getAddress(), 20181);
                            }
                            break;
                        case "06":
                            // conectar na porta informada
                            inGame = true;
                            break;
                    }
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    protected void process(List<String> msg) {
        // Lidar com mensagens
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public void enviarMsg(String broadcastMessage, InetAddress address, int port)
            throws IOException {

        socketBroadcast = new DatagramSocket();
        socketBroadcast.setBroadcast(true);

        byte[] buffer = broadcastMessage.getBytes();

        DatagramPacket sentPacket = new DatagramPacket(buffer, buffer.length, address, port);
        socketBroadcast.send(sentPacket);
        socketBroadcast.close();
    }
}
