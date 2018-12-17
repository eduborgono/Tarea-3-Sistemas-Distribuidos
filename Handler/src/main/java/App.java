import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;

import util.Operacion;

class App {

    private ServerSocket serverSocket;
    private Socket socket;
    private final Map<String, ClientHandler> threadMap;
    private final Object mutexMap;
    private final Queue<Operacion> opPendientes;
    private final Object mutexPendientes;
    private final AtomicBoolean cerrar;
    private String direccionIp;
    private Gson gson;

    private App() {
        cerrar = new AtomicBoolean(false);
        threadMap = new HashMap<String, ClientHandler>();
        opPendientes = new LinkedList<>();
        mutexMap = new Object();
        mutexPendientes = new Object();
        GsonBuilder builder = new GsonBuilder();  
        gson = builder.create();
        try {
            serverSocket = new ServerSocket(7777);
            direccionIp = InetAddress.getLocalHost().getHostAddress();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void Listen() {
        ReSender reSender = new ReSender();
        reSender.start();
        while(!cerrar.get()) {
            try {
                socket = serverSocket.accept();
                System.out.println(socket.getInetAddress().getHostAddress() + " " + socket.getPort());
                ClientHandler clientHandler = new ClientHandler(socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), socket);
                synchronized(mutexMap){
                    threadMap.put(socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), clientHandler);
                    //System.out.println(threadMap.toString());
                }
                clientHandler.start();

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void Dispose() {
        synchronized(mutexMap){
            for (Map.Entry<String, ClientHandler> entry : threadMap.entrySet()) {
                try {
                entry.getValue().socket.close();
                } catch (Exception e) { }
            }
            try {
                socket.close();
            } catch (Exception e) {}
            System.out.println("Cleaned");
        }
        cerrar.set(true);
        System.out.println("Xao");
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
 
        } catch(Exception e) {}
		return isAlive;
	}

    private class ReSender extends Thread {
        public void run() {
            while(!cerrar.get()) {
                synchronized(mutexPendientes) {
                    
                    while(opPendientes.size() > 0) {
                        Operacion op = opPendientes.remove();
                        if(Objects.equals(op.getDest(),Operacion.BROADCAST)) {
                            synchronized(mutexMap) {
                                for (Map.Entry<String, ClientHandler> entry : threadMap.entrySet()) {
                                    String[] address = entry.getKey().split(":");
                                    System.out.println(address[0]+ " "+direccionIp);
                                    if(Objects.equals(address[0], direccionIp)) {
                                        try {
                                            synchronized(entry.getValue().mutexWriter) {
                                                entry.getValue().socketWriter.write(gson.toJson(op));
                                                entry.getValue().socketWriter.write("\n");
                                                entry.getValue().socketWriter.flush();
                                            }
                                        } catch(Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private String id;
        BufferedReader socketReader = null;
        BufferedWriter socketWriter = null;
        Object mutexWriter;

        public ClientHandler(String id, Socket socket) throws IOException {
            mutexWriter = new Object();
            this.id = id;
            this.socket = socket;
            this.socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @Override
        public void run() {
            GsonBuilder builder = new GsonBuilder(); 
            Gson gson = builder.create();

            try {
                String msj = null;
                while((msj = socketReader.readLine()) != null) {
                    Operacion op = gson.fromJson(msj, Operacion.class);
                    if(Objects.equals(op.getDest(), Operacion.BROADCAST_LOCAL)) {
                        synchronized(mutexPendientes) {
                            opPendientes.add(op);
                        }
                    }
                    else {
                        synchronized(mutexWriter) {
                            socketWriter.write(gson.toJson(op));
                            socketWriter.write("\n");
                            socketWriter.flush();
                        }
                    }
                }
            }
            catch (Exception e) { }
            finally {
                synchronized(mutexMap) {
                    threadMap.remove(id);
                }
                try {
                    socket.close();
                    System.out.println("Conexion cerrada");
                } catch (Exception e) { }
            }
        }
    }

    public static void main(String[] args) {
        try {
            final App app = new App();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                   System.out.println("Matando servidor....");
                   try {
                      app.Dispose();
                   } catch (Exception e) {
                   }
                }
             });
            app.Listen();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
