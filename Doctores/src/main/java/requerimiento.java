import java.util.*;

public class requerimiento{
  public int id;
  public String  cargo;
  public HashMap<String, String> procedimientos;
  //Constructor
  public requerimiento(int id, String cargo, HashMap<String, String> proc) {
    id = id;
    cargo = cargo;
    procedimientos = proc;
    }
}
