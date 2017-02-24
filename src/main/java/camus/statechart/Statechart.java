package camus.statechart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Statechart<C extends StatechartExecution<C>> {
	private final State<C> m_root;
	
	/**
	 * 상태차트 객체를 생성한다.
	 * 
	 * @param root	상태 차트의 최상위 상태 객체.
	 */
	public Statechart(State<C> root) {
		m_root = root;
	}
	
	/**
	 * 상태 차트의 최상위 상태 객체를 얻는다.
	 * 
	 * @return 최상위 상태 객체.
	 */
	public State<C> getRootState() {
		return m_root;
	}
	
	/**
	 * 주어진 전역 식별자에 해당하는 상태 객체를 얻는다.
	 * 
	 * @param guid	검색할 상태의 전역 식별자.
	 * @return	검색된 상태 객체.
	 * @throws StateNotFoundException	전역 식별자에 해당하는 상태가 없는 경우.
	 */
	public State<C> getState(String guid) throws StateNotFoundException {
		String[] parts = guid.split("/");
		
		State<C> current = getRootState();
		for ( int i =1; i < parts.length; ++i ) {
			String luid = parts[i];
			
			current = current.getChildState(luid);
		}
		
		return current;
	}
	
	public Set<State<C>> findStateByLuid(String luid) {
		Set<State<C>> found = new HashSet<State<C>>();
		
		List<State<C>> states = new ArrayList<State<C>>();
		states.add(getRootState());
		
		while ( !states.isEmpty() ) {
			State<C> state = states.remove(0);
			if ( state.getLuid().equals(luid) ) {
				found.add(state);
			}
			
			states.addAll(state.getChildStates());
		}
		
		return found;
	}
	
	public State<C> locateState(State<C> fromState, String path) throws StateNotFoundException {
		State<C> current = fromState;

		int idx = 0;
		List<String> parts = splitPath(path);
		String head = parts.get(0);
		if ( head.length() == 0 ) {
			current = getRootState();
			++idx;
		}
		else if ( head.startsWith("@") ) {
			Set<State<C>> founds = findStateByLuid(head.substring(1));
			if ( founds.size() == 1 ) {
				current = founds.iterator().next();
				++idx;
			}
			else if ( founds.size() == 0 ) {
				throw new StateNotFoundException("LUID=" + head);
			}
			else {
				throw new RuntimeException("ambiguous state id=" + head);
			}
		}

		for (; idx < parts.size(); ++idx ) {
			if ( parts.get(idx).equals("..") ) {
				current = current.getParentState();
				if ( current == null ) {
					throw new StateNotFoundException("from=" + fromState.getGuid() + ", path=" + path);
				}
			}
			else if ( parts.get(idx).equals(".") ) { }
			else {
				current = current.getParentState().getChildState(parts.get(idx));
			}
		}
		
		return current;
	}
	
    private static ArrayList<String> splitPath(String path) {
        ArrayList<String> vList = new ArrayList<String>();
        char[] buf = path.toCharArray();
        for ( int start = 0; start < buf.length;  ) {
            int i;

            StringBuffer appender = new StringBuffer();
            for ( i =start; i < buf.length; ++i ) {
                char c = buf[i];

                if ( c == '/' ) {
                    break;
                }
                else if ( c == '\\' ) {
                    if ( ++i >= buf.length ) {
                        throw new IllegalArgumentException("Corrupted CSV string");
                    }
                    c = buf[i];
                }
                
                appender.append(c);
            }

            vList.add(appender.toString());
            start = i + 1;
        }

        return vList;
    }
}