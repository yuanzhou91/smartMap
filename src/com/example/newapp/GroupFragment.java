package com.example.newapp;

import java.util.List;

import models.Group;
import util.DBHelper;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.model.GraphUser;

import dao.RelationDAO;

/**
 * A placeholder fragment containing a simple view.
 */
public class GroupFragment extends ListFragment {
	// ListView Adapter
	GroupListAdapter adapter;
	List<Group> groupList ;
	private static final String ARG_SECTION_NUMBER = "section_number";
	// Search EditText
	EditText inputSearch;
	Runnable listRefresherRunnable;

	/**
	 * Returns a new instance of this fragment for the given section
	 * number.
	 */
	public GroupFragment(int sectionNumber) {
		GroupFragment fragment = new GroupFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
	}

	public GroupFragment() {
	}
	@Override
	public void onActivityCreated (Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);

	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// storing string resources into Array

		RelationDAO relationDAO = new RelationDAO(new DBHelper(getActivity()));
		GraphUser user = ((SmartMapApplication)getActivity().getApplication()).getUser();
		if(user == null){
			return ;
		}
		groupList = relationDAO.findGroupsByUserAccount(user.getId());
		//groupNames = new ArrayList<String>(groupList.size());
		//		for (int i = 0; i < groupList.size(); i++) {
		//			groupNames.add(groupList.get(i).getGroupName());
		//		}
		//		//( groupNames)
		//		adapter = new ArrayAdapter<String>(getActivity(), R.layout.group_list_item, R.id.label, groupNames);


		adapter = new GroupListAdapter(getActivity(), android.R.id.list, groupList);

		// Binding resources Array to ListAdapter
		setListAdapter(adapter);
		listRefresherRunnable = new Runnable(){
			public void run(){
				//reload content
				adapter.clear();
				RelationDAO relationDAO = new RelationDAO(new DBHelper(getActivity()));
				groupList = relationDAO.findGroupsByUserAccount(((SmartMapApplication)getActivity().getApplication()).getUser().getId());
				adapter.addAll(groupList);
				adapter.notifyDataSetChanged();
			}
		};
		getActivity().runOnUiThread(listRefresherRunnable);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_group, container, false);

		inputSearch = (EditText)rootView.findViewById(R.id.input_search_group);
		inputSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				// When user changed the Text
				adapter.getFilter().filter(cs);   
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {

			}
			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		Button createGroupButton = (Button) rootView.findViewById(R.id.create_group);
		createGroupButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Launching new Activity on selecting single List Item
				Intent i = new Intent(getActivity().getApplicationContext(), CreateGroupActivity.class);
				// sending data to new activity
				startActivity(i);
			} 
		}); 
		getActivity().runOnUiThread(listRefresherRunnable);
		return rootView;
	}
	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		// selected item 
		String groupName = ((TextView) v.findViewById(R.id.label)).getText().toString();

		// Launching new Activity on selecting single List Item
		Intent i = new Intent(getActivity().getApplicationContext(), GroupInfoActivity.class);
		// sending data to new activity
		i.putExtra("group_name", groupName);
		startActivity(i);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().runOnUiThread(listRefresherRunnable);
	}

}