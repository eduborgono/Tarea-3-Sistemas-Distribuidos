package bully;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BullyClient {
    private int idOperacion;
    private int identificador;
    private int prioridad1;
    private int prioridad2;
    private final BullyListener bl;
    private AtomicBoolean salir;
    private final Queue<Operacion> opPendientes; 
    private final Object mutexOp;
    private final ArrayList<String> mayores;
    private AtomicBoolean coordinador;
    private AtomicReference<String> coordinadorDir;
    private AtomicReference<String> tsEleccion;

    public BullyClient(int id, int experiencia, int estudios) throws IOException {
        identificador = id;
        idOperacion = 0;
        prioridad1 = experiencia;
        prioridad2 = estudios;
        salir = new AtomicBoolean(false);
        opPendientes = new LinkedList<>();
        mutexOp = new Object();
        mayores = new ArrayList<>();

        coordinadorDir = new AtomicReference<>();
        tsEleccion = new AtomicReference<>();
        coordinador = new AtomicBoolean(false);

        bl = new BullyListener(opPendientes, mutexOp);
        bl.start();
        new Trabajar().start();
    }


    public void EmpezarEleccion() throws IOException {
        if(mayores.size() > 0) {
            for (String nodo : mayores) {
                idOperacion++;
                Operacion op = new Operacion(idOperacion, prioridad1+prioridad2, "0");
                op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), nodo);
                op.setEspecial(Operacion.NUEVO_COORDINADOR_REQUEST);
                bl.SendOp(op);
            }
            tsEleccion.set(Instant.now().toString());
        }
        else {
            AscenderNodo();
        }
    }

    private void AscenderNodo() {
        try {
            idOperacion++;
            Operacion op = new Operacion(0, 0, "0");
            op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), Operacion.BROADCAST);
            op.setEspecial(Operacion.NUEVO_COORDINADOR_ALL);
            bl.SendOp(op);
            tsEleccion.set(null);
            coordinador.set(true);
            System.out.println("Ahora yo soy el lider");
        } catch (Exception e) { }
    }

    public void Discovery() throws IOException  {
        idOperacion++;
        Operacion op = new Operacion(idOperacion, prioridad1+prioridad2, "0");
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
                    long diffInSeconds = Duration.between(Instant.parse(tsEleccion.get()), Instant.now()).getSeconds();
                    if(diffInSeconds > 5) {
                        try {
                            AscenderNodo();
                        } catch (Exception e) { }
                    } 
                }
                synchronized(mutexOp) {
                    while(opPendientes.size() > 0) {
                        Operacion op = opPendientes.remove();
                        switch(op.getEspecial()) {
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
                                }
                                break;

                            case Operacion.DISCOVERY_RESPONSE:
                                mayores.add(op.getOrigen());
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_REQUEST:
                                try {
                                    Operacion opResponse = new Operacion(op.getId(), 0, "0");
                                    opResponse.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), op.getOrigen());
                                    opResponse.setEspecial(Operacion.NUEVO_COORDINADOR_RESPONSE);
                                    bl.SendOp(opResponse);
                                    tsEleccion.set(Instant.now().toString());
                                } catch (Exception e) { }
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_RESPONSE:
                                tsEleccion.set(null);
                                coordinadorDir.set(Operacion.ESPERANDO_COORDINADOR);
                                break;
                            
                            case Operacion.NUEVO_COORDINADOR_ALL:
                                coordinadorDir.set(op.getOrigen());
                                break;
                        }
                    }
                }
                try{
                    Thread.sleep(150);
                } catch(Exception e) {}           
            }
        }
    }

    public void SendOp(int paciente, String procedimeinto) throws IOException {
        if(coordinadorDir.get() == null) {
            EmpezarEleccion();
        }
        while((coordinadorDir.get() == null) || Objects.equals(coordinadorDir.get(), Operacion.ESPERANDO_COORDINADOR));
        Operacion op = new Operacion(idOperacion, paciente, procedimeinto);
        op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), coordinadorDir.get());
        bl.SendOp(op);
    }



}