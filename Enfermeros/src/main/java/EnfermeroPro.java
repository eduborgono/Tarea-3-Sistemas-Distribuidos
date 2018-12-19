import java.io.IOException;

import bully.BullyClient;
import lombok.Getter;
import lombok.Setter;

public class EnfermeroPro {
    @Getter @Setter public Enfermero enf;
    @Getter public final BullyClient bullyClient;

    public EnfermeroPro(Enfermero enf) throws IOException {
        String identificador = String.valueOf(enf.id)+"|enfermero";
        bullyClient = new BullyClient(identificador, enf.experiencia, enf.estudios);
        this.enf = enf;
    }

    public void Dispose() {
        bullyClient.Dispose();
    }
}