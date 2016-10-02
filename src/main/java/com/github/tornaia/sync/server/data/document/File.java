package com.github.tornaia.sync.server.data.document;

import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class File {
	
	@Id
	private String id;
	
	private String path;
	
	private byte[] data;
	
	private String userId;
	
	private long creationDate;

	private long lastModifiedDate;
	
	public File(){}

	public File(String path, byte[] data, String userId, long creationDate, long lastModifiedDate) {
		super();
		this.path = path;
		this.data = data;
		this.userId = userId;
		this.creationDate = creationDate;
		this.lastModifiedDate = lastModifiedDate;
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(long lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public String getName(){
		if(path == null || Paths.get(getPath()).getFileName() == null) {
			return StringUtils.EMPTY;
		}
		return Paths.get(getPath()).getFileName().toString();
	}
	
}
