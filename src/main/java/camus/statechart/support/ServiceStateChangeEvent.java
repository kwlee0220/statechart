package camus.statechart.support;

import async.Service;
import async.ServiceState;
import event.Event;
import event.EventProperty;


/**
 * 
 * @author Kang-Woo Lee
 */
public interface ServiceStateChangeEvent extends Event {
	public static final String PROP_SERVICE = "service";
	public static final String PROP_FROM_STATE = "fromState";
	public static final String PROP_TO_STATE = "toState";
	public static final String PROP_TAG = "tag";
	
    @EventProperty(name=PROP_SERVICE)
    public Service getService();
	
    @EventProperty(name=PROP_FROM_STATE)
    public ServiceState getFromState();
	
    @EventProperty(name=PROP_TO_STATE)
    public ServiceState getToState();
	
    @EventProperty(name=PROP_TAG)
    public String getTag();
}