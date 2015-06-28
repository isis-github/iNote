//MainActivity.java
package com.inote.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.inote.adapter.ICursorAdapter;
import com.inote.db.DateTimeUtil;
import com.inote.db.Db.NoteItems;
import com.inote.log.ILog;
import com.inote.model.Restore;
import com.inote.model.Backup;
import com.inote.R;

/*
 * 显示所有的文件夹和没有父文件夹的便签
 */
public class MainActivity extends Activity  {


	//显示记录的控件
	private ListView mListview;
	
	//游标适配器，绑定数据到listview
	private ICursorAdapter mAdapter;
	
	//游标
	private Cursor mCursor;
	
	//保存当前点击位置
	private int currentId;
	
	// 菜单
	private static final int MENU_DELETE_ID = 1002;
	private static final int MENU_NEW_NOTE = Menu.FIRST;
	private static final int MENU_NEW_FOLDER = Menu.FIRST + 1;
	private static final int MENU_MOVE_TO_FOLDER = Menu.FIRST + 2;
	private static final int MENU_DELETE = Menu.FIRST + 3;
	private static final int MENU_BACKUP_DATA = Menu.FIRST + 4;
	private static final int MENU_RESTORE_DATA_FROM_SDCARD = Menu.FIRST + 5;
	private static final int MENU_SEARCH_NOTE = Menu.FIRST + 10;
	private static final int MENU_REFRESH = Menu.FIRST + 11;
	
	//新建笔记按钮
	private Button btnCreateNote;
	
	//调试标签
	public static final String TAG = "Note";
	
	//新建笔记按钮监听器
	private OnClickListener listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.create_note:
				newNote();
				break;

			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_page);

		btnCreateNote = (Button) findViewById(R.id.create_note);
		btnCreateNote.setOnClickListener(listener);
		mListview = (ListView) findViewById(R.id.list);
		registerForContextMenu(mListview);
		
		// 更新ListView数据
		this.updateDisplay();
		mListview.setOnItemClickListener(new OnItemClickListener() {
			// 点击文件夹或者便签执行该回调函数
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				// mCursor在updateDisplay函数中进行初始化
				mCursor.moveToPosition(position);
				ILog.d(TAG, "MainActivity==>被点击的记录的Position : " + position);
				// 传递被选中记录的ID
				intent.putExtra(NoteItems._ID,
						mCursor.getInt(mCursor.getColumnIndex(NoteItems._ID)));
				// 取得此记录的IS_FOLDER字段的值,用以判断选中文件夹还是便签
				String is_Folder = mCursor.getString(mCursor
						.getColumnIndex(NoteItems.IS_FOLDER));
				if (is_Folder.equals("no")) {
					// 不是文件夹
					// 跳转到详细内容页面
					// 传递此记录的CONTENT字段的值
					intent.putExtra(NoteItems.CONTENT, mCursor
							.getString(mCursor
									.getColumnIndex(NoteItems.CONTENT)));
					// 告诉NoteActivity打开它是为了编辑便签
					intent.putExtra("Open_Type", "editNote");
					intent.setClass(MainActivity.this, NoteActivity.class);
				} else if (is_Folder.equals("yes")) {
					// 是文件夹
					// 跳转到FileNotesActivity,显示选中的文件夹下所有的便签
					intent.setClass(MainActivity.this,
							FolderNotesActivity.class);
				}
				startActivity(intent);
			}
		});

	}

	// 创建菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 新建便签
		menu.add(Menu.NONE, MENU_NEW_NOTE, 1, R.string.new_note);
		// 新建文件夹
		menu.add(Menu.NONE, MENU_NEW_FOLDER, 2, R.string.new_folder);
		// 移进文件夹
		menu.add(Menu.NONE, MENU_MOVE_TO_FOLDER, 3, R.string.move_to_folder);
		// 删除
		menu.add(Menu.NONE, MENU_DELETE, 4, R.string.delete);
		// 备份数据
		menu.add(Menu.NONE, MENU_BACKUP_DATA, 5, R.string.backup_data);
		// 从SD卡还原
		menu.add(Menu.NONE, MENU_RESTORE_DATA_FROM_SDCARD, 6,
				R.string.restore_data);
		
		menu.add(Menu.NONE, MENU_SEARCH_NOTE, 7, R.string.search_note);
		
		menu.add(Menu.NONE, MENU_REFRESH, 8, R.string.refresh);

		return super.onCreateOptionsMenu(menu);
	}

	// 菜单选中事件处理函数
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW_NOTE:
			newNote();
			break;
		case MENU_REFRESH:
			updateDisplay();
			break;
		case MENU_SEARCH_NOTE:
			search();
			break;
		case MENU_NEW_FOLDER:
			newFolder();
			break;
		case MENU_MOVE_TO_FOLDER:
			moveToFolder();
			break;
		case MENU_DELETE:
			delete();
			break;
		case MENU_BACKUP_DATA:
			this.backupData();
			break;
		case MENU_RESTORE_DATA_FROM_SDCARD:
			this.restoreDataFromSDCard();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		currentId = (int)info.position;
		menu.add(0, MENU_DELETE_ID, 0, "删除");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (item.getItemId() == MENU_DELETE_ID) {
			//to do
			mCursor.moveToPosition(currentId);
			int itemId = mCursor.getInt(mCursor
					.getColumnIndex(NoteItems._ID));
			String id = String.valueOf(itemId);
			getContentResolver().delete(
					NoteItems.CONTENT_URI,
					" _id = ? or " + NoteItems.PARENT_FOLDER
					+ " = ? ", new String[] { id, id });
		}

		this.updateDisplay();

		return super.onContextItemSelected(item);
	}

	// 新建便签函数
	private void newNote() {
		Intent i = new Intent();
		i.putExtra("Open_Type", "newNote");
		i.setClass(MainActivity.this, NoteActivity.class);
		startActivity(i);
	}

	// 新建文件夹函数
	private void newFolder() {
		Context mContext = MainActivity.this;
		// 使用AlertDialog来处理新建文件夹的动作
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.new_folder);
		builder.setIcon(null);
		// 自定义AlertDialog的布局
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.new_folder,
				(ViewGroup) findViewById(R.id.dialog_layout_new_folder_root));
		builder.setView(layout);
		// 设置一个类似确定的按钮
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 实例化AlertDialog中的EditText对象
				EditText et_folder_name = (EditText) layout
						.findViewById(R.id.et_dialog_new_folder);
				// 取得EditText对象的值
				String newFolderName = et_folder_name.getText()
						.toString();
				// 判断文件夹名称是否为空
				if (!TextUtils.isEmpty(newFolderName)) {
					// 名称符合条件则插入数据库
					ContentValues values = new ContentValues();
					values.put(NoteItems.CONTENT, newFolderName);
					values.put(NoteItems.UPDATE_DATE,
							DateTimeUtil.getDate());
					values.put(NoteItems.UPDATE_TIME,
							DateTimeUtil.getTime());
					values.put(NoteItems.IS_FOLDER, "yes");
					values.put(NoteItems.PARENT_FOLDER, -1);
					getContentResolver().insert(NoteItems.CONTENT_URI,
							values);
					// 更新ListView的数据源
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		// 设置一个类似取消的按钮
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 点击取消按钮,撤销新建文件夹对话框
				dialog.dismiss();
			}
		});
		AlertDialog ad = builder.create();
		ad.show();
	}
	
	private void search() {
		Context mContext = MainActivity.this;
		// 使用AlertDialog来处理新建文件夹的动作
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.search_note);
		builder.setIcon(null);
		// 自定义AlertDialog的布局
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.new_folder,
				(ViewGroup) findViewById(R.id.dialog_layout_new_folder_root));
		builder.setView(layout);
		// 设置一个类似确定的按钮
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 实例化AlertDialog中的EditText对象
				EditText et_folder_name = (EditText) layout
						.findViewById(R.id.et_dialog_new_folder);
				// 取得EditText对象的值
				String newFolderName = et_folder_name.getText()
						.toString();
				// 判断文件夹名称是否为空
				if (!TextUtils.isEmpty(newFolderName)) {
					// 名称符合条件则插入数据库
//					ContentValues values = new ContentValues();
//					values.put(NoteItems.CONTENT, newFolderName);
//					values.put(NoteItems.UPDATE_DATE,
//							DateTimeUtil.getDate());
//					values.put(NoteItems.UPDATE_TIME,
//							DateTimeUtil.getTime());
//					values.put(NoteItems.IS_FOLDER, "yes");
//					values.put(NoteItems.PARENT_FOLDER, -1);
//					getContentResolver().insert(NoteItems.CONTENT_URI,
//							values);
//					// 更新ListView的数据源
//					mAdapter.notifyDataSetChanged();
					searchDisplay(newFolderName);
				}
			}
		});
		// 设置一个类似取消的按钮
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 点击取消按钮,撤销新建文件夹对话框
				dialog.dismiss();
			}
		});
		AlertDialog ad = builder.create();
		ad.show();
	}

	// 移进文件夹函数
	private void moveToFolder() {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, MoveToFolderActivity.class);
		startActivity(intent);
	}

	// 删除函数
	private void delete() {
		Intent i = new Intent(getApplicationContext(),
				DeleteRecordsActivity.class);
		startActivity(i);
	}



	// 备份数据函数(备份到XML文件中)
	private void backupData() {
		Backup wx = new Backup(this);
		try {
			// 如果写入失败,则用Toast提醒用户
			if (!wx.writeXml()) {
				Toast.makeText(this, R.string.backupDataFailed,
						Toast.LENGTH_SHORT).show();
				ILog.d(TAG, "MainActivity==>backup to SDCard failed");
			} else {
				Toast.makeText(this, R.string.backupDataSuc, Toast.LENGTH_SHORT)
				.show();
				ILog.d(TAG, "MainActivity==>backup to SDCard successfully");
			}
		} catch (Exception e) {
			// 有待改进,以改善UE
			ILog.d(TAG,
					"MainActivity==>backupData get Exception : "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

	// 从SD卡恢复数据函数
	private void restoreDataFromSDCard() {
		Restore rsd = new Restore(getContentResolver());
		try {
			rsd.restoreData();
			mAdapter.notifyDataSetChanged();
			Toast.makeText(this, R.string.restoreDataSuc, Toast.LENGTH_SHORT)
			.show();
		} catch (Exception e) {
			Toast.makeText(this, R.string.restoreDataFailed, Toast.LENGTH_SHORT)
			.show();
			ILog.d(TAG,
					"MainActivity==>restoreDataFromSDCard Failed Exception : "
							+ e.getMessage());
			e.printStackTrace();
		}
	}


	// 负责更新ListView中的数据
	private void updateDisplay() {
		// 查询条件，查询所有文件夹记录及显示在主页的便签记录
		String selection = NoteItems.PARENT_FOLDER + " = " + "-1";

		mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
				selection, null, null);

		mAdapter = new ICursorAdapter(this, mCursor, true);
		mListview.setAdapter(mAdapter);
		ILog.d(TAG, "MainActivity==>Update Display finished...");
	}
	
	// 负责更新ListView中的数据
	private void searchDisplay(String searchValue) {
		// 查询条件，查询所有文件夹记录及显示在主页的便签记录
		String selection = NoteItems.CONTENT + " like " + "'%"+ searchValue +"%'";

		mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
				selection, null, null);

		mAdapter = new ICursorAdapter(this, mCursor, true);
		mListview.setAdapter(mAdapter);
	}
	


}