package blender.distributed.Worker.Tools;

import java.io.File;

public class DirectoryTools {

	
	
	
	public static long getFolderSize(File folder) {
	    long length = 0;
	    File[] files = folder.listFiles();
	 
	    int count = files.length;
	 
	    for (int i = 0; i < count; i++) {
	        if (files[i].isFile()) {
	            length += files[i].length();
	        }
	        else {
	            length += getFolderSize(files[i]);
	        }
	    }
	    return length;
	}
}

