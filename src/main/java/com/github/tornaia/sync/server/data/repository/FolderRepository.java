package com.github.tornaia.sync.server.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.tornaia.sync.server.data.document.Folder;

public interface FolderRepository extends MongoRepository<Folder, String> {

}
