package it.unibo.vanetapp.utility;

import it.unibo.vanetapp.model.impl.StorageFactory;
import it.unibo.vanetapp.model.interfaces.IStorage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.UUID;

import com.example.vanetapp.BluetoothListActivity;
import com.example.vanetapp.MainActivity;
import com.example.vanetapp.R;
import com.example.vanetapp.RoadListItem;



import android.R.integer;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;

public class BluetoothTaskOut extends AsyncTask<BluetoothDevice, Void, Void>
{
	public static final int PORT_LISTENED_FROM_SERVER=8666;
	private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    public static final String TAG="Bluetooth socket";
    private Context context;
    private BluetoothSocket socket = null;
    public static ProgressDialog dialog;
    public static final String NUMBER_OF_DATA_TO_SEND="it.unibo.vanetapp.utility.number_of_data_to_send_action";
    public static final String INCREMENT_PROGRESS_DIALOG="it.unibo.vanetapp.utility.increment_progress_dialog_action";
    public static final String DISMISS_DIALOG="it.unibo.vanetapp.utility.dismiss_dialog.action";
    
    /**
     * this class runs in a different thread from the UI, like all the AsyncTask, and realize all the logic to connect to bluetooth devices
     * @param context
     */
   
	public BluetoothTaskOut(Context context) 
	{
		this.context=context;
	} 
	@Override
	protected void onPreExecute()
	{
	/*	dialog=new ProgressDialog(context);
		dialog.setCustomTitle(LayoutInflater.from(context).inflate(R.layout.custom_dialog_bluetooth, null));
		dialog.setIndeterminate(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMessage("Sending traffic data");
		dialog.show();*/
	}
	@Override
	protected void onPostExecute(Void result) 
	{
	//	dialog.dismiss();
	}
	@Override
	protected Void doInBackground(BluetoothDevice ...device) 
	{

		BluetoothDevice deviceToconnect=device[0];
		try {
			
			socket = deviceToconnect.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
			Log.i(TAG, "socket created");

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			 Log.e(TAG, "Socket create() failed", e);
		}

		 try {
             // This is a blocking call and will only return on a
             // successful connection or an exception
             socket.connect();
             Log.i(TAG, "connected to "+deviceToconnect.getName());
         } 
		 catch (IOException e)  
		 {
             // Close the socket
             try 
             {
                 socket.close();
             } 
             catch (IOException e2) {
                 Log.e(TAG, "unable to close() socket during connection failure", e2);
             }
		
		 }
		 Log.i(TAG, "start thread out");
		 new ThreadOut(socket, context).start();
		 return null; 
		 
		 
	}
	

class ThreadOut extends Thread
{
	BluetoothSocket socket;
	Context context;

	public ThreadOut(BluetoothSocket socket,Context context) 
	{
		this.socket=socket;
		this.context=context;
	}
	
	public void run() {
		
		OutputStream os = null;
		try 
		{
			os = socket.getOutputStream();
			
		} 
		catch (IOException e) 
		{
			Log.e(TAG,e.getMessage()+" error");
		}
		Log.i(BluetoothTaskOut.TAG, "ostream created");
		ObjectOutputStream objStream = null; 
		try 
		{
			objStream = new ObjectOutputStream(os);
		} 
		catch (IOException e1) 
		{
			Log.e(TAG,e1.getMessage()+" error");
		}
		try 
		{
			if(objStream!=null)
				objStream.writeObject(fromCursorToRoadList());
			
			socket.close();

		} catch (IOException e) 
		{
			Log.e(TAG,e.getMessage()+" error");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(BluetoothTaskOut.TAG, "obj sent no exception thrown");
		
	}
	private RoadListItem[] fromCursorToRoadList() throws InterruptedException
	{
		IStorage storage=StorageFactory.getStorageSQLiteImpl(context);
		Cursor c=storage.getAllRecords();
		ArrayList<RoadListItem> list=new ArrayList<RoadListItem>();
		c.moveToFirst();
		Intent numberOfDataTosend=new Intent(NUMBER_OF_DATA_TO_SEND);
		numberOfDataTosend.putExtra("maxData", c.getCount());
		context.sendBroadcast(numberOfDataTosend);
		while(!c.isAfterLast())
		{
			RoadListItem r=new RoadListItem(c.getString(1), c.getInt(2), c.getInt(3));
			list.add(r);
			c.moveToNext();
			context.sendBroadcast(new Intent(INCREMENT_PROGRESS_DIALOG));
			sleep(500);
		}
		context.sendBroadcast(new Intent(DISMISS_DIALOG));
		return list.toArray(new RoadListItem[0]);
		
	}
	
	
}

	
}