package com.example.newapp;

import java.util.ArrayList;
import java.util.List;
import models.Group;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

public class GroupListAdapter extends ArrayAdapter<Group>{

	Context context; 
	int layoutResourceId; 
	List<Group> groupList;
	List<Group> rawGroups;
	public GroupListAdapter(Context context, int resource, List<Group> objects) {
		super(context, resource, objects);
		this.context = context;
		this.layoutResourceId = resource;
		this.groupList = objects;
		this.rawGroups = new ArrayList<Group>(objects);
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

		Group group = groupList.get(position);
		holder.txtTitle.setText(group.getGroupName());
		holder.imgIcon.setImageResource(group.getGroupThumnail());

		return row;
	}
	static class GroupHolder
	{
		ImageView imgIcon;
		TextView txtTitle;
	}
	@Override
	public int getCount(){
		return groupList.size();
	}

	@Override
	public Filter getFilter() {
		return new Filter() {
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				// Now we have to inform the adapter about the new list filtered
				groupList = (List<Group>) results.values;
				notifyDataSetChanged();

			}

			@SuppressLint("DefaultLocale")
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults  filteredResults = new FilterResults();

				// We implement here the filter logic
				if (constraint == null || constraint.length() == 0) {
					// No filter implemented we return all the list
					filteredResults.values = rawGroups;
					filteredResults.count = rawGroups.size();
				}
				else {
					// We perform filtering operation
					List<Group> gList = new ArrayList<Group>();
					groupList = rawGroups;
					for (Group group : groupList) {
						if(group.getGroupName().toUpperCase().startsWith(constraint.toString().toUpperCase())){
							gList.add(group);
						}
					}

					filteredResults.values = gList;
					filteredResults.count = gList.size();

				}

				return filteredResults;
			}

		};
	}


}
