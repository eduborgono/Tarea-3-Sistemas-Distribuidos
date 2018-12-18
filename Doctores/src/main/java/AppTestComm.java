/*
 * This Java source file was generated by the Gradle 'init' task.
 */
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import bully.BullyClient;
import bully.Operacion;

public class AppTestComm {
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
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("Matando cliente....");
                    try {
                        bullyClient.Dispose();
                    } catch (Exception e) { }
                }
            });
            while((in = input.nextInt()) != 0) {
                
                if(in == 1) {
                    bullyClient.SendOp(0, "bailar");
                }
                else if(in == 2) {
                    bullyClient.SendOp(2, "cantar");
                }
            } 
            bullyClient.Dispose();
            
        } catch(Exception e) {
            //e.printStackTrace();
        }
    }
}
