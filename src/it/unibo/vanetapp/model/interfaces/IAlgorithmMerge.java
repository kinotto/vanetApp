package it.unibo.vanetapp.model.interfaces;

import com.example.vanetapp.RoadListItem;

import android.content.Context;


public interface IAlgorithmMerge 
{/**
	 * criterion for merge records of different traffic tables 
	 * @return
	 */
	public void calculate(RoadListItem[] roadList,Context context);
}
