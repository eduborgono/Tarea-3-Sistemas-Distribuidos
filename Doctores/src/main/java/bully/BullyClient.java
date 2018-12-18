package bully;

import java.io.IOException;
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


    public void EmpezarEleccion() {
        if(!Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_2)) {
            System.out.println("Empezar eleccion");
            tsEleccion.set(Instant.now().toString());
            coordinadorDir.set(ESPERANDO_COORDINADOR_FASE_1);
            if(mayores.size() > 0) {
                for (String nodo : mayores) {
                    Operacion op = new Operacion(0, 0, "0");
                    op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), nodo);
                    op.setEspecial(Operacion.NUEVO_COORDINADOR_REQUEST);
                    try {
                        bl.SendOp(op);
                        System.out.println("\t\tEnviando consulta a " + op.getDest());
                    } catch(Exception e) {}
                }
            }
        }
    }

    private void AscenderNodo() {
        if(Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1)) {
            try {
                Operacion op = new Operacion(1, 0, "0");
                op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), Operacion.BROADCAST);
                op.setEspecial(Operacion.NUEVO_COORDINADOR_ALL);
                bl.SendOp(op);
                tsEleccion.set(null);
                coordinadorDir.set(bl.getDireccionIp() + ":" + bl.getPuerto());
                System.out.println("\t\tAhora yo soy el lider");
            } catch (Exception e) { }
        }
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
                    while(opPendientes.size() > 0) {
                        if(Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1)) {
                            if(tsEleccion.get() != null) {
                                try {
                                    long diffInSeconds = Duration.between(Instant.parse(tsEleccion.get()), Instant.now()).getSeconds();
                                    if(diffInSeconds > 10) {
                                        AscenderNodo();
                                    } 
                                } catch (Exception e) { }
                            }
                        }
                        else {
                            if(coordinadorDir.get() != null && !Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_2)) {
                                synchronized(idOperacionMutex) {
                                    for (Map.Entry<Integer, Operacion> entry : porComprobar.entrySet()) {
                                        long diffInSeconds = Duration.between(Instant.parse(entry.getValue().getTimestamp()), Instant.now()).getSeconds();
                                        //Murio el coordinador
                                        if(diffInSeconds > 10) {
                                            EmpezarEleccion();
                                            break;
                                        }
                                    }
                                }
                            }
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
                                    if(!Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1) && !Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_2)) {
                                        EmpezarEleccion();
                                    }
                                } catch (Exception e) { }
                                break;

                            case Operacion.NUEVO_COORDINADOR_RESPONSE:
                                if(Objects.equals(coordinadorDir.get(), ESPERANDO_COORDINADOR_FASE_1)) {
                                    coordinadorDir.set(ESPERANDO_COORDINADOR_FASE_2);
                                    tsEleccion.set(null);
                                }
                                break;

                            case Operacion.NUEVO_COORDINADOR_ALL:
                                tsEleccion.set(null);
                                coordinadorDir.set(op.getOrigen());
                                try {
                                    Thread.sleep(500);
                                } catch (Exception e) { }
                                synchronized(idOperacionMutex) {
                                    for (Map.Entry<Integer, Operacion> entry : porComprobar.entrySet()) {
                                        try {
                                            entry.getValue().setDest(coordinadorDir.get());
                                            op.setEspecial("DEFECTO1");
                                            bl.SendOp(op);
                                        } catch (Exception e) { }
                                    } 
                                }
                                break;

                            case Operacion.ENTREGA_CORRECTA:
                                synchronized(idOperacionMutex) {
                                    porComprobar.remove(op.getId());
                                }
                                break;
                        }
                    }
                }     
            }
        }
    }

    public void SendOp(int paciente, String procedimeinto) throws IOException {
        if(coordinadorDir.get() == null) {
            EmpezarEleccion();
        }
        while((coordinadorDir.get() == null));
        synchronized(idOperacionMutex) {
            idOperacion++;
            Operacion op = new Operacion(idOperacion, paciente, procedimeinto);
            op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), coordinadorDir.get());
            op.setTimestamp(Instant.now().toString());
            porComprobar.put(idOperacion, op);
            try {
                op.setEspecial("DEFECTO2");
                bl.SendOp(op);
            } catch(Exception e) { }
        }
    }
}