package com.github.tornaia.sync.server.data.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.tornaia.sync.server.data.document.File;

public interface FileRepository extends MongoRepository<File, String> {

	@Query("{'path' : ?0}")
	File findByPath(String path);
	
	@Query("{'id' : ?0, 'userId' : ?1}")
	File findByIdAndUserId(String path, String userId);
	
	@Query("SELECT * FROM File f where f.userId = :userId and f.lastModifiedDate > :lastModifiedDate")
	List<File> findAllModified(@Param("lastModifiedDate")Long lastModifiedDate,@Param("userId") String userId);

}
