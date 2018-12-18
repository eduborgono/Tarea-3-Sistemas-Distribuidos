package bully;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BullyClient {

    public static final String ESPERANDO_COORDINADOR_FASE_1 = "ESPERANDO_COORDINADOR_FASE_1";
    public static final String ESPERANDO_COORDINADOR_FASE_2 = "ESPERANDO_COORDINADOR_FASE_2";

    private int idOperacion;
    private final Object idOperacionMutex;
    private int identificador;
    private int prioridad1;
    private int prioridad2;
    private final BullyListener bl;
    private AtomicBoolean salir;
    private final Queue<Operacion> opPendientes; 
    private final Object mutexOp;
    private final Set<String> mayores;
    private final Map<Integer, Operacion> porComprobar;
    private AtomicReference<String> coordinadorDir;
    private AtomicReference<String> tsEleccion;

    public BullyClient(int id, int experiencia, int estudios) throws IOException {
        identificador = id;
        idOperacion = 0;
        idOperacionMutex = new Object();
        prioridad1 = experiencia;
        prioridad2 = estudios;
        salir = new AtomicBoolean(false);
        opPendientes = new LinkedList<>();
        mutexOp = new Object();
        mayores = new HashSet<String>();
        porComprobar = new HashMap<Integer, Operacion>();
        coordinadorDir = new AtomicReference<String>();
        tsEleccion = new AtomicReference<String>();

        bl = new BullyListener(opPendientes, mutexOp);
        bl.start();
        new Trabajar().start();
    }


    private void EmpezarEleccion() {
        Operacion op = new Operacion(0, 0, "0");
        op.setEspecial(Operacion.NUEVO_COORDINADOR_INTENT);
        opPendientes.add(op);
    }

    private void AscenderNodo() {
        Operacion op = new Operacion(0, 0, "0");
        op.setEspecial(Operacion.ASCENDER_INTENT);
        opPendientes.add(op);
    }

    public void Discovery() throws IOException  {
        Operacion op = new Operacion(2, prioridad1+prioridad2, "0");
        op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), Operacion.BROADCAST);
        op.setEspecial(Operacion.DISCOVERY_REQUEST);
        bl.SendOp(op);
    }

    public void Dispose() {
        bl.Dispose();
        salir.set(true);
    }

    private class Trabajar extends Thread {
        @Override
        public void run() {
            try {
                Discovery();
            } catch(Exception e) { }
            while(!salir.get()) {
                synchronized(mutexOp) {
                    if(Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1)) {
                        if(tsEleccion.get() != null) {
                            try {
                                long diffInSeconds = Duration.between(Instant.parse(tsEleccion.get()), Instant.now()).getSeconds();
                                if(diffInSeconds > 15) {
                                    AscenderNodo();
                                } 
                            } catch (Exception e) { }
                        }
                    }
                    while(opPendientes.size() > 0) {
                        Operacion op = opPendientes.remove();
                        switch(op.getEspecial()) {
                            case Operacion.DISCOVERY_REQUEST:
                                if((prioridad1+prioridad2) > op.getIdPaciente())
                                {
                                    try {
                                        Operacion opResponse = new Operacion(3, 0, "0");
                                        opResponse.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), op.getOrigen());
                                        opResponse.setEspecial(Operacion.DISCOVERY_RESPONSE);
                                        bl.SendOp(opResponse);
                                    } catch (Exception e) { }
                                }
                                else {
                                    mayores.add(op.getOrigen());
                                    System.out.println("\t\tAgregado como mayor "+op.getOrigen());
                                }
                                break;

                            case Operacion.DISCOVERY_RESPONSE:
                                mayores.add(op.getOrigen());
                                System.out.println("\t\tAgregado como mayor "+op.getOrigen());
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_REQUEST:
                                try {
                                    Operacion opResponse = new Operacion(4, 0, "0");
                                    opResponse.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), op.getOrigen());
                                    opResponse.setEspecial(Operacion.NUEVO_COORDINADOR_RESPONSE);
                                    bl.SendOp(opResponse);
                                    if(!Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_2)) {
                                        EmpezarEleccion();
                                    }
                                } catch (Exception e) { }
                                break;

                            case Operacion.NUEVO_COORDINADOR_RESPONSE:
                                if(Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1)) {
                                    System.out.println("\tEsperando nuevo coordinador");
                                    coordinadorDir.set(ESPERANDO_COORDINADOR_FASE_2);
                                    tsEleccion.set(null);
                                }
                                break;

                            case Operacion.NUEVO_COORDINADOR_ALL:
                                tsEleccion.set(null);
                                coordinadorDir.set(op.getOrigen());
                                break;
                            
                            case Operacion.POR_ENVIAR:
                                if(coordinadorDir.get() == null) {
                                    opPendientes.add(op);
                                    try {
                                        EmpezarEleccion();
                                    } catch(Exception e) { }
                                }
                                else {
                                    if(!Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_2)) {
                                        try {
                                            op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), coordinadorDir.get());
                                            op.setEspecial("DEFECTO");
                                            bl.SendOp(op);
                                        } catch(Exception e) { }
                                    }
                                    else {
                                        opPendientes.add(op);
                                    }
                                }
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_INTENT:
                                if(!Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_2)) {
                                    System.out.println("Empezar eleccion");
                                    tsEleccion.set(Instant.now().toString());
                                    coordinadorDir.set(ESPERANDO_COORDINADOR_FASE_1);
                                    if(mayores.size() > 0) {
                                        for (String nodo : mayores) {
                                            Operacion auxOp = new Operacion(0, 0, "0");
                                            auxOp.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), nodo);
                                            auxOp.setEspecial(Operacion.NUEVO_COORDINADOR_REQUEST);
                                            try {
                                                bl.SendOp(auxOp);
                                                System.out.println("\t\tEnviando consulta a " + auxOp.getDest());
                                            } catch(Exception e) {}
                                        }
                                    }
                                }
                                break;

                            case Operacion.ASCENDER_INTENT:
                                if(Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1)) {
                                    try {
                                        Operacion auxOp = new Operacion(1, 0, "0");
                                        auxOp.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), Operacion.BROADCAST);
                                        auxOp.setEspecial(Operacion.NUEVO_COORDINADOR_ALL);
                                        bl.SendOp(auxOp);
                                        tsEleccion.set(null);
                                        coordinadorDir.set(bl.getDireccionIp() + ":" + bl.getPuerto());
                                        System.out.println("\t\tAhora yo soy el lider");
                                    } catch (Exception e) { }
                                }
                                break;
                        }
                    }
                }     
            }
        }
    }

    public void SendOp(int paciente, String procedimeinto) throws IOException {
        synchronized(idOperacionMutex) {
            synchronized(mutexOp) {
                idOperacion++;
                Operacion op = new Operacion(idOperacion, paciente, procedimeinto);
                op.setEspecial(Operacion.POR_ENVIAR);
                opPendientes.add(op);
            }
        }
    }
}