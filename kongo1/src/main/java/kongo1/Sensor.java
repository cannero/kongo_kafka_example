

/*
 * Sensor objects are for warehouse and truck environmental metric values.
 */
package kongo1;
public class Sensor {  

	long time;
	String doc;
	String tag;
	String metric;
	double value;

	public Sensor(long time, String doc, String tag, String metric, double value) {
		this.time = time;
		this.doc = doc;
		this.tag = tag;
		this.metric = metric;
		this.value = value;
	}
	
	public void print()
	{
		System.out.println(time + ", " + doc + ", " + tag + ", " + metric + "=" + value);
	}
}
