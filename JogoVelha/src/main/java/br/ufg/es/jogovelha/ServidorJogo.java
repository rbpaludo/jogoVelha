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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private String apelido;
    private DefaultListModel onlineUsers;
    private Random random = new Random();
    private MinhaConexao novaConexao;
    private Socket socketCliente;
    private long endTimeMillis = -1;
    private long timeoutInterval = 30000;

    public ServidorJogo(NewJFrame mainFrame, ServerSocket server,
            String apelido, DefaultListModel onlineUsers, InetAddress addr) {

        try {
            this.onlineUsers = onlineUsers;
            this.mainFrame = mainFrame;
            this.server = server;
            this.apelido = apelido;
            socketBroadcast = new DatagramSocket(20181, addr);
            socketBroadcast.setReuseAddress(true);

        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                try {
                    String mensagem = "01";
                    mensagem += String.format("%03d", apelido.length() + 5);
                    mensagem += apelido;

                    enviarMsg(mensagem, InetAddress.getByName("255.255.255.255"), 20181);
                } catch (IOException ex) {
                    Logger.getLogger(NewJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, 180000, 180000);
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        try {

            String msgReceb;

            // escutando na porta tal
            while (true) {
                if (endTimeMillis != -1) {
                    if(System.currentTimeMillis() > endTimeMillis + timeoutInterval){
                        JOptionPane.showMessageDialog(mainFrame, "O jogador convidado demorou demais para responder...");
                    }
                }
                if (inGame) {
                    // espera conexão
                    Socket connection = server.accept();

                    // cria conexão com cliente
                    novaConexao = new MinhaConexao(mainFrame, connection, false);
                    mainFrame.setConexao(novaConexao);
                    // processa as comunicações com o cliente
                    novaConexao.execute();
                    endTimeMillis = -1;
                }
                DatagramPacket packet;
                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);
                socketBroadcast.receive(packet);

                msgReceb = new String(packet.getData(), "UTF-8")/*.trim()*/;
                System.out.println("Recebida: " + msgReceb);
                publish(msgReceb + " - " + packet.getAddress().getHostAddress());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    protected void process(List<String> msg) {
        String msgEnviar;
        String address;
        String apelidoOnline;
        int porta = 0;

        for (String msgReceb : msg) {
            address = msgReceb.split(" - ")[1];
            try {
                switch (msgReceb.substring(0, 2)) {
                    case "01":
                        // responder com mensagem 2 para o remetente
                        msgEnviar = "02";
                        msgEnviar += String.format("%03d",
                                apelido.length() + 5);
                        msgEnviar += apelido;
                        enviarMsg(msgEnviar,
                                InetAddress.getByName(address),
                                20181);
                        apelidoOnline = msgReceb.substring(5,
                                msgReceb.length()) + "-" + InetAddress.getByName(address);
                        if (!apelidoOnline.split("-")[0].equals(apelido + " ")) {
                            onlineUsers.addElement(apelidoOnline);
                        }
                        break;
                    case "02":
                        // adicionar o remetente à lista de usuários ativos
                        apelidoOnline = msgReceb.substring(5,
                                msgReceb.length()) + "-" + InetAddress.getByName(address);
                        if (!apelidoOnline.split("-")[0].equals(apelido + " ")) {
                            onlineUsers.addElement(apelidoOnline);
                        }
                        break;
                    case "03":
                        // remover o remetente`da lista de usuários ativos
                        apelidoOnline = msgReceb.substring(5,
                                msgReceb.length()) + "-" + InetAddress.getByName(address);
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
                            do {
                                porta = random.nextInt(65536);
                            } while (porta == 20181 || porta == 0);
                            msgEnviar += String.format("%03d",
                                    apelido.length() + 6
                                    + String.valueOf(porta).length());
                            msgEnviar += apelido;
                            msgEnviar += "|" + porta;
                            enviarMsg(msgEnviar, InetAddress.getByName(address), 20181);
                            endTimeMillis = System.currentTimeMillis();
                        } else {
                            msgEnviar += String.format("%03d", apelido.length() + 7);
                            msgEnviar += apelido;
                            msgEnviar += "|0";
                            enviarMsg(msgEnviar, InetAddress.getByName(address), 20181);
                            endTimeMillis = -1;
                        }
                        break;
                    case "05":
                        // criar conexão TCP e responder com mensagem 6 para o remetente
                        if (!msgReceb.contains("|0")) {
                            msgEnviar = "06007OK";
                            mainFrame.setPorta(Integer.parseInt(msgReceb.split("|")[1]));
                            server = new ServerSocket(porta, 10, server.getInetAddress());
                            inGame = true;
                            enviarMsg(msgEnviar, InetAddress.getByName(address), 20181);

                            endTimeMillis = System.currentTimeMillis();
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "O convite foi rejeitado");
                            endTimeMillis = -1;
                        }
                        break;
                    case "06":
                        // conectar na porta informada
                        socketCliente = new Socket(InetAddress.getByName(address), porta);
                        novaConexao = new MinhaConexao(this.mainFrame, socketCliente, true);
                        novaConexao.execute();
                        endTimeMillis = -1;
                        break;

                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
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
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }
}
