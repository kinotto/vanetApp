package it.unibo.vanetapp.services;

import it.unibo.vanetapp.utility.BluetoothTaskIn;
import it.unibo.vanetapp.utility.BluetoothTaskOut;

import com.example.vanetapp.MainActivity;
import com.example.vanetapp.R;
import com.example.vanetapp.TrafficListActivity;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewDebug.FlagToString;
import android.widget.Toast;

public class BluetoothService extends Service
{

	private final Context context;
	private BluetoothAdapter bluetoothAdapter;
	public static final String DATA_RECEIVE_VIA_BLUETOOTH="it.unibo.vanetapp-services.data_receive_via_bluetooth_action";

	public static final String TAG="BluetoothService";

	private final BluetoothReceiver mReceiver;


	public BluetoothService()
	{
		context=MainActivity.context;
		mReceiver=new BluetoothReceiver(context);
		bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter == null)
			Toast.makeText(context, "bluetooth not supported", Toast.LENGTH_SHORT).show();

	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		ensureDiscoverableBluetooth();
		
		Log.i(TAG, "start bluetoothRealService");
		new BluetoothTaskIn(context).execute(bluetoothAdapter);

		IntentFilter i=new IntentFilter();
		i.addAction(BluetoothDevice.ACTION_FOUND);
		i.addAction(DATA_RECEIVE_VIA_BLUETOOTH);
		registerReceiver(mReceiver, i);

		doDiscovery();

	}

	@Override
	public IBinder onBind(Intent arg0) 
	{		
		return null;
	}

	private void doDiscovery()  
	{
		// If we're already discovering, stop it
		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		bluetoothAdapter.startDiscovery();
		Toast.makeText(context, "start discovering devices", Toast.LENGTH_SHORT).show();

	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (bluetoothAdapter != null)
		{
			bluetoothAdapter.cancelDiscovery();
		}
		unregisterReceiver(mReceiver);
		Log.i(TAG, "bluetoothService destroyed");
	}

	private void ensureDiscoverableBluetooth() {

		if (bluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(discoverableIntent);
		} 
	}

class BluetoothReceiver extends BroadcastReceiver
{
	Context context;
	public BluetoothReceiver(Context context) 
	{
		this.context=context;

	}
	public void onReceive(Context c, Intent intent) {
		String action = intent.getAction();
		// When discovery finds a device
		if (BluetoothDevice.ACTION_FOUND.equals(action)) 
		{
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Toast.makeText(context, "new device "+device.getName() + "\n" + device.getAddress()+": sending data", Toast.LENGTH_SHORT).show();
			//start sending data
			Log.i(BluetoothService.TAG, "send data to "+device.getName()+" "+device.getAddress());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				new BluetoothTaskOut(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device);
			else
				new BluetoothTaskOut(context).execute(device);



		}
		if(BluetoothService.DATA_RECEIVE_VIA_BLUETOOTH.equals(action))
		{

			final AlertDialog.Builder builder=new AlertDialog.Builder(context);
			builder.setCustomTitle(LayoutInflater.from(context).inflate(R.layout.custom_dialog_bluetooth_post, null));
			builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					context.startActivity(new Intent(context,TrafficListActivity.class));


				}
			});
			builder.setNegativeButton("no", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface v, int arg1) {
					v.dismiss();

				}
			});
			
			builder.show();
			stopSelf();

		}

	}
}
}
