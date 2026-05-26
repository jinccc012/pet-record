package com.harumi.petrecord.file;

import java.time.Duration;

public interface R2StorageService {

    /** Presigned PUT URL the client uploads the object to directly. */
    String presignUploadUrl(String objectKey, String contentType, Duration ttl);

    /** True if the object exists in the bucket (used to confirm a direct upload). */
    boolean objectExists(String objectKey);

    /** Short-lived presigned GET URL for viewing/downloading. */
    String presignDownloadUrl(String objectKey, Duration ttl);
}
