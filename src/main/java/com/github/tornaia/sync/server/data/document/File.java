package com.github.tornaia.sync.server.data.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class File {
	
	@Id
	private String id;
	
	private String name;
	
	private byte[] data;
	
	@DBRef
	private Folder folder;
	
	public File(String name, byte[] data, Folder folder){
		this.name = name;
		this.data = data;
		this.folder = folder;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}
	
}
