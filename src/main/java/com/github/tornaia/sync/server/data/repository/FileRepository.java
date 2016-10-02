package com.github.tornaia.sync.server.data.repository;

import com.github.tornaia.sync.server.data.document.File;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface FileRepository extends MongoRepository<File, String> {

    @Query("{'path' : ?0}")
    File findByPath(String path);

    @Query("{'id' : ?0, 'userId' : ?1}")
    File findByIdAndUserId(String path, String userId);

    @Query("{'userId' : ?0}, 'lastModifiedDate' : {$gt : ?1}")
    List<File> findAllModified(String userId, long lastModifiedDate);
}
