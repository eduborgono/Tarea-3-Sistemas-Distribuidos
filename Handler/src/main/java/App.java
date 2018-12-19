import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;

import util.Operacion;
import util.Escritura;
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
    private final Queue<Operacion> prioritarias;
    private final Object mutexPendientes;
    private final AtomicBoolean cerrar;
    private String direccionIp;
    private Gson gson;


    /**
     * 
     * El handler se encarga de conectar todos los clientes que existan en una máquina
     * ya sean medicos, enfermeros o paramedicos. Recibe los mensajes de estos y los reenvía 
     * a los otros Handlers, para que estos se encarguen de enviar los mensajes a sus destinatarios.
     * Se puede imaginar como los supernodos de una red peer-to-peer.
     * 
     */
    private App() {
        machineMap = new HashMap<String, Listener>();
        cerrar = new AtomicBoolean(false);
        threadMap = new HashMap<String, ClientHandler>();
        opPendientes = new LinkedList<>();
        prioritarias = new LinkedList<>();
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

    /**
     * 
     * Función que mantiene a la espera de nuevos nodos. Cuando recibe uno se lo delega a un thread que
     * maneja la conexion. 
     * 
     */
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

    /**
     * 
     * Función que elimina los threads cuando estos ya no se seguirán usando.
     * En otras palabras, cuando el programa ce cierra se encarga de cerrar los
     * threads que puedan estar corriendo ens segundo plano.
     * 
     */
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

    /**
     * 
     * Thread de reenvío de mensajes. Se encarga que entregar los mensajes que provengan de otros handlers
     * o nodos.
     * 
     */
    private class ReSender extends Thread {
        private String ultimoSpreading;
        public void run() {
            Instant ultimoSpreading = Instant.now();
            while(!cerrar.get()) {
                if(Objects.equals(direccionIp, "10.6.40.205")) {
                    if(Duration.between(ultimoSpreading, Instant.now()).getSeconds() > 20) {
                        try {
                            Escritura.Aux();
                            ultimoSpreading = Instant.now();
                            String archivo = Escritura.CopiarLog();
                            Operacion opFile = new Operacion(0, 0, archivo);
                            opFile.setEspecial(Operacion.COPY_LOG);
                            opFile.Empaquetar("0", "10.6.40.206:7777");
                            machineMap.get("10.6.40.206").SendOp(opFile);
                            opFile.Empaquetar("0", "10.6.40.207:7777");
                            machineMap.get("10.6.40.207").SendOp(opFile);
                            opFile.Empaquetar("0", "10.6.40.208:7777");
                            machineMap.get("10.6.40.208").SendOp(opFile);
                        } catch(Exception e) {}
                    }
                }
                synchronized(mutexPendientes) {
                    if(!opPendientes.isEmpty()) {
                        Operacion op = opPendientes.poll();
                        if(Objects.equals(op.getEspecial(), Operacion.WRITE_FLAG)) {
                            if(Objects.equals(direccionIp, "10.6.40.205"))
                            {
                                try {
                                    System.out.println(op.getProcedimeinto());
                                    String[] separacion = op.getProcedimeinto().split("\\|");
                                    System.out.println(separacion[0]);
                                    System.out.println(separacion[1]);
                                    System.out.println(separacion[2]);
                                    Escritura.Aux();
                                    //Escritura.EscribirPaciente(op.getIdPaciente(), separacion[1], separacion[2]);
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    Date date = new Date();
                                    String log = "["+dateFormat.format(date)+"] "+separacion[1]+" "+separacion[0]+" "+separacion[2];
                                    //Escritura.EscribirLogFinal(log);
                                }
                                catch(Exception e) { 
                                    e.printStackTrace(); }
                            }
                        }
                        else if(Objects.equals(op.getEspecial(), Operacion.COPY_LOG)) {
                            try {
                                Escritura.UpdateLog(op.getProcedimeinto());
                            } catch(Exception e) {}
                        }
                        synchronized(mutexMachineMap) {
                            try {
                                String[] address = op.getDest().split(":");
                                if(!Objects.equals(address[0], direccionIp)) {
                                    try {
                                        machineMap.get(address[0]).SendOp(op);
                                    } catch(Exception e) {
                                        System.out.println("MAQUINA: No se pudo enviar mensaje a " + op.toString());
                                    }
                                }
                            } catch(Exception e) {
                                System.out.println("FAIL: " + op.toString());
                            }
                        
                        }
                        synchronized(mutexMap) {
                            //Recepcion de mensajes, reenvío hacia la subred
                            for (Map.Entry<String, ClientHandler> entry : threadMap.entrySet()) {
                                if(Objects.equals(op.getDest(), entry.getKey())) {
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
                    if(!prioritarias.isEmpty()) {
                        Operacion op = prioritarias.poll();
                        if(Objects.equals(op.getEspecial(), Operacion.NUEVO_COORDINADOR_ALL)) {
                            if(Objects.equals(direccionIp, "10.6.40.205"))
                            {
                                try {
                                    Escritura.Aux();
                                    String[] separacion = op.getProcedimeinto().split("\\|");
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    Date date = new Date();
                                    String log = "["+dateFormat.format(date)+"] "+separacion[1]+" "+separacion[0]+" es ahora el coordinador";
                                    Escritura.EscribirLogFinal(log);
                                }
                                catch(Exception e) { e.printStackTrace(); }
                            }
                        }
                        synchronized(mutexMachineMap) {
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
                        synchronized(mutexMap) {
                            for (Map.Entry<String, ClientHandler> entry : threadMap.entrySet()) {
                                if(!Objects.equals(op.getOrigen(), entry.getKey())) {
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

    /**
     * 
     * Thread que administra la conexion de los nodos.
     * 
     */
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
                    if(Objects.equals(op.getDest(), Operacion.BROADCAST)) {
                        prioritarias.offer(op);
                    }
                    else {
                        opPendientes.offer(op);
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
