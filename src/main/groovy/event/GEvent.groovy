package event

import com.google.common.base.Preconditions

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GEvent implements Event {
	String[] eventTypeIds
	Map<String,Object> properties = [:]
	
	GEvent(Map props) {
		properties = props
	}

	@Override
	public String[] getPropertyNameAll() {
		return properties.values() as String[];
	}
	
	@Override
    public Object getProperty(String name) {
		Preconditions.checkArgument(name != null, "Property name was null");
		
		def prop = properties[name]
		if ( !prop ) {
			throw new IllegalArgumentException("Property not found: name=" + name);
		}
		
		prop
    }
	
	def getAt(String name) {
		getProperty(name)
	}
	
	def propertyMissing(String name) {
		getProperty(name)
	}
}
