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
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final Set<String> mayores;
    private final Map<Integer, Boolean> porComprobar;
    private String coordinadorDir;
    private String tsEleccion;

    public BullyClient(int id, int experiencia, int estudios) throws IOException {
        identificador = id;
        idOperacion = 0;
        idOperacionMutex = new Object();
        prioridad1 = experiencia;
        prioridad2 = estudios;
        salir = new AtomicBoolean(false);
        opPendientes = new ConcurrentLinkedQueue<>();
        mayores = new HashSet<String>();
        porComprobar = new HashMap<Integer, Boolean>();
        coordinadorDir = null;
        tsEleccion = null;

        bl = new BullyListener(opPendientes);
        bl.start();
        new Trabajar().start();
    }

    public void Dispose() {
        bl.Dispose();
        salir.set(true);
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

    public void SendOp(int paciente, String procedimeinto) throws IOException {
        synchronized(idOperacionMutex) {
            idOperacion++;
            Operacion op = new Operacion(idOperacion, paciente, procedimeinto);
            op.setEspecial(Operacion.POR_ENVIAR);
            op.setTimestamp(null);
            porComprobar.put(op.getId(), false);
            opPendientes.add(op);
        }
    }

    private class Trabajar extends Thread {
        @Override
        public void run() {
            try {
                Discovery();
            } catch(Exception e) { }
            String asdasdsda = Instant.now().toString();
            while(!salir.get()) {
                if(Duration.between(Instant.parse(asdasdsda), Instant.now()).getSeconds() > 1) {
                    asdasdsda = Instant.now().toString();
                    System.out.println("\t\t\t\t1 "+coordinadorDir);
                }
                if(Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_1)) {
                    System.out.println("timeout win");
                    if(tsEleccion != null) {
                        try {
                            long diffInSeconds = Duration.between(Instant.parse(tsEleccion), Instant.now()).getSeconds();
                            if(diffInSeconds > 15) {
                                AscenderNodo();
                            } 
                        } catch (Exception e) { }
                    }
                }
                while(opPendientes.size() > 0) {
                    if(Duration.between(Instant.parse(asdasdsda), Instant.now()).getSeconds() > 1) {
                        asdasdsda = Instant.now().toString();
                        System.out.println("\t\t\t\t\t2 "+coordinadorDir);
                    }
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
                                if(!Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_2)) {
                                    EmpezarEleccion();
                                }
                            } catch (Exception e) { }
                            break;

                        case Operacion.NUEVO_COORDINADOR_RESPONSE:
                            if(Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_1)) {
                                System.out.println("\tEsperando nuevo coordinador");
                                coordinadorDir = ESPERANDO_COORDINADOR_FASE_2;
                                tsEleccion = null;
                            }
                            break;

                        case Operacion.NUEVO_COORDINADOR_ALL:
                            tsEleccion = null;
                            coordinadorDir = op.getOrigen();
                            System.out.println("\t\tEl nuevo coordinador es " + coordinadorDir);
                            break;
                        
                        case Operacion.DEFECTO:
                            Operacion opAux = new Operacion(op.getId(), 0, "0");
                            opAux.Empaquetar(op.getDest(), op.getOrigen());
                            opAux.setEspecial(Operacion.ENTREGA_CORRECTA);
                            try {
                                bl.SendOp(opAux);
                            } catch(Exception e) { }
                            break;

                        case Operacion.ENTREGA_CORRECTA:
                            porComprobar.put(op.getId(), true);
                            System.out.println("\t\t\tPck:"+op.getId()+"+");
                            break;

                        case Operacion.POR_ENVIAR:
                            if(!porComprobar.get(op.getId())) {
                                if(coordinadorDir == null) {
                                    try {
                                        EmpezarEleccion();
                                        opPendientes.add(op);
                                    } catch(Exception e) { }
                                }
                                else {
                                    if(!Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_2)) {
                                        if(op.getTimestamp() == null) {
                                            try {
                                                op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), coordinadorDir);
                                                op.setEspecial(Operacion.DEFECTO);
                                                op.setTimestamp(Instant.now().toString());
                                                bl.SendOp(op);
                                            } catch(Exception e) { }
                                        }
                                        else {
                                            long diffInSeconds = Duration.between(Instant.parse(op.getTimestamp()), Instant.now()).getSeconds();
                                            if(diffInSeconds > 15) {
                                                op.setTimestamp(null);
                                                EmpezarEleccion();
                                            }
                                        }
                                    }
                                    op.setEspecial(Operacion.POR_ENVIAR);
                                    opPendientes.add(op);
                                }
                            }
                            else {
                                //deshechar
                                porComprobar.remove(op.getId());
                            }
                            break;
                        
                        case Operacion.NUEVO_COORDINADOR_INTENT:
                            if(!Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_2)) {
                                System.out.println("Empezar eleccion");
                                tsEleccion = Instant.now().toString();
                                coordinadorDir = ESPERANDO_COORDINADOR_FASE_1;
                                if(mayores.size() > 0) {
                                    for (String nodo : mayores) {
                                        Operacion auxOp = new Operacion(0, 0, "0");
                                        auxOp.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), nodo);
                                        auxOp.setEspecial(Operacion.NUEVO_COORDINADOR_REQUEST);
                                        try {
                                            bl.SendOp(auxOp);
                                        } catch(Exception e) {}
                                        System.out.println("\t\tEnviando consulta a " + auxOp.getDest());
                                    }
                                }
                            }
                            break;

                        case Operacion.ASCENDER_INTENT:
                            if(Objects.equals(coordinadorDir, ESPERANDO_COORDINADOR_FASE_1)) {
                                try {
                                    Operacion auxOp = new Operacion(1, 0, "0");
                                    auxOp.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), Operacion.BROADCAST);
                                    auxOp.setEspecial(Operacion.NUEVO_COORDINADOR_ALL);
                                    bl.SendOp(auxOp);
                                    tsEleccion = null;
                                    coordinadorDir = bl.getDireccionIp() + ":" + bl.getPuerto();
                                    System.out.println("\t\tAhora yo soy el lider");
                                } catch (Exception e) { }
                            }
                            break;
                    }
                }
                
                try {
                    Thread.sleep(100);
                } catch(Exception e) { }
            }
        }
    }
}