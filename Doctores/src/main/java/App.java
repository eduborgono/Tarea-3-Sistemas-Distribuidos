import java.*;
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
    File file = new File("/home/fran/Escritorio/Distribuidos/Tarea3/Tarea-3-Sistemas-Distribuidos/Doctores/src/main/java/funcionarios.json");
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

    /**** LECTURA DE PACIENTES ****/
    /*El mismo procedimiento explicado anteriormente*/
    paciente pac;
    //Listas de pacientes
    List<paciente> pacientes = new ArrayList<paciente>();
    File file2 = new File("/home/fran/Escritorio/Distribuidos/Tarea3/Tarea-3-Sistemas-Distribuidos/data/pacientes.json");
    String content2 = FileUtils.readFileToString(file2, "utf-8");
    //Extraer arreglo de pacientes
    JSONArray pacientesJsonArray = new JSONArray(content2);
    //Extraer pacientes
    for(int i=0; i < pacientesJsonArray.length(); i++){
      List<String> enfermedadesList = new ArrayList<String>();
      List<String> procesoAList = new ArrayList<String>();
      List<String> procesoCList = new ArrayList<String>();
      List<String> examenRList = new ArrayList<String>();
      List<String> examenNRList = new ArrayList<String>();
      List<String> medRList = new ArrayList<String>();
      List<String> medSList = new ArrayList<String>();
      //Extraer a los pacientes
      JSONObject pacienteList = pacientesJsonArray.getJSONObject(i);
      //Extraer id
      int id = pacienteList.getInt("paciente_id");
      //Extraer datos del paciente
      JSONObject d = null;
      JSONArray datos = pacienteList.getJSONArray("datos personales");
      for(int j=0; j<datos.length(); j++){
        d = datos.getJSONObject(j);
      }
      //Extraer enfermedades del paciente
      JSONArray enfermedades = pacienteList.getJSONArray("enfermedades");
      for(int j=0; j<enfermedades.length(); j++){
        enfermedadesList.add(enfermedades.get(j).toString());
      }
      //Extraer tratamientos del paciente
      JSONArray tratamientos = pacienteList.getJSONArray("tratamientos/procedimientos");
      for(int j=0; j<tratamientos.length(); j++){
        JSONObject t = tratamientos.getJSONObject(j);
        //Extraer tratamientos asignados
        JSONArray asignados = t.getJSONArray("asignados");
        for(int k=0; k<asignados.length(); k++){
          procesoAList.add(asignados.get(k).toString());
        }
        //Extraer tratamientos completados
        JSONArray completados = t.getJSONArray("completados");
        for(int k=0; k<completados.length(); k++){
          procesoCList.add(completados.get(k).toString());
        }
      }
      //Extraer examenes
      JSONArray examenes = pacienteList.getJSONArray("examenes");
      for(int j=0; j<examenes.length(); j++){
        JSONObject e = examenes.getJSONObject(j);
        //Extraer examenes realizados
        JSONArray realizados = e.getJSONArray("realizados");
        for(int k=0; k<realizados.length(); k++){
          examenRList.add(realizados.get(k).toString());
        }
        //Extraer examenes no realizados
        JSONArray NRealizados = e.getJSONArray("no realizados");
        for(int k=0; k<NRealizados.length(); k++){
          examenNRList.add(NRealizados.get(k).toString());
        }
      }
      //Extraer medicamentos
      JSONArray medicamentos = pacienteList.getJSONArray("medicamentos");
      for(int j=0; j<medicamentos.length(); j++){
        JSONObject m = medicamentos.getJSONObject(j);
        //Extraer medicamentos recetados
        JSONArray recetados = m.getJSONArray("recetados");
        for(int k=0; k<recetados.length(); k++){
          medRList.add(recetados.get(k).toString());
        }
        //Extraer examenes suministrados
        JSONArray suministrados = m.getJSONArray("suministrados");
        for(int k=0; k<suministrados.length(); k++){
          medSList.add(suministrados.get(k).toString());
        }
      }
      pac = new paciente(id,
                         d.getString("nombre"),
                         d.getString("rut"),
                         d.getString("edad"),
                         enfermedadesList,
                         procesoAList,
                         procesoCList,
                         examenRList,
                         examenNRList,
                         medRList,
                         medSList
                         );
      pacientes.add(pac);
    }

    /**** LECTURA DE REQUERIMIENTOS ****/
    /*El mismo procedimiento explicado anteriormente*/
    requerimiento request;
    //Listas de requerimientos
    List<requerimiento> requerimientos = new ArrayList<requerimiento>();

    File file3 = new File("/home/fran/Escritorio/Distribuidos/Tarea3/Tarea-3-Sistemas-Distribuidos/data/requerimientos.json");
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
        Set keys = nproc.keySet();
        Object[] llave = keys.toArray();
        String llaveid = String.valueOf((String)llave[0]);
        procedimientos.put(llaveid,nproc.get(llaveid).toString());
      }
      request = new requerimiento(id,
                     cargo,
                     procedimientos);
      requerimientos.add(request);
    }

    Scanner sc = new Scanner(System.in);
    System.out.println("Ingrese id Doctor: ");
    int id = sc.nextInt();

    for(int indice = 0;indice<requerimientos.size();indice++){
        requerimiento reqI = requerimientos.get(indice);
        if(reqI.id == id){
            System.out.println(reqI.procedimientos);
        }
    }

  }
}
