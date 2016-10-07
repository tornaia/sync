package com.github.tornaia.sync.server.data.repository;

import com.github.tornaia.sync.server.data.document.File;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface FileRepository extends MongoRepository<File, String> {

    @Query("{'path' : ?0}")
    File findByPath(String path);

    @Query("{'id' : ?0, 'userid' : ?1}")
    File findByIdAndUserId(String path, String userid);

    @Query("{$and: [{'userid' : ?0}, {'lastModifiedDate' : {$gt : ?1}}] }")
    List<File> findByUserIdAndLastModifiedDateAfter(String userid, long lastModifiedDate);

    @Query("{'userid': ?0}")
    List<File> findByUserid(String userid);
}
