package com.example.newapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import models.User;

import org.json.JSONObject;

import util.DirectionsJSONParser;
import util.GeocodeJSONParser;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import dao.DBUpdater;

public class SmartMapFragment extends MapFragment {
	GoogleMap map;
	SupportMapFragment mMapFragment;
	LatLng curLatLng;
	String lastGroupName = null;
	LatLng targetLatLng;
	String closestTargetName = null;
	MarkerOptions markerOptions = null;
	MarkerOptions receivedMarkerOptions = null;
	Thread syncThread;
	View view;
	String address;
	List<User> memberAddresses;
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public SmartMapFragment(int sectionNumber) {
		SmartMapFragment fragment = new SmartMapFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
	}

	public SmartMapFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(lastGroupName == null || !lastGroupName.equals(((SmartMapApplication)getActivity().getApplication()).groupName))
			(new GetAddressTask()).execute();
	}
	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		View v = super.onCreateView(arg0, arg1, arg2);
		initMap();
		SearchView editText = new SearchView(getActivity());
		editText.setBackgroundColor(Color.LTGRAY);
		editText.setOnQueryTextListener(new SearchView.OnQueryTextListener( ) {
			@Override
			public boolean onQueryTextChange( String newText ) {
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				search(query);
				return true;
			}
		});
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				150);
		((ViewGroup)v).addView(editText, params);

		view = v;
		//getActivity().runOnUiThread(new AutoSyncThread());
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if(((SmartMapApplication)getActivity().getApplication()).getUser() != null){
						DBUpdater.postUserLocation(((SmartMapApplication)getActivity().getApplication()).getUser().getId(), 
								((SmartMapApplication)getActivity().getApplication()).getUser().getUsername(), 
								curLatLng);
						(new GetAddressTask()).execute();
						try {
							Thread.sleep(15000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
					}

				}
			}
		});
		thread.start();

		return v;
	}

	private void initMap(){
		UiSettings settings = getMap().getUiSettings();
		settings.setAllGesturesEnabled(true);
		settings.setMyLocationButtonEnabled(true);
		map = getMap();
		if(map != null) {
			LocationManager locationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);
			MapsInitializer.initialize(getActivity());
			// Creating a criteria object to retrieve provider
			Criteria criteria = new Criteria();
			// Getting the name of the best provider
			String provider = locationManager.getBestProvider(criteria, true);
			// Getting Current Location
			Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			// Getting latitude of the current location
			double latitude = location.getLatitude();
			// Getting longitude of the current location
			double longitude = location.getLongitude();
			// Creating a LatLng object for the current location
			LatLng latLng = new LatLng(latitude, longitude);
			//set current to the 
			curLatLng = latLng;
			//move camera and add marker
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
			//get address
			String address = getLocationAddress(latitude, longitude);
			map.addMarker(new MarkerOptions().position(latLng).title("My Location").snippet(address).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))).showInfoWindow();
		}
		if(((SmartMapApplication)getActivity().getApplication()).groupName == null){
			Intent i = new Intent(getActivity().getApplicationContext(), ChooseMapActivity.class);
			startActivityForResult(i, 1);
		}
	}

	private String getLocationAddress(double latitude, double longitude) {
		Geocoder gc = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
		List<Address> addresses = null;
		try {
			//get all address
			addresses = gc.getFromLocation(latitude, longitude, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (addresses != null && addresses.size() > 0) {
			//get the first address
			Address address = addresses.get(0);
			String addressText = String.format("%s, %s, %s", address.getAddressLine(0), address.getLocality(), address.getCountryName());
			return addressText;
		} else {
			return "Cannot find current address.";
		}
	}

	private void search(String location) {
		if(location==null || location.equals("")){
			Toast.makeText(getActivity().getBaseContext(), "No Place is entered", Toast.LENGTH_SHORT).show();
			return;
		}

		String url = "https://maps.googleapis.com/maps/api/geocode/json?";

		try {
			// encoding special characters like space in the user input place
			location = URLEncoder.encode(location, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String address = "address=" + location;

		String sensor = "sensor=false";

		// url , from where the geocoding data is fetched
		url = url + address + "&" + sensor;

		// Instantiating DownloadTask to get places from Google Geocoding service
		// in a non-ui thread
		DownloadTaskSearch downloadTask = new DownloadTaskSearch();

		// Start downloading the geocoding places
		downloadTask.execute(url);
	}

	private String downloadUrlSearch(String strUrl) throws IOException{
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try{
			URL url = new URL(strUrl);
			// Creating an http connection to communicate with url
			urlConnection = (HttpURLConnection) url.openConnection();

			// Connecting to url
			urlConnection.connect();

			// Reading data from url
			iStream = urlConnection.getInputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

			StringBuffer sb = new StringBuffer();

			String line = "";
			while( ( line = br.readLine()) != null){
				sb.append(line);
			}

			data = sb.toString();
			br.close();

		}catch(Exception e){
			Log.d("Exception while downloading url", e.toString());
		}finally{
			iStream.close();
			urlConnection.disconnect();
		}

		return data;
	}
	/** A class, to download Places from Geocoding webservice */
	private class DownloadTaskSearch extends AsyncTask<String, Integer, String>{

		String data = null;

		// Invoked by execute() method of this object
		@Override
		protected String doInBackground(String... url) {
			try{
				data = downloadUrlSearch(url[0]);
			}catch(Exception e){
				Log.d("Background Task",e.toString());
			}
			return data;
		}

		// Executed after the complete execution of doInBackground() method
		@Override
		protected void onPostExecute(String result){

			// Instantiating ParserTask which parses the json data from Geocoding webservice
			// in a non-ui thread
			ParserTaskSearch parserTask = new ParserTaskSearch();

			// Start parsing the places in JSON format
			// Invokes the "doInBackground()" method of the class ParseTask
			parserTask.execute(result);
		}
	}

	/** A class to parse the Geocoding Places in non-ui thread */
	class ParserTaskSearch extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

		JSONObject jObject;

		// Invoked by execute() method of this object
		@Override
		protected List<HashMap<String,String>> doInBackground(String... jsonData) {

			List<HashMap<String, String>> places = null;
			GeocodeJSONParser parser = new GeocodeJSONParser();

			try{
				jObject = new JSONObject(jsonData[0]);

				/** Getting the parsed data as a an ArrayList */
				places = parser.parse(jObject);

			}catch(Exception e){
				Log.d("Exception",e.toString());
			}
			return places;
		}

		// Executed after the complete execution of doInBackground() method
		@Override
		protected void onPostExecute(List<HashMap<String,String>> list){

			// Clears all the existing markers
			//map.clear();

			LatLng closestTarget = null;
			closestTargetName = null;
			double minDist = Double.MAX_VALUE;

			for(int i=0;i<list.size();i++){

				// Getting a place from the places list
				HashMap<String, String> hmPlace = list.get(i);

				// Getting latitude of the place
				double lat = Double.parseDouble(hmPlace.get("lat"));

				// Getting longitude of the place
				double lng = Double.parseDouble(hmPlace.get("lng"));

				// Getting name
				String name = hmPlace.get("formatted_address");

				LatLng latLng = new LatLng(lat, lng);
				if(distance(curLatLng, latLng) < minDist) {
					minDist = distance(curLatLng, latLng);
					closestTarget = latLng;
					closestTargetName = name;
				}
			}

			if(closestTarget != null) {
				//target the closet one
				// Creating a marker
				markerOptions = new MarkerOptions();

				// Setting the position for the marker
				markerOptions.position(closestTarget);
				// Setting the title for the marker
				markerOptions.title(closestTargetName);
				// Placing a marker on the touched position
				map.addMarker(markerOptions).showInfoWindow();
				//set the function when user click the marker
				map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
					@Override
					public void onInfoWindowClick(Marker marker) {
						LatLng markerLocation = marker.getPosition();
						if(markerLocation.latitude != curLatLng.latitude ||
								markerLocation.longitude != curLatLng.longitude) {
							// Getting URL to the Google Directions API
							targetLatLng = new LatLng(markerLocation.latitude, markerLocation.longitude);
							new AlertDialog.Builder(getActivity())
							.setTitle("Set as target")
							.setMessage("Set this location as target:\n" + marker.getTitle())
							.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
									new Thread(new Runnable(){
										@Override
										public void run() {
											if(targetLatLng == null) {
												return;
											}
											if(((SmartMapApplication)getActivity().getApplication()).groupName != null){
												DBUpdater.postTargetMarkerOptions(((SmartMapApplication)getActivity().getApplication()).groupName, markerOptions);
											}										
											if(targetLatLng !=null && curLatLng !=null){
												String url = getDirectionsUrl(
														new LatLng(curLatLng.latitude, 
																curLatLng.longitude), 
																new LatLng(targetLatLng.latitude, 
																		targetLatLng.longitude));
												DownloadTaskDraw downloadTask = new DownloadTaskDraw();
												downloadTask.execute(url);
											}
											(new GetAddressTask()).execute();
										}
									}).start();
								}
							})
							.setNegativeButton("No",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
									//empty
								}
							}).show();
						}
					}
				});
				map.animateCamera(CameraUpdateFactory.newLatLng(closestTarget));
				// add marker of the cur location
				String myAddress = getLocationAddress(curLatLng.latitude, curLatLng.longitude);
				map.addMarker(new MarkerOptions().position(curLatLng).title("My Location").snippet(myAddress).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
			} else {
				//show a message that the location cannot be found
				Toast.makeText(getActivity(), "Sorry, but we cannot find this address.", Toast.LENGTH_LONG).show();
			}
		}
	}

	private String getDirectionsUrl(LatLng origin,LatLng dest){

		// Origin of route
		String str_origin = "origin="+origin.latitude+","+origin.longitude;

		// Destination of route
		String str_dest = "destination="+dest.latitude+","+dest.longitude;

		// Sensor enabled
		String sensor = "sensor=false";

		// Building the parameters to the web service
		String parameters = str_origin+"&"+str_dest+"&"+sensor;

		// Output format
		String output = "json";

		// Building the url to the web service
		String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

		return url;
	}

	/** A method to download json data from url */
	private String downloadUrlDraw(String strUrl) throws IOException{
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try{
			URL url = new URL(strUrl);

			// Creating an http connection to communicate with url
			urlConnection = (HttpURLConnection) url.openConnection();

			// Connecting to url
			urlConnection.connect();

			// Reading data from url
			iStream = urlConnection.getInputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

			StringBuffer sb = new StringBuffer();

			String line = "";
			while( ( line = br.readLine()) != null){
				sb.append(line);
			}

			data = sb.toString();

			br.close();

		}catch(Exception e){
			Log.d("Exception while downloading url", e.toString());
		}finally{
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}

	// Fetches data from url passed
	private class DownloadTaskDraw extends AsyncTask<String, Void, String>{

		// Downloading data in non-ui thread
		@Override
		protected String doInBackground(String... url) {

			// For storing data from web service
			String data = "";

			try{
				// Fetching the data from web service
				data = downloadUrlDraw(url[0]);
			}catch(Exception e){
				Log.d("Background Task",e.toString());
			}
			return data;
		}

		// Executes in UI thread, after the execution of
		// doInBackground()
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			ParserTaskDraw parserTask = new ParserTaskDraw();

			// Invokes the thread for parsing the JSON data
			parserTask.execute(result);
		}
	}

	/** A class to parse the Google Places in JSON format */
	private class ParserTaskDraw extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

		// Parsing the data in non-ui thread
		@Override
		protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

			JSONObject jObject;
			List<List<HashMap<String, String>>> routes = null;

			try{
				jObject = new JSONObject(jsonData[0]);
				DirectionsJSONParser parser = new DirectionsJSONParser();

				// Starts parsing data
				routes = parser.parse(jObject);
			}catch(Exception e){
				e.printStackTrace();
			}
			return routes;
		}

		// Executes in UI thread, after the parsing process
		@Override
		protected void onPostExecute(List<List<HashMap<String, String>>> result) {
			ArrayList<LatLng> points = null;
			PolylineOptions lineOptions = null;
			markerOptions = new MarkerOptions();

			// Traversing through all the routes
			for(int i=0;i<result.size();i++){
				points = new ArrayList<LatLng>();
				lineOptions = new PolylineOptions();

				// Fetching i-th route
				List<HashMap<String, String>> path = result.get(i);

				// Fetching all the points in i-th route
				for(int j=0;j<path.size();j++){
					HashMap<String,String> point = path.get(j);

					double lat = Double.parseDouble(point.get("lat"));
					double lng = Double.parseDouble(point.get("lng"));
					LatLng position = new LatLng(lat, lng);

					points.add(position);
				}

				// Adding all the points in the route to LineOptions
				lineOptions.addAll(points);
				lineOptions.width(15);
				lineOptions.color(Color.rgb(28, 191, 255));
			}

			// Drawing polyline in the Google Map for the i-th route
			map.addPolyline(lineOptions);
		}
	}

	private double distance(LatLng l1, LatLng l2) {
		return Math.sqrt((l1.latitude - l2.latitude) * (l1.latitude - l2.latitude)
				+(l1.longitude - l2.longitude) * (l1.longitude - l2.longitude));
	}

	private class GetAddressTask extends
	AsyncTask<Location, Void, String> {

		/**
		 * A method that's called once doInBackground() completes. Turn
		 * off the indeterminate activity indicator and set
		 * the text of the UI element that shows the address. If the
		 * lookup failed, display the error message.
		 */
		@Override
		protected void onPostExecute(String saddress) {
			// Set activity indicator visibility to "gone"
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					map.clear();
					//map.moveCamera(CameraUpdateFactory.newLatLngZoom(curLatLng, 17));
					String myAccount = ((SmartMapApplication)getActivity().getApplication()).getUser().getId();
					if(memberAddresses!=null){
						for (int i = 0; i < memberAddresses.size(); i++) {
							User user = memberAddresses.get(i);
							if(!user.getFacebookAccount().equals(myAccount)){
								LatLng nLatLng = new LatLng(user.getLatitude(), user.getLongitude());
								String labelName = user.getUsername();
								map.addMarker(new MarkerOptions().
										position(nLatLng).
										title(labelName).
										icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
										).showInfoWindow();
							}
						}
					}
					map.addMarker(new MarkerOptions().position(curLatLng).title("My Location").snippet(address).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))).showInfoWindow();

					if(targetLatLng != null){
						String url = getDirectionsUrl(
								new LatLng(curLatLng.latitude, 
										curLatLng.longitude), 
										new LatLng(targetLatLng.latitude, 
												targetLatLng.longitude));
						DownloadTaskDraw downloadTask = new DownloadTaskDraw();
						downloadTask.execute(url);
						markerOptions = new MarkerOptions().title(closestTargetName).position(targetLatLng);
						map.addMarker(markerOptions).showInfoWindow();
					}
				}
			});
		}

		/**
		 * Get a Geocoder instance, get the latitude and longitude
		 * look up the address, and return it
		 *
		 * @params params One or more Location objects
		 * @return A string containing the address of the current
		 * location, or an empty string if no address can be found,
		 * or an error message
		 */

		@Override
		protected String doInBackground(Location... params) {
			LocationManager locationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);
			Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			// Getting latitude of the current location
			double latitude = location.getLatitude();
			// Getting longitude of the current location
			double longitude = location.getLongitude();
			// Creating a LatLng object for the current location
			LatLng latLng = new LatLng(latitude, longitude);
			//set current to the 
			curLatLng = latLng;
			//move camera and add marker
			//get address
			address = getLocationAddress(latitude, longitude);
			//map.addMarker(new MarkerOptions().position(curLatLng).title("My Location").snippet(address).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))).showInfoWindow();

			//TODO : ADD refreshing Target.
			if(((SmartMapApplication)getActivity().getApplication()).groupName!= null){
				memberAddresses = DBUpdater.getRemoteUsersLocationsByGroupName(((SmartMapApplication)getActivity().getApplication()).groupName);
				MarkerOptions m = DBUpdater.getTargetMarkerOptions(((SmartMapApplication)getActivity().getApplication()).groupName);
				if(m == null || m.getPosition()==null){
					targetLatLng = null;
					closestTargetName = null;
				}else {
					targetLatLng = new LatLng(m.getPosition().latitude, m.getPosition().longitude);
					closestTargetName = m.getTitle();
				}
			}
			return null;
		}
	}

}
