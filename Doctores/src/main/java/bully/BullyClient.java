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
    private AtomicBoolean coordinador;
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
        coordinador = new AtomicBoolean(false);

        bl = new BullyListener(opPendientes, mutexOp);
        bl.start();
        new Trabajar().start();
    }


    public void EmpezarEleccion() {
        tsEleccion.set(Instant.now().toString());
        coordinadorDir.set(null);
        if(mayores.size() > 0) {
            for (String nodo : mayores) {
                Operacion op = new Operacion(0, prioridad1+prioridad2, "0");
                op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), nodo);
                op.setEspecial(Operacion.NUEVO_COORDINADOR_REQUEST);
                try {
                    bl.SendOp(op);
                    System.out.println("\t\tEnviando consulta a " + op.getDest());
                } catch(Exception e) {}
            }
        }
    }

    private void AscenderNodo() {
        try {
            Operacion op = new Operacion(0, 0, "0");
            op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), Operacion.BROADCAST);
            op.setEspecial(Operacion.NUEVO_COORDINADOR_ALL);
            bl.SendOp(op);
            tsEleccion.set(null);
            coordinador.set(true);
            coordinadorDir.set(bl.getDireccionIp() + ":" + bl.getPuerto());
            System.out.println("\t\tAhora yo soy el lider");
        } catch (Exception e) { }
    }

    public void Discovery() throws IOException  {
        Operacion op = new Operacion(0, prioridad1+prioridad2, "0");
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
                if(tsEleccion.get() != null) {
                    try {
                        long diffInSeconds = Duration.between(Instant.parse(tsEleccion.get()), Instant.now()).getSeconds();
                        if(diffInSeconds > 10) {
                        
                            AscenderNodo();
                        } 
                    } catch (Exception e) { }
                }
                synchronized(idOperacionMutex) {
                    for (Map.Entry<Integer, Operacion> entry : porComprobar.entrySet()) {
                        long diffInSeconds = Duration.between(Instant.parse(entry.getValue().getTimestamp()), Instant.now()).getSeconds();
                        //Murio el coordinador
                        if(diffInSeconds > 10) {
                            EmpezarEleccion();
                        }
                    }
                }
                synchronized(mutexOp) {
                    while(opPendientes.size() > 0) {
                        Operacion op = opPendientes.remove();
                        switch(op.getEspecial()) {
                            case Operacion.DEFECTO:
                                try {
                                    Operacion opResponse = new Operacion(op.getId(), 0, "0");
                                    opResponse.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), op.getOrigen());
                                    opResponse.setEspecial(Operacion.ENTREGA_CORRECTA);
                                    bl.SendOp(opResponse);
                                    //procesar cosas
                                } catch (Exception e) { }
                                break;
                            case Operacion.DISCOVERY_REQUEST:
                                if(op.getIdPaciente() < (prioridad1+prioridad2))
                                {
                                    try {
                                        Operacion opResponse = new Operacion(op.getId(), 0, "0");
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
                                    Operacion opResponse = new Operacion(op.getId(), 0, "0");
                                    opResponse.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), op.getOrigen());
                                    opResponse.setEspecial(Operacion.NUEVO_COORDINADOR_RESPONSE);
                                    bl.SendOp(opResponse);
                                    EmpezarEleccion();
                                } catch (Exception e) { }
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_RESPONSE:
                                tsEleccion.set(null);
                                coordinadorDir.set(Operacion.ESPERANDO_COORDINADOR);
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_ALL:
                                tsEleccion.set(null);
                                coordinadorDir.set(op.getOrigen());
                                synchronized(idOperacionMutex) {
                                    for (Map.Entry<Integer, Operacion> entry : porComprobar.entrySet()) {
                                        try {
                                            entry.getValue().setDest(coordinadorDir.get());
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
        while((coordinadorDir.get() == null) || Objects.equals(coordinadorDir.get(), Operacion.ESPERANDO_COORDINADOR));
        synchronized(idOperacionMutex) {
            idOperacion++;
            Operacion op = new Operacion(idOperacion, paciente, procedimeinto);
            op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), coordinadorDir.get());
            op.setTimestamp(Instant.now().toString());
            porComprobar.put(idOperacion, op);
            try {
                bl.SendOp(op);
            } catch(Exception e) { }
        }
    }
}