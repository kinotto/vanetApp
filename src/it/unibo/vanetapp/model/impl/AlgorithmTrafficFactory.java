package it.unibo.vanetapp.model.impl;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import it.unibo.vanetapp.model.interfaces.IAlgorithmTraffic;
import it.unibo.vanetapp.model.interfaces.IStorage;
import it.unibo.vanetapp.services.TrafficService;
import it.unibo.vanetapp.utility.Constants;

public class AlgorithmTrafficFactory 
{
	/**
	 * This class according to the pattern factory provide a public method to get an instance (static) of MyAlgorithmTraffic which is used to calculate the traffic 
	 * knowing the current location of the device and the acceleration 
	 */
	private static MyAlgorithmTraffic instance=null;

	public static IAlgorithmTraffic getAlgorithmTraffic()
	{
		if(instance == null)
			instance=new MyAlgorithmTraffic();
		
		return instance;
	}
}
class MyAlgorithmTraffic implements IAlgorithmTraffic
{ 
	
	/**
	 * this class realize the logic to calculate the traffic knowing the current location of the device and the acceleration. in particular some thresholds are used
	 * to determine whether the traffic is low, medium or high. At the end the record built is saved locally
	 */
	public static String TAG="AlgorithmTrafficFactory";
	
	public MyAlgorithmTraffic() 
	{
		
	}
	
	@Override
	public int calculate(Context context, float[] lastAcceleration,float[] deltaOrientation) 
	{
		Log.i(TrafficService.TAG, "pre inserting record");
		if(IsOrientationChanged(deltaOrientation))
			return -1;
		
		GPSTracker gps=GPSTracker.getInstance(context);
		Location currentLocation=gps.getLastLocation();

		/**
		 * You can then ask lastAccelration wherever you want in the application for the current acceleration, 
		 * independent from the axis and cleaned from static acceleration such as gravity. It will be approx 0
		 *  if there is no movement, and, lets say >2 if the device is moved.
		 */
		
		float x = lastAcceleration[0];
		float y = lastAcceleration[1];
		float z = lastAcceleration[2];
		
		
		TrafficService.mAccelLast = TrafficService.mAccelCurrent;
		TrafficService.mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
		float delta = TrafficService.mAccelCurrent - TrafficService.mAccelLast;
		TrafficService.mAccel = TrafficService.mAccel * 0.9f + delta; // perform low-cut filter
		Log.i(TAG,"acceleration " +TrafficService.mAccel+" cur location lat "+currentLocation.getLatitude()+" long "+currentLocation.getLongitude());


		IStorage storage=StorageFactory.getStorageSQLiteImpl(context);
		Log.i(TAG, "geocoder present: "+Geocoder.isPresent());
		if(Geocoder.isPresent())
		{
			/**
			 * "Geocoding", the process that allow to retrieve address from lat/long and viceversa, 
			 *  has nothing in common with GPS but it requires complete map DB server (to search cities and streets).
			 *  Google map is stored on the web therefore 3G,Edge or WiFi channels are needed 
			 *  in order to use Geocoding! Without such connection is possibile to see some 
			 *  pieces of the map because of GoogleMap caching but geocode interface require 
			 *  direct channel!
			 *	Geocoder basically is a stub. The isPresent() static method check if an implementation 
			 *	of geocoder is available on the device
			 */
			Geocoder geocoder=new Geocoder(context,Locale.getDefault());
			Date now = Calendar.getInstance().getTime();
			int timePortion = (int) (now.getTime() % Constants.SECOND_PER_DAY);		
			try 
			{

				int tLevel=-1;
				if(TrafficService.mAccel<Constants.THRESHOLD_LOW)
					tLevel=Constants.TRAFFIC_LOW;

				if(TrafficService.mAccel>Constants.THRESHOLD_LOW && TrafficService.mAccel<Constants.THRESHOLD_MEDIUM)
					tLevel=Constants.TRAFFIC_MEDIUM;

				if(TrafficService.mAccel>Constants.THRESHOLD_MEDIUM)
					tLevel=Constants.TRAFFIC_HIGH;

				storage.insertRecord(geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 3).get(0).getAddressLine(0)+"", timePortion,tLevel);
				storage.closeConnectionToDB();
				return tLevel;
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		return -1;
		
	}
	/**
	 * check if the orientation of the phone has changed, if so, the traffic algorithm is not triggered (orientation changed -> curve)
	 * @param deltaOrientation
	 * @return
	 */
	private boolean IsOrientationChanged(float[] deltaOrientation)
	{
		if (deltaOrientation[0] > Constants.THRESHOLD_CURVE || deltaOrientation[1] > Constants.THRESHOLD_CURVE || deltaOrientation[2] > Constants.THRESHOLD_CURVE)
		{
			Log.i(TAG, "orientation has changed ! ");
			Log.i(TrafficService.TAG, "orientation has changed ! TRUE ");
			return true;
		}
		return false;
	}
}