package it.unibo.vanetapp.model.impl;

import java.util.Calendar;
import java.util.Date;

import com.example.vanetapp.RoadListItem;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;
import it.unibo.vanetapp.model.interfaces.IAlgorithmTraffic;
import it.unibo.vanetapp.model.interfaces.IAlgorithmMerge;
import it.unibo.vanetapp.model.interfaces.IStorage;
import it.unibo.vanetapp.utility.Constants;

public class AlgorithmMergeFactory 
{
	private static IAlgorithmMerge instance=null;
	/**
	 * This class according to the pattern factory provide a public method to get an instance (static) of MyAlgorithmMerge wich is used merge traffic data 
	 * see below for details
	 * 
	 * @return
	 */
	public static IAlgorithmMerge getAlgorithmImpl()
	{
		if(instance==null)
			return new MyAlgorithmMerge();
		else
			return instance;
	}
	
}
class MyAlgorithmMerge implements IAlgorithmMerge
{
	public static final String TAG="Bluetooth socket";
	/**
	 * @author karim
	 * 
	 * This implementation of the algorithm provide a "merge" of traffic informations, coming from different sources. In particular we have a local traffic table
	 *  and a remote table received from bluetooth or other form of ad oc communication. Simply, the algorithm calculate the  arithmetic average of the information 
	 *  collected. 
	 */
	public MyAlgorithmMerge() {
		
	}
	
	@Override
	public void calculate(RoadListItem[] roadList,Context context) 
	{

		IStorage storage=StorageFactory.getStorageSQLiteImpl(context);
		
		Log.i(TAG, "algorithm started");
		for(int i=0;i<roadList.length;i++)
		{
			String road=roadList[i].getRoad();		
			Cursor cursorGotFromMyDB=storage.getAllRecords();
			cursorGotFromMyDB.moveToFirst();
			boolean find=false;
			int trafficLevelFromMyDBSingleRow=-1;
			while(!cursorGotFromMyDB.isAfterLast() && !find)
			{
				String roadtoSearch=cursorGotFromMyDB.getString(1);
				if(road.equals(roadtoSearch))
				{
					find=true;
					trafficLevelFromMyDBSingleRow=cursorGotFromMyDB.getInt(3);
				}
				else
					cursorGotFromMyDB.moveToNext();
			}
			if(find)/* if the information of traffic of that specific road is already  on the local table let's calculate the arithmetic average of the traffic level*/
			{
				Date now = Calendar.getInstance().getTime();
				int newTimestamp = (int) (now.getTime() % Constants.SECOND_PER_DAY); /* new timestamp*/
				int newTrafficLevel=(roadList[i].getTrafficLevel()+trafficLevelFromMyDBSingleRow)/2; /*arithmetic average*/
				storage.insertRecord(road, newTimestamp, newTrafficLevel);
			}
			if(!find) /* if the information of traffic of that specific road received is not on the local table let's expand it */
				storage.insertRecord(road, roadList[i].getTimestamp(), roadList[i].getTrafficLevel());
		}
		Log.i(TAG, "algorithm terminated");
		
	}
	
}


