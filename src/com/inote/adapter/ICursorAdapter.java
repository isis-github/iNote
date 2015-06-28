//ICursorAdapter.java
package com.inote.adapter;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inote.db.Db.NoteItems;
import com.inote.R;

/*
 * 自定义Adapter,在移动、删除、显示记录时使用
 */
public class ICursorAdapter extends CursorAdapter {
	// This class is used to instantiate layout XML file into its corresponding
	// View objects.
	private LayoutInflater mListContainer; // 视图容器
	// 存储CheckBox的状态,即是否被选中
	public static Map<Integer, Boolean> isSelected;
	// 删除记录、移动记录还是显示记录
	private boolean isShowingRecords = true;
	// 自定义视图
	private ListItemView listItemView = null;

	// 构造器
	@SuppressWarnings("deprecation")
	public ICursorAdapter(Context context, Cursor c, boolean isShowingRecords) {
		super(context, c);
		this.isShowingRecords = isShowingRecords;
		mListContainer = LayoutInflater.from(context); // 创建视图容器并设置上下文
		isSelected = new HashMap<Integer, Boolean>();
		// 初始化多选框的状态
		int count = c.getCount();
		for (int i = 0; i < count; i++) {
			isSelected.put(i, false);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View convertView = null;
		listItemView = new ListItemView();
		if (!isShowingRecords) {// 删除或移动记录
			// 获取listview中每个item的布局文件的视图
			convertView = mListContainer.inflate(
					R.layout.del_or_move_item, null);
			// 获取控件对象
			listItemView.tv_left = (TextView) convertView
					.findViewById(R.id.tv_left);
			listItemView.cb_right = (CheckBox) convertView
					.findViewById(R.id.cb_right);
		} else { // 显示记录
			// 获取listview中每个item的布局文件的视图
			convertView = mListContainer.inflate(R.layout.item_layout,
					null);
			// 获取控件对象
			listItemView.tv_left = (TextView) convertView
					.findViewById(R.id.tv_left);
			listItemView.tv_right = (TextView) convertView
					.findViewById(R.id.tv_right);
		}
		// 初始化ListView中每一行布局中的LinearLayout
		listItemView.linearlayout = (LinearLayout) convertView
				.findViewById(R.id.listview_linearlayout);
		// 设置控件集到convertView
		convertView.setTag(listItemView);
		return convertView;
	}

	// view newView函数的返回值
	// Cursor cursor记录的位置有系统管理，使用的时候将它当作只含有一个记录的对象。用户只需要直接用。
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		listItemView = (ListItemView) view.getTag();
		int position = cursor.getPosition();
		// 取出字段的值,判断该记录是否为文件夹
		String is_Folder = cursor.getString(cursor
				.getColumnIndex(NoteItems.IS_FOLDER));
		if (is_Folder.equals("no")) {
			// 不是文件夹

			Drawable drawable= listItemView.linearlayout.getContext().getResources().getDrawable(R.drawable.ic_listitem_edit);

			drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());

			listItemView.tv_left.setCompoundDrawables(drawable,null,null,null);

		} else if (is_Folder.equals("yes")) {
			// 是文件夹

			Drawable drawable= listItemView.linearlayout.getContext().getResources().getDrawable(R.drawable.ic_listitem_folder);

			drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());

			listItemView.tv_left.setCompoundDrawables(drawable,null,null,null);

		}
		// 设置标题(或内容)
		String content = cursor.getString(cursor
				.getColumnIndex(NoteItems.CONTENT));

		listItemView.tv_left.setText(content);

		if (!isShowingRecords) { // 移动或删除记录,使用CheckBox来供用户选择要操作的记录
			listItemView.cb_right.setChecked(isSelected.get(position));
		} else {// 显示记录,使用TextView来显示记录的最后更新时间
			// 显示创建(最后更新)记录的日期时间
			listItemView.tv_right.setText(cursor.getString(cursor
					.getColumnIndex(NoteItems.UPDATE_DATE))
					+ "\t"
					+ cursor.getString(
							cursor.getColumnIndex(NoteItems.UPDATE_TIME))
							.substring(0, 8));
		}
	}
}