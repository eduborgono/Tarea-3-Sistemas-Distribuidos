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
    JSONArray doc = funcionariosJsonObject.getJSONArray("Doctor");
    //Instancia de cada tipo de funcionario
    Doctor dr;
    //Listas para cada tipo de funcionarios
    List<Doctor> doctores = new ArrayList<Doctor>();
    //JSONObject obtener datos de cada doctor
    for (int i = 0; i < doc.length(); i++) {
      JSONObject nDoctor = doc.getJSONObject(i);
      dr = new Doctor(nDoctor.getInt("id"),
                     nDoctor.getString("nombre"),
                     nDoctor.getString("apellido"),
                     nDoctor.getInt("estudios"),
                     nDoctor.getInt("experiencia"));
      doctores.add(dr);
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
        JSONObject nproc = pacient.getJSONObject(j);
        Set<String> keys = nproc.keySet();
        Object[] llave = keys.toArray();
        String llaveid = String.valueOf((String)llave[0]);
        procedimientos.put(llaveid,nproc.get(llaveid).toString());
      }
      request = new Requerimiento(id,
                     cargo,
                     procedimientos);
      requerimientos.add(request);
    }

    Scanner sc = new Scanner(System.in);
    System.out.println("Ingrese id Doctor: ");
    int id = sc.nextInt();

    Doctor doctorfinal = null;
    for(int indice = 0;indice<doctores.size();indice++){
        Doctor doctor = doctores.get(indice);
        if(doctor.id == id){
          doctorfinal = doctor;
          break;
        }
    }

    try {
      if(doctorfinal != null) {
        DoctorPro docPro = new DoctorPro(doctorfinal);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Matando cliente....");
                try {
                    docPro.Dispose();
                    sc.close();
                } catch (Exception e) { }
            }
        });

        for(int indice = 0;indice<requerimientos.size();indice++){
          Requerimiento reqI = requerimientos.get(indice);
          String doctor = new String("doctor");
          if(reqI.id == id && doctor.equals(reqI.cargo)){
            for (HashMap.Entry<String, String> entry : reqI.procedimientos.entrySet()) {
              System.out.println("Enter");
              sc.nextLine();
              docPro.bullyClient.SendOp(Integer.valueOf(entry.getKey()), entry.getValue());
            }
          }
        }

      }

    } catch (Exception e) { }

  }
}