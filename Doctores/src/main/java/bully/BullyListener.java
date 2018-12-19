package bully;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Queue;

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

    /**
     * Los objetos de esta clase se manejar la conexión con los Handlers, 
     * enviandoles mensajes, o recibiendolos.
     * Es un Thread
     */
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
                if(Objects.equals(op.getEspecial(), Operacion.DEFECTO)) {
                    System.out.println("\t" + op.toString());
                }
                opPendientes.offer(op);
            }
        } catch (Exception e) {  }
        System.out.println("Adios");
        
    }   
    
    /**
     * Limpieza del thread, cierra la conexion con el Handler
     */
    public void Dispose() {
        try {
            clientSocket.close();
        } catch(Exception e) {
            
        }
    }

    /**
     * Envía un mensaje al handler para que este lo redistribuya
     */
    public void SendOp(Operacion op) throws IOException {
        socketWriter.write(gson.toJson(op));
        socketWriter.write("\n");
        socketWriter.flush();
    }
}