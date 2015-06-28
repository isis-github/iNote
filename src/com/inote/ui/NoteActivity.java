//NoteActivity.java
package com.inote.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.inote.db.DateTimeUtil;
import com.inote.db.Db.NoteItems;
import com.inote.log.ILog;
import com.inote.R;

/*
 * 一条便签的详细信息页面。
 */

public class NoteActivity extends Activity {
	private TextView tv_note_title;
	private EditText et_content;

	// 用户创建或更新便签的日期/时间
	private String updateDate;
	private String updateTime;
	// 用于判断是新建便签还是更新便签
	private String openType;
	// 数据库中原有的便签的内容
	private String oldContent;
	// 接受传递过来的Intent对象
	private Intent intent;
	// 被编辑的便签的ID
	private int _id;
	// 被编辑便签所在的文件夹的ID
	private int folderId;
	// 菜单
	private static final int MENU_DELETE = Menu.FIRST;
	private static final int MENU_SHARE = Menu.FIRST + 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.note);
		// 得到有前一个Activity传递过来的Intent对象
		intent = getIntent();
		// 如果没有传递Intent对象,则返回主页(MainActivity)
		if (intent.equals(null)) {
			startActivity(new Intent(NoteActivity.this, MainActivity.class));
		}
		// 取得Open_Type的值,判断是新建便签还是更新便签
		openType = intent.getStringExtra("Open_Type");
		ILog.d(MainActivity.TAG, "NoteActivity==>" + String.valueOf(openType));
		// 被编辑的便签的ID
		_id = intent.getIntExtra(NoteItems._ID, -1);
		ILog.d(MainActivity.TAG, "NoteActivity==>被编辑的便签的id:" + _id);
		// 得到文件夹的ID(如果从文件夹页面内新建或编辑便签则要求传递文件夹的ID)
		folderId = intent.getIntExtra("FolderId", -1);
		ILog.d(MainActivity.TAG, "NoteActivity==>要操作的文件夹的 id :" + folderId);

		
		//初始化部件
		initViews();
	}

	@Override
	protected void onResume() {

		super.onResume();
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 删除
		menu.add(Menu.NONE, MENU_DELETE, 1, R.string.delete);

		// 修改文件夹
		menu.add(Menu.NONE, MENU_SHARE, 2, R.string.share_sms_or_email);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DELETE:
			deleteNote();
			break;

		case MENU_SHARE:

			showShareDialog();
			break;

		case android.R.id.home:
			onBackPressed();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 初始化组件
	private void initViews() {

		tv_note_title = (TextView) findViewById(R.id.tv_note_date_time);
		et_content = (EditText) findViewById(R.id.et_content);
		if (_id != -1) {// 正常得到_id,编辑主页或文件夹下的便签
			// 根据便签的ID查询该便签的详细内容
			Cursor c = getContentResolver().query(
					ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id),
					null, null, null, null);
			c.moveToFirst();
			// 最后更新便签的日期时间及其内容
			oldContent = c.getString(c.getColumnIndex(NoteItems.CONTENT));
			updateDate = c.getString(c.getColumnIndex(NoteItems.UPDATE_DATE));
			updateTime = c.getString(c.getColumnIndex(NoteItems.UPDATE_TIME));


			c.close();
		}
		// 判断打开方式
		if (openType.equals("newNote")) {// 新建"顶级便签",即没有放在文件夹内的便签
			// 初始化新建便签的日期时间
			updateDate = DateTimeUtil.getDate();
			updateTime = DateTimeUtil.getTime();

		} else if (openType.equals("editNote")) {// 编辑顶级便签(不在文件夹内的便签)
			et_content.setText(oldContent);
			et_content.setSelection(et_content.getText().length());

		} else if (openType.equals("newFolderNote")) {// 在某文件夹下新建便签
			// 初始化新建便签的日期时间
			updateDate = DateTimeUtil.getDate();
			updateTime = DateTimeUtil.getTime();

		} else if (openType.equals("editFolderNote")) {// 编辑某文件夹下的便签
			et_content.setText(oldContent);
			et_content.setSelection(et_content.getText().length());

		}

		tv_note_title.setText(updateDate + "\t" + updateTime.substring(0, 8));
	}




	@Override
	public void onBackPressed() {

		String content = et_content.getText().toString();
		// 判断是更新还是新建便签
		if (openType.equals("newNote")) {
			// 创建主页上的便签(顶级便签)
			if (!TextUtils.isEmpty(content)) {
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				values.put(NoteItems.IS_FOLDER, "no");
				values.put(NoteItems.PARENT_FOLDER, -1);
				getContentResolver().insert(NoteItems.CONTENT_URI, values);
			}
		} else if (openType.equals("newFolderNote")) {
			// 创建文件夹下的便签
			if (!TextUtils.isEmpty(content)) {
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				values.put(NoteItems.IS_FOLDER, "no");
				values.put(NoteItems.PARENT_FOLDER, folderId);
				getContentResolver().insert(NoteItems.CONTENT_URI, values);
			}
		} else if (openType.equals("editNote")) {
			// 编辑主页上的便签
			if (!TextUtils.isEmpty(content)) {
				// 内容不为空,更新记录
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				getContentResolver().update(
						ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id),
						values, null, null);
			}
		} else if (openType.equals("editFolderNote")) {
			// 更新文件夹下的便签
			if (!TextUtils.isEmpty(content)) {
				// 更新记录
				ContentValues values = new ContentValues();
				values.put(NoteItems.CONTENT, content);
				values.put(NoteItems.UPDATE_DATE, DateTimeUtil.getDate());
				values.put(NoteItems.UPDATE_TIME, DateTimeUtil.getTime());
				values.put(NoteItems.IS_FOLDER, "no");
				values.put(NoteItems.PARENT_FOLDER, folderId);
				getContentResolver().update(
						ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id),
						values, null, null);
				ILog.d(MainActivity.TAG, "NoteActivity==>编辑文件夹下的记录时,文件夹的id : "
						+ folderId);
			}
		}
		if (!TextUtils.isEmpty(content)) {
			oldContent = content;
		}
		super.onBackPressed();
	}


	// 删除便签
	private void deleteNote() {
		Context mContext = NoteActivity.this;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.delete_note);
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				String content = et_content.getText().toString();

				if(content.equals("")){

					return;
				}

				// 构造Uri
				Uri deleUri = ContentUris.withAppendedId(
						NoteItems.CONTENT_URI, _id);
				getContentResolver().delete(deleUri, null, null);
				ILog.d(MainActivity.TAG,
						"NoteActivity==>deleteNote() via ContentResolver");

				// 返回上一级
				Intent intent = new Intent();
				if (openType.equals("editNote")) {
					// 显示主页
					intent.setClass(NoteActivity.this,
							MainActivity.class);
				} else if (openType.equals("editFolderNote")) {
					// 显示便签所属文件夹下页面
					intent.putExtra(NoteItems._ID, folderId);
					intent.setClass(NoteActivity.this,
							FolderNotesActivity.class);
				}
				startActivity(intent);
			}
		});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 点击取消按钮,撤销删除便签对话框
				dialog.dismiss();
			}
		});
		AlertDialog ad = builder.create();
		ad.show();
	}


	private void showShareDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("选择分享类型");
		final CharSequence[] items = {"邮件","短信","其他"};
		builder.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				String noteText = et_content.getText().toString();

				dialog.dismiss();
				switch (which) {
				case 0: //邮件
					sendMail(noteText);
					break;
				case 1: //短信
					sendSMS(noteText);
					break;
				case 2: //调用系统分享
					Intent intent=new Intent(Intent.ACTION_SEND); 
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_SUBJECT,"分享笔记");   
					intent.putExtra(Intent.EXTRA_TEXT, noteText);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   
					startActivity(Intent.createChooser(intent, "分享到"));
					break;
				default:
					break;
				}

			}
		});
		builder.setNegativeButton( "取消" ,  new  DialogInterface.OnClickListener() {    
			@Override    
			public   void  onClick(DialogInterface dialog,  int  which) {    
				dialog.dismiss();    
			}    
		});    
		builder.create().show();
	}

	private void sendMail(String content){

		Intent email = new Intent(android.content.Intent.ACTION_SEND);
		email.setType("plain/text");

		String emailBody = content;

		//邮件主题
		email.putExtra(android.content.Intent.EXTRA_SUBJECT, "分享笔记");

		//邮件内容
		email.putExtra(android.content.Intent.EXTRA_TEXT, emailBody);  

		startActivityForResult(Intent.createChooser(email,  "请选择邮件发送内容" ), 1001 ); 
	}


	private   void  sendSMS(String content){

		String smsBody = content;
		Uri smsToUri = Uri.parse( "smsto:" );  
		Intent sendIntent =  new  Intent(Intent.ACTION_VIEW, smsToUri);  
		sendIntent.putExtra( "sms_body", smsBody);  
		sendIntent.setType( "vnd.android-dir/mms-sms" );  
		startActivityForResult(sendIntent, 1002 );  
	}

}