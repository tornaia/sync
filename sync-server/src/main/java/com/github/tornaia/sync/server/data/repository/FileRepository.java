package com.github.tornaia.sync.server.data.repository;

import com.github.tornaia.sync.server.data.document.File;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface FileRepository extends MongoRepository<File, String> {

    @Query("{'userid' : ?0, 'path' : ?1 }")
    File findByUseridAndPath(String userid, String path);

    @Query("{$and: [{'userid' : ?0}, {'lastModifiedDate' : {$gt : ?1}}] }")
    List<File> findByUseridAndLastModifiedDateAfter(String userid, long lastModifiedDate);

    @Query("{'userid' : ?0, 'path' : {'$regex' : '^?1', '$options' : 'i'} }")
    List<File> findByUseridAndPathStartsWith(String userid, String path);

    File findByUseridAndId(String userid, String id);
}
