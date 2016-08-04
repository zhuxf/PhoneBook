package net.alpha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.alpha.AlphaView.OnAlphaChangedListener;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Activity01 extends Activity implements OnAlphaChangedListener {
	private ListView listView;
	private AlphaView alphaView;
	private TextView overlay;

	private WindowManager windowManager;
	private AsyncQueryHandler queryHandler;
	private List<ContactItem> list;
	private ListAdapter adapter;
	private HashMap<String, Integer> alphaIndexer; // 存放存在的汉语拼音首字母和与之对应的列表位置
	private OverlayThread overlayThread;

	private static final Uri uri = Uri
			.parse("content://com.android.contacts/data/phones");
	private static final String[] projection = { "_id", "display_name",
			"data1", "sort_key" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		queryHandler = new MyAsyncQueryHandler(getContentResolver());
		list = new ArrayList<ContactItem>();
		alphaIndexer = new HashMap<String, Integer>();
		overlayThread = new OverlayThread();
		intitWidget();
		initOverlay();
	}

	@Override
	protected void onResume() {
		super.onResume();
		startQuery();
	}

	@Override
	protected void onStop() {
		try {
			windowManager.removeViewImmediate(overlay);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onStop();
	}

	// 初始化控件
	private void intitWidget() {
		listView = (ListView) findViewById(R.id.list_view);
		alphaView = (AlphaView) findViewById(R.id.alphaView);
		alphaView.setOnAlphaChangedListener(this);
	}

	// 初始化汉语拼音首字母弹出提示框
	private void initOverlay() {
		LayoutInflater inflater = LayoutInflater.from(this);
		overlay = (TextView) inflater.inflate(R.layout.overlay, null);
		overlay.setVisibility(View.INVISIBLE);
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_APPLICATION,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
		windowManager = (WindowManager) this
				.getSystemService(Context.WINDOW_SERVICE);
		windowManager.addView(overlay, lp);
	}

	private void startQuery() {
		queryHandler.startQuery(1, null, uri, projection, "data1 is not null",
				null, "sort_key COLLATE LOCALIZED asc");
	}

	// 异步查询类
	private class MyAsyncQueryHandler extends AsyncQueryHandler {

		public MyAsyncQueryHandler(ContentResolver cr) {
			super(cr);

		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			list.clear();
			if (cursor != null) {
				while (cursor.moveToNext()) {
					ContactItem item = new ContactItem();
					item.setName(cursor.getString(1));
					item.setNumber(Utils.formatNumber(cursor.getString(2)));
					item.setAlpha(Utils.formatAlpha(cursor.getString(3)));
					System.out.println(">>>>>>>>>>>>>>>>>>>>>"+item.getName()+":"+item.getAlpha());
					list.add(item);
				}
			}
			ContactItem item = new ContactItem();
			item.setName("123");
			item.setNumber("123");
			item.setAlpha("推荐");
			list.add(item);
			if (list.size() > 0) {
				setAdapter();
			}
		}

	}

	private void setAdapter() {
		if (adapter == null) {
			adapter = new ListAdapter();
			listView.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	private class ListAdapter extends BaseAdapter {
		private LayoutInflater inflater;

		public ListAdapter() {
			this.inflater = LayoutInflater.from(Activity01.this);
			for (int i = 0; i < list.size(); i++) {
				// 当前汉语拼音首字母
				String currentAlpha = list.get(i).getAlpha();
				// 上一个汉语拼音首字母，如果不存在为“ ”
				String previewAlpha = (i - 1) >= 0 ? list.get(i - 1).getAlpha()
						: " ";
				if (!previewAlpha.equals(currentAlpha)) {
					String alpha = list.get(i).getAlpha();
					alphaIndexer.put(alpha, i);
				}
			}
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item, null);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ContactItem item = list.get(position);
			holder.name.setText(item.getName());
			holder.number.setText(item.getNumber());

			String currentAlpha = list.get(position).getAlpha();
			String previewAlpha = (position - 1) >= 0 ? list.get(position - 1)
					.getAlpha() : " ";
			if (!previewAlpha.equals(currentAlpha)) {
				holder.alpha.setVisibility(View.VISIBLE);
				holder.alpha.setText(currentAlpha);
			} else {
				holder.alpha.setVisibility(View.GONE);
			}
			return convertView;
		}

	}

	private final class ViewHolder {
		TextView alpha;
		TextView name;
		TextView number;

		public ViewHolder(View v) {
			alpha = (TextView) v.findViewById(R.id.alpha_text);
			name = (TextView) v.findViewById(R.id.name);
			number = (TextView) v.findViewById(R.id.number);
		}
	}

	private Handler handler = new Handler();

	// 设置overlay不可见
	private class OverlayThread implements Runnable {

		@Override
		public void run() {
			overlay.setVisibility(View.GONE);
		}

	}

	@Override
	public void OnAlphaChanged(String s, int index) {
		if (s != null && s.trim().length() > 0) {
//			overlay.setText(s);
//			overlay.setVisibility(View.VISIBLE);
//			handler.removeCallbacks(overlayThread);
//			handler.postDelayed(overlayThread, 700);
			if (alphaIndexer.get(s) != null) {
				int position = alphaIndexer.get(s);
				listView.setSelection(position);
			}
		}
	}

}