package com.javora.commerce.common.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

	private static final String PUBLIC_PREFIX = "products";

	private final MinioClient client;
	private final StorageProperties properties;

	private volatile boolean bucketReady;

	@Override
	public String store(String directory, InputStream content, long size, String contentType, String extension) {
		String objectKey = "%s/%s.%s".formatted(directory, UUID.randomUUID(), extension);
		try {
			ensureBucket();
			client.putObject(PutObjectArgs.builder()
					.bucket(properties.bucket())
					.object(objectKey)
					.stream(content, size, -1L)
					.contentType(contentType)
					.build());
			return objectKey;
		} catch (Exception ex) {
			log.error("Could not store object {}", objectKey, ex);
			throw new StorageException("File storage is currently unavailable");
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			client.removeObject(RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
		} catch (Exception ex) {
			// A leftover object is harmless, so a failed cleanup must not fail the request.
			log.warn("Could not delete object {}: {}", objectKey, ex.getMessage());
		}
	}

	@Override
	public String publicUrl(String objectKey) {
		String base = properties.publicBaseUrl();
		return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + objectKey;
	}

	private void ensureBucket() throws Exception {
		if (bucketReady) {
			return;
		}
		String bucket = properties.bucket();
		if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
			client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
		}
		// Only product images are readable anonymously; everything else stays private.
		String policy = """
				{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
				"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/%s/*"]}]}""".formatted(bucket, PUBLIC_PREFIX);
		client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
		bucketReady = true;
	}

}
