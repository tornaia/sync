package com.github.tornaia.sync.server.controller.dto;

import com.github.tornaia.sync.server.data.document.File;

public class FileState {
	
private String id;
	
	private String path;
	
	private Integer contentLength;
	
	private String userId;
	
	private Integer revision;
	
	public FileState(File file){
		this.id = file.getId();
		this.path = file.getPath();
		this.contentLength = file.getData() != null ? file.getData().length : 0;
		this.userId = file.getUserId();
		this.revision = file.getRevision();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getContentLength() {
		return contentLength;
	}

	public void setContentLength(Integer contentLength) {
		this.contentLength = contentLength;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}
	
}
