package camus.statechart.support;

import java.util.concurrent.atomic.AtomicInteger;

import camus.statechart.State;
import camus.statechart.StatechartExecution;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;

import async.Service;
import async.ServiceState;
import async.ServiceStateChangeEvent;
import event.Event;




/**
 * <code>ServiceTaskState</code>는 {@link Service} 작업을 수행하는 상태차트 기반 응용을
 * 구성하는 상태를 위한 추상 클래스를 정의한다.
 * <p>
 * 하나의 <code>Service</code> 작업을 수행하는 상태를 구현하는 경우, 본 추상 클래스를 상속하면
 * 간편하게 상태 클래스를 구현할 수 있다.
 * 본 클래스를 상속하여 상태 클래스를 구현하는 경우 다음과 같은 메소드를 재정의해야 한다.
 * <ul>
 * 	<li> {@link #getNextState(Service)}: 수행하는  <code>Service</code>가 종료된 경우
 * 		다음으로 이전할 상태 객체를 반환한다.
 * 	<li> {@link #getFaultState(Service)}: 수행하는 <code>Service</code>가 오류 발생으로
 * 		종료된 경우 (또는 오류 발생으로 작업 시작이 실패한 경우) 다음으로 이전할 상태 객체를 반환한다.
 * </ul> 
 * 
 * @author Kang-Woo Lee
 */
public abstract class ServiceExecutionState<C extends StatechartExecution<C>>
																		extends AbstractState<C> {
	private volatile Service m_svc;
	private volatile String m_execId;
	private final AtomicInteger m_execIdGen = new AtomicInteger(0);
	
	/**
	 * 수행하는  <code>Service</code>가 종료된 경우 다음으로 이전할 상태 객체를 반환한다.
	 * <p>
	 * 본 메소드는 <code>ServiceTaskState</code> 객체에서 호출된다.
	 * 기본 동작을 "/finished"라는 상태로 전이하며, 이를 수정하는 경우 본 메소드를 override하여
	 * 수정한다.
	 * 
	 * @param context		상태차트 수행 문맥
	 * @return	다음으로 이전할 상태 객체.
	 */
	protected abstract String getNextStateIfStopped(C context);
	
	/**
	 * {@link Service} 태스크 상태 중에 태스크가 오류가 발생한 경우, 다음으로
	 * 이동할 상태를 반환한다.
	 * <p>
	 * 기본 동작을 "/finished"라는 상태로 전이하며, 이를 수정하는 경우 본 메소드를 override하여
	 * 수정한다.
	 * 
	 * @param context		상태차트 수행 문맥
	 * @param failureCause	발생된 오류 예외 객체.
	 * @return	다음으로 이전할 상태 객체.
	 */
	protected abstract String getNextStateIfFailed(C context, Throwable failureCause);

	/**
	 * {@link Service} 작업을 수행하는 상태 객체를 생성한다.
	 * <p>
	 * 객체 생성 이후에는 {@link #setServiceTask(Service)}를 통해 본 상태에서 수행할
	 * 작업을 설정하여야 한다.
	 * 
	 * @param appl		상태를 포함하는 상태 차트 응용.
	 * @param parent	상위 상태. 없는 경우는 <code>null</code>.
	 * @param luid		생성할 상태의 지역 식별자.
	 * 					이 식별자는 동일 상위 상태에 포함돤 sibling 상태들 사이에서는 유일해야 한다.
	 */
	public ServiceExecutionState(State<C> parent, String guid, boolean keepHistory,
								String exceptionChildStateId) {
		super(parent, guid, keepHistory, exceptionChildStateId);
	}
	
	/**
	 * 상태 작업 수행 식별자를 반환한다.
	 * <p>
	 * 상태 작업 수행 식별자는 본 상태에 설정된 작업이 수행될 때마다 다르게 부여된다.
	 * 
	 * @return	작업 수행 식별자.
	 */
	public final String getExecId() {
		return m_execId;
	}
	
	/**
	 * 설정된 <code>Service</code> 객체를 반환한다.
	 * 
	 * @return	<code>Service</code> 작업 객체.
	 */
	public final Service getService() {
		return m_svc;
	}
	
	/**
	 * 본 상태로 진입시 수행시킬 <code>Service</code> 객체를 설정한다.
	 * 
	 * @param task	수행시킬  <code>Service</code> 작업 객체
	 */
	public final void setService(Service task) {
		m_svc = task;
	}

	@Override
	public String enter(C context) {
		Preconditions.checkState(m_svc != null, "Service has not been set");
		try {
			m_svc.addStateChangeListener(new EventRelayListener<>(context, m_execId));
			m_svc.start();
			
			m_execId = getGuid() + ":" + m_execIdGen.getAndIncrement();
		}
		catch ( Exception e ) {
			return getNextStateIfFailed(context, e);
		}
		
		return null;
	}

	@Override
	public void leave(C context) {
		m_svc.removeStateChangeListener(new EventRelayListener<>(context, m_execId));
		
		m_svc.stop();
	}

	@Override
	public String handleEvent(C context, Event event) {
		if ( event instanceof ServiceStateChangeEvent ) {
			ServiceStateChangeEvent ssce = (ServiceStateChangeEvent)event;
			
			if ( !ssce.getTag().equals(m_execId) ) {
				return State.STOP_PROPAGATE_GUID;
			}
			
			if ( ssce.getToState() == ServiceState.STOPPED ) {
				return getNextStateIfStopped(context);
			}
			else if ( ssce.getToState() == ServiceState.FAILED ) {
				return getNextStateIfFailed(context, m_svc.getFailureCause());
			}
		}
		
		return null;
	}
    
	/**
	 * 강제로 <code>Service</code> 작업이 종료시킨다.
	 * <p>
	 * 설정 작업을 종료시키고 {@link #getNextState(Service)}를 호출하여 획득한 상태로 전이시킨다.
	 */
    public void finishTask() {
    	m_svc.stop();
    }
	
	private static class EventRelayListener<C extends StatechartExecution<C>> {
		private final C m_execution;
		private final String m_execId;
		
		EventRelayListener(C execution, String execId) {
			m_execution = execution;
			m_execId = execId;
		}
		
		@Subscribe
		public void onStateChanged(ServiceStateChangeEvent event) {
			if ( event.getFromState() == ServiceState.RUNNING ) {
				ServiceStateChangeEvent tagged = new ServiceStateChangeEvent(event.getService(),
																		event.getFromState(),
																		event.getToState(),
																		m_execId);
				m_execution.receiveEvent(tagged);
			}
		}
	}
}
