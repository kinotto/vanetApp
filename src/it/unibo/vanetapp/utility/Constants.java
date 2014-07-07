package it.unibo.vanetapp.utility;

public final class Constants 
{
	
	public static final int THRESHOLD_LOW=4; // acceleration threshold in m/s^2
	public static final int THRESHOLD_MEDIUM=8; // acceleration threshold in m/s^2
	public static final int THRESHOLD_HIGH=10; // acceleration threshold in m/s^2
	
	
	public static final int TRAFFIC_LOW=0; //constant that represent low traffic
	public static final int TRAFFIC_MEDIUM=1; //constant that represent medium traffic
	public static final int TRAFFIC_HIGH=2; //constant that represent high traffic
	
	public static final float THRESHOLD_CURVE= 2; //acceleration threshold used for x and y axis, useful to determine whether the acceleration was caused by a curve 
	
	public static final long SECOND_PER_DAY = 24 * 60 * 60; // used to calculate the timestamp of each records //
	

}
