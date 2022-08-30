package blender.multiThread;

public class BlenderMultiThread {

    public static void main(String[] args) {

        final int cores = 2;
        final int totalFrames = 100;
        int rangeFrame = totalFrames / cores;
        int startFrame = 1;
        int endFrame = rangeFrame;

        for (int i = 0; i < cores; i++) {
            BlenderProcess blenderProcess = new BlenderProcess(startFrame, endFrame);
            new Thread(blenderProcess).start();
            startFrame += rangeFrame;
            endFrame += rangeFrame;
        }

    }
}