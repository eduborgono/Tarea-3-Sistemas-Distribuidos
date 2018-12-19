package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class Escritura {
    /**
     * 
     * Función que se encarga de retornar el log de la maquina central.
     * 
     */
    public static String CopiarLog() throws IOException{
        FileReader fr = new FileReader("/root/Tarea-3-Sistemas-Distribuidos/data/logs.txt");
        BufferedReader br = new BufferedReader(fr);
        String linea;
        StringBuilder strBld = new StringBuilder();
        boolean first = true;
        while((linea = br.readLine()) != null){
            if(!first) strBld.append("&");
            strBld.append(linea);
            if(first) first = false;
        }
        fr.close();
        return strBld.toString();
    }

    /**
     * Funcion que se encarga de sobreescribir todo lo que tenga un log
     */
    public static void UpdateLog(String contenido)throws IOException{
        BufferedWriter bw = null;
        FileWriter file = new FileWriter("/root/Tarea-3-Sistemas-Distribuidos/data/logs.txt");
        bw = new BufferedWriter(file);
        String[] partC = contenido.split("\\&");
        for(int i=0; i < partC.length; i++){
            bw.write(partC[i]);
            bw.newLine();
        }
        bw.close();
    }

    /**
     * Funcion que añade al final del log un evento.
     */
    public static void EscribirLogFinal(String contenido) throws IOException {
        File file = new File("/root/Tarea-3-Sistemas-Distribuidos/data/logs.txt");
        // Si el archivo no existe es creado
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter bw = null;
        FileWriter fw = null;
        fw = new FileWriter(file.getAbsoluteFile(), true);
        bw = new BufferedWriter(fw);
        bw.write(contenido);
        bw.newLine();
        bw.close();
    }

    /**
     * 
     * Funcion que se encarga de agregar un evento relacionado con un paciente, al archivo
     * de pacientes.
     */
    public static void EscribirPaciente(int id_paciente, String cargo, String procedimiento) throws Exception {
        //Separo procedimientos
        String[] proce = procedimiento.split(" ");
        String accion = proce[0];
        String tipoProc = proce[1];
        String Proc = proce[2];

        /*ARCHIVO PACIENTES*/
        File file2 = new File("/root/Tarea-3-Sistemas-Distribuidos/data/pacientes.json");
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

			FileWriter file = new FileWriter("/root/Tarea-3-Sistemas-Distribuidos/data/pacientes.json");
			file.write(modifPacList.toString());
			file.flush();
			file.close();

		} catch (IOException e) {
			System.out.println("Archivo no encontrado");
		}
    }
}