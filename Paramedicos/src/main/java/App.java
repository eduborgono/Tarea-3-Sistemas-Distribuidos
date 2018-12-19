import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.JSONArray;

public class App {
  public static void main(String[] args)  throws Exception {
    /**** LECTURA DE FUNCIONARIOS ****/
    /*Se lee el archivo json de los funcionarios del hospital para luego parsear
    el json con los metodos de la libreria usada. En primer lugar el json en
    convertido en un objeto json, luego cada tiop array de cada funcionario es convertido
    en un Jsonarray para volver a parsearlo y poder crear una lista del tipo correspondiente
    a cada tipo de funcionario */
    File file = new File("funcionarios.json");
    String content = FileUtils.readFileToString(file, "utf-8");
    // Convert JSON string to JSONObject
    JSONObject funcionariosJsonObject = new JSONObject(content);
    //Json array: obtener doctores, enfermeros y paramedicos desde el JSON
    JSONArray par = funcionariosJsonObject.getJSONArray("Paramedico");

    //Instancia de cada tipo de funcionario
    Paramedico paramedico;
    //Listas para cada tipo de funcionarios
    List<Paramedico> paramedicos = new ArrayList<Paramedico>();

    //JSONObject obtener datos de cada paramedicos
    for (int i = 0; i < par.length(); i++) {
      JSONObject nParamedico = par.getJSONObject(i);
      paramedico = new Paramedico(nParamedico.getInt("id"),
                     nParamedico.getString("nombre"),
                     nParamedico.getString("apellido"),
                     nParamedico.getInt("estudios"),
                     nParamedico.getInt("experiencia"));
      paramedicos.add(paramedico);
    }

    /**** LECTURA DE REQUERIMIENTOS ****/
    /*El mismo procedimiento explicado anteriormente*/
    Requerimiento request;
    //Listas de requerimientos
    List<Requerimiento> requerimientos = new ArrayList<Requerimiento>();

    File file3 = new File("requerimientos.json");
    String content3 = FileUtils.readFileToString(file3, "utf-8");
    // Convert JSON string to JSONObject
    JSONObject requerimientosJsonObject = new JSONObject(content3);
    JSONArray req = requerimientosJsonObject.getJSONArray("requerimientos");
    for (int i = 0; i < req.length(); i++) {
      JSONObject nReq = req.getJSONObject(i);
      int id = nReq.getInt("id");
      String cargo = nReq.getString("cargo");
      HashMap<String, String> procedimientos = new HashMap<>();
      JSONArray pacient = nReq.getJSONArray("pacientes");

      for(int j = 0;j < pacient.length(); j++){
        procedimientos.put(Integer.toString(j+1),pacient.get(j).toString());
      }
      request = new Requerimiento(id,
                     cargo,
                     procedimientos);
      requerimientos.add(request);
    }
    Scanner sc = new Scanner(System.in);
    System.out.println("Ingrese id paramedico: ");
    int id = sc.nextInt();

    Paramedico paramedicofinal = null;
    for(int indice = 0;indice<paramedicos.size();indice++){
        Paramedico paraaux = paramedicos.get(indice);
        if(paraaux.id == id){
          paramedicofinal = paraaux;
        }
    }

    try {
      if(paramedicofinal != null) {
        ParamedicoPro paramedicoPro = new ParamedicoPro(paramedicofinal);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Matando cliente....");
                try {
                  paramedicoPro.Dispose();
                  sc.close();
                } catch (Exception e) { }
            }
        });

        for(int indice = 0;indice<requerimientos.size();indice++){
          Requerimiento reqI = requerimientos.get(indice);
          String paramedicoStr = new String("paramedico");
          if(reqI.id == id && paramedicoStr.equals(reqI.cargo)){
            for (HashMap.Entry<String, String> entry : reqI.procedimientos.entrySet()) {
              System.out.println("Enter");
              sc.nextLine();
              paramedicoPro.bullyClient.SendOp(Integer.valueOf(entry.getKey()), entry.getValue());
            }
          }
        }
      }
    } catch (Exception e) { }
  }
}
