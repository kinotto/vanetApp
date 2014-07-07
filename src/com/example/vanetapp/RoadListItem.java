package com.example.vanetapp;

import java.io.Serializable;

public class RoadListItem implements Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String road;
	private int timestamp;
	private int photoId;
	private int trafficLevel;
	private int photoTrafficId;
	public RoadListItem(String road,int timestamp,int photoId,int phototrafficid,int trafficLevel) /* this constructor is used in TrafficListActivity to fill the data in the listview*/
	{
		super();
		this.road=road;
		this.timestamp=timestamp;
		this.photoId=photoId;
		this.photoTrafficId=phototrafficid;
		this.trafficLevel=trafficLevel;
	}
	
	public RoadListItem(String road,int timestamp,int trafficLevel) /* this constructor is used when the table of traffic is sent via ad oc network*/
	{
		super();
		this.road=road;
		this.timestamp=timestamp;
		this.trafficLevel=trafficLevel;
	}
	
	public int getPhotoId() {
		return photoId;
	}
	public String getRoad() {
		return road;
	}
	public int getTimestamp() {
		return timestamp;
	}
	public int getPhotoTrafficId() {
		return photoTrafficId;
	}
	public int getTrafficLevel() {
		return trafficLevel;
	}
	
}
