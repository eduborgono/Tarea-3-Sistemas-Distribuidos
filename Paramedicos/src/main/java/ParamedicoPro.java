import java.io.IOException;

import bully.BullyClient;
import lombok.Getter;
import lombok.Setter;

public class ParamedicoPro {
    @Getter @Setter public Paramedico para;
    @Getter public final BullyClient bullyClient;

    public ParamedicoPro(Paramedico para) throws IOException {
        String identificador = String.valueOf(para.id)+"|paramedico";
        bullyClient = new BullyClient(identificador, para.experiencia, para.estudios);
        this.para = para;
    }

    public void Dispose() {
        bullyClient.Dispose();
    }
}