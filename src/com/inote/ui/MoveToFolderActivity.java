//MoveToFolderActivity.java
package com.inote.ui;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.inote.adapter.ListItemView;
import com.inote.adapter.ICursorAdapter;
import com.inote.db.Db.NoteItems;
import com.inote.log.ILog;
import com.inote.R;

/*
 * 移进文件夹页面
 */
public class MoveToFolderActivity extends Activity {
	private ICursorAdapter mAdapter;
	private ListView mListview;
	private Button btnOK, btnCancel;
	private int folderId;

	// 数组,用于收集被选中的item的id
	private Map<Integer, Integer> mIds;
	private Cursor mCursor;

	private OnClickListener listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnOK:
				// 弹出对话框,供用户选择目标文件夹
				chooseFolder();
				break;
			case R.id.btnCancelDel:
				finish();
				break;
			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.del_or_move_records);

		btnOK = (Button) findViewById(R.id.btnOK);
		btnCancel = (Button) findViewById(R.id.btnCancelDel);
		mListview = (ListView) findViewById(R.id.listview);

		mIds = new HashMap<Integer, Integer>();

		// 判断删除主页的记录还是文件夹下的记录
		folderId = getIntent().getIntExtra("FolderId", -1);
		if (folderId == -1) {// 要删除主页上的记录,因此查询主页上的记录
			String selection = NoteItems.IS_FOLDER + " = '" + "no" + "' and "+ NoteItems.PARENT_FOLDER + " = " + "-1";
			mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
					selection, null, null);

		} else {// 删除文件夹下的记录，因为我们得到了文件夹的_id
			// 查询ID为folderId的文件夹下的记录
			String selection = NoteItems.IS_FOLDER + " = '" + "no" + "' and "+NoteItems.PARENT_FOLDER + "  = ? ";
			String[] selectionArgs = new String[] { String.valueOf(folderId) };
			mCursor = getContentResolver().query(NoteItems.CONTENT_URI, null,
					selection, selectionArgs, null);
		}



		mAdapter = new ICursorAdapter(getApplicationContext(), mCursor, false);
		mListview.setAdapter(mAdapter);
		mListview.setItemsCanFocus(false);
		mListview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mListview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListItemView listItems = (ListItemView) view.getTag();
				listItems.cb_right.toggle();
				ICursorAdapter.isSelected.put(position,
						listItems.cb_right.isChecked());
				mCursor.moveToPosition(position);
				// 获取对应位置上的记录的ID
				int itemId = mCursor.getInt(mCursor
						.getColumnIndex(NoteItems._ID));
				if (ICursorAdapter.isSelected.get(position)) {
					mIds.put(position, itemId);
					ILog.d(MainActivity.TAG,
							"MoveToFolderActivity==>被点击的记录的id : " + itemId
							+ "\t" + position);
				} else {
					mIds.remove(position);
				}
			}
		});
		btnOK.setOnClickListener(listener);
		btnCancel.setOnClickListener(listener);
	}

	// 选择目标文件夹
	private void chooseFolder() {
		// 先判断是否选择了记录,如果没有选择记录则不弹出选择文件夹的对话框
		final int noteCount = mIds.size();
		ILog.d(MainActivity.TAG, "MoveToFolderActivity==>被选择的记录的数量:"
				+ noteCount);
		if (noteCount > 0) {// 选择了要移进文件夹的便签
			// 查询所有的文件夹记录
			String selection = NoteItems.IS_FOLDER + " = '" + "yes" + "' and "+NoteItems.PARENT_FOLDER + "  = ? ";
			String[] selectionArgs = new String[] { String.valueOf(folderId) };
			final Cursor folderCursor = getContentResolver()
					.query(NoteItems.CONTENT_URI, null, selection,
							selectionArgs, null);
			// 文件夹的数量
			int count = folderCursor.getCount();
			ILog.d(MainActivity.TAG, "MoveToFolderActivity==>文件夹的数量:" + count);
			if (count > 0) {// 有文件夹
				// 将从数据库中查询到的文件夹的名称放入字符串数组
				String[] folders = new String[count];

				for (int i = 0; i < count; i++) {
					folderCursor.moveToPosition(i);
					folders[i] = folderCursor.getString(folderCursor
							.getColumnIndex(NoteItems.CONTENT));
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setItems(folders,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						folderCursor.moveToPosition(item);
						// 取得被选中的文件夹的ID
						int folderId = folderCursor.getInt(folderCursor
								.getColumnIndex(NoteItems._ID));
						// 更新记录
						int count = mCursor.getCount();
						for (int i = 0; i < count; i++) {
							String strTmp = String.valueOf(mIds.get(i));
							if (!(strTmp == "null")) {// 如果不为"null",则可更新
								// 得到被选择的记录的ID
								int noteId = mIds.get(i);
								Uri tmpUri = ContentUris
										.withAppendedId(
												NoteItems.CONTENT_URI,
												noteId);
								// 更新记录
								ContentValues values = new ContentValues();
								values.put(NoteItems.PARENT_FOLDER,
										folderId);
								getContentResolver().update(tmpUri,
										values, null, null);
								ILog.d(MainActivity.TAG,
										"MoveToFolderActivity==>要将选中的记录移进id为 : "
												+ folderId + " 的文件夹");
							}
						}
						finish();
					}
				});
				builder.create().show();
			} else {// 用户未曾创建文件夹
				Toast.makeText(getApplicationContext(), "不存在文件夹!",
						Toast.LENGTH_LONG).show();
			}
		} else {// 用户没有选择任何要移进文件夹的便签
			Toast.makeText(getApplicationContext(), "您没有选中任何便签!",
					Toast.LENGTH_LONG).show();
		}
	}
}