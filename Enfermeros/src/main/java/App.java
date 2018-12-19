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
    JSONArray enf = funcionariosJsonObject.getJSONArray("enfermero");

    //Instancia de cada tipo de funcionario
    Enfermero enfermero;
    //Listas para cada tipo de funcionarios
    List<Enfermero> enfermeros = new ArrayList<Enfermero>();

    //JSONObject obtener datos de cada enfermeros
    for (int i = 0; i < enf.length(); i++) {
      JSONObject nEnfermero = enf.getJSONObject(i);
      enfermero = new Enfermero(nEnfermero.getInt("id"),
                     nEnfermero.getString("nombre"),
                     nEnfermero.getString("apellido"),
                     nEnfermero.getInt("estudios"),
                     nEnfermero.getInt("experiencia"));
      enfermeros.add(enfermero);
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
    System.out.println("Ingrese id enfermero: ");
    int id = sc.nextInt();

    Enfermero enfermerofinal = null;
    for(int indice = 0;indice<enfermeros.size();indice++){
        Enfermero enfaux = enfermeros.get(indice);
        if(enfaux.id == id){
          enfermerofinal = enfaux;
        }
    }

    try {
      if(enfermerofinal != null) {
        EnfermeroPro enfermeroPro = new EnfermeroPro(enfermerofinal);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Matando cliente....");
                try {
                  enfermeroPro.Dispose();
                  sc.close();
                } catch (Exception e) { }
            }
        });

        for(int indice = 0;indice<requerimientos.size();indice++){
            Requerimiento reqI = requerimientos.get(indice);
            String sEnfermero = new String("enfermero");
            if(reqI.id == id && sEnfermero.equals(reqI.cargo)){
              for (HashMap.Entry<String, String> entry : reqI.procedimientos.entrySet()) {
                System.out.println("Enter");
                sc.nextLine();
                enfermeroPro.bullyClient.SendOp(Integer.valueOf(entry.getKey()), entry.getValue());
                
              }
            }
        }
      }
    } catch (Exception e) { }
  }
}
