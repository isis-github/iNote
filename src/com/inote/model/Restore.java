//Restore.java
package com.inote.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;

import com.inote.db.Db.NoteItems;
import com.inote.log.ILog;
import com.inote.ui.MainActivity;

/*
 * 从Xml文件中读取数据,并插入数据库
 */
public class Restore {
	private ContentResolver mContentResolver;
	private String BASE_DIR = "/sdcard";
	private String MID_DIR = "iNote";
	private String FILE_NAME = "notes_backup.xml";

	public Restore(ContentResolver mContentResolver) {
		this.mContentResolver = mContentResolver;
	}

	// 读取XML文件中的记录,保存到List对象中
	public List<INote> readXml(InputStream is) throws Exception {
		SAXParserFactory pf = SAXParserFactory.newInstance();
		SAXParser saxp = pf.newSAXParser();
		IDefaultHandler mdh = new IDefaultHandler();
		saxp.parse(is, mdh);
		is.close();
		return mdh.getNotes();
	}

	// 从List对象导入数据库
	public void restoreData() throws Exception {
		ILog.d(MainActivity.TAG,
				"RestoreDataFromXml==>start to restore data from SDCard");
		String path = BASE_DIR + File.separator + MID_DIR + File.separator
				+ FILE_NAME;
		File file = new File(path);
		FileInputStream fis = new FileInputStream(file);
		List<INote> records = this.readXml(fis);
		ILog.d(MainActivity.TAG, "RestoreDataFromXml==>XML文件中Note Item 的数量："
				+ records.size());
		int id = -1;
		for (INote tmpNote : records) {
			// 我们恢复数据使用insert而非update
			String content = tmpNote.getContent();
			String updateDate = tmpNote.getUpdate_date();
			String updateTime = tmpNote.getUpdate_time();
			String isFolder = tmpNote.getIsfolder();
			ILog.d(MainActivity.TAG, "RestoreDataFromXml==>isfolder:"
					+ isFolder);
			int parentFolder = tmpNote.getParentfolder();
			ILog.d(MainActivity.TAG, "RestoreDataFromXml==>原来的parentfolder:"
					+ parentFolder);
			// 恢复记录
			ContentValues cv = new ContentValues();
			cv.put(NoteItems.CONTENT, content);
			cv.put(NoteItems.UPDATE_DATE, updateDate);
			cv.put(NoteItems.UPDATE_TIME, updateTime);
			cv.put(NoteItems.IS_FOLDER, isFolder);
			// 如果记录既不是文件夹,也不是主页上的便签,那我们就把他存在上一个插入到数据库的文件夹中
			if (isFolder.equals("no") && id != -1 && parentFolder != -1) {
				cv.put(NoteItems.PARENT_FOLDER, id);
				ILog.d(MainActivity.TAG,
						"RestoreDataFromXml==>恢复数据后的parentfolder:" + id);
			} else {// 如果是文件夹或主页便签,则PARENT_FOLDER字段为-1
				cv.put(NoteItems.PARENT_FOLDER, -1);
			}
			ILog.d(MainActivity.TAG,
					"RestoreDataFromXml==>insert one record...");
			if (isFolder.equals("yes")) {// 如果是文件夹,我们记录它被插入到数据库时的id
				String strId = (mContentResolver.insert(NoteItems.CONTENT_URI,
						cv)).getPathSegments().get(1);
				// 记录文件夹被插入到数据库后的id,该文件夹下的便签的PARENT_FOLDER字段将被设置为该值
				id = Integer.parseInt(strId);
				ILog.d(MainActivity.TAG,
						"RestoreDataFromXml==>文件夹被插入到数据库后的id: " + strId);
			} else {// 不是文件夹,我们就不需要记录它的id
				mContentResolver.insert(NoteItems.CONTENT_URI, cv);
			}
		}
		ILog.d(MainActivity.TAG, "RestoreDataFromXml==>Restore finished...");
	}
}