package blender.distributed.shared;

import org.slf4j.Logger;

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

	public static boolean checkOrCreateFolder(String path, Logger log){
		File fFolder = new File(path);
		if(!fFolder.isDirectory()){
			fFolder.mkdir();
			log.info("Success: "+fFolder.getAbsolutePath()+" Directorio creado.");
		} else {
			log.info(fFolder.getAbsolutePath()+" ---->Directorio");
		}
		return true;
	}

	public static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}
}

