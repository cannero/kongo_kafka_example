package com.instaclustr.kongokafka1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import com.google.common.eventbus.EventBus;

// This Kakfa consumer is just a wrapper for the Event bus topic really.

public class SensorConsumer extends Thread {
	  private final KafkaConsumer<String, Sensor> consumer;
	  
	  private final String topic;

	  public SensorConsumer(String topic)
	  {
	   
	    this.topic = topic;
	    
	 	Properties props = new Properties();    
	 	props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
	    props.put(ConsumerConfig.GROUP_ID_CONFIG, "KongoSensorConsumer");
	    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
	    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
	    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
	    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
	    String sensorSerializer = SensorSerializer.class.getName(); 
	    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, sensorSerializer);
	    
	    this.consumer = new KafkaConsumer<>(props);
	    
	    System.out.println("topic " + this.topic + " partitions=" + this.consumer.partitionsFor(this.topic).size());
	  }
	 
	  @Override
	  public void run()
	  {
	    try
	    {
	    		long threadId = Thread.currentThread().getId();
		    	consumer.subscribe(Collections.singletonList(this.topic));
		    	System.out.println(threadId + " ********* SensorConsumer subscribed to topic " + this.topic);
		    	
		    	// Experiment to keep track of gap 
		    	// https://stackoverflow.com/questions/38428196/how-can-i-get-the-latest-offset-of-a-kafka-topic/38448775
		    	// https://blog.knoldus.com/2017/11/05/case-study-to-understand-kafka-consumer-and-its-offsets/
		    			    	
		    	while (true)
		    {
		        ConsumerRecords<String, Sensor> records = consumer.poll(Long.MAX_VALUE);
		        
		        for (ConsumerRecord<String, Sensor> record : records)
		        {
	        	  		System.out.println(threadId + " ***** SensorConsumer records = " + records.count());
	        	  		System.out.println(threadId + " ***** SensorConsumer, Received message: (" + record.key() + ", " + record.value() + ") at offset " + record.offset());
	        	  		Sensor s = record.value();
	        	  		s.print();
	        	  		
	        	  		// find the topic corresponding to the warehouse location
	    				EventBus topic = Simulate.topics.get(s.tag);
	    				
	    				if (topic == null)
	    					System.out.println(threadId + " ***** SensorConsumer unable to find EventBus topic for " + s.tag);
	    				else
	    					topic.post(s);
	    				
	    				long lastOffset = record.offset();
	    				Set<TopicPartition> partitionsAssigned = consumer.assignment();
	    				Map<TopicPartition, Long> endOffsetsPartitionMap = consumer.endOffsets(consumer.assignment());
	    				Iterator iter = partitionsAssigned.iterator();
	    				TopicPartition first = (TopicPartition) iter.next();
	    				long consumerLag = endOffsetsPartitionMap.get(first) - lastOffset;
	    				System.out.println("Sensor consumer LAG = " + consumerLag);
	    				
		        }
		    }   
	    }
	    catch (WakeupException e) {
	    } finally {
	      consumer.close();
	    }
	  }

	  public void shutdown() {
	    consumer.wakeup();
	  }
	}