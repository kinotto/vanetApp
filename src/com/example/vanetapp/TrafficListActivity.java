package com.example.vanetapp;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.example.vanetapp.R;

import it.unibo.vanetapp.model.impl.StorageFactory;
import it.unibo.vanetapp.model.interfaces.IStorage;
import it.unibo.vanetapp.persistence.XMLFactory;
import it.unibo.vanetapp.utility.Constants;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class TrafficListActivity extends Activity {

	private IStorage storage;
	private ListView trafficList;
	private SimpleAdapter listAdapter;
	private Context context;
	public static final String DELETE_ACTION="com.example.vanetapp.deleteaction";
	/**
	 * TrafficlistActivity is used to show traffic data stored locally,but offer other functionalities like: e.g. remove all the data, save and load traffic data in xml format.
	 */
	/*two below fields are used for display the dialog box to create and parse traffic xml documents*/
	private ListView customDialogListView;
	private ArrayAdapter<String> customDialogAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_traffic_list);
		
	/*	ActionBar ab=getActionBar();
		ab.setDisplayShowHomeEnabled(false);*/
		context=this;
		setTitle("Traffic data loaded from DB");
		trafficList=(ListView) findViewById(R.id.mainListView);
		registerForContextMenu(trafficList);
		getAndShowDataFromDB();
		
	} 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_traffic_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId())
		{
			case R.id.menu_delete:
			{
				
				AlertDialog.Builder builder=new AlertDialog.Builder(this);
				builder.setCustomTitle(LayoutInflater.from(context).inflate(R.layout.custom_dialog_delete, null));
			//	builder.setMessage("Remove all traffic data stored?");
				
				builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						cancelAllDataStoredOnDB();
						listAdapter=new SimpleAdapter(getApplicationContext(),new ArrayList<HashMap<String, Object>>(),R.layout.row_layout,new String[]{},new int[]{});
						trafficList.setAdapter(listAdapter);
						listAdapter.notifyDataSetChanged();
						/* need to update the map, send a broadcast to a receiver that'll do it*/
				    	Intent intent=new Intent(DELETE_ACTION);
						sendBroadcast(intent);  
					}
				});
				builder.setNegativeButton("no", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						
					}
				});
				builder.show();
				return true;
			}
			
			case R.id.menu_xml_save:
			{
				AlertDialog.Builder builder=new AlertDialog.Builder(context);
				builder.setCustomTitle(LayoutInflater.from(this).inflate(R.layout.custom_title_dialog_save, null));

				builder.setPositiveButton("yes", new OnClickListener() {
				
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
	
						final Dialog dialog2=new Dialog(context);
						dialog2.setContentView(R.layout.custom_dialog_save_file_name);
						final EditText editText=(EditText) dialog2.findViewById(R.id.customDialogeditText);
						Button buttonOK=(Button) dialog2.findViewById(R.id.customDialogbuttonOK);
						final Button buttonCANCEL=(Button) dialog2.findViewById(R.id.customDialogbuttonCANCEL);
						editText.clearFocus();
						editText.setOnFocusChangeListener(new OnFocusChangeListener() {
							
							@Override
							public void onFocusChange(View v, boolean hasFocus) {
									if(hasFocus)
									{
										InputMethodManager in=((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE));
										in.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
									}
								
							}
						});
						
						buttonOK.setOnClickListener(new View.OnClickListener() {
							
							@Override
							public void onClick(View v) 
							{
								dialog2.dismiss();
								ArrayList<String> nodeList=new ArrayList<String>();
								nodeList.add(XMLFactory.ROAD);
								nodeList.add(XMLFactory.TIMESTAMP);
								nodeList.add(XMLFactory.TRAFFICLEVEL);
								try 
								{
									Log.i("traffic", editText.getText()+"");
									XMLFactory.createXML(nodeList, context,editText.getText()+"");	
									
								} 
								catch (ParserConfigurationException e) 
								{
									e.printStackTrace();
								} 
								catch (TransformerException e) 
								{
									e.printStackTrace();
								}
								getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
								Toast.makeText(context, "file saved", Toast.LENGTH_SHORT).show();
		
							}
						});
						buttonCANCEL.setOnClickListener(new View.OnClickListener() {
							
							@Override
							public void onClick(View v) {
								dialog2.dismiss();
								getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
							}
						});

						dialog2.show();
					}
				});
				builder.setNegativeButton("no", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						dialog.dismiss();						
					}
				});
				
				builder.show();
				return true;
			}
			case R.id.menu_xml_load:
			{
				AlertDialog.Builder builder=new AlertDialog.Builder(this);
				View viewInflated=LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
				builder.setView(viewInflated);
				builder.setCustomTitle(LayoutInflater.from(this).inflate(R.layout.custom_title_dialog_load, null));
				AlertDialog dialog=builder.create();
				customDialogListView=(ListView) viewInflated.findViewById(R.id.dialogListView);
				customDialogAdapter=new ArrayAdapter<String>(context, R.layout.custom_dialog_row);
				customDialogListView.setAdapter(customDialogAdapter);
				
				XMLFactory.findAndShowAllXMLavailable(customDialogAdapter);
				MyOnClickListItemListener itemClickListener=new MyOnClickListItemListener();
				customDialogListView.setOnItemClickListener(itemClickListener);
				
				DialogOkClickListener dialogOkClickListener=new DialogOkClickListener(itemClickListener,dialog);
				dialog.setButton(Dialog.BUTTON_POSITIVE, "ok",dialogOkClickListener);
				dialog.setButton(Dialog.BUTTON_NEGATIVE, "cancel",new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						
					}
				});
				dialog.show();
				
				
				return true;
			}
		}
		return true;
	} 
	@Override 
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
			menu.add("delete");
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    int index = info.position;
	    HashMap<String, Object> map= (HashMap<String, Object>) trafficList.getItemAtPosition(index);
	    
	    switch(item.getItemId())
	    {
		    case 0:
		    {
		    	deleteSingleRecordAndUpdateList((Integer)map.get("timestamp"));
		    	Intent intent=new Intent(DELETE_ACTION);
				sendBroadcast(intent); 
				/* need to update the map, send a broadcast to a receiver that'll do it*/
		    	 
		    	return true;
		    }
	    }

		return true;
	}
	public void getAndShowDataFromDB()
	{
		storage=StorageFactory.getStorageSQLiteImpl(this);
		Cursor c=storage.getAllRecords();
		ArrayList<RoadListItem> roadList=new ArrayList<RoadListItem>();
		c.moveToFirst();
		while(!c.isAfterLast())
		{
			RoadListItem r=null;
			String road=c.getString(1);
			int timestamp=c.getInt(2);
			int trafficLevel=c.getInt(3);
			if(trafficLevel==Constants.TRAFFIC_LOW)
				r=new RoadListItem(road, timestamp, R.drawable.roadim,R.drawable.tl_green,Constants.TRAFFIC_LOW);
			if(trafficLevel==Constants.TRAFFIC_MEDIUM)
				r=new RoadListItem(road, timestamp, R.drawable.roadim,R.drawable.tl_yellow,Constants.TRAFFIC_MEDIUM);
			if(trafficLevel==Constants.TRAFFIC_HIGH)
				r=new RoadListItem(road, timestamp, R.drawable.roadim,R.drawable.tl_red,Constants.TRAFFIC_HIGH);
			if(r!=null)
				roadList.add(r);
			c.moveToNext();
		}
		
		//Questa è la lista che rappresenta la sorgente dei dati della listview
        //ogni elemento è una mappa(chiave->valore)
        ArrayList<HashMap<String, Object>> data=new ArrayList<HashMap<String,Object>>();
		
		for(int i=0;i<roadList.size();i++)
		{
			RoadListItem r=roadList.get(i);
           
            HashMap<String,Object> roadMap=new HashMap<String, Object>();//creiamo una mappa di valori
           
            roadMap.put("imagestatic", r.getPhotoId()); // per la chiave image, inseriamo la risorsa dell immagine
            roadMap.put("road", r.getRoad()); // per la chiave name,l'informazine sul nome
            roadMap.put("timestamp", r.getTimestamp());// per la chiave surnaname, l'informazione sul cognome
            roadMap.put("imagetraffic", r.getPhotoTrafficId());
            data.add(roadMap);  //aggiungiamo la mappa di valori alla sorgente dati
		}
		String[] from={"imagestatic","road","timestamp","imagetraffic"}; //dai valori contenuti in queste chiavi
        int[] to={R.id.roadImage,R.id.roadId,R.id.timestampId,R.id.roadTraffic};//agli id delle view
       
        //costruzione dell adapter
        listAdapter=new SimpleAdapter(
                        getApplicationContext(),
                        data,//sorgente dati
                        R.layout.row_layout, //layout contenente gli id di "to"
                        from,
                        to);
       

        
		trafficList.setAdapter(listAdapter);
	}
	public boolean cancelAllDataStoredOnDB()
	{
		return storage.deleteAllRecords();
	}
	public void deleteSingleRecordAndUpdateList(int timestamp)
	{
		Log.i("content", "content "+timestamp);
		Cursor cursor=storage.getRecordWithSpecificTimestamp(timestamp); // cursor is a read-only copy of the db
		storage.deleteRecordWithSpecificTimeStamp(timestamp);
		
		getAndShowDataFromDB();
		listAdapter.notifyDataSetChanged();
		String tLevel=cursor.getInt(3)==Constants.TRAFFIC_LOW? "traffic low" : cursor.getInt(3)==Constants.TRAFFIC_MEDIUM ? "traffic medium" : "traffic high";
		Toast.makeText(context, "deleted "+cursor.getString(1)+ ": "+tLevel, Toast.LENGTH_SHORT).show();
	}



class MyOnClickListItemListener implements android.widget.AdapterView.OnItemClickListener
{	
	View lastSelected=null;

	@Override
	public void onItemClick(AdapterView<?> arg0, View v,
			int arg2, long arg3) 
	{
		if(lastSelected==null)
		{
			v.setBackgroundResource(android.R.color.white);
			lastSelected=v;
			
		}
		else
		{
			lastSelected.setBackgroundResource(android.R.color.transparent);
			v.setBackgroundResource(android.R.color.white);
			lastSelected=v;
		}
	}
	public View getLastSelected() {
		return lastSelected;
	}
	
}

class DialogOkClickListener implements DialogInterface.OnClickListener{

	MyOnClickListItemListener itemClickListener;
	Dialog customDialog;
	public DialogOkClickListener(MyOnClickListItemListener itemClickListener,Dialog customDialog) {
		this.itemClickListener=itemClickListener;
		this.customDialog=customDialog;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(itemClickListener.getLastSelected()!=null)
		{
			String relativeFilePath=((TextView)itemClickListener.getLastSelected()).getText()+"";
			Log.i("traffic", "selected "+XMLFactory.DEFAULTFILEPATH+"/"+relativeFilePath);
			try 
			{
				XMLFactory.ParseXML(XMLFactory.DEFAULTFILEPATH+"/"+relativeFilePath, context);
			} 
			catch (ParserConfigurationException e) 
			{
				e.printStackTrace();
			}
			customDialog.dismiss();
			getAndShowDataFromDB();
			customDialogAdapter.notifyDataSetChanged();
			Toast.makeText(context, relativeFilePath +" traffic info loaded", Toast.LENGTH_SHORT).show();
		}
		else
			Toast.makeText(context, "Select a file to load", Toast.LENGTH_SHORT).show();
		
	}
	
}
}