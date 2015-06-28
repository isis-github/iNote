//DateTimeUtil.java
package com.inote.db;


import android.annotation.SuppressLint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("SimpleDateFormat")
public final class DateTimeUtil {
	// 日期操作
	public static String getDate() {

		Locale locale = new Locale("zh_CN");
		Locale.setDefault(locale);

		String pattern = "yyyy-MM-dd HH:mm:ss Z";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		String date = formatter.format(new Date()).substring(0, 11);

		return date;
	}

	// 时间操作
	public static String getTime() {
		Locale locale = new Locale("zh_CN");
		Locale.setDefault(locale);

		String pattern = "yyyy-MM-dd HH:mm:ss Z";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		String time = formatter.format(new Date()).substring(11, 20);
		return time;
	}
}