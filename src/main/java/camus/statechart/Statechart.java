package camus.statechart;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface Statechart<T extends State<T>> {
	public T getRootState();
}
