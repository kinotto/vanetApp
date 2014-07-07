package it.unibo.vanetapp.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBhelper extends SQLiteOpenHelper
{
	public static final String NOME_DB="vanetdb";
	public static final int VERSIONE_DB=1;
	
	private static final String CREATE_TABLE="create table vanet (_id integer primary key autoincrement, "
		     + "road"+ " TEXT,"
		     + "timestamp"+ " INTEGER,"
		     + "trafficlevel"+ " INTEGER"+");";
		    
	private static final String DROP_TABLE="DROP TABLE IF EXISTS vanet";
	
	/**
	 * DBhelper is an utility class that realize the logic neeeded to create a sqlite db
	 * @param context
	 */
	public DBhelper(Context context) {
		super(context, NOME_DB, null, VERSIONE_DB);
		// TODO Auto-generated constructor stub
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
		try
		{
			db.execSQL(CREATE_TABLE);
		}
		catch(Exception e)
		{
			Log.e("error", "error during creation of DB");
			e.printStackTrace();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL(DROP_TABLE);
		onCreate(db);
		
	}

}
