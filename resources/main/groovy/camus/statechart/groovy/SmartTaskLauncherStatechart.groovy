
statechart {
	defaultStateId "idle"
	
	state ("idle") {
		on ('camus.user.UserEntered') { ev ->
			residents.add ev.userId
			ownerId = ev.userId
			'../active'
		}
	}
	
	state ("active") {
		defaultStateId "ownerSelecting"
		
		on ('camus.user.UserEntered') { ev-> residents.add ev.userId }
		on ('camus.user.UserLeft') { ev->
			residents.remove ev.userId
			
			def toStateId = null
			if ( !residents ) {
				"/idle"
			}
			else if ( ev.userId == ownerId ) {
				'ownerSelecting'
			}
		}
		
		state ("ownerSelecting") {
			entry {
				if ( residents.size() == 1 ) {
					ownerId = residents.get(0)
					return '../ownerSelected'
				}
				else {
					println "LIST USERS: $residents"
					return null
				}
			}
			exit { println "CLOSE USER LIST" }
			on ('test.statechart.OwnerSelected') { ev->
				ownerId = ev.ownerId
				'../ownerSelected'
			}
		}
		
		state ("ownerSelected") {
			defaultStateId "taskSelecting"
			
			state ("taskSelecting") {
				entry {
					def tasks = recommendTask()
					if ( tasks.size() == 1 ) {
						taskSelected = tasks[0]
						return '../taskSelected'
					}
					else {
						println "LIST TASKS: ${tasks}"
						return null
					}
				}
			
				exit {
					println "CLOSE TASK LIST"
					ownerId = null
				}
				
				on ('test.statechart.TaskSelected') { ev->
					taskSelected = ev.taskId
					'../taskSelected'
				}
			}
			
			state ("taskSelected") {
				exit {
					println "STOP task $taskSelected"
					taskSelected = null
				}
				
				state ("taskRunning") {
					entry { println "START task ${taskSelected}" }
				}
			}
		}
	}
}