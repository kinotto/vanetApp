package it.unibo.vanetapp.model.interfaces;

import android.content.Context;


public interface IAlgorithmTraffic 
{
	/**
	 * criterion for calculate the traffic level of a specific road knowing the acceleration and the orientation of the phone
	 * @return
	 */
	

	int calculate(Context context, float[] lastAcceleration ,float[] delta_orientation);



}
 