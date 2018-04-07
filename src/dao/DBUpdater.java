package dao;

import http.HttpClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import models.Group;
import models.GroupUserRelation;
import models.User;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import util.MySQLiteHelper;

public class DBUpdater implements Runnable{

	private String userAccount;

	private MySQLiteHelper dbHelper;

	public DBUpdater(MySQLiteHelper dbHelper,String id) {
		this.dbHelper = dbHelper;
		userAccount = id;
	}

	@Override
	public void run() {
		while (true) {
			updateUserGroups();
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateUserGroups(){
		List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("method", "GET_USER_GROUPS"));
		nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_OWNERACCOUNT, userAccount));
		String json = HttpClient.postData(nameValuePairs);
		GroupDAO groupDAO = new GroupDAO(dbHelper);
		RelationDAO relationDAO = new RelationDAO(dbHelper);
		List<Group> groups = relationDAO.findGroupsByUserAccount(userAccount);

		try {
			HashSet<String> dict = new HashSet<String>();
			JSONArray groupArray = new JSONArray(json);
			for (int i = 0; i < groupArray.length(); i++) {
				JSONObject object = (JSONObject) groupArray.get(i);
				String groupName = object.getString(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME);
				dict.add(groupName);
			}
			for(Group g : groups){
				if(!dict.contains(g.getGroupName())) {
					relationDAO.deleteRelation(g.getGroupName(), userAccount, false);
				}
			}

			for (int i = 0; i < groupArray.length(); i++) {
				JSONObject object = (JSONObject) groupArray.get(i);
				String groupName = object.getString(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME);
				String ownerAccount = object.getString(MySQLiteHelper.TABLE_GROUP_COLUMN_OWNERACCOUNT);
				String thumbnail = object.getString(MySQLiteHelper.TABLE_GROUP_COLUMN_THUMBNAIL);
				Group group = new Group();
				group.setGroupName(groupName);
				group.setOwnerAccount(ownerAccount);
				group.setGroupThumnail(Integer.parseInt(thumbnail));
				groupDAO.createGroup(group,false);
				if (!group.getOwnerAccount().equals(userAccount)) {
					GroupUserRelation relation = new GroupUserRelation();
					relation.setGroupName(group.getGroupName());
					relation.setUserAccount(userAccount);
					relationDAO.createRelation(relation, false);
				}
			}			

		} catch (JSONException e) {
			e.printStackTrace();
		}
		updateRelations();
	}

	private void updateRelations(){
		List<Group> groups = new RelationDAO(dbHelper).findGroupsByUserAccount(userAccount);
		for(Group group : groups){
			List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("method", "GET_GROUP_LOCATIONS"));
			nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, group.getGroupName()));
			String json = HttpClient.postData(nameValuePairs);
			try {

				JSONArray groupArray = new JSONArray(json);
				for (int i = 0; i < groupArray.length(); i++) {
					JSONObject object = (JSONObject) groupArray.get(i);
					String facebookAccount = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT);
					String userName = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME);
					String latitude = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE);
					String longitude = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE);

					RelationDAO relationDAO = new RelationDAO(dbHelper);
					GroupUserRelation relation = new GroupUserRelation();
					relation.setGroupName(group.getGroupName());
					relation.setUserAccount(facebookAccount);
					relationDAO.createRelation(relation,false);

					UserDAO userDAO = new UserDAO(dbHelper);
					userDAO.postUserLocation(facebookAccount, userName, Double.parseDouble(latitude), Double.parseDouble(longitude), false);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public List<User> getLocalUsersLocationsByGroupName(String groupName) {
		UserDAO userDAO = new UserDAO(dbHelper);
		HashSet<String> userIds = new RelationDAO(dbHelper).getUserIdsByGroup(groupName);
		List<User> userList = new ArrayList<User>();
		for (String userIdString : userIds) {
			userList.add(userDAO.getUserByAccount(userIdString));
		}
		return userList;
	}

	public static List<User> getRemoteUsersLocationsByGroupName(String groupName) {
		List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("method", "GET_GROUP_LOCATIONS"));
		nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, groupName));
		String json = HttpClient.postData(nameValuePairs);
		List<User> userList = new ArrayList<User>();
		try {
			JSONArray groupArray = new JSONArray(json);
			for (int i = 0; i < groupArray.length(); i++) {
				JSONObject object = (JSONObject) groupArray.get(i);
				String facebookAccount = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT);
				String userName = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME);
				String latitude = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE);
				String longitude = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE);

				//				RelationDAO relationDAO = new RelationDAO(dbHelper);
				//				GroupUserRelation relation = new GroupUserRelation();
				//				relation.setGroupName(groupName);
				//				relation.setUserAccount(facebookAccount);
				//				relationDAO.createRelation(relation,false);

				//UserDAO userDAO = new UserDAO(dbHelper);

				User newUser = new User();
				newUser.setFacebookAccount(facebookAccount);
				newUser.setUsername(userName);
				newUser.setLatitude(Double.parseDouble(latitude));
				newUser.setLongitude(Double.parseDouble(longitude));
				userList.add(newUser);
				//userDAO.postUserLocation(facebookAccount, userName, Double.parseDouble(latitude), Double.parseDouble(longitude), false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return userList;
	}


	public static MarkerOptions getTargetMarkerOptions(String groupName){
		MarkerOptions marker = new MarkerOptions();
		List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("method", "GET_DESTINATION"));
		nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, groupName));
		String json = HttpClient.postData(nameValuePairs);
		try {
			JSONObject object = new JSONObject(json);
			String title = object.getString("destination");
			String latitude = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE);
			String longitude = object.getString(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE);

			marker.title(title);
			marker.position(new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude)));
			marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
			return marker;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


	public static void postUserLocation(String userId, String userName,LatLng latLng){
		final String userIdtoPost = userId;
		final String userNameToPost = userName;
		final LatLng latLngToPost = latLng;
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("method", "POST_LOCATION"));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, userIdtoPost));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME, userNameToPost));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE, String.valueOf(latLngToPost.latitude)));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE, String.valueOf(latLngToPost.longitude)));
					HttpClient.postData(nameValuePairs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start(); 
	}


	public static void postTargetMarkerOptions(String groupName, MarkerOptions options){
		final MarkerOptions optionsToPost = options;
		final String groupNameToPost = groupName;
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					LatLng latLng = optionsToPost.getPosition();
					List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("method", "ASSIGN_DESTINATION"));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, groupNameToPost));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE, String.valueOf(latLng.latitude)));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE, String.valueOf(latLng.longitude)));
					nameValuePairs.add(new BasicNameValuePair("destination", optionsToPost.getTitle()));
					HttpClient.postData(nameValuePairs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start(); 
	}



	public String getUserAccount() {
		return userAccount;
	}

	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

}
