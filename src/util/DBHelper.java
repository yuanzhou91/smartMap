package util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper extends MySQLiteHelper  {
	public DBHelper(Context context) {
		super(context);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}
