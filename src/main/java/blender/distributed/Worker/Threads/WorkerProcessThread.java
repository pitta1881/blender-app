package blender.distributed.Worker.Threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

public class WorkerProcessThread implements Runnable{
	private String cmd;
	private final CountDownLatch latchSignal;

	public WorkerProcessThread(CountDownLatch latch, String cmd) {
		this.latchSignal = latch;
		this.cmd = cmd;
	}
	
	@Override
	public void run() {
		ejecutar(this.cmd);
	}

	private void ejecutar(String cmd) {
		try {
			long startTime = System.currentTimeMillis();
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while (true) {
				line = input.readLine();
				if (line == null) break;
				if (line.contains("| Rendered ")) {

				} else {
					System.out.println("Line: " + line);
				}
			}
			p.waitFor();
			long endTime = System.currentTimeMillis()-startTime;
			System.out.println("Time Elapsed: " + endTime + "ms");
			this.latchSignal.countDown();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}


}
