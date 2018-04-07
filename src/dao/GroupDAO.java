package dao;



import http.HttpClient;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import models.Group;
import models.GroupUserRelation;
import util.MySQLiteHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class GroupDAO {
	private MySQLiteHelper dbHelper;

	public GroupDAO(MySQLiteHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	public void createGroup(Group group, boolean update) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, group.getGroupName()); // Group Name
		values.put(MySQLiteHelper.TABLE_GROUP_COLUMN_OWNERACCOUNT,group.getOwnerAccount()); // 
		values.put(MySQLiteHelper.TABLE_GROUP_COLUMN_THUMBNAIL, group.getGroupThumnail());
		// Inserting Row
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		database.replace(MySQLiteHelper.TABLE_GROUP, null, values);

		RelationDAO relationDAO = new RelationDAO(dbHelper);
		GroupUserRelation relation = new GroupUserRelation();
		relation.setGroupName(group.getGroupName());
		relation.setUserAccount(group.getOwnerAccount());
		relationDAO.createRelation(relation, update);

		if(update){
			final Group grouptoPost = group;
			Thread thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
						nameValuePairs.add(new BasicNameValuePair("method", "CREATE_GROUP"));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_OWNERACCOUNT, grouptoPost.getOwnerAccount()));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, grouptoPost.getGroupName()));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_THUMBNAIL, String.valueOf(grouptoPost.getGroupThumnail())));
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

	public Group getGroupByName(String groupName) {
		// Select All Query
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		Cursor cursor = database.query(MySQLiteHelper.TABLE_GROUP,
				new String[] {MySQLiteHelper.TABLE_GROUP_COLUMN_ID,MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME,MySQLiteHelper.TABLE_GROUP_COLUMN_OWNERACCOUNT,MySQLiteHelper.TABLE_GROUP_COLUMN_THUMBNAIL},
				MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME +"= '"+groupName+"'", null, null, null, null);
		Group group = new Group();
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			while(!cursor.isAfterLast()){
				if(groupName.equals(cursor.getString(1))){
					group.setId(Integer.parseInt(cursor.getString(0)));
					group.setGroupName(cursor.getString(1));
					group.setOwnerAccount(cursor.getString(2));
					// Adding Group to list
					group.setGroupThumnail(Integer.parseInt(cursor.getString(3)));
					break;
				}
				cursor.moveToNext();
			};
		}
		//database.close();
		// return Group list
		return group;
	}

	public void deleteGroupByName(String groupName, boolean update) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		Group group = getGroupByName(groupName);
		database.delete(MySQLiteHelper.TABLE_GROUP, MySQLiteHelper.TABLE_GROUP_COLUMN_ID
				+ " = " + group.getId(), null);

		database.delete(MySQLiteHelper.TABLE_RELATION, MySQLiteHelper.TABLE_RELATION_COLUMN_GROUPNAME
				+ " = '" + group.getGroupName()+"'", null);
		//database.close();

		if(update){
			final Group grouptoPost = group;
			Thread thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
						nameValuePairs.add(new BasicNameValuePair("method", "DELETE_GROUP"));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_GROUP_COLUMN_GROUPNAME, grouptoPost.getGroupName()));
						HttpClient.postData(nameValuePairs);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
		}
	}

}
