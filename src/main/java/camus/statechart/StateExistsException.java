package camus.statechart;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class StateExistsException extends RuntimeException {
	private static final long serialVersionUID = 5626253671713733487L;

	public StateExistsException(String name) {
		super(name);
	}
}
