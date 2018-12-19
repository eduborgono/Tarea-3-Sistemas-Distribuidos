import java.io.IOException;

import bully.BullyClient;
import lombok.Getter;
import lombok.Setter;

public class DoctorPro {
    @Getter @Setter public Doctor dr;
    @Getter public final BullyClient bullyClient;

    public DoctorPro(Doctor dr) throws IOException {
        String identificador = String.valueOf(dr.id)+"|doctor";
        bullyClient = new BullyClient(identificador, dr.experiencia, dr.estudios);
        this.dr = dr;
    }

    public void Dispose() {
        bullyClient.Dispose();
    }
}