package com.github.tornaia.sync.shared.api;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

public class FileMetaInfo implements Serializable {

	public final String id;
    public final String relativePath;
    public final long length;
    public final long creationDateTime;
    public final long modificationDateTime;

    public FileMetaInfo() {
    	this.id = null;
        this.relativePath = null;
        this.length = -1;
        this.creationDateTime = -1;
        this.modificationDateTime = -1;
    }

    public FileMetaInfo(String id, String relativePath, File file) throws IOException {
    	this.id = id;
        this.relativePath = relativePath;
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        this.length = attr.size();
        this.creationDateTime = attr.creationTime().toMillis();
        this.modificationDateTime = attr.lastModifiedTime().toMillis();
    }

	public FileMetaInfo(String id, String relativePath, long length, long creationDateTime, long modificationDateTime) {
		super();
		this.id = id;
		this.relativePath = relativePath;
		this.length = length;
		this.creationDateTime = creationDateTime;
		this.modificationDateTime = modificationDateTime;
	}
    
    

}
