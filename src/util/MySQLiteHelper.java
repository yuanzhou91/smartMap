package util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
	private static MySQLiteHelper instance;

    public static synchronized MySQLiteHelper getInstance(Context context)
    {
        if (instance == null)
            instance = new MySQLiteHelper(context);
        return instance;
    }
    
	public static SQLiteDatabase database;
	private static final String DATABASE_NAME = "smartmap.db";
	private static final int DATABASE_VERSION = 1;

	//Informations for user table.
	public static final String TABLE_USER = "smartmap_user";
	public static final String TABLE_USER_COLUMN_USERNAME = "username";
	public static final String TABLE_USER_COLUMN_FACEBOOKACCOUNT = "facebookAccount";
	public static final String TABLE_USER_COLUMN_LATITUDE = "latitude";
	public static final String TABLE_USER_COLUMN_LONGITUDE = "longitude";
	private static final String TABLE_USER_CREATE = "create table IF NOT EXISTS "
			+ TABLE_USER + "(" + TABLE_USER_COLUMN_FACEBOOKACCOUNT
			+ " text primary key, " + TABLE_USER_COLUMN_USERNAME
			+ " text not null, "+TABLE_USER_COLUMN_LATITUDE
			+ " text, "+TABLE_USER_COLUMN_LONGITUDE
			+ " text);";

	//Informations for group table.
	public static final String TABLE_GROUP = "smartmap_group";
	public static final String TABLE_GROUP_COLUMN_ID = "id";
	public static final String TABLE_GROUP_COLUMN_GROUPNAME = "groupName";
	public static final String TABLE_GROUP_COLUMN_OWNERACCOUNT = "ownerAccount";
	public static final String TABLE_GROUP_COLUMN_THUMBNAIL = "thumbnail";
	private static final String TABLE_GROUP_CREATE = "create table IF NOT EXISTS "
			+ TABLE_GROUP + "(" + TABLE_GROUP_COLUMN_ID
			+ " integer primary key autoincrement, " + TABLE_GROUP_COLUMN_GROUPNAME
			+ " text not null,"+ TABLE_GROUP_COLUMN_OWNERACCOUNT
			+ " text not null,"+ TABLE_GROUP_COLUMN_THUMBNAIL
			+ " text not null);";
	//Informations for group table.
	public static final String TABLE_RELATION = "smartmap_group_user_relation";
	public static final String TABLE_RELATION_COLUMN_GROUPNAME = "groupName";
	public static final String TABLE_RELATION_COLUMN_USERACCOUNT = "userAccount";
	// + TABLE_RELATION_COLUMN_ID + " integer primary key autoincrement, " + 
	private static final String TABLE_RELATION_CREATE = "create table IF NOT EXISTS "
			+ TABLE_RELATION + "("+TABLE_RELATION_COLUMN_GROUPNAME
			+ " text not null,"+ TABLE_RELATION_COLUMN_USERACCOUNT
			+ " text not null, "
			+ " PRIMARY KEY ( "+TABLE_RELATION_COLUMN_GROUPNAME+", "+TABLE_RELATION_COLUMN_USERACCOUNT+"));";



	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		if(MySQLiteHelper.database == null){
			MySQLiteHelper.database = this.getWritableDatabase();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(TABLE_USER_CREATE);
		database.execSQL(TABLE_GROUP_CREATE); 
		database.execSQL(TABLE_RELATION_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP);
		onCreate(db);
	}

} 
