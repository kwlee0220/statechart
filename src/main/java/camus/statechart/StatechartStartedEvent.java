package camus.statechart;



/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StatechartStartedEvent extends StatechartEvent {
	private final Statechart m_schart;

	public StatechartStartedEvent(Statechart schart) {
		m_schart = schart;
	}

	public Statechart getStatechart() {
		return m_schart;
	}

	@Override
	public String toString() {
		return "Started[sc=" + m_schart + "]";
	}
}
