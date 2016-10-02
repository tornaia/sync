package com.github.tornaia.sync.server.data.document;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class File {
	
	private static final String DELIMITER = "\\";
	
	@Id
	private String id;
	
	private String path;
	
	private byte[] data;
	
	private String userId;
	
	private Integer revision;
	
	public File(){}
	
	public File(String path, byte[] data, String userId, Integer revision) {
		super();
		this.path = path;
		this.data = data;
		this.userId = userId;
		this.revision = revision;
	}

	public File(String path, byte[] data){
		this.path = path;
		this.data = data;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getName(){
		if(path == null || Paths.get(getPath()).getFileName() == null) {
			return StringUtils.EMPTY;
		}
		return Paths.get(getPath()).getFileName().toString();
	}
	
}
