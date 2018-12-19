import java.util.*;

//Clase paciente
public class paciente{
  //Atributos
  public int id;
  public String nombre;
  public String rut;
  public String edad;
  public List<String> enfermedades;
  public List<String> procedimientoAsig;
  public List<String> procedimientoComp;
  public List<String> examenRealizado;
  public List<String> examenNoRealizado;
  public List<String> medicamentosRec;
  public List<String> medicamentosSum;

  //Constructor
  public paciente(int ide, String Nombre, String Rut, String Edad, List<String> enf, List<String> procAsig, List<String> procComp, List<String> examR, List<String> examNR, List<String> medRec, List<String> medSum){
    id = ide;
    nombre = Nombre;
    rut = Rut;
    edad = Edad;
    enfermedades = enf;
    procedimientoAsig = procAsig;
    procedimientoComp = procComp;
    examenRealizado = examR;
    examenNoRealizado = examNR;
    medicamentosRec = medRec;
    medicamentosSum = medSum;
  }
}
