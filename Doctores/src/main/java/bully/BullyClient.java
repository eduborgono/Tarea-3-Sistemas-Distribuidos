package bully;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;

public class BullyClient {
    private String idClient;
    private int idOperacion;
    private int identificador;
    private int prioridad1;
    private int prioridad2;
    private final BullyListener bl;

    public BullyClient(int id, int experiencia, int estudios) throws IOException {
        identificador = id;
        idOperacion = 0;
        prioridad1 = experiencia;
        prioridad2 = estudios;
        bl = new BullyListener();
        bl.start();
    }

    public void Dispose() {
        bl.Dispose();
    }

    public void SendOp(int paciente, String procedimeinto, String destino) throws IOException {
        Operacion op = new Operacion(idOperacion, paciente, procedimeinto);
        op.Empaquetar(bl.getDireccionIp() + ":" + bl.getPuerto(), destino);
        bl.SendOp(op);
    }

}