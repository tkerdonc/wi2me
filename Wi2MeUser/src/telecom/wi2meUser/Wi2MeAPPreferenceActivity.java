/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 
 * The Network Preference screen of Wi2MeUser.
 * @author Xin CHEN
*/

package telecom.wi2meUser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.model.entities.User;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meUser.controller.ApplicationService;
import telecom.wi2meUser.controller.ApplicationService.ServiceBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;


public class Wi2MeAPPreferenceActivity extends Activity{

	private static final String ACCOUNT_FILE = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.COMMUNITY_ACCOUNTS_FILE;
	private static final String APPreference_File =ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.AP_GRADE_FILE;
	ListView lvListe;
	Activity currentActivity;
	Button btSave;
	Button btCancel;

	ServiceBinder binder;
	ServiceConnection serviceConnection;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		Log.d(getClass().getSimpleName(), "?? " + "Running onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_ap);
		currentActivity = this;

		lvListe = (ListView) findViewById(R.id.listviewAP);
		btSave = (Button) findViewById(R.id.buttonSave);
		btCancel = (Button) findViewById(R.id.buttonCancel);


		//Verify the USB storage
		File accountFile = new File(Environment.getExternalStorageDirectory() +ACCOUNT_FILE);
		File apGradeFile = new File(Environment.getExternalStorageDirectory() +APPreference_File);
		if(!accountFile.exists() || !apGradeFile.exists()){
			Toast.makeText(currentActivity, "ERROR LOADING ACCOUNT ou APGRADE FILE. Please, check ensure USB storage is off.", Toast.LENGTH_LONG).show();
		}

		serviceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(getClass().getSimpleName(), "?? " + "Bind connection");

				binder = (ServiceBinder) service;
				if (binder.loadingError){
					Log.d(getClass().getSimpleName(), "!! " + "Error Binding connection");
					finish();
				}else{

					try {
						loadPreferredNetwork();

					} catch (IOException e) {
						e.printStackTrace();
					}  

					setButtons();

				}
			}
			public void onServiceDisconnected(ComponentName name){}
		};

		getApplicationContext().bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);


	}

	public void setButtons() {


		btSave.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// get the grades and the ssid
				HashMap<String,Integer> getAPGrade = new HashMap<String,Integer>();
				APPrefListAdapter adapter = (APPrefListAdapter)lvListe.getAdapter();
				for(HashMap<String,String> np : adapter.getPrefs())
				{
					String ssid = np.get("SSID");
					String grade = np.get("Grade");

					if ((ssid != null) && (grade != null))
					{
						getAPGrade.put(ssid, Integer.parseInt(grade));
					}
				}

				// modify the AP_GRADE_MAP parameter
				binder.parameters.setParameter(Parameter.AP_GRADE_MAP, getAPGrade);

				// delete all the content of the file
				try {
					FileOutputStream apGradeOutPut = new FileOutputStream (Environment.getExternalStorageDirectory() + APPreference_File);
					OutputStreamWriter apGradeOutPutWriter = new OutputStreamWriter(apGradeOutPut);
					apGradeOutPutWriter.write("");
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//rewrite the file
				for (String ssid : getAPGrade.keySet()){
					try {
						FileWriter filewriter = new FileWriter(Environment.getExternalStorageDirectory() + APPreference_File,true);
						BufferedWriter out = new BufferedWriter(filewriter);
						out.write(ssid+"="+getAPGrade.get(ssid).toString()+"\n");
						out.close();
						filewriter.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}


				finish();

			}
		});

		btCancel.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View arg0) 
			{
				finish();
			}

		});
	}

	/** Function called to load the grades of all the networks: saved personal networks; saved community networks; scanned networks*/
	public void loadPreferredNetwork() throws IOException{

		ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();
		if(!ControllerServices.getInstance().getWifi().isInterfaceEnabled()){
			(ControllerServices.getInstance().getWifi()).enableWiFi();
			try {
				while(!ControllerServices.getInstance().getWifi().isInterfaceEnabled()){
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Object apGradeMapObj = binder.parameters.getParameter(Parameter.AP_GRADE_MAP);
		HashMap<String,Integer> apGradeMap = (HashMap<String,Integer>) apGradeMapObj;
		HashMap<String,Integer> apGradeMapCopy = (HashMap<String,Integer>) apGradeMap.clone();// To evade concurrent modification
		ArrayList<HashMap<String, String>> networkPreferenceList = new ArrayList<HashMap<String, String>>();
		HashMap<String,String> networkPreference;

		for (String network : apGradeMapCopy.keySet()){
			if(!network.equals("NotNull")){
				boolean nouveauAP=true;
				if(network.length()>7){
					if(network.substring(network.length()-7, network.length()).equals("_OLD***")){
						/* This case means the user previously asked to connect manually to one network
						 * In this case, we display the previous grade given by the user before asking to force a connection.
						 */
						if(apGradeMap.get(network.substring(0, network.length()-7))==null){
							//The grade has been removed. We don't put the previous saved value.
							nouveauAP=false;
						}else if(apGradeMap.get(network.substring(0, network.length()-7))!=10){
							//If the grade of the AP is not 10, it means it has been changed after the manual connection. We don't put the previous saved value.
							nouveauAP=false;
						}else{
							apGradeMap.put(network.substring(0, network.length()-7), apGradeMap.get(network));
							network=network.substring(0, network.length()-7);

							ArrayList<HashMap<String, String>> networkPreferenceListCopy =(ArrayList<HashMap<String, String>>) networkPreferenceList.clone();
							for(HashMap<String,String> np : networkPreferenceListCopy){
								if(np.get("SSID").equals(network)){
									networkPreferenceList.remove(np);
								}
							}
						}
					}
				}
				if(network.length()>6){
					if(network.substring(network.length()-6, network.length()).equals("_OLD**")){
						//This case should never happen.
						if(apGradeMap.get(network.substring(0, network.length()-6))==null){
							//The grade has been removed. We don't put the previous saved value.
							nouveauAP=false;
						}else if(apGradeMap.get(network.substring(0, network.length()-6))!=10){
							//If the grade of the AP is not 10, it means it has been changed after the manual connection. We don't put the previous saved value.
							nouveauAP=false;
						}else{
							apGradeMap.put(network.substring(0, network.length()-6), apGradeMap.get(network));
							network=network.substring(0, network.length()-6);

							ArrayList<HashMap<String, String>> networkPreferenceListCopy =(ArrayList<HashMap<String, String>>) networkPreferenceList.clone();
							for(HashMap<String,String> np : networkPreferenceListCopy){
								if(np.get("SSID").equals(network)){
									networkPreferenceList.remove(np);
								}
							}
						}
					}
				}
				for(HashMap<String,String> np : networkPreferenceList){
					if(np.get("SSID").equals(network)){
						nouveauAP=false;
						break;
					}
				}
				if(nouveauAP==true){
					networkPreference = new HashMap<String, String>();
					networkPreference.put("SSID",network);
					networkPreference.put("Grade", String.valueOf(apGradeMap.get(network)));
					networkPreferenceList.add(networkPreference);
				}
			}
		}


		List<WifiConfiguration> knownNetworks = ControllerServices.getInstance().getWifi().getKnownNetworks();
		for (WifiConfiguration ap : knownNetworks){
			boolean nouveauAP = true;
			for(HashMap<String,String> np : networkPreferenceList){
				if(np.get("SSID").equals(ap.SSID.replaceAll("\"","").toString())){
					nouveauAP=false;
				}
			}
			if(nouveauAP){
				networkPreference = new HashMap<String, String>();
				networkPreference.put("SSID",ap.SSID.replaceAll("\"", ""));
				networkPreference.put("Grade", "0");
				networkPreferenceList.add(networkPreference);
			}

		}

		List<String> communityNetworks=new ArrayList<String>();
		communityNetworks.add("NOTNULL");
		ArrayList<User> users = (ArrayList<User>) binder.parameters.getParameter(Parameter.COMMUNITY_NETWORK_USERS);
		for(User user : users){
			communityNetworks.add(communityService.getNameInApplication(user.getCommunityNetwork()));
		}

		for(String cn : communityNetworks){
			if(!cn.equals("NOTNULL")){
				boolean nouveauAP = true;
				for(HashMap<String,String> np : networkPreferenceList){
					if( np.get("SSID").equals(cn)){
						nouveauAP=false;
					}
				}
				if(nouveauAP){
					networkPreference = new HashMap<String, String>();
					networkPreference.put("SSID",cn);
					networkPreference.put("Grade", "0");
					networkPreferenceList.add(networkPreference);
				}
			}

		}
		Collections.sort(networkPreferenceList, new APComparator());
		APPrefListAdapter adapter = new APPrefListAdapter(networkPreferenceList,currentActivity);
		lvListe.setAdapter(adapter);




	}
	private class APPrefListAdapter extends BaseAdapter{
		private ArrayList<HashMap<String, String>> netPrefs;
		private LayoutInflater mInflater;
		public APPrefListAdapter(ArrayList<HashMap<String, String>> list, Context context){
			netPrefs = list;
			mInflater = LayoutInflater.from(context);
		}

		public ArrayList<HashMap<String, String>> getPrefs()
		{
			return netPrefs;
		}

		@Override
		public int getCount() {
			return netPrefs.size();
		}
		@Override
		public Object getItem(int position) {
			return netPrefs.get(position);
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			final int pos = position;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.preference_ap_row, parent);
				holder = new ViewHolder();
				holder.v = (TextView) convertView.findViewById(R.id.SSID_cell);
				holder.rating = (RatingBar)convertView.findViewById(R.id.rating_cell);

				holder.rating.setOnTouchListener(new OnTouchListener()
				{
			        	@Override
				        public boolean onTouch(View v, MotionEvent event)
					{
			                	if (event.getAction() == MotionEvent.ACTION_UP)
						{
							float touchPositionX = event.getX();
							float width = holder.rating.getWidth();
							float starsf = (touchPositionX / width) * 5.0f;
 							int stars = (int)starsf + 1;
							holder.rating.setRating(stars);

							Log.d(getClass().getSimpleName(), "?? " + "Setting rating for " + holder.v.getText());
							for(HashMap<String,String> np : netPrefs)
							{
								if(np.get("SSID").equals(holder.v.getText()))
								{
									np.put("Grade", "" + stars);
								}
			}

							v.setPressed(false);
				                }
			        	        if (event.getAction() == MotionEvent.ACTION_DOWN)
						{
							v.setPressed(true);
				                }

						if (event.getAction() == MotionEvent.ACTION_CANCEL)
						{
							v.setPressed(false);
			                	}

				                return true;
					}
				});
				convertView.setTag(holder);
			}
			else 
			{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.v.setText((String) netPrefs.get(position).get("SSID"));
			holder.rating.setRating(Integer.parseInt(netPrefs.get(position).get("Grade")));
			addListener(convertView,position);
			return convertView;
		}
		class ViewHolder {
			TextView v;
			RatingBar rating;
		}

		public void addListener(final View itemView,final int position) {  

			TextView SSID_tv = (TextView)(itemView.findViewById(R.id.SSID_cell));
			final String SSID_get = SSID_tv.getText().toString();

			itemView.setOnLongClickListener(  
					new View.OnLongClickListener() {  
						@Override  
						public boolean onLongClick(View v) {
							final CharSequence[] items = {"Connect Now!", "Remove Rate"};

							AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
							builder.setTitle("Select an action for "+SSID_get);
							builder.setItems(items, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									switch(item){
									case 0:
										//Case Connect Now!
										HashMap<String,Integer> apGradeMap = (HashMap<String,Integer>) binder.parameters.getParameter(Parameter.AP_GRADE_MAP);
										if(apGradeMap.containsKey(SSID_get)){
											int oldGrade = apGradeMap.get(SSID_get);
											apGradeMap.put(SSID_get+"_OLD**", oldGrade); //To be able to get the previous grade afterwards.
										}else{
											apGradeMap.put(SSID_get+"_OLD**", 0);
										}
										apGradeMap.put(SSID_get, 10);
										Log.e(getClass().getSimpleName(),"++ "+apGradeMap.toString());
										binder.parameters.setParameter(Parameter.AP_GRADE_MAP, apGradeMap);
										//If service is already running, we force a disconnection. Else start the service.
										if(binder.isRunning()){
											ControllerServices.getInstance().getWifi().cleanNetworks();
										}else{
											binder.start();
										}
										finish();
										break;
									case 1:
										//Case Remove Rate
										AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
										builder.setTitle("Removal");
										builder.setMessage("Are you sure to delete the rate for this AP?");
										builder.setPositiveButton("Yes", new OnClickListener(){
											public void onClick(DialogInterface dialog, int itemId) {
												try {
													String line;
													StringBuffer sb = new StringBuffer();
													FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + APPreference_File);
													BufferedReader reader = new BufferedReader(new InputStreamReader(
															fis));
													while ((line = reader.readLine()) != null) {
														if (!line.contains(SSID_get)) {
															sb.append(line + "\n");
														}
													}
													reader.close();
													BufferedWriter out = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory() + APPreference_File));
													out.write(sb.toString());
													out.close();
												} catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}

												netPrefs.get(position).put("Grade", "0");
												APPrefListAdapter adapter = new APPrefListAdapter(netPrefs,currentActivity);
												lvListe.setAdapter(adapter);

												HashMap<String,Integer> apgrade = new HashMap<String,Integer>();
												apgrade.put("NotNull", 0);
												for (HashMap<String,String> item : netPrefs){
													apgrade.put(item.get("SSID"), Integer.parseInt(item.get("Grade")));
												}
												binder.parameters.setParameter(Parameter.AP_GRADE_MAP, apgrade);
											}


										});
										builder.setNegativeButton("Cancel", new OnClickListener(){
											public void onClick(DialogInterface dialog, int itemId) {

											}
										});
										builder.show();
										break;
									}
								}
							});
							builder.show();
							return false;
						}  
					});  
		}  
	}

	@Override
	public void onPause(){


		Log.d(getClass().getSimpleName(), "?? " + "Running onPause");
		super.onPause();
		if (serviceConnection != null){
			getApplicationContext().unbindService(serviceConnection);
			serviceConnection = null;
		}
	}

	/** Function called when Back button pressed*/

	@Override 
	public void onBackPressed(){

		Log.d(getClass().getSimpleName(), "?? " + "Running onBackPressed");
		boolean changed = false;
		boolean newAP = false;


		HashMap<String,Integer> getAPGrade = new HashMap<String,Integer>();
		APPrefListAdapter adapter = (APPrefListAdapter)lvListe.getAdapter();
		for(HashMap<String,String> np : adapter.getPrefs())
		{

			String ssid = np.get("SSID");
			String grade = np.get("Grade");

			if ((ssid != null) && (grade != null))
			{
				getAPGrade.put(ssid, Integer.parseInt(grade));
			}
		}

		HashMap<String,Integer> savedAPGrade = new HashMap<String,Integer>();
		savedAPGrade = (HashMap<String,Integer>)binder.parameters.getParameter(Parameter.AP_GRADE_MAP);
		for (String ap : savedAPGrade.keySet()){
			if(!ap.equals("NotNull") && getAPGrade.containsKey(ap) && getAPGrade.get(ap)!=savedAPGrade.get(ap)){
				changed = true;
			}
		}

		for (String ap : getAPGrade.keySet()){
			if(!savedAPGrade.containsKey(ap) && getAPGrade.get(ap)!=0){
				newAP = true;
			}
		}

		if (changed || newAP){
			AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
			builder.setTitle("Alert");
			builder.setMessage("You have changed the rates of some Access Points. Please save before quit this page!");
			builder.setPositiveButton("OK", new OnClickListener(){
				public void onClick(DialogInterface dialog, int itemId) {

				}
			});
			builder.setNegativeButton("Ignore", new OnClickListener(){
				public void onClick(DialogInterface dialog, int itemId) {

					finish();
				}
			});
			builder.show();
		}
		else{
			super.onBackPressed();
		}

	}

	/**
	 * This class is used to compare AP grades and sort the APs in the list.
	 * @author Gilles Vidal
	 *
	 */
	private class APComparator implements Comparator<HashMap<String,String>>{

		public APComparator(){
		}

		/**
		 * Compares an AP to another:
		 * if AP1 grade > AP2 grade, AP1>AP2
		 * if AP1 grade=AP2grade, sort alphabetically.
		 */
		public int compare(HashMap<String,String> ap1, HashMap<String,String> ap2){
			int ap1Grade = Integer.parseInt(ap1.get("Grade"));
			int ap2Grade = Integer.parseInt(ap2.get("Grade"));
			int result = ap2Grade-ap1Grade;
			if(result!=0){
				return ap2Grade-ap1Grade;
			}else {
				return ap1.get("SSID").compareToIgnoreCase(ap2.get("SSID"));
			}
		}
	}
}
