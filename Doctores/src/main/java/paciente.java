import java.util.*;

//Clase paciente
public class paciente{
  //Atributos
  private String nombre;
  private String rut;
  private String edad;
  private List<String> enfermedades;
  private List<String> procedimientoAsig;
  private List<String> procedimientoComp;
  private List<String> examenRealizado;
  private List<String> examenNoRealizado;
  private List<String> medicamentosRec;
  private List<String> medicamentosSum;

  //Constructor
  public paciente(int id, String nombre, String rut, String edad, List<String> enf, List<String> procAsig, List<String> procComp, List<String> examR, List<String> examNR, List<String> medRec, List<String> medSum){
  nombre = nombre;
  rut = rut;
  edad = edad;
  enfermedades = enf;
  procedimientoAsig = procAsig;
  procedimientoComp = procComp;
  examenRealizado = examR;
  examenNoRealizado = examNR;
  medicamentosRec = medRec;
  medicamentosSum = medSum;
  }
}
