package verification.timed_state_exploration.zone;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

import lpn.parser.LhpnFile;
import lpn.parser.Transition;
import verification.platu.lpn.LPNTran;
import verification.platu.lpn.LpnTranList;
import verification.platu.stategraph.State;
import verification.platu.stategraph.StateGraph;

public class StateGraph_timed extends StateGraph{

	public StateGraph_timed(LhpnFile lpn) {
		super(lpn);
		// TODO Auto-generated constructor stub
	}
	
	
	
//	public LpnTranList getEnabled(TimedState curState)
	public LpnTranList getEnabled(State curState)
	{
		// TODO: Overrides the base state and must add checks with zones.
		// Note : Can also just make a new method signature with the TimedState
		
//		LpnTranList initEnabled = super.getEnabled(curState);
//		
//		List<Transition> enabledByZone = curState.getEnabledTransitionByZone();
//		
//		for(Transition T : enabledByZone)
//		{
//			initEnabled.remove(T);
//		}
//		
//		return initEnabled;
		
		return new LpnTranList(((TimedState) curState).getEnabledTransitionByZone());
	}
	
	 //public TimedState[] fire(final StateGraph_timed[] curSgArray, 
	//		 final State[] curStateArray, Transition firedTran)
	public State[] fire(final StateGraph_timed[] curSgArray,
			final State[] curStateArray, Transition firedTran)
	 {
		 // TODO: Overrides the base and must add the zone factor.
		 
		 State[] newState = super.fire(curSgArray, curStateArray, firedTran);
		 //TimedState[] newTimedStates = new TimedState[newState.length];
		 State[] newTimedStates = new State[newState.length];
		 
		 for(int i=0; i<newState.length; i++)
		 {
			 LpnTranList enabledTransitions = super.getEnabled(newState[i]);
			 int[] tranArray = new int[enabledTransitions.size()];
			 
			 int j=0;
			 
			 // TODO: Might be able to change the signature of the fire method
			 // for zones so that this translation step is not necessary.
			 for(Transition T : enabledTransitions)
			 {
				 tranArray[j] = T.getIndex();
				 j++;
			 }
			 
			 TimedState ts = (TimedState) curStateArray[i];
			 
			 
			 if(ts.usingGraphs()){
				 ZoneType newZone = ts.getZoneGraph().extractZone()
				 		.fireTransitionbyTransitionIndex(firedTran.getIndex(),
			 				tranArray, newState[i]);
				 
				 newTimedStates[i] = new TimedState(newState[i], newZone, true);
				 
			 }
			 else{
				 ZoneType newZone = ts.getZone()
				 	.fireTransitionbyTransitionIndex(firedTran.getIndex(),
			 				tranArray, newState[i]);
				 
				 newTimedStates[i] = new TimedState(newState[i], newZone, false);
			 }
//			 ZoneType newZone = ((TimedState) curStateArray[i]).getZone()
//			 		.fireTransitionbyTransitionIndex(firedTran.getIndex(),
//			 				tranArray, newState[i]);
			 
			 
			 
//			 newTimedStates[i] = new TimedState(newState[i], newZone);
		 }
		 
		 
		 
		 return newTimedStates;
	 }



	public TimedState getInitStateTimed(boolean usegraph) {
		
		State initialStateNoTime = getInitState();
		
		// TODO Auto-generated method stub
		// Adds the zone factor.
		if(usegraph){
			return new TimedState(initialStateNoTime, true);
		}
		else{
			return new TimedState(initialStateNoTime, false);
		}
	}
	 
	
	public void outputLocalStateGraph(String file) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write("digraph G {\n");
			for (State curState : nextStateMap.keySet()) {
				String curStateName = "S" + curState.getIndex();
				out.write(curStateName + "[shape=\"ellipse\",label=\"" + curStateName + "\\n" + 
						((TimedState)curState).getZone().toString().replace("\n", "\\n") + "\"]\n");
			}
			
			for (State curState : nextStateMap.keySet()) {
				HashMap<Transition, State> stateTransitionPair = nextStateMap.get(curState);
				for (Transition curTran : stateTransitionPair.keySet()) {
					String curStateName = "S" + curState.getIndex();
					String nextStateName = "S" + stateTransitionPair.get(curTran).getIndex();
					String curTranName = curTran.getName();
					out.write(curStateName + " -> " + nextStateName + " [label=\"" + curTranName + "\"]\n");
				}
			}
			out.write("}");
			out.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error outputting state graph as dot file.");
		}
		
	}

	
//	public TimedState addState(State mState) 
//	{
//		return null;
//	}

}
