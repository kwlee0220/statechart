package camus.statechart;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import camus.statechart.support.StateChartUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import async.AsyncOperationState;
import async.support.AbstractService;
import event.Event;
import event.EventSubscriber;
import net.jcip.annotations.GuardedBy;
import utils.Lambdas;
import utils.Utilities;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class StatechartExecution<C extends StatechartExecution<C>> extends AbstractService
														implements EventSubscriber {
    private static final Logger s_logger = LoggerFactory.getLogger(StatechartExecution.class);

	private final Statechart<C> m_schart;
	private final ReentrantLock m_scLock = new ReentrantLock();
	private final StatechartEventQueue m_eventQueue;
	@GuardedBy("m_scLock") private final List<State<C>> m_path;
	@GuardedBy("m_scLock") private final EventBus m_eventBus;
    
    public StatechartExecution(Statechart<C> schart) {
		m_schart = schart;
		m_eventQueue = new StatechartEventQueue(this);
		m_path = Lists.newArrayList();
		m_eventBus = new EventBus();
		
		setLogger(s_logger);
    }
	
	/**
	 * 본 수행에서 사용하는 상태차트를 반환한다.
	 * 
	 * @return	상태차트.
	 */
	public Statechart<C> getStatechart() {
		return m_schart;
	}

	/**
	 * 상태차트 수행 중에 현재 상태 객체를 반환한다.
	 */
	public State<C> getCurrentState() {
		return Lambdas.guardedGet(m_scLock, ()-> {
			return (m_path.size() > 0) ? m_path.get(m_path.size()-1) : null;
		});
	}

	@Override
	public void receiveEvent(Event event) {
		if ( isRunning() ) {
			Lambdas.guradedRun(m_scLock, ()->m_eventQueue.receiveEvent(event));
		}
	}

	@Override
	protected void startService() throws Exception {
		m_scLock.lock();
		try {
			notifyScEventInGuard(new StatechartStartedEvent(m_schart));
	
			State<C> root = m_schart.getRootState();
			State<C> to = enterLeafStateInGuard(root);
			if ( to != null ) {
				gotoStateInGuard(to, null);
			}
	
			if ( m_path.get(m_path.size()-1).isFinal() ) {
				final State<C> s = m_path.get(m_path.size()-1);
				
				Utilities.executeAsynchronously(getExecutor(), () -> {
					if ( s instanceof FinalState ) {
						FinalState fs = (FinalState)s;
						stopInGuard(fs.getAsyncOperationState(), fs.getFailureCause());
					}
					else {
						stopInGuard(AsyncOperationState.COMPLETED, null);
					}
				});
			}
		}
		finally {
			m_scLock.unlock();
		}
	}
	
	@Override
	protected void stopService() {
		m_scLock.lock();
		try {
			stopInGuard(AsyncOperationState.COMPLETED, null);
		}
		finally {
			m_scLock.unlock();
		}
	}

	public void addStatechartListener(Object listener) {
		m_scLock.lock();
		try {
			m_eventBus.register(listener);
		}
		finally {
			m_scLock.unlock();
		}
	}

	public void removeStatechartListener(Object listener) {
		m_scLock.lock();
		try {
			m_eventBus.unregister(listener);
		}
		finally {
			m_scLock.unlock();
		}
	}

	public String toString() {
		return "Statechart[current=" + getCurrentState() + "]";
	}

	void handleEvent(Event event) {
		if ( !isRunning() ) {
			return;
		}

		if ( event instanceof EndOfEvent ) {
			stopInGuard(AsyncOperationState.COMPLETED, null);

			return;
		}
		
		m_scLock.lock();
		try {
			int idx = m_path.size() -1;
	
			State<C> toState = null;
			String toStateId =null;
			for ( ; idx >= 0; --idx ) {
				State<C> state = m_path.get(idx);
				try {
					toStateId = state.handleEvent((C)this, event);
				}
				catch ( Throwable fault ) {
					State<C> faultState = getFaultHandleStateInGuard(state, fault);
					s_logger.warn("fails to handle event: state={}, event={}, cause={}",
									state, event, fault);
	
					notifyScEventInGuard(new FaultRaisedEvent(fault, state, faultState,
													StatechartFaultCase.HANDLE_EVENT, event));
				}
				
				notifyScEventInGuard(new EventHandledEvent(event, state, toStateId));
				if ( toStateId != null ) {
					if ( !toStateId.equals(State.STOP_PROPAGATE_GUID) ) {
						toState = StateChartUtils.traverse(m_schart, state, toStateId);
						s_logger.info("handled: event={}, {}, goto={}", event, state, toState);
					}
					else {
						s_logger.info("handled: event={}, {}", event, state);
					}
					break;
				}
			}
	
			if ( toState != null ) {
				gotoStateInGuard(toState, event);
			}
	
			if ( m_path.get(m_path.size()-1).isFinal() ) {
				final State<C> s = m_path.get(m_path.size()-1);
				if ( s instanceof FinalState ) {
					FinalState fs = (FinalState)s;
					switch ( fs.getAsyncOperationState() ) {
						case COMPLETED:
						case CANCELLED:
							stopInGuard(AsyncOperationState.COMPLETED, null);
							break;
						case FAILED:
							stopInGuard(fs.getAsyncOperationState(), fs.getFailureCause());
							break;
						default:
							throw new RuntimeException();
					}
				}
				else {
					stopInGuard(AsyncOperationState.COMPLETED, null);
				}
			}
		}
		finally {
			m_scLock.unlock();
		}
	}

	private void stopInGuard(AsyncOperationState asyncState, Throwable fault) {
		for ( int i = m_path.size()-1; i >= 0; --i ) {
			State<C> state = m_path.get(i);
			exitIGEInGuard(state);

			notifyScEventInGuard(new StateLeftEvent(state));
			s_logger.info("exited: {}", state);
		}

		notifyScEventInGuard(new StatechartFinishedEvent(asyncState, fault));
		notifyServiceInterrupted();
	}

	private void gotoStateInGuard(State<C> to, Event causingEvent) {
		while ( true ) {
			State<C> current = getCurrentState();

			if ( current == to ) {
				if ( to.isComposite() ) {
					try {
						to = to.getInitialChildState();
					}
					catch ( Throwable fault ) {
						State<C> reactState = to;
						to = getFaultHandleStateInGuard(reactState, fault);

						notifyScEventInGuard(new FaultRaisedEvent(fault, reactState, to,
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
				exitUptoAncestorInGuard(to);

				if ( to.isComposite() ) {
					try {
						to = to.getInitialChildState();
					}
					catch ( Throwable fault ) {
						State<C> reactState = to;
						to = getFaultHandleStateInGuard(reactState, fault);

						notifyScEventInGuard(new FaultRaisedEvent(fault, reactState, null,
												StatechartFaultCase.GET_INITIAL_SUBSTATE, null));
					}
				}
				else {
					return;
				}
			}
			else if ( to.getParentState().isAncestorOf(current) ) {
				try {
					exitUptoAncestorInGuard(to.getParentState());
				}
				catch ( RuntimeException fault ) {
					stopInGuard(AsyncOperationState.FAILED, fault);
					
					throw new RuntimeException(fault);
				}
			}
			else {
				throw new AssertionError("Cannot goto the substate: current=" + current
										+ ", to=" + to);
			}

			to = enterLeafStateInGuard(to);
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
	private State<C> enterLeafStateInGuard(State<C> state) {
		State<C> from = getCurrentState();
		while ( true ) {
			State<C> next;

			if ( from != state ) {
				while ( true ) {
					try {
						String bouncePath;
						if ( (bouncePath = state.enter((C)this)) == null ) {
							break;
						}
						next = m_schart.traverse(state, bouncePath);
						notifyScEventInGuard(new StateBouncedEvent<>(state, next));

						// state의 진입이 허가되지 않고 다른 state 'next'로 이동이 추천된 경우.
						// 만일 추천된 state가 sibling state인 경우는 해당 state로의 진입을 시도하고,
						// 그렇지 않은 경우는 해당 state를 반환한다.
						//
						if ( next.getParentState() != state.getParentState() ) {
							return next;
						}
					}
					catch ( Throwable e ) {
						s_logger.warn("fails to call entry for " + state, e);
						
						next = getFaultHandleStateInGuard(state, e);
						notifyScEventInGuard(new FaultRaisedEvent(e, state, next,
														StatechartFaultCase.STATE_ENTRY, null));
						if ( next == null ) {
							// 별도의 failure handle state가 설정되지 않은 경우는 Statechart 수행을 종료시킨다.
							stopInGuard(AsyncOperationState.FAILED, e);
							throw new RuntimeException();
						}
					}

					state = next;
				}

				notifyScEventInGuard(new StateEnteredEvent(state));

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
					state = getFaultHandleStateInGuard(raiser, e);
					
					notifyScEventInGuard(new FaultRaisedEvent(e, raiser, state,
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
	private void exitUptoAncestorInGuard(State<C> ancestor) {
		for ( int i = m_path.size()-1; i >= 0; --i ) {
			State<C> state = m_path.get(i);
			if ( state == ancestor ) {
				return;
			}

			exitIGEInGuard(state);
			m_path.remove(i);

			notifyScEventInGuard(new StateLeftEvent(state));

			if ( s_logger.isInfoEnabled() ) {
				s_logger.info("exited: state[" + state.getGuid() + "]");
			}
		}

		throw new AssertionError("Should not be here: class=" + getClass().getName()
								+ ".exitUptoAncestor()");
	}

	private void exitIGEInGuard(State<C> state) {
		State<C> parent = state.getParentState();
		if ( parent != null ) {
			parent.setRecentChildState(state.getLuid());
		}
		
		try {
			state.leave((C)this);
		}
		catch ( Exception e ) {
			s_logger.warn("ignored exception at exit: state=" + state + ", exception=" + e);
		}
	}

	private State<C> getFaultHandleStateInGuard(State<C> state, Throwable fault) {
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

	private void notifyScEventInGuard(final StatechartEvent scEvent) {
		m_eventBus.post(scEvent);
	}
}
