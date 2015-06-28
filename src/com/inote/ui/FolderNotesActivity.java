//FolderNotesActivity.java
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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.inote.adapter.ICursorAdapter;
import com.inote.db.DateTimeUtil;
import com.inote.db.Db.NoteItems;
import com.inote.log.ILog;
import com.inote.R;

/*
 * 显示某一文件夹下的所有便签
 */
public class FolderNotesActivity extends Activity {
	private ListView mListview;

	private ICursorAdapter mAdapter;

	private Cursor mCursor;
	// 得到点击修改文件夹名称Menu以前的名称
	private String oldFolderName;
	// 菜单
	private static final int MENU_DELETE_ID = 1002;
	private int currentId;
	private static final int MENU_NEW_NOTE = Menu.FIRST;
	// 修改文件夹名称
	private static final int MENU_UPDATE_FOLDER = Menu.FIRST + 1;
	private static final int MENU_MOVE_OUTOF_FOLDER = Menu.FIRST + 2;
	private static final int MENU_DELETE = Menu.FIRST + 3;
	private static final int MENU_NEW_FOLDER = Menu.FIRST + 4;
	private static final int MENU_MOVE_TO_FOLDER = Menu.FIRST + 5;
	private Button btnCreateNote;
	// 文件夹的ID
	private int _id;

	private OnClickListener listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.create_note:
				newFolderNote();

				break;

			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.main_page);

		btnCreateNote = (Button) findViewById(R.id.create_note);
		btnCreateNote.setOnClickListener(listener);


		Intent i = getIntent();
		// 如果没有传递Intent对象,则返回主页(MainActivity)
		if (i.equals(null)) {
			startActivity(new Intent(FolderNotesActivity.this,
					MainActivity.class));
		}
		_id = i.getIntExtra(NoteItems._ID, -1);
		// 查询该文件夹记录.内容保存到Cursor对象中
		Uri tmpUri = ContentUris.withAppendedId(NoteItems.CONTENT_URI, _id);
		Cursor c2 = getContentResolver().query(tmpUri, null, null, null, null);
		c2.moveToFirst();
		oldFolderName = c2.getString(c2.getColumnIndex(NoteItems.CONTENT));
		c2.close();
		initViews();
		registerForContextMenu(mListview);
		mListview.setOnItemClickListener(new OnItemClickListener() {


			// 点击文件夹或者便签执行该回调函数
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				// mCursor在updateDisplay函数中进行初始化
				mCursor.moveToPosition(position);

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
					intent.putExtra("FolderId", _id);
					ILog.d(MainActivity.TAG, "FolderNotesActivity==>进入id为: " + _id
							+ " 的文件夹");
					// 传递被编辑便签的内容
					intent.putExtra(NoteItems.CONTENT, mCursor.getString(mCursor
							.getColumnIndex(NoteItems.CONTENT)));
					// 编辑便签的方式
					intent.putExtra("Open_Type", "editFolderNote");
					// 跳转到NoteActivity
					intent.setClass(FolderNotesActivity.this, NoteActivity.class);
					//startActivity(intent);
				} else if (is_Folder.equals("yes")) {
					// 是文件夹
					// 跳转到FileNotesActivity,显示选中的文件夹下所有的便签
					intent.setClass(FolderNotesActivity.this,
							FolderNotesActivity.class);
				}
				startActivity(intent);
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 新建便签
		menu.add(Menu.NONE, MENU_NEW_NOTE, 1, R.string.new_note);
		// 修改文件夹名称
		menu.add(Menu.NONE, MENU_UPDATE_FOLDER, 2, R.string.edit_folder_title);
		// 移出文件夹
		menu.add(Menu.NONE, MENU_MOVE_OUTOF_FOLDER, 3,
				R.string.move_out_of_folder);
		// 删除
		menu.add(Menu.NONE, MENU_DELETE, 4, R.string.delete);

		// 新建文件夹
		menu.add(Menu.NONE, MENU_NEW_FOLDER, 5, R.string.new_folder);


		menu.add(Menu.NONE, MENU_MOVE_TO_FOLDER, 6, R.string.move_to_folder);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW_NOTE:
			newFolderNote();
			break;
		case MENU_UPDATE_FOLDER:
			updateFolderName();
			break;
		case MENU_MOVE_OUTOF_FOLDER:
			moveOutOfFolder();
			break;
		case MENU_DELETE:
			delete();
			break;
		case MENU_MOVE_TO_FOLDER:
			moveToFolder();
			break;

		case MENU_NEW_FOLDER:
			newFolder();
			break;

		case android.R.id.home:
			onBackPressed();
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

		updateDisplay(oldFolderName);

		return super.onContextItemSelected(item);
	}

	// 初始化组件
	private void initViews() {

		mListview = (ListView) findViewById(R.id.list);


		updateDisplay(oldFolderName);
	}


	// 移进文件夹函数
	private void moveToFolder() {
		Intent intent = new Intent();
		intent.putExtra("FolderId", _id);
		intent.setClass(FolderNotesActivity.this, MoveToFolderActivity.class);
		startActivity(intent);
	}

	// 新建文件夹函数
	private void newFolder() {
		Context mContext = this;
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
					values.put(NoteItems.PARENT_FOLDER, _id);
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

	// 新建便签函数
	private void newFolderNote() {
		Intent i = new Intent();
		// 传递打开NoteActivity的方式
		i.putExtra("Open_Type", "newFolderNote");
		// 传递文件夹ID
		i.putExtra("FolderId", _id);
		i.setClass(FolderNotesActivity.this, NoteActivity.class);
		startActivity(i);
	}

	// 修改文件夹名称
	private void updateFolderName() {
		Context mContext = FolderNotesActivity.this;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.edit_folder_title);

		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.new_folder,
				(ViewGroup) findViewById(R.id.dialog_layout_new_folder_root));
		builder.setView(layout);
		builder.setPositiveButton(R.string.Ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText et_folder_name = (EditText) layout
						.findViewById(R.id.et_dialog_new_folder);
				String newFolderName = et_folder_name.getText()
						.toString();
				if ((!TextUtils.isEmpty(newFolderName))
						&& newFolderName != oldFolderName) {
					// 新文件夹名称不为空,且不等于原有的名称,则更新
					Uri tmpUri = ContentUris.withAppendedId(
							NoteItems.CONTENT_URI, _id);
					ContentValues values = new ContentValues();
					values.put(NoteItems.CONTENT, newFolderName);
					values.put(NoteItems.UPDATE_DATE,
							DateTimeUtil.getDate());
					values.put(NoteItems.UPDATE_TIME,
							DateTimeUtil.getTime());
					getContentResolver().update(tmpUri, values, null,
							null);
					oldFolderName = newFolderName;

				}
			}
		});
		builder.setNegativeButton(R.string.Cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 点击取消按钮,撤销修改文件夹名称对话框
				dialog.dismiss();
			}
		});
		AlertDialog ad = builder.create();
		ad.show();
	}

	// 移出文件夹
	private void moveOutOfFolder() {
		Intent i = new Intent();
		i.setClass(FolderNotesActivity.this, MoveOutOfFolderActivity.class);
		// 传递文件夹的ID
		i.putExtra("folderId", _id);
		startActivity(i);
	}

	// 删除函数,选择删除文件夹下的便签
	private void delete() {
		Intent i = new Intent(getApplicationContext(),
				DeleteRecordsActivity.class);
		// 传递文件夹的ID
		i.putExtra("folderId", _id);
		startActivity(i);
	}

	// 负责更新数据
	private void updateDisplay(String folderName) {
		// 查询所属文件夹Id为_id的记录
		String selection = NoteItems.PARENT_FOLDER + "  = ? ";
		String[] selectionArgs = new String[] { String.valueOf(_id) };
		mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
				selection, selectionArgs, null);
		mAdapter = new ICursorAdapter(this, mCursor, true);
		mListview.setAdapter(mAdapter);


	}

}