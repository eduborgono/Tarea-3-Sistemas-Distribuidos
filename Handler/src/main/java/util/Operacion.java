package util;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Operacion {

    public static final String CERRAR = "CERRAR";
    public static final String BROADCAST = "BROADCAST";
    public static final String BROADCAST_LOCAL = "BROADCAST_LOCAL";
    public static final String BROADCAST_GLOBAL = "BROADCAST_GLOBAL";
    public static final String CONFIGURACION = "CONFIGURACION";

    @Getter @Setter private String origen;
    @Getter @Setter private String dest;
    @Getter @Setter private int id;
    @Getter @Setter private int idPaciente;
    @Getter @Setter private String procedimeinto;
    @Getter @Setter private int especial;

    public Operacion() {}

    public Operacion(int id, int idPaciente, String procedimeinto) {
        this.id = id;
        this.procedimeinto = procedimeinto;
        this.idPaciente = idPaciente;
        this.especial = -1;
    }

    public void Empaquetar(String origen, String dest) {
        this.origen = origen;
        this.dest = dest;
    }

    public Operacion(int especial) {
        this.especial = especial;
    }

    public int getTipo() {
        return especial;
    }
}