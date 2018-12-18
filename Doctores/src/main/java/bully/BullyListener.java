package bully;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;


class BullyListener extends Thread {

    private Socket clientSocket;
    private BufferedWriter socketWriter;
    private BufferedReader socketReader;
    private Gson gson;
    @Getter private String direccionIp;
    @Getter private int puerto;
    private final Queue<Operacion> opPendientes;

    public BullyListener(Queue<Operacion> opPendientes) throws IOException {
        this.opPendientes = opPendientes;
        clientSocket = new Socket(InetAddress.getLocalHost().getHostAddress(), 7777); 
        direccionIp = InetAddress.getLocalHost().getHostAddress();//InetAddress.getLocalHost().getHostAddress();
        puerto = clientSocket.getLocalPort();
        socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        socketWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        GsonBuilder builder = new GsonBuilder();  
        gson = builder.create();
    }

    public void run() {
        String msj = null;
        try {
            while((msj = socketReader.readLine()) != null) {
                Operacion op = gson.fromJson(msj, Operacion.class);
                if(!Objects.equals(op.getEspecial(), Operacion.ERROR_ENTREGA) && !Objects.equals(op.getEspecial(), Operacion.ENTREGA_CORRECTA)) {
                    System.out.println("\t" + op.toString());
                }
                opPendientes.add(op);
            }
        } catch (Exception e) {  }
        System.out.println("Adios");
        
    }   
    
    public void Dispose() {
        try {
            clientSocket.close();
        } catch(Exception e) {
            
        }
    }

    public void SendOp(Operacion op) throws IOException {
        socketWriter.write(gson.toJson(op));
        socketWriter.write("\n");
        socketWriter.flush();
    }
}