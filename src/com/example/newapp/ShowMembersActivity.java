package com.example.newapp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import util.DBHelper;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;

import dao.RelationDAO;


public class ShowMembersActivity extends ListActivity {
	//ListView Adapter
	MemberListAdapter adapter;
	Activity activity;
	String groupName;
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = 
			new Session.StatusCallback() {
		@Override
		public void call(Session session, 
				SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_members);
		// Add the fragment on initial activity setup
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
		activity = this;
		getFriends(Session.getActiveSession());
		groupName = this.getIntent().getStringExtra("groupName");
		
		
		Button doneButton = (Button) findViewById(R.id.done);
		doneButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			} 
		});
		
	}

	private void onSessionStateChange(final Session session, SessionState sessionState, Exception ex){
		if(session != null && session.isOpened()){
			getFriends(session);
		}
	}
	private void getFriends(final Session session){
		Session activeSession = Session.getActiveSession();
		if(activeSession.getState().isOpened()){
			Request friendRequest = Request.newMyFriendsRequest(activeSession, 
					new GraphUserListCallback(){
				@Override
				public void onCompleted(List<GraphUser> users,
						Response response) {
					if (session == Session.getActiveSession()) {
						HashSet<String> userIds = new RelationDAO(new DBHelper(activity.getApplicationContext())).getUserIdsByGroup(groupName);
						List<GraphUser> friendList = new ArrayList<GraphUser>();
						for (GraphUser user: users) {
							if(userIds.contains(user.getId())){
								friendList.add(user);
							}
						}
						adapter = new MemberListAdapter(activity, R.layout.group_list_item, friendList);
						// Binding resources Array to ListAdapter
						setListAdapter(adapter);
					}
					if (response.getError() != null) {
						// Handle errors, will do so later.
					}
				}
			});
			Bundle params = new Bundle();
			params.putString("fields", "id, name, picture");
			friendRequest.setParameters(params);
			friendRequest.executeAsync();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
		Session session = Session.getActiveSession();
		session.addCallback(callback);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession()
		.onActivityResult(this, requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);


	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}


}
