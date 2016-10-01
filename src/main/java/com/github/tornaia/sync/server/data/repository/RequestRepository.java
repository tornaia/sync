package com.github.tornaia.sync.server.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.tornaia.sync.server.data.document.Request;

public interface RequestRepository extends MongoRepository<Request, String> {

}
