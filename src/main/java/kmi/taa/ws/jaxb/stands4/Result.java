package kmi.taa.ws.jaxb.stands4;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "result")
@XmlAccessorType (XmlAccessType.FIELD)
@XmlType(propOrder = {
        "term",
        "definition",
        "category"
})
public class Result {
	
    private String term;
    
    private String definition;
    
    private String category;
    
    public void setTerm(String t) {
    	this.term = t;
    }
    
    public String getTerm() {
    	return this.term;
    }
    
    public void setDefinition(String d) {
    	this.definition = d;
    }
    
    public String getDefinition() {
    	return definition;
    }
    
    public void setCategory(String c) {
    	this.category = c;
    }
    
    public String getCategory() {
    	return this.category;
    }
}
