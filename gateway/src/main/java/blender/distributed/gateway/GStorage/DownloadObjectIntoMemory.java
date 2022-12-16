package blender.distributed.gateway.GStorage;


import blender.distributed.gateway.Gateway;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;

public class DownloadObjectIntoMemory {
    static Dotenv dotenv = Dotenv.load();
    public static byte[] downloadObjectIntoMemory(String projectId, String bucketName, String objectName) throws IOException {

        Credentials credentials = GoogleCredentials.fromStream(Gateway.class.getClassLoader().getResourceAsStream(dotenv.get("PATH_AUTH")));
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        return storage.readAllBytes(bucketName, objectName);

    }
}