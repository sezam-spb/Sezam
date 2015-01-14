package com.spb.sezam.management;

public enum ElementType {
	FILE("file"),
	GROUP("group");
	
	private String name;
	
	ElementType(String name){
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
