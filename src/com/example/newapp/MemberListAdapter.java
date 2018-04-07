package com.example.newapp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.model.GraphUser;

public class MemberListAdapter extends ArrayAdapter<GraphUser>{
	private final String TAG = "MemberListAdapter";
	Context context; 
	int layoutResourceId; 
	List<GraphUser> userList;
	public MemberListAdapter(Context context, int resource, List<GraphUser> objects) {
		super(context, resource, objects);
		this.context = context;
		this.layoutResourceId = resource;
		this.userList = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		GroupHolder holder = null;

		if(row == null)
		{
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(R.layout.group_list_item, parent, false);
			holder = new GroupHolder();
			holder.imgIcon = (ImageView)row.findViewById(R.id.imgIcon);
			holder.txtTitle = (TextView)row.findViewById(R.id.label);

			row.setTag(holder);
		}
		else
		{
			holder = (GroupHolder)row.getTag();
		}

		GraphUser user = userList.get(position);
		holder.txtTitle.setText(user.getName());
		final GroupHolder finalHolder = holder;
		final String url = "http://graph.facebook.com/"+user.getId()+"/picture";
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					finalHolder.imgIcon.setImageBitmap(getImageBitmap(url));
					MemberListAdapter.this.notifyDataSetChanged();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start(); 
		return row;
	}
	static class GroupHolder
	{
		ImageView imgIcon;
		TextView txtTitle;
	}
	@Override
	public int getCount(){
		return userList.size();
	}
	private Bitmap getImageBitmap(String url) {
		Bitmap bm = null;
		try {
			URL aURL = new URL(url);
			URLConnection conn = aURL.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			bm = BitmapFactory.decodeStream(bis);
			bis.close();
			is.close();
		} catch (IOException e) {
			Log.e(TAG, "Error getting bitmap", e);
		}
		return bm;
	} 
}
