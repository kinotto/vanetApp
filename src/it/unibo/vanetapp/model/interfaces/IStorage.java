package it.unibo.vanetapp.model.interfaces;


import android.database.Cursor;
import android.database.SQLException;
/**
 * As the name sad contains all the methods useful to manage traffic data.
 * @author karim
 *
 */
public interface IStorage
{
	public boolean insertRecord(String road, int timestamp,int trafficLevel);
	public Cursor getRecord(long id) throws SQLException;
	public Cursor getRecordWithSpecificRoad(String road) throws SQLException;
	public Cursor getAllRecords();
	public boolean deleteRecord(long id);
	public boolean deleteAllRecords();
	public boolean updateRecord(long id, String road, int timestamp,int trafficLevel);
	public boolean updateRecordWithSpecificRoad(String road, int timestamp,int trafficLevel);
	public void closeConnectionToDB();
	public boolean deleteRecordWithSpecificTimeStamp(int timestamp);
	public Cursor getRecordWithSpecificTimestamp(int timestamp) throws SQLException; 
}
   