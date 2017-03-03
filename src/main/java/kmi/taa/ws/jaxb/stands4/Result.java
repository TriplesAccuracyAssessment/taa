/*
 * (C) Copyright 2017 Shuangyan Liu
 * Shuangyan.Liu@open.ac.uk 
 * Knowledge Media Institute
 * The Open University, United Kingdom
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
