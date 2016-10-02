package com.github.tornaia.sync.server.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.github.tornaia.sync.server.data.document.File;

public interface FileRepository extends MongoRepository<File, String> {

	@Query("{'path' : ?0}")
	File findByPath(String path);
	
	@Query("{'path' : ?0, 'userId' : ?1}")
	File findByPathAndUserId(String path, String userId);

}
