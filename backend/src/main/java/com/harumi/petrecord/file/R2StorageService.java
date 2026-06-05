package com.harumi.petrecord.file;

import java.time.Duration;

public interface R2StorageService {

    String presignUploadUrl(String objectKey, String contentType, Duration ttl);

    boolean objectExists(String objectKey);

    String presignDownloadUrl(String objectKey, Duration ttl);
}
