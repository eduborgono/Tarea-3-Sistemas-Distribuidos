import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;

import util.Operacion;
import util.Listener;

class App {
    public static final String[] machines = {"10.6.40.205", "10.6.40.206", "10.6.40.207", "10.6.40.208"};
    private final Map<String, Listener> machineMap;
    private final Object mutexMachineMap;
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
        machineMap = new HashMap<String, Listener>();
        cerrar = new AtomicBoolean(false);
        threadMap = new HashMap<String, ClientHandler>();
        opPendientes = new LinkedList<>();
        mutexMap = new Object();
        mutexMachineMap = new Object();
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
        for (String machine : machines) {
            if(!Objects.equals(machine, direccionIp)) {
                synchronized(mutexMachineMap) {
                    try {
                        Listener listenerMachine = new Listener(machine);
                        machineMap.put(machine, listenerMachine);
                        listenerMachine.start();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        while(!cerrar.get()) {
            try {
                socket = serverSocket.accept();
                System.out.println(socket.getInetAddress().getHostAddress() + " " + socket.getPort());
                ClientHandler clientHandler = new ClientHandler(socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), socket);
                synchronized(mutexMap){
                    threadMap.put(socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), clientHandler);
                }
                clientHandler.start();

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void Dispose() {
        synchronized(mutexMachineMap){
            for (Map.Entry<String, Listener> entry : machineMap.entrySet()) {
                try {
                    entry.getValue().Dispose();
                } catch (Exception e) { }
            }
        }
        synchronized(mutexMap) {
            for (Map.Entry<String, ClientHandler> entry : threadMap.entrySet()) {
                try {
                    entry.getValue().socket.close();
                } catch (Exception e) { }
            }
        }
        try {
            socket.close();
        } catch (Exception e) {}
        cerrar.set(true);
        System.out.println("Xao");
    }

    private class ReSender extends Thread {
        public void run() {
            while(!cerrar.get()) {
                synchronized(mutexPendientes) {
                    
                    while(opPendientes.size() > 0) {
                        Operacion op = opPendientes.remove();
                        synchronized(mutexMachineMap) {
                            //Recibe mensaje de la subred y envía al resto (BROADCAST )
                            if(Objects.equals(op.getDest(),Operacion.BROADCAST)) {
                                String[] address = op.getOrigen().split(":");
                                if(Objects.equals(address[0], direccionIp)) {
                                    for (Map.Entry<String, Listener> machine : machineMap.entrySet()) {
                                        try {
                                            machine.getValue().SendOp(op);
                                        } catch(Exception e) {
                                            System.out.println("MAQUINA: No se pudo enviar mensaje a " + machine.getKey() );
                                        }
                                    }
                                }
                            }
                            //Recibe mensaje de la subred y envía fuera de la subred (otra maquina)
                            else {
                                String[] address = op.getDest().split(":");
                                if(!Objects.equals(address[0], direccionIp)) {
                                    try {
                                        machineMap.get(address[0]).SendOp(op);
                                    } catch(Exception e) {
                                        System.out.println("MAQUINA: No se pudo enviar mensaje a " + address[0] );
                                    }
                                }
                            }
                        }
                        synchronized(mutexMap) {
                            //Recepcion de mensajes, reenvío hacia la subred
                            for (Map.Entry<String, ClientHandler> entry : threadMap.entrySet()) {
                                if(Objects.equals(op.getDest(), entry.getKey()) || Objects.equals(op.getDest(), Operacion.BROADCAST)) {
                                    try {
                                        synchronized(entry.getValue().mutexWriter) {
                                            entry.getValue().socketWriter.write(gson.toJson(op));
                                            entry.getValue().socketWriter.write("\n");
                                            entry.getValue().socketWriter.flush();
                                        }
                                    } catch(Exception e) {
                                        System.out.println("Mensaje directo: No se pudo enviar mensaje a " + entry.getKey() );
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
                    opPendientes.add(op);
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
                   } catch (Exception e) { }
                }
             });
            app.Listen();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
