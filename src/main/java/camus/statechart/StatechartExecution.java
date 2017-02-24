package camus.statechart;

import event.EventSubscriber;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface StatechartExecution<C extends StatechartExecution<C>> extends EventSubscriber {
	public Statechart<C> getStatechart();
}
