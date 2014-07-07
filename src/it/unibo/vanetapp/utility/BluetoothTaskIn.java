package it.unibo.vanetapp.utility;

import it.unibo.vanetapp.model.impl.AlgorithmMergeFactory;
import it.unibo.vanetapp.model.interfaces.IAlgorithmMerge;
import it.unibo.vanetapp.services.BluetoothService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.UUID;

import com.example.vanetapp.BluetoothListActivity;
import com.example.vanetapp.R;
import com.example.vanetapp.RoadListItem;
import com.example.vanetapp.TrafficListActivity;
import com.google.android.gms.internal.bt;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

public class BluetoothTaskIn extends AsyncTask<BluetoothAdapter, Void, Void>{

	public static final int PORT_LISTENED_FROM_SERVER=8666;
	private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
	private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    public static final String TAG="Bluetooth socket";
    private Context context;
    private BluetoothSocket socket = null;
    public static String BLUETOOTH_RECEIVED_DATA="it.unibo.vanetapp.utility.bluetooth_received_data";
    
	public BluetoothTaskIn(Context context)
	{
		this.context=context;
	}
	
	@Override
	protected Void doInBackground(BluetoothAdapter ...bluetoothAdapter) 
	{

		BluetoothServerSocket serverSocket=null;
		try {

			BluetoothAdapter bAdapter=bluetoothAdapter[0];
			serverSocket=bAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
			Log.i(TAG, "serversocket in created");

		} catch (IOException e) 
		{
			Log.e(TAG, "serversocket in create failed", e);
		}

	//	while(true) /*always listen*/ /*da provare service vero, che attiva una sola istanza di bluetooth taskin con while true, e fa discovery ogni x seconds*/
	//	{
			try 
			{ 
				socket=serverSocket.accept();
				Log.i(TAG, "accepted connection");
			} 
			catch (IOException e) 
			{
				Log.e(TAG, e.getMessage());
			}
			if (socket != null)
			{
				// Do work to manage the connection (in a separate thread)
				Log.i(TAG, "start thread in");
				new ThreadIn(socket,context).start();
			}

	//	}
			
		return null;
		
	}
	
}
class ThreadIn extends Thread
{
	BluetoothSocket socket;
	Context context;
	public ThreadIn(BluetoothSocket socket,Context context) {
		this.socket=socket;
		this.context=context;
	}
	public void run() {
		InputStream is = null;
		try 
		{
			is = socket.getInputStream();			
		} 
		catch (IOException e) 
		{
			Log.e(BluetoothTaskIn.TAG,e.getMessage()+" error");
		}
		Log.i(BluetoothTaskIn.TAG, "istream created");
		ObjectInputStream objStream = null;
		try 
		{
			objStream = new ObjectInputStream(is);
			Log.i(BluetoothTaskIn.TAG, "objin stream created");
		} 
		catch (StreamCorruptedException e1) 
		{
			Log.e(BluetoothTaskIn.TAG,e1.getMessage()+" error");
		} 
		catch (IOException e1) 
		{
			Log.e(BluetoothTaskIn.TAG,e1.getMessage()+" error");
		}
	
		IAlgorithmMerge algorithm=AlgorithmMergeFactory.getAlgorithmImpl();
		RoadListItem[] tableReceived=null;
		try 
		{ 
			Object received=objStream.readObject(); 		
			if(received!=null)
				tableReceived=(RoadListItem[])received; 
		} 
		catch (OptionalDataException e) 
		{
			Log.e(BluetoothTaskIn.TAG,e.getMessage()+" error");
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) 
		{
			Log.e(BluetoothTaskIn.TAG,e.getMessage()+" error");
			e.printStackTrace();
		} 
		catch (IOException e) 
		{ 
			Log.e(BluetoothTaskIn.TAG,e.getMessage()+" error");
			e.printStackTrace();
		}
		String tableString="";
		for(int i=0;i<tableReceived.length;i++)
			tableString+=tableReceived[i].getRoad()+" ";
		Log.i(BluetoothTaskIn.TAG, tableString);
		
		if(tableReceived!=null)
			algorithm.calculate(tableReceived, context);
		
		
		context.sendBroadcast(new Intent(BluetoothTaskIn.BLUETOOTH_RECEIVED_DATA)); /*update MAP markers  MarkerDataBroadcastReceiver class*/
		context.sendBroadcast(new Intent(BluetoothService.DATA_RECEIVE_VIA_BLUETOOTH)); /*show some information in UI */

	
		return;
	}
	 
}