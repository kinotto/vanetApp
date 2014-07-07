package com.example.vanetapp;




import java.io.IOException;
import java.util.ArrayList;

import it.unibo.vanetapp.broadcastreceiver.MarkerDataBroadCastReceiver;
import it.unibo.vanetapp.model.impl.GPSTracker;
import it.unibo.vanetapp.model.impl.StorageFactory;
import it.unibo.vanetapp.model.interfaces.IStorage;
import it.unibo.vanetapp.services.BluetoothService;
import it.unibo.vanetapp.services.TrafficService;

import com.example.vanetapp.R;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;



import android.graphics.Bitmap;

import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;

import android.os.Bundle;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


public class MainActivity extends Activity {

	private GoogleMap map;
	public final int TIME_TO_DISCOVER_BLUETOOTH_DEVICE=60*1000;
	private MarkerDataBroadCastReceiver markerDataReceiver=null;
	private ArrayList<Marker> markerList;
	private IStorage storage;
	private IntentFilter intentFromTraffic;
	private PendingIntent pendingIntent;
	private Intent TrafficServiceIntent = null;
	public static Context context;
	private BluetoothAdapter bluetoothAdapter;
	public static final int REQUEST_ENABLE_BT = 0;
	public static final int BLUETOOTH_IS_NOT_ENABLED = 1;
	private MenuItem itemMonitoring;
	public static boolean inFront=false;
	/**
	 * MainActivity is the entry-point of the application. Inside the view there are the map, and an action bar containing all the buttons to interact with the user.
	 * 
	 */
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMapType(map.MAP_TYPE_HYBRID); 
		map.setMyLocationEnabled(true);  
		storage=StorageFactory.getStorageSQLiteImpl(this);
		context=this;
		markerList=new ArrayList<Marker>();
		
		/*move the camera to the current position */
		GPSTracker gps=GPSTracker.getInstance(this);
		LatLng currentLocation=new LatLng(gps.getLatitude(), gps.getLongitude());
		CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(currentLocation.latitude,  currentLocation.longitude));
	    CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
	    map.moveCamera(center);
	    map.animateCamera(zoom);
		 
	    map.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			@Override
			public View getInfoWindow(Marker marker) 
			{
				return null;
			}
			
			@Override
			public View getInfoContents(Marker marker) {
				View v=getLayoutInflater().inflate(R.layout.info_window_layout, null);
				return v;
			}
		});
	    
	    loadMarkerListAndMapAtStart();
				 
		Marker startLocMarker=map.addMarker(new MarkerOptions().position(currentLocation).title("").snippet("").icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_down)));
		startLocMarker.showInfoWindow(); 
		map.setInfoWindowAdapter(null); 
		
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    markerDataReceiver=new MarkerDataBroadCastReceiver(this,markerList,map);
	    intentFromTraffic=new IntentFilter();
	    intentFromTraffic.addAction(TrafficService.TRAFFIC_CHANGE_ACTION);
	    intentFromTraffic.addAction(TrafficListActivity.DELETE_ACTION);
	  
	
	    registerReceiver(markerDataReceiver, intentFromTraffic);
	
	    inFront=true;  /*check whether the activity is in foreground*/
	    
	}

	@Override
	protected void onPause() 
	{
	    super.onPause();   	   
	    inFront=false; /*check whether the activity is in foreground*/
	   unregisterReceiver(markerDataReceiver);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);
		switch(item.getItemId())
		{
			case R.id.menu_refresh:
			{
				markerList.clear();
				loadMarkerListAndMapAtStart();
				return true;
			}
			case R.id.menu_monitoring:
			{ 
				Bitmap bitmap= ((BitmapDrawable)item.getIcon()).getBitmap();
				Bitmap bitmapPlay=((BitmapDrawable)getResources().getDrawable(android.R.drawable.ic_media_play)).getBitmap();
				Bitmap bitmapPause=((BitmapDrawable)getResources().getDrawable(android.R.drawable.ic_media_pause)).getBitmap();
				
				if(bitmap==bitmapPlay)
				{
					bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
					ensureDiscoverableBluetooth();
					itemMonitoring=item;
					Intent intentBluetooth=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					if(!bluetoothAdapter.isEnabled())
						startActivityForResult(intentBluetooth, REQUEST_ENABLE_BT);
					else
					{
						TrafficServiceIntent=new Intent(this,TrafficService.class);
						startService(TrafficServiceIntent);
						setProgressBarIndeterminateVisibility(true);
						itemMonitoring.setIcon(android.R.drawable.ic_media_pause);
						Toast.makeText(this, "Start monitoring traffic", Toast.LENGTH_SHORT).show();
						scheduleTimerBluetoothService();
					}
					return true;
					
				}
				if(bitmap==bitmapPause)
				{
					stopService(TrafficServiceIntent);
					removeScheduleTimerBluetoothService();
					setProgressBarIndeterminateVisibility(false);
					item.setIcon(android.R.drawable.ic_media_play);
					Toast.makeText(this, "Stop monitoring traffic", Toast.LENGTH_SHORT).show();
					return true;
				}
				
			} 
			case R.id.menu_send:
			{
				startActivity(new Intent(this,BluetoothListActivity.class));
				return true;
			}
			case R.id.menu_archive:
			{
				startActivity(new Intent(this,TrafficListActivity.class));
				return true;
			}
		}
		return true;
	}

	private void ensureDiscoverableBluetooth() {

		if (bluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		} 
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) 
		{
			case REQUEST_ENABLE_BT:
			{
				if(resultCode==RESULT_OK)
				{
				/*	if(TrafficServiceIntent!=null)
						stopService(TrafficServiceIntent);
					if(BluetoothServiceIntent!=null)
						stopService(BluetoothServiceIntent);
					*/
					TrafficServiceIntent=new Intent(this,TrafficService.class);
					startService(TrafficServiceIntent);
					setProgressBarIndeterminateVisibility(true);
					itemMonitoring.setIcon(android.R.drawable.ic_media_pause);
					Toast.makeText(this, "Start monitoring traffic, Bluetooth ENABLED", Toast.LENGTH_SHORT).show();
					scheduleTimerBluetoothService();
				}
				else if(resultCode==RESULT_CANCELED)
				{
					TrafficServiceIntent=new Intent(this,TrafficService.class);
					startService(TrafficServiceIntent);
					setProgressBarIndeterminateVisibility(true);
					itemMonitoring.setIcon(android.R.drawable.ic_media_pause);
					Toast.makeText(this, "Start monitoring traffic, Bluetooth DISABLED", Toast.LENGTH_SHORT).show();
				}
					
				break;
			}
			default:
				break;
			}
	}

	/**
	 * schedule a service for dynamic bluetooth each 60 seconds.
	 */
	private void scheduleTimerBluetoothService()
	{

		AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, BluetoothService.class);
        pendingIntent = PendingIntent.getService(this, 0, intent, 0);
      
        //After after x seconds
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), TIME_TO_DISCOVER_BLUETOOTH_DEVICE , pendingIntent); 


	}
	/**
	 * remove the scheduled service.
	 */
	

	private void removeScheduleTimerBluetoothService()
	{	
		AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);
	}
	
	/**
	 * load data from db, display into the map and add to the markerlist
	 * @throws IOException
	 */
	public  void loadMarkerListAndMapAtStart() 
	{
		map.clear();
		Cursor c=storage.getAllRecords();
		Geocoder geocoder;
		while(c.moveToNext())
		{
			String road=c.getString(1);
		//	int timestamp=c.getInt(2);
			int trafficLevel=c.getInt(3);
			int idMarkerToDraw=trafficLevel==0 ? R.drawable.tl_green : trafficLevel==1 ? R.drawable.tl_yellow : R.drawable.tl_red;
		
			if(Geocoder.isPresent())
			{
				geocoder=new Geocoder(this);
				Address address=null;
				try {
					address = geocoder.getFromLocationName(road, 3).get(0);
					LatLng point=new LatLng(address.getLatitude(), address.getLongitude());
					Marker marker=map.addMarker(new MarkerOptions().position(point).title(address.getAddressLine(0)).snippet("").icon(BitmapDescriptorFactory.fromResource(idMarkerToDraw)));
				
					markerList.add(marker);
				} catch (IOException e) {
					Log.e("error geocoder", "error geocoder");
					e.printStackTrace();
				}
			
			}
			else
				Toast.makeText(this, "Geocoder not present", Toast.LENGTH_SHORT).show();
		}
		
	}

	
}
 