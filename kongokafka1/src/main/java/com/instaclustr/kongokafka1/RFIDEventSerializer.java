package com.instaclustr.kongokafka1;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

/**
 * (De)serializes RFIDEvent objects from/to strings.
 */

public class RFIDEventSerializer implements Closeable, AutoCloseable, Serializer<RFIDEvent>, Deserializer<RFIDEvent> {
	
    public static final Charset CHARSET = Charset.forName("UTF-8");

    @Override
    public void configure(Map<String, ?> map, boolean b) {
    }
    
    @Override
    public byte[] serialize(String s, RFIDEvent rfid) {
    		String line =
    				rfid.load + ", " +
    				rfid.time + ", " +
    				rfid.warehouseKey + ", " +
    				rfid.goodsKey + ", " +
    				rfid.truckKey;
        return line.getBytes(CHARSET);
    }
	
    @Override
    public RFIDEvent deserialize(String topic, byte[] bytes) {
        try {
            String[] parts = new String(bytes, CHARSET).split(", ");

            int i = 0;
            
            boolean t = Boolean.parseBoolean(parts[i++]);
            long time = Long.parseLong(parts[i++]);
            String warehouseKey = parts[i++];
            String goodsKey = parts[i++];
            String truckKey = parts[i++];
            
            // 	Must use args in correct order: public RFIDLoadEvent(long time, String goodsKey, String warehouseKey, String truckKey)
            return new RFIDEvent(t, time, goodsKey, warehouseKey, truckKey);
        }
        catch(Exception e) {
            throw new IllegalArgumentException("Error reading bytes", e);
        }
    }

    @Override
    public void close() {

    }
}