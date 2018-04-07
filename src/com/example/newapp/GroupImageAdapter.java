package com.example.newapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class GroupImageAdapter extends BaseAdapter {
	private Context mContext;

	public GroupImageAdapter(Context c) {
		mContext = c;
	}

	public int getCount() {
		return mThumbIds.length;
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) {  // if it's not recycled, initialize some attributes
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(3, 3, 3, 3);
		} else {
			imageView = (ImageView) convertView;
		}

		imageView.setImageResource(mThumbIds[position]);
		return imageView;
	}

	// references to our images
	public Integer[] mThumbIds = {
			R.drawable.map,R.drawable.bookmark, 
			R.drawable.calendar,R.drawable.lock, 
			R.drawable.camera, R.drawable.cloud,
			R.drawable.gift, R.drawable.home,
			R.drawable.music, R.drawable.picture,
			R.drawable.settings, R.drawable.shopping,
			R.drawable.t_shirt, R.drawable.tag,
			R.drawable.thumb_up, R.drawable.tools,
			R.drawable.trophy, R.drawable.twitter,
	};
}
