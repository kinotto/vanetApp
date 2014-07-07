package it.unibo.vanetapp.services;

import it.unibo.vanetapp.model.impl.AlgorithmTrafficFactory;
import it.unibo.vanetapp.model.impl.GPSTracker;
import it.unibo.vanetapp.model.interfaces.IAlgorithmTraffic;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class TrafficService extends Service implements SensorEventListener
{
	
	public static float mAccel; // acceleration apart from gravity
	public static float mAccelCurrent; // current acceleration including gravity
	public static float mAccelLast; // last acceleration including gravity
	public static boolean gyroscopeIsPresent;
	private SensorManager sensorManager;
	private Sensor accelerationSensor; 
	private Sensor magneticFieldSensor;
	public static final String TAG="Normal Service";
	private GPSTracker gps;
	private Context context;
	public static final String TRAFFIC_CHANGE_ACTION = "it.unibo.vanetapp.services.actionmarker";
	
	/*used to get orientation */
	private float[] accelerometerValues = new float[3];
	private float[] geomagneticMatrix = new float[3];
	private float[] delta_orientation = new float[]{0F,0F,0F}; //measure the variation in orientation
	private boolean sensorReady = false;
	private static final int THRESHOLD_EVENTS = 30;
	private int eventIesim= 0;
	
	/**
	 * TrafficService, like all the Service Class runs in the same thread of the ui, and in this case is used to provide the sensing function using the accelerometer.
	 */
	public TrafficService() {
		Log.i(TAG, "service started");
	}
	@Override
	public void onCreate() {
		super.onCreate();
		
		checkSensorsArePresent();

		context=this;
		Log.i(TAG, "service started");
		gps=GPSTracker.getInstance(this);
		mAccel = 0.00f;
		mAccelCurrent = SensorManager.GRAVITY_EARTH;
		mAccelLast = SensorManager.GRAVITY_EARTH;


		sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
		accelerationSensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magneticFieldSensor=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, magneticFieldSensor,SensorManager.SENSOR_DELAY_UI);
		
	}
	private void checkSensorsArePresent()
	{
		PackageManager PM= this.getPackageManager();
		boolean gpsIsPresent = PM.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
		boolean accelerometerIsPresent = PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
	  //  boolean gyroscopeIsPresent=PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
	    Log.i(TAG, "gps is present: "+gpsIsPresent+" accelerometer is present: "+accelerometerIsPresent);
		
	}
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent se) {

		/**The methods included in the Android APIs to get the orientation does not include readings from the gyroscope sensor. 
		 * Gyroscope does not provide information about orientation, since it only has information about rotation speed in rad/s
		 */
		/**
		 * the android system launch events each 20 microseconds, the variable eventIesim is used to filter event 
		 */
		eventIesim++;
		if(eventIesim == THRESHOLD_EVENTS)
		{

			switch (se.sensor.getType())
			{
				case Sensor.TYPE_ACCELEROMETER:
				{
					accelerometerValues = se.values.clone();
					new ListenerThread(accelerometerValues,delta_orientation,context,gps).start(); // use the last orientation set
					
					break;
				}
				case Sensor.TYPE_MAGNETIC_FIELD:
				{
					geomagneticMatrix = se.values.clone();
					sensorReady = true;
					break;
				}
				default:
					break;
			}   

			if (geomagneticMatrix != null && accelerometerValues != null && sensorReady) 
			{
				sensorReady = false;

				float[] R = new float[16];
				float[] I = new float[16];

				SensorManager.getRotationMatrix(R, I, accelerometerValues, geomagneticMatrix);

				float[] actual_orientation = new float[3];
				SensorManager.getOrientation(R, actual_orientation); // lastorientation filled with orientation in radiant
				
				delta_orientation[0]= (delta_orientation[0] > actual_orientation[0]) ? (delta_orientation[0] - actual_orientation[0]) : (actual_orientation[0] - delta_orientation[0]);
				delta_orientation[1]= (delta_orientation[1] > actual_orientation[1]) ? (delta_orientation[1] - actual_orientation[1]) : (actual_orientation[1] - delta_orientation[1]);
				delta_orientation[2]= (delta_orientation[2] > actual_orientation[2]) ? (delta_orientation[2] - actual_orientation[2]) : (actual_orientation[2] - delta_orientation[2]);

				Log.i(TAG, "acceleration with gravity x:"+accelerometerValues[0]+" y:"+accelerometerValues[1]+" z:"+accelerometerValues[2]);

				new ListenerThread(accelerometerValues,delta_orientation,context,gps).start();

				Log.i(TAG, "orientation x:"+delta_orientation[0]+" y:"+delta_orientation[1]+" z:"+delta_orientation[2]);
				//Toast.makeText(context, "orientation x:"+delta_orientation[0]+" y:"+delta_orientation[1]+" z:"+delta_orientation[2], Toast.LENGTH_SHORT).show();
				
				delta_orientation = actual_orientation;
			}
			eventIesim = 0;
		}
		


	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		sensorManager.unregisterListener(this); /*unregister the listener */
		Log.i(TAG, "normal service destroyed");
	}

	
}
class ListenerThread extends Thread
{
	private Context context;
	private float[] accelerometerValues;
	private float[] deltaOrientation;
	private GPSTracker gps;
	public ListenerThread(float[] accelerometerValues,float[] deltaOrientation,Context context,GPSTracker gps) 
	{
		this.accelerometerValues = accelerometerValues;
		this.deltaOrientation = deltaOrientation;
		this.context = context;
		this.gps = gps;
	}
	@Override
	public void run() 
	{
		
		if(gps.isLocationChanged())
		{
			
			
			Location currentLocation = gps.getLastLocation();
			//Log.i("location ", "last location "+currentLocation.getLatitude());

			IAlgorithmTraffic myAlgorithmTraffic=AlgorithmTrafficFactory.getAlgorithmTraffic();
			int tLevel=myAlgorithmTraffic.calculate(context,accelerometerValues,deltaOrientation);


			if(tLevel!= -1)
			{
				Intent intent=new Intent(TrafficService.TRAFFIC_CHANGE_ACTION);
				intent.putExtra("lat", currentLocation.getLatitude());
				intent.putExtra("long", currentLocation.getLongitude());
				intent.putExtra("tLevel", tLevel);
				context.sendBroadcast(intent);  
			}
		}
		else
			Log.i("location ", "location changed false ");


	}
}

