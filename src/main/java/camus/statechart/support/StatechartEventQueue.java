package camus.statechart.support;

import camus.statechart.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import event.Event;
import event.EventSubscriber;
import utils.thread.AsyncConsumer;


/**
 * 
 * 본 클래스는 ThreadSafe하도록 구현되었다.
 * 
 * @author Kang-Woo Lee
 */
class StatechartEventQueue<S extends State<S>> implements EventSubscriber {
	static final Logger s_logger = LoggerFactory.getLogger(StatechartEventQueue.class);

	private final StatechartExecutor<S> m_scExecutor;
	private final AsyncConsumer<Runnable> m_dispatcher;

	StatechartEventQueue(StatechartExecutor<S> scExec) {
		m_scExecutor = scExec;
		m_dispatcher = AsyncConsumer.singleWorker(task -> {
			try {
				task.run();
			}
			catch ( Throwable e ) {
				s_logger.warn("fails to handle event={}", task);
			}
		});
	}

	@Override
	public final void receiveEvent(Event event) {
		if ( event != null ) {
			s_logger.debug("submitting event: {}", event);

			m_dispatcher.accept(new EventDeliveryAction(event));
		}
	}

	public void enqueueAction(Runnable action) {
		s_logger.debug("submitting action: {}", action);
		
		m_dispatcher.accept(action);
	}
	
	class EventDeliveryAction implements Runnable {
		private final Event m_event;
		
		EventDeliveryAction(Event event) {
			Preconditions.checkNotNull(event, "Event was null");
			
			m_event = event;
		}
		
		public Event getEvent() {
			return m_event;
		}
		
		public void run() {
			m_scExecutor.handleEvent(m_event);
	    }
		
		public String toString() {
			return "Action[event=" + m_event.toString() + "]";
		}
	}
}