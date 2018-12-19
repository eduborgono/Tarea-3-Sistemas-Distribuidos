import java.util.*;

public class Requerimiento{
  public int id;
  public String cargo;
  public HashMap<String, String> procedimientos;
  //Constructor
  public Requerimiento(int ide, String Cargo, HashMap<String, String> proc) {
    id = ide;
    cargo = Cargo;
    procedimientos = proc;
    }
}
