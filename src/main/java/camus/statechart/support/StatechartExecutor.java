package camus.statechart.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import camus.statechart.EndOfEvent;
import camus.statechart.EventHandledEvent;
import camus.statechart.FaultRaisedEvent;
import camus.statechart.FinalState;
import camus.statechart.State;
import camus.statechart.StateBouncedEvent;
import camus.statechart.StatechartExecution;
import camus.statechart.StateEnteredEvent;
import camus.statechart.StateLeftEvent;
import camus.statechart.Statechart;
import camus.statechart.StatechartEvent;
import camus.statechart.StatechartFaultCase;
import camus.statechart.StatechartFinishedEvent;
import camus.statechart.StatechartListener;
import camus.statechart.StatechartStartedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import async.AsyncOperationState;
import event.Event;
import event.EventSubscriber;
import net.jcip.annotations.GuardedBy;
import utils.Utilities;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StatechartExecutor<C extends StatechartExecution<C>> implements EventSubscriber {
	private static final Logger s_logger = LoggerFactory.getLogger(StatechartExecutor.class);

	private final C m_context;
	private final Statechart<C> m_schart;
	private final StatechartEventQueue m_eventQueue;
	private final Executor m_executor;
	
	private final ReentrantLock m_scLock = new ReentrantLock();
	private final Condition m_scCond = m_scLock.newCondition();
	@GuardedBy("m_scLock") private boolean m_started = false;

	private final List<State<C>> m_path;
	private final CopyOnWriteArrayList<StatechartListener> m_listeners
											= new CopyOnWriteArrayList<StatechartListener>();

	/**
	 * 상태차트 실행기를 생성한다.
	 * 
	 * @param schart		실행 대상 상태차트 객체.
	 * @param eventQueue	상태차트 진행에 사용될 이벤트를 받을 큐.
	 */
	public StatechartExecutor(C context, Executor executor) {
		Preconditions.checkArgument(context != null, "StatechartExecution is null");
		
		m_context = context;
		m_schart = context.getStatechart();
		m_eventQueue = new StatechartEventQueue(this);
		m_executor = executor;
		m_path = Lists.newArrayList();
	}

	public C getContext() {
		return m_context;
	}

	public Statechart<C> getStatechart() {
		return m_schart;
	}

	public State<C> getCurrentState() {
		return (m_path.size() > 0) ? m_path.get(m_path.size()-1) : null;
	}

	@Override
	public void receiveEvent(Event event) {
		m_eventQueue.receiveEvent(event);
	}

	public void addStatechartListener(StatechartListener listener) {
		m_listeners.add(listener);
	}

	public void removeStatechartListener(StatechartListener listener) {
		m_listeners.remove(listener);
	}

	public void start() throws Exception {
		m_scLock.lock();
		try {
			if ( m_started ) {
				throw new IllegalStateException("already started");
			}

			m_started = true;
			m_scCond.signalAll();
		}
		finally {
			m_scLock.unlock();
		}

		notifyScEvent(new StatechartStartedEvent(m_schart));

		State<C> root = m_schart.getRootState();
		State<C> to = enterLeafState(root);
		if ( to != null ) {
			gotoState(to, null);
		}

		if ( m_path.get(m_path.size()-1).isFinal() ) {
			final State<C> s = m_path.get(m_path.size()-1);
			
			Utilities.executeAsynchronously(m_executor, () -> {
				if ( s instanceof FinalState ) {
					FinalState fs = (FinalState)s;
					stop(fs.getAsyncOperationState(), fs.getFailureCause());
				}
				else {
					stop(AsyncOperationState.COMPLETED, null);
				}
			});
		}
	}

	public void stop(AsyncOperationState asyncState, Throwable fault) {
		m_scLock.lock();
		try {
			if ( !m_started ) {
				return;
			}

			m_started = false;
			m_scCond.signalAll();
		}
		finally {
			m_scLock.unlock();
		}

		for ( int i = m_path.size()-1; i >= 0; --i ) {
			State<C> state = m_path.get(i);
			exitIGE(state);

			notifyScEvent(new StateLeftEvent(state));
			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("exited: state[" + state.getGuid() + "]");
			}
		}

		notifyScEvent(new StatechartFinishedEvent(asyncState, fault));
		m_listeners.clear();
	}

	public boolean isStopped() {
		m_scLock.lock();
		try {
			return !m_started;
		}
		finally {
			m_scLock.unlock();
		}
	}

	public void waitForFinished() throws InterruptedException {
		m_scLock.lock();
		try {
			while ( m_started ) {
				m_scCond.await();
			}
		}
		finally {
			m_scLock.unlock();
		}
	}

	public void handleEvent(Event event) {
		m_scLock.lock();
		try {
			if ( !m_started ) {
				return;
			}
		}
		finally {
			m_scLock.unlock();
		}

		if ( event instanceof EndOfEvent ) {
			stop(AsyncOperationState.COMPLETED, null);

			return;
		}

		int idx = m_path.size() -1;

		State<C> toState = null;
		String toStateId =null;
		for ( ; idx >= 0; --idx ) {
			State<C> state = m_path.get(idx);
			try {
				toState = state.handleEvent(m_context, event);
			}
			catch ( Throwable fault ) {
				State<C> faultState = getFaultHandleState(state, fault);
				s_logger.warn("fails to handle event: state=" + state + ", event=" + event
								+ ", cause=" + fault);

				notifyScEvent(new FaultRaisedEvent(fault, state, faultState,
												StatechartFaultCase.HANDLE_EVENT, event));
			}
			
			notifyScEvent(new EventHandledEvent(event, state, toStateId));
			if ( toStateId != null ) {
				if ( !toStateId.equals(State.STOP_PROPAGATE) ) {
					toState = StateChartUtils.traverse(m_schart, state, toStateId);
					s_logger.info("handled: event={}, state[{}], goto=state[{}]",
									event, state.getGuid(), toStateId);
				}
				else {
					s_logger.info("handled: event={}, state[{}]", event, state.getGuid());
				}
				break;
			}
		}

		if ( toState != null ) {
			gotoState(toState, event);
		}

		if ( m_path.get(m_path.size()-1).isFinal() ) {
			final State<C> s = m_path.get(m_path.size()-1);
			if ( s instanceof FinalState ) {
				FinalState fs = (FinalState)s;
				switch ( fs.getAsyncOperationState() ) {
					case COMPLETED:
					case CANCELLED:
						stop(AsyncOperationState.COMPLETED, null);
						break;
					case FAILED:
						stop(fs.getAsyncOperationState(), fs.getFailureCause());
						break;
					default:
						throw new RuntimeException();
				}
			}
			else {
				stop(AsyncOperationState.COMPLETED, null);
			}
		}
	}

	public String toString() {
		return "Statechart[current=" + getCurrentState() + "]";
	}

	private void gotoState(State<C> to, Event causingEvent) {
		while ( true ) {
			State<C> current = getCurrentState();

			if ( current == to ) {
				if ( to.isComposite() ) {
					try {
						to = to.getInitialChildState();
					}
					catch ( Throwable fault ) {
						State<C> reactState = to;
						to = getFaultHandleState(reactState, fault);

						notifyScEvent(new FaultRaisedEvent(fault, reactState, to,
											StatechartFaultCase.GET_INITIAL_SUBSTATE, causingEvent));
					}
				}
				else {
					return;
				}
			}
			else if ( current.isAncestorOf(to) ) {
				throw new AssertionError("Cannot goto the substate: current=" + current
										+ ", to=" + to);
			}
			else if ( to.isAncestorOf(current) ) {
				exitUptoAncestor(to);

				if ( to.isComposite() ) {
					try {
						to = to.getInitialChildState();
					}
					catch ( Throwable fault ) {
						State<C> reactState = to;
						to = getFaultHandleState(reactState, fault);

						notifyScEvent(new FaultRaisedEvent(fault, reactState, null,
												StatechartFaultCase.GET_INITIAL_SUBSTATE, null));
					}
				}
				else {
					return;
				}
			}
			else if ( to.getParentState().isAncestorOf(current) ) {
				try {
					exitUptoAncestor(to.getParentState());
				}
				catch ( RuntimeException fault ) {
					stop(AsyncOperationState.FAILED, fault);
					
					throw new RuntimeException(fault);
				}
			}
			else {
				throw new AssertionError("Cannot goto the substate: current=" + current
										+ ", to=" + to);
			}

			to = enterLeafState(to);
			if ( to == null ) {
				// 목표 상태로의 진입이 성공한 경우
				return;
			}
		}
	}

	/**
	 * 주어진 후손 상태까지 내려간다.
	 *
	 * @param state		이동 대상 후손 상태.
	 * @return		성공적으로 진입된 경우는 <code>null</code>을 반환하고,
	 * 				다른 state로 전이가 추천된 경우는 해당 state를 반환한다.
	 */
	private State<C> enterLeafState(State<C> state) {
		State<C> from = getCurrentState();
		while ( true ) {
			State<C> next;

			if ( from != state ) {
				while ( true ) {
					try {
						if ( (next = state.enter(m_context)) == null ) {
							break;
						}
						notifyScEvent(new StateBouncedEvent(state));

						// state의 진입이 허가되지 않고 다른 state 'next'로 이동이 추천된 경우.
						// 만일 추천된 state가 sibling state인 경우는 해당 state로의 진입을 시도하고,
						// 그렇지 않은 경우는 해당 state를 반환한다.
						//
						if ( next.getParentState() != state.getParentState() ) {
							return next;
						}
					}
					catch ( Throwable e ) {
						s_logger.warn("fails to call entry for state=" + state.getGuid(), e);
						
						next = getFaultHandleState(state, e);
						notifyScEvent(new FaultRaisedEvent(e, state, next,
														StatechartFaultCase.STATE_ENTRY, null));
						if ( next == null ) {
							// 별도의 failure handle state가 설정되지 않은 경우는 Statechart 수행을 종료시킨다.
							stop(AsyncOperationState.FAILED, e);
							throw new RuntimeException();
						}
					}

					state = next;
				}

				notifyScEvent(new StateEnteredEvent(state));

				// 대상 자식  state로의 진입이 성공된 경우.
				if ( s_logger.isInfoEnabled() ) {
					s_logger.info("entered: state[" + state.getGuid() + "]");
				}

				m_path.add(state);
			}

			// 만일 진입한 상태가 nesting state인 경우는 시작 상태로 진입을 시도한다.
			if ( state.isComposite() ) {
				try {
					state = state.getInitialChildState();
				}
				catch ( Throwable e ) {
					State<C> raiser = state;
					state = getFaultHandleState(raiser, e);
					
					notifyScEvent(new FaultRaisedEvent(e, raiser, state,
												StatechartFaultCase.GET_INITIAL_SUBSTATE, null));
				}
			}
			else {
				return null;
			}
		}
	}

	/**
	 * 현재 상태에서 주어진 조상 state까지 exit을 호출하여 상태를 빠져나간다.
	 * <p>
	 * 대상 조상 state의 'exit'은 호출되지 않음.
	 * 'ancestor' state가 현재 state의 ancestor 여부는 확인하지 않으므로, 미리 validate되어야 함.
	 *
	 * @param ancestor	대상 조상 state
	 * @throws	AssertionError	'ancestor'가 현재 상태의 조상이 아닌 경우.
	 */
	private void exitUptoAncestor(State<C> ancestor) {
		for ( int i = m_path.size()-1; i >= 0; --i ) {
			State<C> state = m_path.get(i);
			if ( state == ancestor ) {
				return;
			}

			exitIGE(state);
			m_path.remove(i);

			notifyScEvent(new StateLeftEvent(state));

			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("exited: state[" + state.getGuid() + "]");
			}
		}

		throw new AssertionError("Should not be here: class=" + getClass().getName()
								+ ".exitUptoAncestor()");
	}

	private void exitIGE(State<C> state) {
		State<C> parent = state.getParentState();
		if ( parent != null ) {
			parent.setRecentChildState(state.getLuid());
		}
		
		try {
			state.leave(m_context);
		}
		catch ( Exception e ) {
			s_logger.warn("ignored exception at exit: state=" + state + ", exception=" + e);
		}
	}

	private State<C> getFaultHandleState(State<C> state, Throwable fault) {
		State<C> superState = state.getParentState();
		while ( true ) {
			if ( superState != null ) {
				state = superState.getExceptionState();
				if ( state != null ) {
					return state;
				}
				
				superState = superState.getParentState();
			}
			else {
				return m_schart.getRootState().getExceptionState();
			}
		}
	}

	private void notifyScEvent(final StatechartEvent scEvent) {
		for ( final StatechartListener listener: m_listeners ) {
			try {
				listener.receiveEvent(scEvent);
			}
			catch ( Exception e ) {
				m_listeners.remove(listener);
			}
		}
	}
}