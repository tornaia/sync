package com.github.tornaia.sync.server.data.document;

import java.util.Date;

import org.springframework.data.annotation.Id;

public class Request {
	
	public Request(Date date, Integer counter) {
		this.date = date;
		this.counter = counter;
	}

	@Id
	private String id;

	private Date date;
	
	private Integer counter;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public Integer getCounter() {
		return counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}

	@Override
	public String toString() {
		return String.format("Request[id=%s, date='%s', counter='%s']", id, date, counter);
	}

}
