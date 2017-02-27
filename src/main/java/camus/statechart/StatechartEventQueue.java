package camus.statechart;

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
class StatechartEventQueue implements EventSubscriber {
	static final Logger s_logger = LoggerFactory.getLogger(StatechartEventQueue.class);

	private final StatechartExecution<?> m_scExec;
	private final AsyncConsumer<Runnable> m_dispatcher;

	StatechartEventQueue(StatechartExecution<?> scExec) {
		m_scExec = scExec;
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
			m_scExec.handleEvent(m_event);
	    }
		
		public String toString() {
			return "Action[event=" + m_event.toString() + "]";
		}
	}
}