//INote.java
package com.inote.model;

/*
 * 从XML文件读数据时,以该类的对象进行存储
 */
public class INote {
	private int id;
	private String content;
	private String update_date;
	private String update_time;
	private String isfolder;
	private int parentfolder;

	public INote() {
		super();
	}

	public INote(int id, String con, String update_date, String update_time,
			String alarm_time, int background_color, String isfolder,
			int parentfolder) {
		this.id = id;
		this.content = con;
		this.update_date = update_date;
		this.update_time = update_time;
		this.isfolder = isfolder;
		this.parentfolder = parentfolder;
	}

	public String getUpdate_date() {
		return update_date;
	}

	public void setUpdate_date(String update_date) {
		this.update_date = update_date;
	}

	public String getUpdate_time() {
		return update_time;
	}

	public void setUpdate_time(String update_time) {
		this.update_time = update_time;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getIsfolder() {
		return isfolder;
	}

	public void setIsfolder(String isfolder) {
		this.isfolder = isfolder;
	}

	public int getParentfolder() {
		return parentfolder;
	}

	public void setParentfolder(int parentfolder) {
		this.parentfolder = parentfolder;
	}
}