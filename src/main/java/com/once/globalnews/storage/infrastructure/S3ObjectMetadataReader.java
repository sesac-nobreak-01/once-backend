package com.once.globalnews.storage.infrastructure;

import com.once.globalnews.storage.config.S3Properties;
import com.once.globalnews.storage.domain.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ObjectMetadataReader {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public Optional<HeadObjectResponse> head(String s3Key) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(s3Key)
                    .build());
            return Optional.of(response);
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw new StorageException("S3 HeadObject 실패: " + s3Key, e);
        }
    }

    public byte[] getObjectBytes(String s3Key) {
        try {
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(s3Key)
                    .build());
            return bytes.asByteArray();
        } catch (S3Exception e) {
            throw new StorageException("S3 GetObject 실패: " + s3Key, e);
        }
    }

    public void deleteObject(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(s3Key)
                    .build());
            log.info("S3 object deleted: {}", s3Key);
        } catch (S3Exception e) {
            log.error("S3 DeleteObject 실패: {}", s3Key, e);
        }
    }

    public String bucket() {
        return s3Properties.getBucket();
    }
}
