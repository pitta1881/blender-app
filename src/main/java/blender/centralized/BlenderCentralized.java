package blender.centralized;

import java.io.IOException;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;


public class BlenderCentralized {

    public static void main(String[] args) {
    long startTime;

        //get blender.exe directory
        String blenderFilePath;
        String blenderPath = "\"C:/Program Files/Blender Foundation/Blender 3.2\"";
        blenderPath = blenderPath.replace("\\", "/");

        //get resource .blend
        URL blenderFile = BlenderCentralized.class.getClassLoader().getResource("Dragon_2.5_For_Animations.blend");
        try {
            File file = Paths.get(blenderFile.toURI()).toFile();
            blenderFilePath = "\"" + file.getAbsolutePath()+ "\"";
            System.out.println(blenderFilePath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        //run cmd command and get time
        Runtime rt = Runtime.getRuntime();
        try {
            startTime = System.currentTimeMillis();
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c", "cd " + blenderPath + " && C: &&  blender.exe -b " + blenderFilePath + " -s 1 -e 100 -a");
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                System.out.println(line);
            }
            long endTime = System.currentTimeMillis()-startTime;
            System.out.println("Time Elapsed: " + endTime + "ms");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }
}