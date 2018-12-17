/*
 * This Java source file was generated by the Gradle 'init' task.
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import bully.BullyClient;
import bully.Operacion;
import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;

public class App {
    public String getGreeting() {
        return "Hello world.";
    }

    public static void main(String[] args) {
        //System.out.println(new App().getGreeting());

        try {
            int id = ThreadLocalRandom.current().nextInt(0, 20 + 1);
            int experiencia = ThreadLocalRandom.current().nextInt(0, 20 + 1);
            int estudios = ThreadLocalRandom.current().nextInt(5, 20 + 1);
            System.out.println(id + " " + experiencia + " " + estudios);
            BullyClient bullyClient = new BullyClient(id, experiencia, estudios);
            Scanner input = new Scanner(System.in);
            int in;
            while((in = input.nextInt()) != 0) {
                if(in == 1) {
                    bullyClient.Dispose();
                    break;
                }
                else if(in == 2) {
                    bullyClient.SendOp(0, "bailar", Operacion.BROADCAST_LOCAL);
                }
                else if(in == 3) {
                    bullyClient.SendOp(2, "cantar", "none");
                }
            } 
            
        } catch(Exception e) {
            //System.err.println(e.getCause().getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
