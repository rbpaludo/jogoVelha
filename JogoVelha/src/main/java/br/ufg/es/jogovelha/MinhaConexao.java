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
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author willi
 */
public class MinhaConexao extends SwingWorker<Boolean, String>  {
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
    
    public MinhaConexao(NewJFrame mainFrame, Socket socket/*,
                        DefaultListModel mensagens*/) throws SocketException
    {
        this.mainFrame = mainFrame;
        this.socket = socket;
        //this.mensagens = mensagens;
        
        try
        {
            entrada  = socket.getInputStream();
            inr = new InputStreamReader(entrada);
            bfr = new BufferedReader(inr);
            
            saida =  this.socket.getOutputStream();
            outw = new OutputStreamWriter(saida);
            bfw = new BufferedWriter(outw); 
        }
        catch (IOException e)
        {
            publish("Erro na conexão com o servidor");
            publish(e.getMessage());
        } 
        
        publish("Servidor conectado na porta " + socket.getLocalPort());
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        String incommingMessage;
        while(true)
        {
            try
            {
                incommingMessage = (String)bfr.readLine();
                
                // Recebeu uma mensagem?
                if (incommingMessage != null)
                {
                    String msg = "Recebida: " + incommingMessage;
                    publish(msg);
                    
                    switch(incommingMessage.substring(0, 2)){
                        case "07":
                            // my turn? send
                            break;
                        
                        case "08":
                            // did he/she win? 
                                // send message 9
                                // send message 8
                            // send message 8
                            break;
                        
                        case "09":
                            // clear board, add 1 victory
                            break;
                            
                        case "10":
                            // terminate connection
                    }
                    // lidar com a mensagem recebida
                }
                else
                {
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
            }
            catch(IOException e)
            {
                // mostra mensagem de erro
                JOptionPane.showMessageDialog(mainFrame,
                        "Erro na leitura\n" + e.getMessage(),
                        "Servidor", JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
    }
    
    @Override
    protected void process(List<String> msg)
    {
        for (int i = 0; i < msg.size(); i++)
            JOptionPane.showMessageDialog(null, msg.get(i));
    }
    
    public boolean enviaMensagem(String msg)
    {
        try
        {
            outw.write(msg + "\n");
            outw.flush();
            System.out.println("Mensagem enviada: " + msg);
            return true;
        }catch(IOException ex)
        {
            publish("Erro ao enviar mensagem.");
            return false;
        }
    }
    
}
