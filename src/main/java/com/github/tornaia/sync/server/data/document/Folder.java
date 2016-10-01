package com.github.tornaia.sync.server.data.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

public class Folder {
	
	@Id
	private String id;
	
	private String name;
	
	private List<String> path = new ArrayList<>();
	
	public Folder() {
	}
	
	public Folder(String name){
		this.name = name;
	}

	public Folder(String name, Folder parent){
		this.name = name;
		ArrayList<String> path = new ArrayList<>(parent.getPath());
		path.add(id);
		this.path = path;
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

	public List<String> getPath() {
		return path;
	}

	public void setPath(List<String> path) {
		this.path = path;
	}
	
}
