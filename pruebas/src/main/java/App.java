import java.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.JSONArray;


public class App {
  public static void main(String[] args)  throws Exception {
    /*****/
    int id_paciente = 1;
    Scanner sc = new Scanner(System.in);
    System.out.println("Ingrese cargo: ");
    String cargo = sc.nextLine();
    System.out.println();
    System.out.println("Ingrese procedimiento: ");
    String procedimiento = sc.nextLine();
    /*****/
    //Separo procedimientos
    String[] proce = procedimiento.split(" ");
    String accion = proce[0];
    String tipoProc = proce[1];
    String Proc = proce[2];

    /*ARCHIVO PACIENTES*/
    File file2 = new File("/home/fran/Escritorio/Distribuidos/Tarea3/Tarea-3-Sistemas-Distribuidos/data/pacientes.json");
    String content2 = FileUtils.readFileToString(file2, "utf-8");
    JSONArray pacientesJsonArray = new JSONArray(content2);

    /*Nuevo JSONArray*/
    JSONArray modifPacList = new JSONArray();

    for(int i=0; i < pacientesJsonArray.length(); i++){
      /*nuevo JSONObject*/
      JSONObject pacModif = new JSONObject();
      //Extraer a los pacientes
      JSONObject pacienteList = pacientesJsonArray.getJSONObject(i);
      //Extraer id
      int id = pacienteList.getInt("paciente_id");
      if(id_paciente == id){
        //datos y enfermedades se agregan tal cual
        pacModif.put("paciente_id", id);
        JSONArray datos = pacienteList.getJSONArray("datos personales");
        pacModif.put("datos personales", datos);
        JSONArray enfermedades = pacienteList.getJSONArray("enfermedades");
        pacModif.put("enfermedades", enfermedades);
        if(tipoProc.equals("procedimiento") || tipoProc.equals("tratamiento")){
          //Se agregan examenes y medicamentos
          JSONArray examenes = pacienteList.getJSONArray("examenes");
          pacModif.put("examenes", examenes);
          JSONArray medicamentos = pacienteList.getJSONArray("medicamentos");
          pacModif.put("medicamentos", medicamentos);
          JSONArray tratamientos = pacienteList.getJSONArray("tratamientos/procedimientos");
          JSONArray tratamientosModif = new JSONArray();
          JSONObject t = tratamientos.getJSONObject(0);
          //Se modifica procedimiento
          JSONArray a = t.getJSONArray("asignados");
          JSONArray c = t.getJSONArray("completados");
          if(accion.equals("asignar")){
            if(cargo.equals("doctor") || cargo.equals("Doctor")){
              //Se modifica remedio suministrado
              a.put(Proc);
            }
            else{
              System.out.println("No se pudo modificar, no tiene permiso o el cargo esta mal escrito");
              //no se hace la modificacion
              pacModif.put("tratamientos/procedimientos", tratamientos);
              modifPacList.put(pacModif);
            }
          }
          else if(accion.equals("completar")){
            if(cargo.equals("doctor") || cargo.equals("Doctor") || cargo.equals("enfermero") || cargo.equals("Enfermero")){
              //Se modifica remedio suministrado
              c.put(Proc);
            }
            else{
              System.out.println("No se pudo modificar, no tiene permiso o el cargo esta mal escrito");
              //no se hace la modificacion
              pacModif.put("tratamientos/procedimientos", tratamientos);
              modifPacList.put(pacModif);
            }
          }
          t.put("asignados", a);
          t.put("completados", c);
          tratamientosModif.put(t);
          pacModif.put("tratamientos/procedimientos", tratamientosModif);
          modifPacList.put(pacModif);

        }
        else if(tipoProc.equals("medicamento")){
          //Se agregan examenes y tratamientos
          JSONArray tratamientos = pacienteList.getJSONArray("tratamientos/procedimientos");
          pacModif.put("tratamientos/procedimientos", tratamientos);
          JSONArray examenes = pacienteList.getJSONArray("examenes");
          pacModif.put("examenes", examenes);
          JSONArray medicamentos = pacienteList.getJSONArray("medicamentos");
          JSONArray medicamentosModif = new JSONArray();
          JSONObject m = medicamentos.getJSONObject(0);
          //Se modifica medicamento
          JSONArray r = m.getJSONArray("recetados");
          JSONArray s = m.getJSONArray("suministrados");
          if(accion.equals("recetar")){
            if(cargo.equals("doctor") || cargo.equals("Doctor")){
              //Se modifica remedio suministrado
              s.put(Proc);
            }
            else{
              System.out.println("No se pudo modificar, no tiene permiso o el cargo esta mal escrito");
              //no se hace la modificacion
              pacModif.put("medicamentos", medicamentos);
              modifPacList.put(pacModif);
            }
          }
          else if(accion.equals("suministrar")){
            if(cargo.equals("doctor") || cargo.equals("Doctor") || cargo.equals("enfermero") || cargo.equals("Enfermero")){
              //Se modifica remedio suministrado
              s.put(Proc);
            }
            else{
              System.out.println("No se pudo modificar, no tiene permiso o el cargo esta mal escrito");
              //no se hace la modificacion
              pacModif.put("medicamentos", medicamentos);
              modifPacList.put(pacModif);
            }
          }
          m.put("recetados", r);
          m.put("suministrados", s);
          medicamentosModif.put(m);
          pacModif.put("medicamentos", medicamentosModif);
          modifPacList.put(pacModif);
        }
        else if(tipoProc.equals("examen")){
          //Se agregan medicamentos y tratamientos
          JSONArray tratamientos = pacienteList.getJSONArray("tratamientos/procedimientos");
          pacModif.put("tratamientos/procedimientos", tratamientos);
          JSONArray medicamentos = pacienteList.getJSONArray("medicamentos");
          pacModif.put("medicamentos", medicamentos);
          JSONArray examenes = pacienteList.getJSONArray("examenes");
          JSONArray examenesModif = new JSONArray();
          JSONObject e = examenes.getJSONObject(0);
          //Se modifica examen
          JSONArray r = e.getJSONArray("realizados");
          JSONArray nr = e.getJSONArray("no realizados");
          if(accion.equals("realizar")){
            if(cargo.equals("doctor") || cargo.equals("Doctor") || cargo.equals("paramedico") || cargo.equals("Paramedico")){
              //modifico examenes realizado
              r.put(Proc);
            }
            else{
              System.out.println("No se pudo modificar, no tiene permiso o el cargo esta mal escrito");
              //no se hace la modificacion
              pacModif.put("examenes", examenes);
              modifPacList.put(pacModif);
            }
          }
          else if(accion.equals("pedir")){
            if(cargo.equals("doctor") || cargo.equals("Doctor")){
              //modifico examenes sin realizar
              nr.put(Proc);
            }
            else{
              System.out.println("No se pudo modificar, no tiene permiso o el cargo esta mal escrito");
              //no se hace la modificacion
              pacModif.put("examenes", examenes);
              modifPacList.put(pacModif);
            }
          }
          e.put("no realizados", nr);
          e.put("realizados", r);
          examenesModif.put(e);
          pacModif.put("examenes", examenesModif);
          modifPacList.put(pacModif);
        }
        else{
          System.out.println("Accion invalida");
          //Armar el JSON como estaba
          JSONArray tratamientos = pacienteList.getJSONArray("tratamientos/procedimientos");
          pacModif.put("tratamientos/procedimientos", tratamientos);
          JSONArray examenes = pacienteList.getJSONArray("examenes");
          pacModif.put("examenes", examenes);
          JSONArray medicamentos = pacienteList.getJSONArray("medicamentos");
          pacModif.put("medicamentos", medicamentos);
          modifPacList.put(pacModif);


        }
      }
      else{
        modifPacList.put(pacienteList);
      }
    }
    try {

			FileWriter file = new FileWriter("/home/fran/Escritorio/Distribuidos/Tarea3/Tarea-3-Sistemas-Distribuidos/data/pacientes.json");
			file.write(modifPacList.toString());
			file.flush();
			file.close();

		} catch (IOException e) {
			System.out.println("Archivo no encontrado");
		}
  }
}
