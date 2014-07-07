package com.example.vanetapp;


import it.unibo.vanetapp.services.BluetoothService;
import it.unibo.vanetapp.utility.BluetoothTaskIn;
import it.unibo.vanetapp.utility.BluetoothTaskOut;

import java.util.ArrayList;
import java.util.Set;

import com.actionbarsherlock.app.SherlockActivity;
import com.example.vanetapp.R;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
public class BluetoothListActivity extends Activity {

	public static final int REQUEST_ENABLE_BT = 0;
	private ListView btList;
	private ArrayAdapter<String> btAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private Button scanButton;
	private Context context;
	private static final String TAG="Bluetooth socket";
	public static ProgressDialog dialogBarBluetooth; // not used
	public static Handler handler;
	private ProgressDialog dialogBluetoothTaskOut;
//	public static String DATA_RECEIVE_VIA_BLUETOOTH="com.example.vanetapp.data_receive_via_bluetooth_action";
	/**
	 * BluetoothListActivity represent the entry-point to exchange traffic data between the devices via bluetooth, statically.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_list);
		context=this;
		btList=(ListView) findViewById(R.id.btListView);
		btAdapter=new ArrayAdapter<String>(this,R.layout.row_layout_bt);
		btList.setAdapter(btAdapter);
		scanButton=(Button) findViewById(R.id.scanbutton);
		scanButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				doDiscovery();
				btAdapter.clear();
				v.setEnabled(false);
			}
		});
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		ensureDiscoverable();	
		
		
		if (mBluetoothAdapter == null)
		{
		    Toast.makeText(this, "bluetooth not supported", Toast.LENGTH_SHORT).show();
		}
		if (!mBluetoothAdapter.isEnabled()) 
		{
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else
		{
			new BluetoothTaskIn(context).execute(mBluetoothAdapter);
			
		}

		getAlreadyKnownDevices();
		btList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
			{
				synchronized(this) //serialize the operation to avoid that, if user click more then once, to use to much resources
				{
			//		v.setEnabled(false);
			//		v.setClickable(false);
					
					mBluetoothAdapter.cancelDiscovery();
					// Cancel discovery because it's costly and we're about to connect
	
		            // Get the device MAC address, which is the last 17 chars in the View
		            String info = ((TextView) v).getText().toString();
		            String address = info.substring(info.length() - 17);
		            BluetoothDevice device=mBluetoothAdapter.getRemoteDevice(address);
		   
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	    				new BluetoothTaskOut(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device);
					else
						new BluetoothTaskOut(context).execute(device);
				} 
	            
			}
			
		});
		// Register the BroadcastReceiver
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothService.DATA_RECEIVE_VIA_BLUETOOTH);
		filter.addAction(BluetoothTaskOut.NUMBER_OF_DATA_TO_SEND);
		filter.addAction(BluetoothTaskOut.INCREMENT_PROGRESS_DIALOG);
		filter.addAction(BluetoothTaskOut.DISMISS_DIALOG);
		registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy/*/
	}
	@Override  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) 
		{
		case REQUEST_ENABLE_BT:
		{
			if(resultCode==RESULT_OK)
				new BluetoothTaskIn(context).execute(mBluetoothAdapter);
			break;
		}
		default:
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_bluetooth_list, menu);
		return true;
	}
	private void ensureDiscoverable() {
		Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}
	private void doDiscovery() {
		

		// Indicate scanning in the title
		setProgressBarIndeterminateVisibility(true);
		setTitle("Scanning for new devices, please wait");


		// If we're already discovering, stop it
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		mBluetoothAdapter.startDiscovery();
	}
	private void getAlreadyKnownDevices()
	{
	//	ArrayList<String> app=new ArrayList<String>();
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();	
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		    	btAdapter.add(device.getName()+" "+device.getAddress());
		    }
		}
		
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		// Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
		unregisterReceiver(mReceiver);
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context c, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) 
	        {
	             BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	             btAdapter.add(device.getName() + "\n" + device.getAddress());
	             Toast.makeText(context, "new device "+device.getName() + "\n" + device.getAddress(), Toast.LENGTH_SHORT).show();
	        }
	        if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
	        {
	        	setProgressBarIndeterminateVisibility(false);
	        	setTitle("BluetoothListActivity");
	        	scanButton.setEnabled(true);
	        	
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
	        }
	        if(BluetoothTaskOut.NUMBER_OF_DATA_TO_SEND.equals(action))
	        {
	        	dialogBluetoothTaskOut=new ProgressDialog(context);
	        	dialogBluetoothTaskOut.setCustomTitle(LayoutInflater.from(context).inflate(R.layout.custom_dialog_bluetooth, null));
	        	dialogBluetoothTaskOut.setIndeterminate(false);
	        	dialogBluetoothTaskOut.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        	dialogBluetoothTaskOut.setMessage("Sending traffic data");
	        	dialogBluetoothTaskOut.show();
	        	int max=intent.getIntExtra("maxData", 1);
	        	dialogBluetoothTaskOut.setMax(max);
	        }
	        if(BluetoothTaskOut.INCREMENT_PROGRESS_DIALOG.equals(action))
	        {
	        	dialogBluetoothTaskOut.incrementProgressBy(1);
	        }
	        if(BluetoothTaskOut.DISMISS_DIALOG.equals(action))
	        {
	        	dialogBluetoothTaskOut.dismiss();
	        }
	    }
	};


}
