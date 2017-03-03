package kmi.taa.wikidata;

import java.util.ArrayList;
import java.util.HashMap;

public class ResponseResults {
	private ArrayList<ResponseBindings> bindings;
	
	public ArrayList<ResponseBindings> getBindings() {
		return bindings;
	}

public class ResponseBindings {
	private HashMap<String, String> p;
	private HashMap<String, String> o;
	
	public HashMap<String, String> getP() {
		return p;
	}
	
	public HashMap<String, String> getO() {
		return o;
	}
}



	
}


