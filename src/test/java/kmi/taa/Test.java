package kmi.taa;

import java.text.SimpleDateFormat;
import java.util.Date;

import kmi.taa.core.OutlierDetector;
import kmi.taa.core.PropertyTypeComparison;

public class Test {

	public static void main(String[] args) {
		OutlierDetector dt = new OutlierDetector();
		PropertyTypeComparison ptc = new PropertyTypeComparison();
		double seconds = ptc.DateToSeconds("1980-07-01");
		double d = Double.valueOf(seconds) * 1000; // convert to milliseconds
		Date dat = new Date((long) d);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
		System.out.println(sdf.format(dat));

	}

}
