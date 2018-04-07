package dao;


import http.HttpClient;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import models.User;
import util.MySQLiteHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.facebook.model.GraphUser;

public class UserDAO {
	private MySQLiteHelper dbHelper;

	public UserDAO(MySQLiteHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	public void createUser(GraphUser user) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, user.getId()); // Group Name
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME,user.getName()); // 
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE,"70.00"); 
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE,"70.00");
		// Inserting Row
		database.replace(MySQLiteHelper.TABLE_USER, null, values);


		final GraphUser usertoPost = user;
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("method", "CREATE_USER"));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, usertoPost.getId()));
					nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME, usertoPost.getName()));
					HttpClient.postData(nameValuePairs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		//database.close();
	}

	public void postUserLocation(String userAccount, String userName, double latitude, double longitude, boolean update) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, userAccount); // Group Name
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME,userName ); // 
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE,String.valueOf(latitude)); 
		values.put(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE,String.valueOf(longitude));
		// Inserting Row
		database.replace(MySQLiteHelper.TABLE_USER, null, values);


		final String facebookAccount = userAccount;
		final String userNameString = userName;
		final String laString = String.valueOf(latitude);
		final String loString = String.valueOf(longitude);

		if(update){
			Thread thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						List<NameValuePair> nameValuePairs  = new ArrayList<NameValuePair>(2);
						nameValuePairs.add(new BasicNameValuePair("method", "POST_LOCATION"));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT, facebookAccount));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_USERNAME, userNameString));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE, laString));
						nameValuePairs.add(new BasicNameValuePair(MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE, loString));
						HttpClient.postData(nameValuePairs);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();	
		}

		database.close();
	}

	public User getUserByAccount(String userId) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();

		// Select All Query
		Cursor cursor = database.query(MySQLiteHelper.TABLE_USER,
				new String[] {MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT,MySQLiteHelper.TABLE_USER_COLUMN_USERNAME,MySQLiteHelper.TABLE_USER_COLUMN_LATITUDE,MySQLiteHelper.TABLE_USER_COLUMN_LONGITUDE},
				MySQLiteHelper.TABLE_USER_COLUMN_FACEBOOKACCOUNT +"= '"+userId+"'", null, null, null, null);
		User user = new User();		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			user.setFacebookAccount(userId);
			user.setUsername(cursor.getString(1));
			if(!cursor.getString(2).equals("")){
				user.setLatitude(Double.parseDouble(cursor.getString(2)));
				user.setLongitude(Double.parseDouble(cursor.getString(3)));
			}

		}else {
			return null;
		}
		cursor.close();
		database.close();
		return user;
	}

}
