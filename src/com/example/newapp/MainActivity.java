package com.example.newapp;

import util.DBHelper;
import util.MySQLiteHelper;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;

import dao.DBUpdater;
import dao.UserDAO;

public class MainActivity extends FragmentActivity {
	MySQLiteHelper sqlLiteHelper;
	DBUpdater updater;
	Thread thread;
	private static final int SPLASH = 0;
	private static final int SETTINGS = 1;
	private static final int FRAGMENT_COUNT = SETTINGS +1;
	private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sqlLiteHelper = new MySQLiteHelper(this);
		SQLiteDatabase database = sqlLiteHelper.getWritableDatabase();
		sqlLiteHelper.onCreate(database);

		// Add the fragment on initial activity setup
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		FragmentManager fm = getSupportFragmentManager();
		fragments[SPLASH] = fm.findFragmentById(R.id.splashFragment);
		fragments[SETTINGS] = fm.findFragmentById(R.id.userSettingsFragment);
		FragmentTransaction transaction = fm.beginTransaction();
		for(int i = 0; i < fragments.length; i++) {
			transaction.hide(fragments[i]);
		}
		transaction.commit();


	}


	private void showFragment(int fragmentIndex, boolean addToBackStack) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		for (int i = 0; i < fragments.length; i++) {
			if (i == fragmentIndex) {
				transaction.show(fragments[i]);
			} else {
				transaction.hide(fragments[i]);
			}
		}
		if (addToBackStack) {
			transaction.addToBackStack(null);
		}
		transaction.commit();
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

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		// Only make changes if the activity is visible
		FragmentManager manager = getSupportFragmentManager();
		// Get the number of entries in the back stack
		int backStackSize = manager.getBackStackEntryCount();
		// Clear the back stack
		for (int i = 0; i < backStackSize; i++) {
			manager.popBackStack();
		}
		if (state.isOpened()) {
			// If the session state is open:
			Intent i = getIntent();
			// getting attached intent data
			int settings = i.getIntExtra("settings", 0);
			if(settings == 1){
				showFragment(SETTINGS, true);
				i.removeExtra("settings");
			}else {
				makeMeRequest(session);
			}

		} else if (state.isClosed()) {
			// If the session state is closed:
			// Show the login fragment

			showFragment(SPLASH, false);
		}

	}

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
		Session session = Session.getActiveSession();

		if (session != null && session.isOpened()) {

			Intent i = getIntent();
			// getting attached intent data
			int settings = i.getIntExtra("settings", 0);
			if(settings == 1){
				showFragment(SETTINGS, true);
				i.removeExtra("settings");
			}else {
				makeMeRequest(session);
			}

		} else {
			// otherwise present the splash screen
			// and ask the person to login.
			showFragment(SPLASH, false);
		}
	}
	private void makeMeRequest(final Session session) {
		if(((SmartMapApplication)getApplication()).getUser() != null){
			return ;
		}
		// Make an API call to get user data and define a 
		// new callback to handle the response.
		Request request = Request.newMeRequest(session, 
				new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser user, Response response) {
				// If the response is successful
				if (session == Session.getActiveSession()) {
					if (user != null) {
						((SmartMapApplication)getApplication()).setUser(user);
						UserDAO userDAO = new UserDAO(new DBHelper(getApplicationContext()));
						if(userDAO.getUserByAccount(user.getId()) == null){
							userDAO.createUser(user);
						}
						if(updater == null || updater.getUserAccount()!=user.getId()){
							updater = new DBUpdater(new MySQLiteHelper(MainActivity.this),user.getId());
							if(thread == null){
								thread = new Thread(updater);
							}else if (thread.isAlive()) {
								thread.interrupt();
								thread = new Thread(updater);
							}
						}else {
							if (thread == null || thread.isInterrupted()) {
								thread = new Thread(updater);
							}
						}
						thread.start();
						//						if(updater == null){
						//							updater = new DBUpdater(new MySQLiteHelper(MainActivity.this),user.getId());
						//							if(thread == null){
						//								thread = new Thread(updater);
						//							}
						//						}else if(updater.getUserAccount()!=user.getId()){
						//							updater.setUserAccount(user.getId());
						//						}
					}
					// If the session state is open:
					Intent intent = new Intent(getApplicationContext(), MajorTabsActivity.class);
					startActivity(intent);
				}
				if (response.getError() != null) {
					// Handle errors, will do so later.
				}
			}
		});
		request.executeAsync();
	}
}