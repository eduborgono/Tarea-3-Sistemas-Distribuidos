package bully;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Operacion {

    public static final String BROADCAST = "BROADCAST";

    public static final String DEFECTO = "DEFECTO";
    public static final String DISCOVERY_REQUEST = "DISCOVERY_REQUEST";
    public static final String DISCOVERY_RESPONSE = "DISCOVERY_RESPONSE";
    public static final String NUEVO_COORDINADOR_REQUEST = "NUEVO_COORDINADOR_REQUEST";
    public static final String NUEVO_COORDINADOR_RESPONSE = "NUEVO_COORDINADOR_RESPONSE";
    public static final String NUEVO_COORDINADOR_ALL = "NUEVO_COORDINADOR_ALL";
    public static final String ERROR_ENTREGA = "ERROR_ENTREGA";
    public static final String ENTREGA_CORRECTA = "ENTREGA_CORRECTA";
    public static final String POR_ENVIAR = "POR_ENVIAR";
    public static final String NUEVO_COORDINADOR_INTENT = "NUEVO_COORDINADOR_INTENT";
    public static final String ASCENDER_INTENT = "ASCENDER_INTENT";

    @Getter @Setter private String origen;
    @Getter @Setter private String dest;
    @Getter @Setter private int id;
    @Getter @Setter private int idPaciente;
    @Getter @Setter private String procedimeinto;
    @Getter @Setter private String especial;
    @Getter @Setter private String timestamp;

    public Operacion() {}

    public Operacion(int id, int idPaciente, String procedimeinto) {
        this.id = id;
        this.procedimeinto = procedimeinto;
        this.idPaciente = idPaciente;
        this.especial = DEFECTO;
    }

    public void Empaquetar(String origen, String dest) {
        this.origen = origen;
        this.dest = dest;
    }
}