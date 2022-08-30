package blender.multiThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class BlenderProcess implements Runnable {

    int startFrame;
    int endFrame;
    final String blenderPath = "\"C:/Program Files/Blender Foundation/Blender 3.2\"";

    public BlenderProcess(int start, int end) {
        this.startFrame = start;
        this.endFrame = end;
    }

    @Override
    public void run() {
        String blenderFilePath = getFilePath("Dragon_2.5_For_Animations.blend");
        runCommand(blenderFilePath);
    }

    private String getFilePath(String path){
        String blenderFilePath;
        URL blenderFile = BlenderMultiThread.class.getClassLoader().getResource(path);
        try {
            File file = Paths.get(blenderFile.toURI()).toFile();
            blenderFilePath = "\"" + file.getAbsolutePath()+ "\"";
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return blenderFilePath;
    }

    private void runCommand(String path){
        Runtime rt = Runtime.getRuntime();
        try {
            long startTime = System.currentTimeMillis();
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c", "cd " + blenderPath + " && C: &&  blender.exe -b " + path + " -s " + startFrame + " -e " + endFrame + " -a");
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
