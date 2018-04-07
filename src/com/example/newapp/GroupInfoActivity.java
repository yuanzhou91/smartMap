package com.example.newapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import models.Group;
import models.GroupUserRelation;
import models.User;

import org.json.JSONException;
import org.json.JSONObject;

import util.DBHelper;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;

import dao.GroupDAO;
import dao.RelationDAO;
import dao.UserDAO;

public class GroupInfoActivity extends Activity{
	Group group;
	private static final int REAUTH_ACTIVITY_CODE = 100;
	private static final String TAG = "SmartMap";
	private GraphUser user; 
	private List<BaseListElement> listElements;
	private UiLifecycleHelper uiHelper;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.group_info_view);

		uiHelper = new UiLifecycleHelper(this, null);
		uiHelper.onCreate(savedInstanceState);

		// Set up the list view items, based on a list of
		// BaseListElement items
		listElements = new ArrayList<BaseListElement>();
		// Add an item for the friend picker
		listElements.add(new PeopleListElement(0));
		if (savedInstanceState != null) {
			// Restore the state for each list element
			for (BaseListElement listElement : listElements) {
				listElement.restoreState(savedInstanceState);
			}   
		}
		user = ((SmartMapApplication)getApplication()).getUser();
		TextView txtGroupName = (TextView) findViewById(R.id.group_name);
		Intent i = getIntent();
		// getting attached intent data
		String groupName = i.getStringExtra("group_name");
		// displaying selected product name
		txtGroupName.setText(groupName);

		GroupDAO groupDAO = new GroupDAO(new DBHelper(getApplicationContext()));
		group = groupDAO.getGroupByName(groupName);
		TextView txtGroupOwner = (TextView) findViewById(R.id.group_owner);
		
		String userAccount = group.getOwnerAccount();
		if(userAccount.length()>0){
			User usr = new UserDAO(new DBHelper(getApplicationContext())).getUserByAccount(userAccount);
			txtGroupOwner.setText(usr.getUsername());
		}else {
			txtGroupOwner.setText("Not known");
		}
		
		Button leaveGroupButton = (Button) findViewById(R.id.leave_group);
		Button deleteGroupButton = (Button) findViewById(R.id.delete_group);
		ViewGroup layout = (ViewGroup) leaveGroupButton.getParent();
		if(group.getOwnerAccount().equals(user.getId())){
			layout.removeView(leaveGroupButton);
			deleteGroupButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// Launching new Activity on selecting single List Item
					Intent i = getIntent();
					String groupName = i.getStringExtra("group_name");
					GroupDAO groupDAO = new GroupDAO(new DBHelper(getApplicationContext()));
					groupDAO.deleteGroupByName(groupName, true);
					finish();
				} 
			}); 
		}else{
			layout.removeView(deleteGroupButton);
			leaveGroupButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// Launching new Activity on selecting single List Item
					Intent i = getIntent();
					String groupName = i.getStringExtra("group_name");
					RelationDAO relationDAO = new RelationDAO(new DBHelper(getApplicationContext()));
					relationDAO.deleteRelation(groupName, user.getId(),true);
					finish();
				} 
			}); 
		}


		ImageView imageView  = (ImageView)findViewById(R.id.imageView1);
		int groupThumbnail = group.getGroupThumnail();

		imageView.setImageResource(groupThumbnail);

		Button shareFBButton = (Button) findViewById(R.id.share_group);
		shareFBButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				HashSet<String> userIds = new RelationDAO(new DBHelper(getApplicationContext())).getUserIdsByGroup(group.getGroupName());
				List<String> userAccounts = new ArrayList<String>(userIds);
				if (FacebookDialog.canPresentShareDialog(getApplicationContext(), 
						FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
					// Publish the post using the Share Dialog
					FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(GroupInfoActivity.this)
					.setLink("https://github.com/yingdaluo/smartmapMobile")
					.setApplicationName("SmartMap")
					.setDescription("I am right now in group:"+group.getGroupName()+" in SmartMap!")
					.setName(group.getGroupName())
					.setFriends(userAccounts)
					.build();
					uiHelper.trackPendingDialogCall(shareDialog.present());

				} else {
					// Fallback. For example, publish the post using the Feed Dialog
				}
				
			} 
		});

		Button inviteButton = (Button) findViewById(R.id.invite_members);
		BaseListElement listElement = listElements.get(0);
		inviteButton.setOnClickListener(listElement.getOnClickListener());


		Button showMemberButton = (Button) findViewById(R.id.show_members);
		showMemberButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(getApplicationContext(), ShowMembersActivity.class);
				intent.putExtra("groupName", group.getGroupName());
				startActivity(intent);
			} 
		});
	}
	private void startPickerActivity(Uri data, int requestCode) {
		Intent intent = new Intent();
		intent.setData(data);
		intent.setClass(this, PickerActivity.class);
		startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
			@Override
			public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
				Log.e("Activity", String.format("Error: %s", error.toString()));
			}

			@Override
			public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
				Log.i("Activity", "Success!");
			}
		});
		if (requestCode == REAUTH_ACTIVITY_CODE) {
			uiHelper.onActivityResult(requestCode, resultCode, data);
		} else if (resultCode == Activity.RESULT_OK && 
				requestCode >= 0 && requestCode < listElements.size()) {
			listElements.get(requestCode).onActivityResult(data);
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	private class PeopleListElement extends BaseListElement {
		private List<GraphUser> selectedUsers;
		private static final String FRIENDS_KEY = "friends";
		public PeopleListElement(int requestCode) {
			super(getResources().getDrawable(R.drawable.user),
					getResources().getString(R.string.action_people),
					getResources().getString(R.string.action_people_default),
					requestCode);
		}

		@Override
		protected View.OnClickListener getOnClickListener() {
			return new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					startPickerActivity(PickerActivity.FRIEND_PICKER, getRequestCode());
				}
			};
		}

		private void setUsersText() {
			String text = null;
			if (selectedUsers != null) {
				// If there is one friend
				if (selectedUsers.size() == 1) {
					text = String.format(getResources()
							.getString(R.string.single_user_selected),
							selectedUsers.get(0).getName());
				} else if (selectedUsers.size() == 2) {
					// If there are two friends 
					text = String.format(getResources()
							.getString(R.string.two_users_selected),
							selectedUsers.get(0).getName(), 
							selectedUsers.get(1).getName());
				} else if (selectedUsers.size() > 2) {
					// If there are more than two friends 
					text = String.format(getResources()
							.getString(R.string.multiple_users_selected),
							selectedUsers.get(0).getName(), 
							(selectedUsers.size() - 1));
				}   
			}   
			if (text == null) {
				// If no text, use the placeholder text
				text = getResources()
						.getString(R.string.action_people_default);
			}   
			// Set the text in list element. This will notify the 
			// adapter that the data has changed to
			// refresh the list view.
			//setText2(text);
			TextView text2 = (TextView) findViewById(R.id.text2);
			if (text2 != null) {
				text2.setText(text);
			}
		}

		@Override
		protected void onActivityResult(Intent data) {
			selectedUsers = ((SmartMapApplication)getApplication())
					.getSelectedUsers();

			String groupNameString = group.getGroupName();
			RelationDAO relationDAO = new RelationDAO(new DBHelper(getApplicationContext()));
			for (GraphUser user: selectedUsers) {
				String userAccount = user.getId();
				GroupUserRelation relation = new GroupUserRelation();
				relation.setGroupName(groupNameString);
				relation.setUserAccount(userAccount);
				relationDAO.createRelation(relation, true);
			}
		}
		private List<GraphUser> restoreByteArray(byte[] bytes) {
			try {
				@SuppressWarnings("unchecked")
				List<String> usersAsString =
				(List<String>) (new ObjectInputStream
						(new ByteArrayInputStream(bytes)))
						.readObject();
				if (usersAsString != null) {
					List<GraphUser> users = new ArrayList<GraphUser>
					(usersAsString.size());
					for (String user : usersAsString) {
						GraphUser graphUser = GraphObject.Factory
								.create(new JSONObject(user), 
										GraphUser.class);
						users.add(graphUser);
					}   
					return users;
				}   
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "Unable to deserialize users.", e); 
			} catch (IOException e) {
				Log.e(TAG, "Unable to deserialize users.", e); 
			} catch (JSONException e) {
				Log.e(TAG, "Unable to deserialize users.", e); 
			}   
			return null;
		}
		private byte[] getByteArray(List<GraphUser> users) {
			// convert the list of GraphUsers to a list of String 
			// where each element is the JSON representation of the 
			// GraphUser so it can be stored in a Bundle
			List<String> usersAsString = new ArrayList<String>(users.size());

			for (GraphUser user : users) {
				usersAsString.add(user.getInnerJSONObject().toString());
			}   
			try {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				new ObjectOutputStream(outputStream).writeObject(usersAsString);
				return outputStream.toByteArray();
			} catch (IOException e) {
				Log.e(TAG, "Unable to serialize users.", e); 
			}   
			return null;
		}   

		@Override
		protected void onSaveInstanceState(Bundle bundle) {
			if (selectedUsers != null) {
				bundle.putByteArray(FRIENDS_KEY,
						getByteArray(selectedUsers));
			}   
		} 

		@Override
		protected boolean restoreState(Bundle savedState) {
			byte[] bytes = savedState.getByteArray(FRIENDS_KEY);
			if (bytes != null) {
				selectedUsers = restoreByteArray(bytes);
				setUsersText();
				return true;
			}   
			return false;
		} 
	}
}