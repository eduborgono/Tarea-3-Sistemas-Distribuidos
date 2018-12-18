import java.util.*;

public class requerimiento{
  public int id;
  public String  cargo;
  public HashMap<String, String> procedimientos;
  //Constructor
  public requerimiento(int ide, String Cargo, HashMap<String, String> proc) {
    id = ide;
    cargo = Cargo;
    procedimientos = proc;
    }
}
