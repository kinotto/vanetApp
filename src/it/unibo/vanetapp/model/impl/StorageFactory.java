package it.unibo.vanetapp.model.impl;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import it.unibo.vanetapp.model.interfaces.IStorage;
import it.unibo.vanetapp.persistence.DBhelper;
import it.unibo.vanetapp.utility.Constants;

public class StorageFactory
{
	private static IStorage instance=null;
	/**
	 * This class according to the pattern factory provide a public method to get an instance (static) of DBSQLiteImpl wich is used to persist the traffic data 
	 */
	public static IStorage getStorageSQLiteImpl(Context context)
	{
		if(instance==null)
			return new DBSQLiteImpl(context);
		else
			return instance;
	}
	
	
}
class DBSQLiteImpl implements IStorage
{
	public static final String ID = "_id";
    public static final String ROAD = "road";
    public static final String TIMESTAMP = "timestamp";
    public static final String TRAFFIC = "trafficlevel";
    private static final String TAG="db helper";
    
    public static final String TABLE = "vanet";
    public static final String[] COLUMNS = new String[]{ID, ROAD, TIMESTAMP,TRAFFIC};
    private SQLiteDatabase db=null;
    
    /**
     * An implemention of IStorage which provide all the functionalities to manage traffic informations
     * @param context
     */
    public DBSQLiteImpl(Context context) {
    	DBhelper dBhelper=new DBhelper(context);
        db=dBhelper.getWritableDatabase();
	}
    @Override
    public boolean insertRecord(String road,int timestamp,int traffic)
    {

    	boolean res=true; // the returned value: true means that previus records on DB (with same roads) were different in the traffic level. it's important for updating the map.
      	
    	ContentValues values=new ContentValues();
    	values.put(ROAD,road);
    	values.put(TIMESTAMP, timestamp);
    	values.put(TRAFFIC, traffic);
    	
    	Cursor c=getRecordWithSpecificRoad(road); //delete previous record with the same street!!
    	if(c.getCount()>0)
    	{
	    	c.moveToFirst();
	    	while(!c.isAfterLast())
	    	{
	    		if(c.getString(1).equals(road) && c.getInt(3)==traffic)
	    			res=false;
	    		deleteRecord(c.getLong(0));
	    		c.moveToNext();
	    	}
	    }
    	
        db.insert(TABLE, null, values);
    		
        return res;
    }
    @Override
    public Cursor getRecord(long id) throws SQLException {
        Cursor c = db.query(true, TABLE, COLUMNS, ID + "=" + id, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }
    @Override
    public Cursor getRecordWithSpecificRoad(String road) throws SQLException {
        Cursor c = db.query(true, TABLE, COLUMNS, ROAD + "=" + "'"+road+"'", null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }
    @Override
    public Cursor getRecordWithSpecificTimestamp(int timestamp) throws SQLException {
    	Cursor c = db.query(true, TABLE, COLUMNS, TIMESTAMP + "=" + timestamp, null, null, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }
    @Override
    public Cursor getAllRecords(){
        return db.query(TABLE, COLUMNS, null, null, null, null, null);
    }
    @Override
    public boolean deleteRecord(long id) {
        return db.delete(TABLE, ID + "=" + id, null) > 0;
        
    }
    @Override
    public boolean deleteAllRecords()
    {
    	return db.delete(TABLE, null, null)>0;
    }
    @Override
    public boolean deleteRecordWithSpecificTimeStamp(int timestamp)
    {
    	
    	return db.delete(TABLE, TIMESTAMP + "=" + timestamp, null)>0;
    }
    @Override
    public boolean updateRecord(long id, String road, int timestamp,int traffic){
  	
        ContentValues v = new ContentValues();
        v.put(ROAD, road);
        v.put(TIMESTAMP, timestamp);
        v.put(TRAFFIC, traffic);
        
        return db.update(TABLE, v, ID + "=" + id, null) >0;
    }
    @Override
    public boolean updateRecordWithSpecificRoad(String road, int timestamp,int traffic){

    	ContentValues v = new ContentValues();
      //  v.put(ID, 1);
        v.put(TIMESTAMP, timestamp);
        v.put(TRAFFIC, traffic);
        
        return db.update(TABLE, v, ROAD + "=" +"'"+road+"'", null) >0;
    }
    @Override
    public void closeConnectionToDB()
    {
    	db.close();
    }


}

