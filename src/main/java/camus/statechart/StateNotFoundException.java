package camus.statechart;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class StateNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -5794470682408034945L;
	
	public StateNotFoundException(String name) {
		super(name);
	}
}
