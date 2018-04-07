package com.example.newapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import models.Group;
import models.GroupUserRelation;
import org.json.JSONException;
import org.json.JSONObject;
import util.DBHelper;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import dao.GroupDAO;
import dao.RelationDAO;

public class CreateGroupActivity extends Activity {

	Bitmap groupThumbnail;
	View view;
	int grid_column_id;
	GroupImageAdapter imageAdapter;
	private ListView listView;
	private List<BaseListElement> listElements;
	private static final int REAUTH_ACTIVITY_CODE = 100;
	private static final String TAG = "SmartMap";
	private UiLifecycleHelper uiHelper;
	private Session.StatusCallback callback = 
			new Session.StatusCallback() {
		@Override
		public void call(Session session, 
				SessionState state, Exception exception) {
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
		setContentView(R.layout.create_group);

		// Find the list view
		listView = (ListView) findViewById(R.id.selection_list);

		// Set up the list view items, based on a list of
		// BaseListElement items
		listElements = new ArrayList<BaseListElement>();
		// Add an item for the friend picker
		listElements.add(new PeopleListElement(0));
		// Set the list view adapter
		listView.setAdapter(new ActionListAdapter(this, 
				R.id.selection_list, listElements));

		if (savedInstanceState != null) {
			// Restore the state for each list element
			for (BaseListElement listElement : listElements) {
				listElement.restoreState(savedInstanceState);
			}   
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REAUTH_ACTIVITY_CODE) {
			uiHelper.onActivityResult(requestCode, resultCode, data);
		} else if (resultCode == Activity.RESULT_OK && 
				requestCode >= 0 && requestCode < listElements.size()) {
			listElements.get(requestCode).onActivityResult(data);
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
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

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		for (BaseListElement listElement : listElements) {
			listElement.onSaveInstanceState(outState);
		}
		uiHelper.onSaveInstanceState(outState);
	}





	private void startPickerActivity(Uri data, int requestCode) {
		Intent intent = new Intent();
		intent.setData(data);
		intent.setClass(this, PickerActivity.class);
		startActivityForResult(intent, requestCode);
	}

	private class ActionListAdapter extends ArrayAdapter<BaseListElement> {
		private List<BaseListElement> listElements;

		public ActionListAdapter(Context context, int resourceId, 
				List<BaseListElement> listElements) {
			super(context, resourceId, listElements);
			this.listElements = listElements;
			// Set up as an observer for list item changes to
			// refresh the view.
			for (int i = 0; i < listElements.size(); i++) {
				listElements.get(i).setAdapter(this);
			}
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			view = convertView;
			if (view == null) {
				LayoutInflater inflater =
						(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.listitem, null);

				GridView gridView = (GridView) view.findViewById(R.id.group_icon_grid_view);
				imageAdapter = new GroupImageAdapter(view.getContext());
				gridView.setAdapter(imageAdapter);

				/**
				 * On Click event for Single Gridview Item
				 * */
				gridView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View v,
							int position, long id) {
						grid_column_id = position;
						ImageView imageView  = (ImageView)findViewById(R.id.imageView1);
						imageView.setImageResource(imageAdapter.mThumbIds[grid_column_id]);
					}
				});

				Button continueButton = (Button) view.findViewById(R.id.group_add_friends);
				continueButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						//	Launching new Activity on selecting single List Item
						String ownerAccount = ((SmartMapApplication)getApplication()).getUser().getId();
						Group newGroup = new Group();
						newGroup.setGroupThumnail(imageAdapter.mThumbIds[grid_column_id]);
						EditText edText = (EditText)findViewById(R.id.enter_group_name);
						String groupNameString = (edText.getText().toString() == null ||
								edText.getText().toString().equals(""))?
										"(No name)":edText.getText().toString();
						newGroup.setGroupName(groupNameString);
						newGroup.setOwnerAccount(ownerAccount);

						GroupDAO groupDAO = new GroupDAO(new DBHelper(getApplicationContext()));
						RelationDAO relationDAO = new RelationDAO(new DBHelper(getApplicationContext()));
						groupDAO.createGroup(newGroup, true);
						List<GraphUser> selectedUsers = ((SmartMapApplication)getApplication())
								.getSelectedUsers();
						if(selectedUsers!=null){
							for (GraphUser user: selectedUsers) {
								String userAccount = user.getId();
								GroupUserRelation relation = new GroupUserRelation();
								relation.setGroupName(groupNameString);
								relation.setUserAccount(userAccount);
								relationDAO.createRelation(relation,true);
							}
						}

						finish();

					} 
				}); 


			}

			BaseListElement listElement = listElements.get(position);
			if (listElement != null) {
				TextView text2 = (TextView) view.findViewById(R.id.text2);
				text2.setOnClickListener(listElement.getOnClickListener());

				if (text2 != null) {
					text2.setText(listElement.getText2());
				}
			}
			return view;
		}

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
			setUsersText();
			//notifyDataChanged();
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
