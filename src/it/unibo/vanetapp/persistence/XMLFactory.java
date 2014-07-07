package it.unibo.vanetapp.persistence;

import it.unibo.vanetapp.model.impl.StorageFactory;
import it.unibo.vanetapp.model.interfaces.IStorage;
import it.unibo.vanetapp.utility.Constants;

import java.io.File;
import java.io.IOException;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.example.vanetapp.TrafficListActivity;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Directory;
import android.util.Log;
import android.widget.ArrayAdapter;


public final class XMLFactory 
{
	public static final String ROAD="road";
	public static final String TIMESTAMP="timestamp";
	public static final String TRAFFICLEVEL="trafficlevel";
	public static final String DEFAULTFILEPATH="/data/data/com.example.vanetapp/myfiles";
	private static final String TAG="xmlfactory";

	/**
	 * This class realize a function really useful, allow the user to save the traffic informations collected, about for example particular route, and save them in xml
	 * 
	 * @param nodeList
	 * @param context
	 * @param fileNameRelative
	 * @return
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public static boolean createXML(ArrayList<String> nodeList,Context context,String fileNameRelative) throws ParserConfigurationException, TransformerException
	{
		IStorage storage=StorageFactory.getStorageSQLiteImpl(context);
		DocumentBuilderFactory docFactory=DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder=docFactory.newDocumentBuilder();
		
		Document doc= docBuilder.newDocument();
		
		Cursor c=storage.getAllRecords();
		c.moveToFirst();
		Element RootElement=doc.createElement("TrafficInfo");
		doc.appendChild(RootElement);
		while(!c.isAfterLast())
		{
			Log.i(TAG, "enter first cycle");
			Element XInfo=doc.createElement("Info");
			RootElement.appendChild(XInfo);
			for(int i=0;i<nodeList.size();i++)
			{
				Log.i(TAG, "enter second cycle");
				Element x=doc.createElement(nodeList.get(i));
				if(nodeList.get(i).equals(ROAD))
					x.appendChild(doc.createTextNode(c.getString(1)));
				if(nodeList.get(i).equals(TIMESTAMP))
					x.appendChild(doc.createTextNode(c.getInt(2)+""));
				if(nodeList.get(i).equals(TRAFFICLEVEL))
				{
					int trafficLevel=c.getInt(3);
					String traffic=trafficLevel == 0 ? "lowtraffic" : trafficLevel == 1 ? "mediumtraffic" : "hightraffic";
					x.appendChild(doc.createTextNode(traffic));
					
				}
				XInfo.appendChild(x);
			}
			c.moveToNext();
		}
		TransformToFile(doc,fileNameRelative);
		return true;
	}
	private static void TransformToFile(Document doc,String fileNameRelative) throws TransformerException
	{
		TransformerFactory factory=TransformerFactory.newInstance();
		Transformer transformer=factory.newTransformer();
		DOMSource domSource=new DOMSource(doc);
	//	Date now = Calendar.getInstance().getTime();
	//	int timePortion = (int) (now.getTime() % Constants.SECOND_PER_DAY);		
		File file=new File("/data/data/com.example.vanetapp/myfiles", fileNameRelative+".xml");

		file.getParentFile().mkdirs();
		StreamResult stream=new StreamResult(file);
		StreamResult streamSysOut=new StreamResult(System.out);
		
		transformer.transform(domSource, stream);
		transformer.transform(domSource, streamSysOut);
	}
	public static void ParseXML(String filePath,Context context) throws ParserConfigurationException
	{
		IStorage storage=StorageFactory.getStorageSQLiteImpl(context);
		DocumentBuilderFactory builderFactory=DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder=builderFactory.newDocumentBuilder();
		Document doc=null;
		try 
		{
			doc=documentBuilder.parse(new File(filePath));
		} 
		catch (SAXException e) 
		{
			e.printStackTrace();
			Log.i(TAG, e.getMessage());
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			Log.i(TAG, e.getMessage());
		}
		doc.getDocumentElement().normalize();
		Log.i(TAG, "root "+doc.getDocumentElement().getNodeName());
		NodeList infoList=doc.getElementsByTagName("Info");
		storage.deleteAllRecords(); /* remove all current records*/
		for(int i=0;i<infoList.getLength();i++)
		{
			Node Xnode=infoList.item(i); 
			NodeList nodeList=((Element)Xnode).getChildNodes();

			String road=((Element)(infoList.item(i))).getElementsByTagName(ROAD).item(0).getChildNodes().item(0).getNodeValue();
			int timestamp=Integer.parseInt(((Element)(infoList.item(i))).getElementsByTagName(TIMESTAMP).item(0).getChildNodes().item(0).getNodeValue());
			String traffic=((Element)(infoList.item(i))).getElementsByTagName(TRAFFICLEVEL).item(0).getChildNodes().item(0).getNodeValue();
			int trafficLevel=traffic.equals("lowtraffic") ? 0 : traffic.equals("mediumtraffic") ? 1 : 2;
			storage.insertRecord(road, timestamp, trafficLevel); 
			Log.i(TAG, road+" "+timestamp+" "+trafficLevel);

		}
	}
	/**
	 * this method is used to load traffic data (updating the ui and all the things connected to it) from xml file, in particular is used to show in a listview all 
	 * the files available
	 * @param customDialogAdapter
	 */
	public static void findAndShowAllXMLavailable(ArrayAdapter<String> customDialogAdapter)
	{
		File folder=new File(DEFAULTFILEPATH);
		folder.getParentFile().mkdirs();
		File[] filesAvailable=folder.listFiles();
		for(int i=0;i<filesAvailable.length;i++)
			customDialogAdapter.add(filesAvailable[i].getName());

	}
}
