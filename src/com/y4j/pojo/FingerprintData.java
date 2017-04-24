package com.y4j.pojo;

import java.util.List;

public class FingerprintData {

	private String userId;
	private String fingerPrintData;
	private String name;
	private Integer fingerId;
	private List students;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}         
	public String getFingerPrintData() {
		return fingerPrintData;
	}
	public void setFingerPrintData(String fingerPrintData) {
		this.fingerPrintData = fingerPrintData;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getFingerId() {
		return fingerId;
	}
	public void setFingerId(Integer fingerId) {
		this.fingerId = fingerId;
	}
	public List getStudents() {
		return students;
	}
	public void setStudents(List students) {
		this.students = students;
	}
	
}
