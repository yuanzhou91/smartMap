package com.example.newapp;

import java.util.List;

import models.Group;
import util.DBHelper;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.model.GraphUser;

import dao.RelationDAO;

public class ChooseMapActivity extends ListActivity {

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// use your custom layout
		setContentView(R.layout.fragment_map);
		RelationDAO relationDAO = new RelationDAO(new DBHelper(this));
		GraphUser user = ((SmartMapApplication)getApplication()).getUser();
		if(user == null){
			return ;
		}
		List<Group> groupList = relationDAO.findGroupsByUserAccount(user.getId());	
		GroupListAdapter adapter = new GroupListAdapter(this, android.R.id.list, groupList);
		setListAdapter(adapter);
		setTitle("Select a Group");
		
		Button createGroupButton = (Button) findViewById(R.id.create_a_group);
		createGroupButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Launching new Activity on selecting single List Item
				Intent i = new Intent(getApplicationContext(), CreateGroupActivity.class);
				// sending data to new activity
				startActivity(i);
			} 
		});
		
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String groupName = ((TextView) v.findViewById(R.id.label)).getText().toString();
		//Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
		((SmartMapApplication)getApplication()).groupName = groupName;
		finish();
	}
}
