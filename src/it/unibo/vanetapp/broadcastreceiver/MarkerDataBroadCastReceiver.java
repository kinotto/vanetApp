package it.unibo.vanetapp.broadcastreceiver;


import it.unibo.vanetapp.services.TrafficService;
import it.unibo.vanetapp.utility.BluetoothTaskIn;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.util.Log;
import android.widget.Toast;

import com.example.vanetapp.MainActivity;
import com.example.vanetapp.R;
import com.example.vanetapp.TrafficListActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MarkerDataBroadCastReceiver extends BroadcastReceiver{

	private MainActivity mainActivity;
	private ArrayList<Marker> markerList;
	private GoogleMap map;
	private static final String TAG="marker receiver";
	/**
	 * MarkerDataBroadCastReceiver listen to the broadcasts sent by other modules of the application. in particular to handle to events: first if the user
	 * remove a single traffic information from the db through the view, the view itself need to be updated, second to handle a change in the traffic and so 
	 * update the ui adding the marker (green, yellow or red) and third to update the ui in case of receive new traffic data from bluetooth
	 * 
	 * @param activity
	 * @param markerList
	 * @param map
	 */
	public MarkerDataBroadCastReceiver(MainActivity activity,ArrayList<Marker> markerList,GoogleMap map) 
	{
		this.mainActivity=activity;
		this.markerList=markerList;
		this.map=map;
	}
	@Override 
	public void onReceive(Context context, Intent intent)
	{
		String action=intent.getAction();
		if(action.equals(TrafficListActivity.DELETE_ACTION)) /* update UI*/
		{
			Log.i(TAG, "broadcast triggered");
			markerList.clear();
			mainActivity.loadMarkerListAndMapAtStart();
		}
		if(action.equals(TrafficService.TRAFFIC_CHANGE_ACTION))/* update UI*/
		{
			Log.i(TAG, "broadcast triggered ");
		//	Toast.makeText(mainActivity, "traffic change action", Toast.LENGTH_LONG).show();
			Geocoder geocoder=null;
			String addresshToBeSearched="";
			
			double latitude=intent.getDoubleExtra("lat",-1D);
			double longitude=intent.getDoubleExtra("long", -1D);
			int trafficLevel=intent.getIntExtra("tLevel", -1);
			Log.i(TAG, "received tlevel "+trafficLevel);
			int idMarkerToDraw=trafficLevel==0 ? R.drawable.tl_green : trafficLevel==1 ? R.drawable.tl_yellow : R.drawable.tl_red;
			
			LatLng point=new LatLng(latitude, longitude);
			/*
			 * this code provide a substitution of previous marker added to the map that are equals to new marker that has to be add.
			 */
			if(Geocoder.isPresent())
			{
				geocoder= new Geocoder(context); 
				try {
					addresshToBeSearched=geocoder.getFromLocation(point.latitude, point.longitude, 3).get(0).getAddressLine(0);
				} catch (IOException e) {
					Log.e("error geocoder","error geocoder");
					e.printStackTrace();
				}
			}
			
			MarkerOptions markerOptionToBeAdd=new MarkerOptions().position(point).title(addresshToBeSearched).snippet("").icon(BitmapDescriptorFactory.fromResource(idMarkerToDraw));
			boolean find=false;
			for(int i=0;i<markerList.size() && find==false;i++)
			{
				if(Geocoder.isPresent())
				{
					
					try {
						double lat=markerList.get(i).getPosition().latitude;
						double longit=markerList.get(i).getPosition().longitude;
						String addressMarked=geocoder.getFromLocation(lat, longit, 3).get(0).getAddressLine(0);
						if(addressMarked.equals(addresshToBeSearched))
						{
							Marker markerToBeRemoved=markerList.get(i);
							markerToBeRemoved.remove(); //removed from the map
							markerList.remove(i); //removed from the list
							Marker newMarkerToBeAdd=map.addMarker(markerOptionToBeAdd);
							newMarkerToBeAdd.showInfoWindow();
							markerList.add(newMarkerToBeAdd);
							find=true;
							
						}
					} catch (IOException e) {
						Log.e("error geocoder","error geocoder");
						e.printStackTrace();
					}
				
				}
			}
			if(find==false)
			{ 
				Marker newMarkerToBeAdd=map.addMarker(markerOptionToBeAdd);
				newMarkerToBeAdd.showInfoWindow();
				markerList.add(newMarkerToBeAdd);
			}
			
		}
		if(action.equals(BluetoothTaskIn.BLUETOOTH_RECEIVED_DATA))/*update UI */
		{
			markerList.clear();
			mainActivity.loadMarkerListAndMapAtStart();
		}
	}
			
}
