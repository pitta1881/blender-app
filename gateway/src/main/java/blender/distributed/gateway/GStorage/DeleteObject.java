package blender.distributed.gateway.GStorage;

import blender.distributed.gateway.Gateway;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;

public class DeleteObject {
    static Dotenv dotenv = Dotenv.load();
    public static void deleteObject(String projectId, String bucketName, String objectName) throws IOException {

        Credentials credentials = GoogleCredentials.fromStream(Gateway.class.getClassLoader().getResourceAsStream(dotenv.get("PATH_AUTH")));
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        Blob blob = storage.get(bucketName, objectName);
        if (blob == null) {
            return;
        }

        // Optional: set a generation-match precondition to avoid potential race
        // conditions and data corruptions. The request to upload returns a 412 error if
        // the object's generation number does not match your precondition.
        Storage.BlobSourceOption precondition =
                Storage.BlobSourceOption.generationMatch(blob.getGeneration());

        storage.delete(bucketName, objectName, precondition);

    }
}