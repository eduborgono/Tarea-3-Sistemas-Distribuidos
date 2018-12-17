package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;


public class Listener extends Thread {

    private AtomicBoolean apagar;
    private Socket clientSocket;
    private BufferedWriter socketWriter;
    private BufferedReader socketReader;
    private Gson gson;
    private String direccionDest;
    @Getter private String direccionIp;
    @Getter private int puerto;

    public Listener(String dir) throws IOException {
        apagar = new AtomicBoolean(false);
        direccionDest = dir;
        direccionIp = InetAddress.getLocalHost().getHostAddress();
        GsonBuilder builder = new GsonBuilder();  
        gson = builder.create();
    }

    private static boolean isSocketAliveUitlity(String hostName, int port) {
		boolean isAlive = false;
		SocketAddress socketAddress = new InetSocketAddress(hostName, port);
		Socket socket = new Socket();
		int timeout = 2000;
		try {
			socket.connect(socketAddress, timeout);
			socket.close();
			isAlive = true;
 
        } catch(Exception e) {
            //System.out.println("No se pudo establecer conexion con la maquina " + hostName);
        }
		return isAlive;
    }

    private void Conectar() throws IOException {
        while(!isSocketAliveUitlity(direccionDest, 7777) && !apagar.get());
        clientSocket = new Socket(direccionDest, 7777); 
        puerto = clientSocket.getLocalPort();
        socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        socketWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        System.out.println("Conectado a " + direccionDest);
    }

    public void run() {
        try {
            Conectar();
            String msj = null;
            while((msj = socketReader.readLine()) != null) {
                //Operacion op = gson.fromJson(msj, Operacion.class);
                //System.out.println(op.toString());
            }
        } catch (Exception e) {  }
        System.out.println("Adios");
        
    }   
    
    public void Dispose() {
        try {
            clientSocket.close();
        } catch(Exception e) { }
        apagar.set(true);
    }

    public void SendOp(Operacion op) throws IOException {
        socketWriter.write(gson.toJson(op));
        socketWriter.write("\n");
        socketWriter.flush();
    }
}