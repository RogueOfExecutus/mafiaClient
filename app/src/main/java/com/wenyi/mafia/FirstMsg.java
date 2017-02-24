package com.wenyi.mafia;

public class FirstMsg {
	private int type;
	private String msg;
	public FirstMsg(int type,String msg) {
		// TODO Auto-generated constructor stub
		super();
		this.type = type;
		this.msg = msg;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
}
