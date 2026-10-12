package com.javora.commerce.common.storage;

import java.io.InputStream;

public interface StorageService {

	// Stores the content under a generated name inside the directory and returns the object key.
	String store(String directory, InputStream content, long size, String contentType, String extension);

	void delete(String objectKey);

	String publicUrl(String objectKey);

}
