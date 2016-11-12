package com.app.client;


public enum TimeInterval {

	I5MIN(0, 5, "I5MIN"),
	I10MIN(1, 10, "I10MIN"),
	I30MIN(2, 30, "I30MIN"),
	
	I4SEC(3, 4, "I4SEC"),
	I10SEC(4, 10, "I10SEC"),
	I16SEC(5, 16, "I16SEC"),
	NEVER(6, Integer.MAX_VALUE, "NEVER");
	
	private int id;
	private int value = 0;
	private String replace;
	
	private TimeInterval(int id, int value, String replace) {
		this.id = id;
		this.value = value;
		this.replace = replace;
	}
	
	public int getValue() {
		return value;
	}
	
	public int getId() {
		return id;
	}
	
	public String getReplace() {
		return replace;
	}
	
	public static TimeInterval lookup(int id) {
		for (TimeInterval it: TimeInterval.values()) {
			if (id == it.getId()) {
				return it;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return WTimer.getString(replace);
	}
}
