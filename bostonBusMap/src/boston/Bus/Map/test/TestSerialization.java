package boston.Bus.Map.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import boston.Bus.Map.data.Path;
import boston.Bus.Map.data.Prediction;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.database.DatabaseHelper;
import boston.Bus.Map.util.Box;
import junit.framework.TestCase;


public class TestSerialization extends TestCase {
	public void testString() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		String x = null;
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		outputBox.writeString(x);
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		String string2 = inputBox.readString();
		
		assertEquals(x, string2);
	}
	public void testString2() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		String x = "A quick brown fox";
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		outputBox.writeString(x);
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		String string2 = inputBox.readString();
		
		assertEquals(x, string2);
	}
	public void testLong() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		long x = -4557498050202912686l;
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		outputBox.writeLong(x);
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		long string2 = inputBox.readLong();
		
		assertEquals(x, string2);
	}
	public void testInt() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		int x = -8455;
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		outputBox.writeInt(x);
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		int string2 = inputBox.readInt();
		
		assertEquals(x, string2);
	}
	
	public void testDouble() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		double x = -8455.34;
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		outputBox.writeDouble(x);
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		double string2 = inputBox.readDouble();
		
		assertEquals(x, string2);
	}
	
	public void testFloat() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		float x = -8455.88f;
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		outputBox.writeFloat(x);
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		float string2 = inputBox.readFloat();
		
		assertEquals(x, string2);
	}
	
	private void assertValid(Box outputBox) throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		RouteConfig routeConfig2 = new RouteConfig(inputBox, null);
		
		Box outputBox2 = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		routeConfig2.serialize(outputBox2);
		
		byte[] blob2 = outputBox2.getBlob();
		
		assertEquals(blob.length, blob2.length);
		
		for (int i = 0; i < blob.length; i++)
		{
			assertEquals(blob[i], blob2[i]);
		}
	}
	
	public void testBasic() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		RouteConfig routeConfig = new RouteConfig("x");
		
		routeConfig.addPath(1, 3, 4);
		routeConfig.addStop(5, new StopLocation(44.0, 55.0, null, 5, "xy", "ture"));
		routeConfig.addDirection("XYZSD", "akosod", "asodkosd");
		
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		routeConfig.serialize(outputBox);

		assertValid(outputBox);
	}
	public void testBasic2() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		RouteConfig routeConfig = new RouteConfig("x");
		
		routeConfig.addPath(1, 3, 4);
		routeConfig.addStop(5, new StopLocation(44.0, 55.0, null, 5, "xy", "ture"));
		//routeConfig.addStop(6, new StopLocation(47.0, 56.0, null, 5, "x", "tue", routeConfig));
		//routeConfig.addDirection("XYZSD", "akosod", "asodkosd");
		
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		routeConfig.serialize(outputBox);

		assertValid(outputBox);
	}
	
	public void testStringMap() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		HashMap<String, String> mapping = new HashMap<String, String>();
		
		mapping.put("Apple", "cranberry");
		mapping.put("avocado", "jellyfish");
		
		mapping.put("sea cucumber", null);
		
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		outputBox.writeStringMap(mapping);
		HashMap<String, String> newMapping = new HashMap<String, String>();
		
		Box inputBox = new Box(outputBox.getBlob(), DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		inputBox.readStringMap(newMapping);
		
		assertEquals(newMapping.size(), mapping.size());
		
		SortedSet<String> list1 = new TreeSet<String>(mapping.keySet());
		SortedSet<String> list2 = new TreeSet<String>(newMapping.keySet());
		Iterator<String> iterator1 = list1.iterator();
		Iterator<String> iterator2 = list2.iterator();
		
		while (iterator1.hasNext())
		{
			String key = iterator1.next();
			String key2 = iterator2.next();
			
			assertEquals(key, key2);
			
			String value1 = mapping.get(key);
			String value2 = newMapping.get(key);
			
			assertEquals(value1, value2);
		}
	}
	
	public void testStopLocation() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		StopLocation stopLocation = new StopLocation(44.6, -45.6, null, 3, "stop", "in");
		stopLocation.toggleFavorite();
		
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		stopLocation.serialize(outputBox);
		
		assertValidStopLocation(outputBox);
	}
	
	public void testPath() throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		Path stopLocation = new Path(3);
		
		Box outputBox = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		stopLocation.serialize(outputBox);
		
		assertValidPath(outputBox);
	}
	

	private void assertValidStopLocation(Box outputBox) throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		StopLocation routeConfig2 = new StopLocation(inputBox, null);
		
		Box outputBox2 = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		routeConfig2.serialize(outputBox2);
		
		byte[] blob2 = outputBox2.getBlob();
		
		assertEquals(blob.length, blob2.length);
		
		for (int i = 0; i < blob.length; i++)
		{
			assertEquals(blob[i], blob2[i]);
		}
	}
	
	private void assertValidPath(Box outputBox) throws IOException
	{
		HashMap<Integer, StopLocation> sharedStops = new HashMap<Integer, StopLocation>();
		
		byte[] blob = outputBox.getBlob();
		
		Box inputBox = new Box(blob, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		
		Path routeConfig2 = new Path(inputBox);
		
		Box outputBox2 = new Box(null, DatabaseHelper.CURRENT_DB_VERSION, sharedStops);
		routeConfig2.serialize(outputBox2);
		
		byte[] blob2 = outputBox2.getBlob();
		
		assertEquals(blob.length, blob2.length);
		
		for (int i = 0; i < blob.length; i++)
		{
			assertEquals(blob[i], blob2[i]);
		}
	}
	
}