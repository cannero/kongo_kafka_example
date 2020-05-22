package com.instaclustr.kongokafka1;

/*
 * Top level main program to run Kongo simulation
 * This is the version for the Kongo blog part 3, Kafkafying Kongo: Serialization & one or many topics.
 * Options are:
 * Simulate.oneTopic = true then create one sensor topic with a single consumer, else create multiple sensor topics with multiple consumers (1 per Goods)
 * Simulate.kafkaRFIDConsumerOn = true then create/use Kafka RFID consumers, else use EventBus handlers for rfid events.
 * Simulate.kafkaSensorConsumerOn = true, only used in Simulate, if true create/use Kafka sensor consumers, else use EventBus handlers for sensor events.
 * 
 */


public class KafkaRun
{
	
	 public static void main(String[] args)
	 {
	     System.out.println("Welcome to the Instaclustr KONGO IoT Demo Application for Kafka!");
	     	     
	     if (Simulate.oneTopic)
	    	 	System.out.println("MODE = one topic with single consumer");
	     else
	    	 	System.out.println("MODE = multiple topics, one per location, each Goods subscribed to location topic");

	     // if we create > 1 consumer make sure that the number of partitions is >= numSensorConsumers
	     // num partitions for topic is printed by SensorConsumer but no error handling performed at present. In theory you may want more consumers than topics to have some as spares.
	     
	     if (Simulate.oneTopic)
	     {
	    	 	// how many concurrent Kafka sensor consumers
	    	 	int numSensorConsumers = 3;
	    	 	System.out.println("Starting numSensorConsumers=" + numSensorConsumers);
	    	 	
	    	 	for (int i=0; i < numSensorConsumers; i++) {
	    	 		SensorConsumer sensorConsumer = new SensorConsumer(Simulate.kafkaSensorTopicBase);
	    	 		sensorConsumer.start();
	    	 	}
	     }
	     
	     // Otherwise code in Simulate makes SensorGroupConsumer objects, 1 per Goods.
	     // Note that this won't work unless both Unload and load consumers are working!
	     
	     if (Simulate.kafkaRFIDConsumerOn)
	     {
	    	 	// create RFID consumers here
	    	 /*
	    	  * This was the old code which sometimes has errors due to ordering problems.
	    	  * 
	    	 	System.out.println("Started kafka unload consumer!");
	    	 	RFIDUnloadEventConsumer unloadConsumer = new RFIDUnloadEventConsumer(Simulate.unloadTopic);
	    	 	unloadConsumer.start();
	    	 	
	    	 	System.out.println("Started kafka load consumer!");
	    	 	RFIDLoadEventConsumer loadConsumer = new RFIDLoadEventConsumer(Simulate.loadTopic);
	    	 	loadConsumer.start();
	    	 	*/
	    	 
	    	 	// Mew code: create single RFID topic to ensure event order
	    	 	RFIDEventConsumer rfidConsumer = new RFIDEventConsumer(Simulate.rfidTopic);
	    	 	rfidConsumer.start();
	     }
	     
	     
	     // Start simulation which produces event streams
	    	 Simulate simulateThread = new Simulate();
	    	 simulateThread.start();
	    	 
	    	 System.out.println("Kafka Run has started Kong!");
	 }
}