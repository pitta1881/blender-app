package blender.distributed.Gateway.GStorage;

import blender.distributed.Gateway.Gateway;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class UploadObjectFromMemory {
    static Dotenv dotenv = Dotenv.load();
    public static void uploadObjectFromMemory(String projectId, String bucketName, String objectName, byte[] content) throws IOException {

            Credentials credentials = GoogleCredentials.fromStream(Gateway.class.getClassLoader().getResourceAsStream(dotenv.get("PATH_AUTH")));
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            storage.createFrom(blobInfo, new ByteArrayInputStream(content));

        }
}