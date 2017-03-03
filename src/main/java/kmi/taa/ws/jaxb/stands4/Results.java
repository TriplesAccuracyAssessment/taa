package kmi.taa.ws.jaxb.stands4;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "results")
@XmlAccessorType (XmlAccessType.FIELD)
public class Results {	
	
	@XmlElement(name = "result")
	private ArrayList<Result> mresults = null;
		
	public ArrayList<Result> getResults() {
		return mresults;
	}
	
	public void setResults(ArrayList<Result> results) {
		this.mresults = results;
	}
		
}
