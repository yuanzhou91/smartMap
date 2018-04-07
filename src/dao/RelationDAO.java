package dao;

import http.HttpClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import models.Group;
import models.GroupUserRelation;
import util.MySQLiteHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class RelationDAO {

	private MySQLiteHelper dbHelper;

	public RelationDAO(MySQLiteHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	public List<Group> findGroupsByUserAccount(String userAccount) {
		List<Group> groupList = new ArrayList<Group>();
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		Cursor cursor = database.query(MySQLiteHelper.TABLE_RELATION,
				new String[] {MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME,MySQLiteHelper.TABLE_RELATION_COLUMN_USERACCOUNT},
				MySQLiteHelper.TABLE_RELATION_COLUMN_USERACCOUNT +"= '"+userAccount+"'", null, null, null, null);
		cursor.moveToFirst();
		GroupDAO groupDAO = new GroupDAO(dbHelper);
		while (!cursor.isAfterLast()) {
			String groupName = cursor.getString(0);
			Group group = groupDAO.getGroupByName(groupName);
			groupList.add(group);
			cursor.moveToNext();
		}
		// make sure to close the cursor
		cursor.close();
		//database.close();
		return groupList;
	}

	public void createRelation(GroupUserRelation relation, boolean update){
		SQLiteDatabase database = dbHelper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME, relation.getGroupName()); // Group Name
		values.put(MySQLiteHelper.TABLE_RELATION_COLUMN_USERACCOUNT,relation.getUserAccount()); // 
		// Inserting Row
		database.replace(MySQLiteHelper.TABLE_RELATION, null, values);
		final GroupUserRelation relationToPost = relation;
		//Send to server
		if(update){
			Thread thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
						nameValuePairs.add(new BasicNameValuePair("method", "CREATE_RELATION"));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, relationToPost.getUserAccount()));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME, relationToPost.getGroupName()));
						HttpClient.postData(nameValuePairs);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			thread.start(); 
		}
		//database.close();
	}
	
	public void deleteRelation(String groupName, String userAccount, boolean update){
		SQLiteDatabase database = dbHelper.getWritableDatabase();

		database.delete(MySQLiteHelper.TABLE_RELATION, MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME
				+ " = '" + groupName+"' AND "+MySQLiteHelper.TABLE_RELATION_COLUMN_USERACCOUNT +" ='"+userAccount+"'" , null);
		//Send to server
		if(update){
			final String userAccountStr = userAccount;
			final String groupNameStr = groupName;
			Thread thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
						nameValuePairs.add(new BasicNameValuePair("method", "DELETE_RELATION"));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, userAccountStr));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME, groupNameStr));
						HttpClient.postData(nameValuePairs);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			thread.start(); 
		}
		//database.close();
	}

	public HashSet<String> getUserIdsByGroup(String groupName) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();

		HashSet<String> userIdSet = new HashSet<String>();
		Cursor cursor = database.query(MySQLiteHelper.TABLE_RELATION,
				new String[] {MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME,MySQLiteHelper.TABLE_RELATION_COLUMN_USERACCOUNT},
				MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME +"= '"+groupName+"'", null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String userId = cursor.getString(1);
			userIdSet.add(userId);
			cursor.moveToNext();
		}
		cursor.close();
		//database.close();
		return userIdSet;
	}
}
