/* Kongo IoT Application Simulation.
 * main() creates the world and calls loop to run the simulation for desired number of "hours" (turns).
 * loop does all the work, unloads Goods (generated RFID unload events), loads Goods (generate RFID load events), moves trucks, generates sensor stream events and checks for Goods/sensor rules violations.
 * 
 * Version 1.0: Paul Brebner, Instaclustr.com, February 2018
 * 
 * This is a simplistic stand-alone monolithic version which combines the simulation and rules checking, and is not particularly efficient or scalable. 
 */


package kongo1;
import java.util.*;


public class Simulate {

	// Turn rules on or off for Goods movement control. Even with rules turned on there may be some violations as some things are random (e.g. truck accelerate and vibrations).	
	static boolean enforceTempRules = false;			// enforce goods temperature category rules for movements from/to trucks/warehouses
	static boolean enforceHazardousRules = false;	// enforce goods co-location rules when loading trucks
	static boolean checkGoods = true; 				// turn on/off co-location rules checking during simulation
	
	static boolean debug = false;
	static boolean verbose = true;
	
	// global data. all goods in the system (in trucks or warehouses)
	static HashMap<String, Goods> allGoods = new HashMap<String, Goods>();
	
	// all warehouses
	static HashMap<String, Warehouses> allWarehouses = new HashMap<String, Warehouses>();
	
	// all goods that are in warehouses
	static HashMap<String, String> goodsInWarehouses = new HashMap<String, String>();
	
	// all goods that are in trucks
	static HashMap<String, String> goodsInTrucks = new HashMap<String, String>();

	// all trucks
	static HashMap<String, Trucks> allTrucks = new HashMap<String, Trucks>();
	
	// trucks at each warehouse
	static HashMap<String, String> trucksAtWarehouses = new HashMap<String, String>();
	
	// truckKey and keep1 are hacks used in Goods loading code in the simulation loop.
	static String truckKey;
	
	// keep truck key first time, and then randomly 50% of time for others
	public static void keep1(String s)
	{
		if (truckKey == null || rand.nextBoolean())
			truckKey = s;	
	}
	
	static Random rand = new Random();
	
	// simulation loop, simulates Goods and Trucks movement for required number of rounds (hours)
	// assumes everything has been created already.
	public static void loop(int hours)
	{
		long t0 = System.currentTimeMillis();
		long totalEvents = 0;
		
		// repeat for hours
		System.out.println("Simulation started");
		
		// loop the loop
		for (int time=0; time < hours; time++)
		{
			System.out.println("************** Time = " + time);
			
			// 1 UNLOAD trucks: move goods from trucks to warehouse where truck docked
			if (debug) System.out.println("Unloading goods from Trucks...");
			
			// Unload all Goods that are in trucks
			Iterator<String> it = goodsInTrucks.keySet().iterator();
			while (it.hasNext())
			{
			    String goodsKey = it.next();
			    String trucksKey = goodsInTrucks.get(goodsKey);
			    			    
			    // find warehouse where the truck is
			    String warehouse = trucksAtWarehouses.get(trucksKey);
			    // change location of goods to warehouse
			    goodsInWarehouses.put(goodsKey, warehouse);
			    if (debug) System.out.println("Unloaded " + goodsKey + " from " + trucksKey + " at " + warehouse); 
			    
			    // generate UNLOAD RFID event
			    String s = time + " RFID " + warehouse + ": UNLOAD " +  goodsKey + " from " + trucksKey;
			    if (verbose) System.out.println(s);
			    
			    totalEvents++;
			    it.remove();
			    // forget categories of loaded goods
			    Trucks t = allTrucks.get(trucksKey);
			    t.resetCats();
			}
			
			// 2 LOAD Goods from warehouse to trucks currently docked at warehouse
			
			if (debug) System.out.println("Loading goods onto trucks");
			it = goodsInWarehouses.keySet().iterator();
			while (it.hasNext())
			{
				String goodsKey = it.next();
				String warehouseKey = goodsInWarehouses.get(goodsKey);
			    
			    // randomly decide if we want to load this good, check if there is a truck at the warehouse, load it, remove it from warehouse
				if (debug) System.out.println("Found goods " + goodsKey + " in " + warehouseKey + " try and load it? ");
			    if (rand.nextDouble() > 0.5)
			    {
			    	    truckKey = null;
			    		Map<String, String> map = trucksAtWarehouses;
			    	    map.entrySet()
			    	    .stream()
			    	    .filter(x -> x.getValue().equals(warehouseKey))
			    	    // hack need to pick 1 truck at random
			    	    .forEach(x -> keep1(x.getKey()));
			   
			    	   // if a truck was found...
			    	   if (truckKey != null)
			    	   {
			    		   if (debug) System.out.println("Found a truck at warehouse " + truckKey);
			    		   // load truck, remove goods from warehouse
			    		   Trucks t = allTrucks.get(truckKey);
			    		   Goods g = allGoods.get(goodsKey);
			    		   // can we load the Goods onto it?
			    		   boolean load = false;
			    		   load = g.allowedInTruck(t.categoriesOnBoard);
			    		
			    		   if (verbose && load) System.out.println("Goods allowed in truck, goods cats=" + g.categories + " no conflict with truck cats=" + t.categoriesOnBoard.allCategories());
			    		   else if (verbose && !load) System.out.println("Goods NOT ALLOWED in truck, goods cats=" + g.categories + " conflict with truck cats=" + t.categoriesOnBoard.allCategories());
			    		
			    		   // keep loading if we can load it or we don't care about enforcing rules
			    		   if (!enforceHazardousRules || enforceHazardousRules && load)
			    		   {
			    			   // check temperature control rules
			    			   if (load = g.truckTempRules(t))
			    				   if (verbose) System.out.println("Goods allowed on truck for temperature rules check");
			    				   else if (verbose) System.out.println("Goods NOT ALLOWED on truck for temperature rules check");
				    		
			    			   if (!enforceTempRules || enforceTempRules && load)
			    			   {
			    				   t.updateCategories(g);
			    				   goodsInTrucks.put(goodsKey, truckKey);
			    				   it.remove();
			    				   if (debug) System.out.println("Loading " + goodsKey + " onto " + truckKey);
				    	
			    				   // generate RFID LOAD event
			    				   String s = time + " RFID " + warehouseKey + ": LOAD " +  goodsKey + " onto " + truckKey;
			    				   
			    				   if (verbose) System.out.println(s);
			    				   totalEvents++;
			    			   }
			    		   }
			    	   }
			    }
			}
			
			// 3 Move TRUCKS
			
			// create shuffled list of warehouses to select destination warehouse from
			List<String> keyList = new ArrayList<String>(allWarehouses.keySet());
			Collections.shuffle( keyList );
			Iterator<String> randKeys = keyList.iterator();
			
			for (Map.Entry<String, String> entry : trucksAtWarehouses.entrySet())
			{
				String truckKey = entry.getKey();
				Trucks truck = allTrucks.get(truckKey);
				String currentLoc = entry.getValue();
				String destination = null;;
				
				// find a warehouse with a compatible temperature control
				Warehouses w = null;
				while (destination == null)
				{
					if (randKeys.hasNext())
						destination = randKeys.next();
					else
					{
						// else start again
						randKeys = keyList.iterator();
						destination = randKeys.next();
					}
					// does destination warehouse have compatible climate control to the truck temp control?
					w = allWarehouses.get(destination);
					if (!enforceTempRules || truck.canDeliverToWarehouse(w))
						break;
					// no good so keep looking
					else destination = null;
				}
				
				trucksAtWarehouses.put(truckKey, destination);
				
				if (verbose) System.out.println(time + " Truck " + truckKey + " temp cat=" + truck.tempRange + " moving from " + currentLoc + " to " + destination + " with temp cat=" + w.tempRange);
			}
			
			// 4 SENSOR stream, simple version, each warehouse and truck produce only out one value per sensor metric per location per hour
			// This is a very inefficient implementation as it checks rules for all goods in each truck every sensor event
			Sensor sensor;
			
			// Truck SENSOR stream
			for (String truckskey : allTrucks.keySet())
			{
				Trucks truck = allTrucks.get(truckskey);
				
				sensor = new Sensor(time, "SENSOR TRUCK", truckskey, "temp", truck.temp.randomTempInRange());
				if (verbose) sensor.print();
				checkGoodsInTruck(truckskey, sensor);
				
				sensor = new Sensor(time, "SENSOR TRUCK", truckskey, "humidity", randBetween(0, 100));
				if (verbose) sensor.print();
				checkGoodsInTruck(truckskey, sensor);
				
				// lux https://en.wikipedia.org/wiki/Lux range 0 - 100,000 (direct sunlight), 500 is office lighting, unit is lux
				sensor = new Sensor(time, "SENSOR TRUCK", truckskey, "illuminance", randBetween(0, 100000));
				if (verbose) sensor.print();
				checkGoodsInTruck(truckskey, sensor);
				
				// acceleration, in standard gravities i.e. 0, 1, 100? Normal should be < 1g? fast car accel if about 0.5g
				// roller coaster is 3-4g
				// car https://physics.info/acceleration/ F1 could be up to 3g! A truck should be < 1g
				sensor = new Sensor(time, "SENSOR TRUCK", truckskey, "acceleration", randBetween(0, 100));
				if (verbose) sensor.print();
				checkGoodsInTruck(truckskey, sensor);
				
				// vibration has amplitude and frequency (but sensors produce data for multiple frequencies!)
				// freq is Hz (0-100000), amp is ms-2 (0-?)
				sensor = new Sensor(time, "SENSOR TRUCK", truckskey, "vibrationDisplacement", randBetween(0, 1000));
				if (verbose) sensor.print();
				checkGoodsInTruck(truckskey, sensor);
				
				sensor = new Sensor(time, "SENSOR TRUCK", truckskey, "vibrationVelocity", randBetween(0, 1000));
				if (verbose) sensor.print();
				checkGoodsInTruck(truckskey, sensor);
				
				totalEvents += 6;
			}
			
			// Warehouse SENSOR stream
			for (String warehouseKey : allWarehouses.keySet())
			{
				
				Warehouses warehouse = allWarehouses.get(warehouseKey);
				
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "temp", warehouse.temp.randomTempInRange());
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "humidity", randBetween(0, 100));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "illuminance", randBetween(0, 100000));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				// Nasty gases: ozone, particulate matter, toxic gas (Propane, Butane, LPG and Carbon Monoxide.), sulfur dioxide, and nitrous oxide
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "ozone", randBetween(0, 10000));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
			
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "particles", randBetween(0, 10000));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "toxicGas", randBetween(0, 10000));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "sulfurDioxide", randBetween(0, 10));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				sensor = new Sensor(time, "SENSOR WAREHOUSE", warehouseKey, "nitrousOxides", randBetween(0, 10));
				if (verbose) sensor.print();
				checkGoodsInWarehouse(warehouseKey, sensor);
				
				totalEvents += 8;
			}
		}
		
		System.out.println("Simulation ended");
		long t1 = System.currentTimeMillis();
		double duration = (t1 - t0)/1000.0;
		System.out.println("Simulation duration (s) = " + duration);
		double eventsSec = totalEvents/duration;
		System.out.println("Events = " + totalEvents + ". Rate (Events/s) = " + eventsSec);
	}
	
	// for every goods in this truck check all sensor rules against this new sensor value and print warning
	private static void checkGoodsInTruck(String truckskey, Sensor sensor)
	{
		if (!checkGoods)
			return;
		
		for (String goodsKey: goodsInTrucks.keySet())
		{
			// which truck is this goods in?
			String truck = goodsInTrucks.get(goodsKey);
			// same location as sensor data?
			if (truck.equals(truckskey))
			{
				Goods goods = allGoods.get(goodsKey);
				String v = goods.violatedSensorCatRules(sensor);
				if (!v.equals(""))
					System.out.println("SENSOR RULE VIOLATION for goods=" + goodsKey + " " + goods.allCategories() + " in truck " + truckskey + " violations: " + v);
			}
		}
	}
	
	// for every goods in warehouse check all sensor rules against new sensor value and print warning
	private static void checkGoodsInWarehouse(String warehousekey, Sensor sensor)
	{
		if (!checkGoods)
			return;
		
		for (String goodsKey: goodsInWarehouses.keySet())
		{
			// which warehouse is this goods in?
			String warehouse = goodsInWarehouses.get(goodsKey);
			// same location as sensor data?
			if (warehouse.equals(warehousekey))
			{
				Goods goods = allGoods.get(goodsKey);
				String v = goods.violatedSensorCatRules(sensor);
				if (!v.equals(""))
					System.out.println("SENSOR RULE VIOLATION for goods=" + goodsKey + " " + goods.allCategories() + " in warehouse " + warehousekey + " violations: " + v);
			}
		}
	}

	public static double randBetween(double min, double max)
	{
		return (rand.nextDouble() * (max-min)) + min;
	}

	public static void main(String[] args) 
	{		
		
		// Parameters, how many Goods, warehouse locations and trucks, and hours to run simulation.
		int numGoods = 1000;
		int maxX = 10;
		int maxY = 10;
		int numWarehouses = maxX * maxY;
		int numTrucks = numWarehouses*2;
		int loops = 10;
	
		// CREATION
		// create random Goods in a hashMap
		for (int i = 0; i < numGoods; i++)
		{
			 Goods g = new Goods();
			 allGoods.put(g.tag, g);
	         String s = g.toStr();
	         System.out.println(s);
		}
	
		System.out.println("Goods created = " + numGoods);
	
		// create warehouses
	
		String aWarehouse = null;
	
		for (int x = 0; x < maxX; x++)
		{
			for (int y = 0; y < maxY; y++)
			{
				Warehouses w = new Warehouses(x, y);
				if (aWarehouse == null)
					aWarehouse = w.id;
				allWarehouses.put(w.id, w);
				String s = w.toStr();
				System.out.println(s);
			}
		}
	
		System.out.println("Warehouses created = " + maxX*maxY);
	
		// find warehouses with compatible environmental controls to put Goods in
		for (String goodskey : allGoods.keySet())
		{
			Goods g = allGoods.get(goodskey);
			boolean found = false;
			for (String warehouseKey: allWarehouses.keySet())
			{
				Warehouses w = allWarehouses.get(warehouseKey);
				if (g.warehouseTempRules(w))
				{
					goodsInWarehouses.put(goodskey, warehouseKey);
					found = true;
					break;
				}
			}
			// can't find anywhere just put goods in 1st warehouse
			if (!found) goodsInWarehouses.put(goodskey, aWarehouse);
		}
	
		for (Map.Entry<String, String> entry : goodsInWarehouses.entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			System.out.println(key + " in " + value);
		}
	
		System.out.println("Goods locations created");

		// Create Trucks to move Goods around
		for (int i = 0; i < numTrucks; i++)
		{
			 Trucks t = new Trucks();
			 t.resetCats();
			 allTrucks.put(t.id, t);
	         String s = t.toStr();
	         System.out.println(s);
		}
		
		System.out.println("Trucks created");
	
		// set Trucks locations to warehouses
		Iterator it = allWarehouses.keySet().iterator();
		String warehousekey;
		for (String truckskey : allTrucks.keySet())
		{
			if (it.hasNext())
				warehousekey = (String) it.next();
			else
				warehousekey = aWarehouse;	// else use first warehouse
			trucksAtWarehouses.put(truckskey, warehousekey);
		}
		
		for (Map.Entry<String, String> entry : trucksAtWarehouses.entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			System.out.println(key + " at " + value);
		}
	
		System.out.println("Truck locations created");
	
		// Run the simulation for loops hours
		loop(loops);
	}
}
