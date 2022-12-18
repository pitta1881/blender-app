package blender.centralized;

import blender.distributed.SharedTools.DirectoryTools;
import blender.distributed.Worker.Threads.WorkerProcessThread;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WorkerCentralized {
    String appDir = System.getProperty("user.dir") + "/app/";
    String workerDir = appDir + "Worker/";
    String workerName;
    String singleWorkerDir;
    String blenderExe = "/blender-portable/blender";
    String worksDir = "/Works/";
    String rendersDir = "/RenderedFiles/";
    String blendDir = "/BlendFile/";
    final String blendName = "Dragon_2.5_For_Animations";
    final int startFrame = 1;
    final int endFrame = 100;
    final int threadsNedeed = 2;
    public WorkerCentralized(){
        if(checkBlenderApp()) render();
    }

    private boolean checkBlenderApp() {
        File workerDir = new File(this.workerDir);
        long sizeWorkerDir = DirectoryTools.getFolderSize(workerDir);
        if (sizeWorkerDir > 30000000) {
            String contents[] = workerDir.list();
            this.workerName = contents[0];
            this.singleWorkerDir = workerDir + "/" + this.workerName + "/";
            return true;
        }
        return false;
    }

    private void render(){
        CountDownLatch latch;
        String cmd;
        int totalFrames = this.endFrame - this.startFrame;
        List<WorkerProcessThread> workerThreads = new ArrayList<>();

        File thisWorkDir = new File(this.singleWorkerDir + this.worksDir + this.blendName);
        File thisWorkRenderDir = new File(thisWorkDir + this.rendersDir);
        File f0 = new File(thisWorkRenderDir.getAbsolutePath());
        File thisWorkBlendDir = new File(thisWorkDir + this.blendDir);
        File blendFile = new File(thisWorkBlendDir.getAbsolutePath() + "/" + this.blendName + ".blend");

        System.out.println("Cantidad de Frames a renderizar: " + (totalFrames + 1));
        System.out.println("Cantidad de Threads a crear: " + threadsNedeed);
        int rangeFrame = (int) Math.ceil((float)totalFrames / (float)threadsNedeed);
        int startVarFrame = this.startFrame;
        int endVarFrame = startVarFrame + rangeFrame;
        latch = new CountDownLatch(threadsNedeed);
        for (int i = 0; i < threadsNedeed; i++) {
            cmd = " -b \"" + blendFile.getAbsolutePath() + "\" -o \"" + f0.getAbsolutePath() + "/frame_#####\"" + " -s " + startVarFrame + " -e " + endVarFrame + " -a";
            File f1 = new File(this.singleWorkerDir + this.blenderExe + cmd);//Normalize backslashs and slashs
            System.out.println("CMD: " + f1.getAbsolutePath());
            workerThreads.add(new WorkerProcessThread(latch, f1.getAbsolutePath()));
            startVarFrame = endVarFrame + 1;
            endVarFrame += rangeFrame;
            if(endVarFrame > this.endFrame){
                endVarFrame = this.endFrame;
            }
        }

        long startTime = System.currentTimeMillis();
        Executor executor = Executors.newFixedThreadPool(workerThreads.size());
        for(final WorkerProcessThread wt : workerThreads) {
            executor.execute(wt);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
        long startTimeZip = System.currentTimeMillis();
        try {
            new ZipFile(thisWorkRenderDir + this.blendName+".zip").addFolder(new File(thisWorkRenderDir.getAbsolutePath()));
        } catch (ZipException e) {
        }
        long endTimeZip = System.currentTimeMillis()-startTimeZip;
        long endTime = System.currentTimeMillis()-startTime;
        System.out.println("Zip elapsed time: " + endTimeZip + "ms");
        System.out.println("Render All Threads Time Elapsed: " + endTime + "ms");
        System.out.println("==========Trabajo Terminado=========");
    }

    public static void main(String[] args) {
        new WorkerCentralized();
    }
}
