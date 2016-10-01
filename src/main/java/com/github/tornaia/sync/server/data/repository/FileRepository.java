package com.github.tornaia.sync.server.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.tornaia.sync.server.data.document.File;

public interface FileRepository extends MongoRepository<File, String> {

}
