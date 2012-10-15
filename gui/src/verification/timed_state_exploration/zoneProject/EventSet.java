package verification.timed_state_exploration.zoneProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lpn.parser.Transition;

/**
 * An EventSet represents a transition to fire or a set of inequalities that must
 * fire together. When the EventSet represents a single transition, it is said to be in
 * Transition mode. When the EventSet represents a list of inequalities, it is said to be in
 * Inequality mode.
 * 
 * @author Andrew N. Fisher
 *
 */
public class EventSet extends Transition implements Iterable<Event>{

	/*
	 * Abstraction Function : An EventSet is either singleton set containing a Transition or
	 * is a set of IneqaulityVariables (but not both). Accordingly, an EventSet is said to operate in one
	 * of two modes: a Transition mode and an Inequality mode. When the EventSet contains no elements it
	 * is said to have No Mode. When the EventSet contains a single Transition it is stored as the
	 * _transition variable. When the EventSet contains a set InequalityVariables, they are stored in
	 * _inequalities.
	 */
	
	
	/*
	 * Representation Invariant :
	 * Exactly one of the fields '_transition' or '_inequalities' should be non-null.
	 * Testing for null is how this class determines whether it represents a 
	 * Transition or a set of inequalities. Both variables can be null in which
	 */
	
	// A variable indicating whether we are a transition or a set of inequalities
	// may not be need since we could test for null or non-null.
	// Indicates whether this EventSet is a transitions or a set of inequalities.
//	boolean _isTransition;
	
	
	// The transition to fire.
	Transition _transition;
	
	// The set of inequalities.
	ArrayList<InequalityVariable> _inequalities;
	
	/**
	 * Creates an uninitialized EventSet. The mode of the EventSet is determined by the first use
	 * of the insert method. If a Transition event is added, then the EventSet will be in the Transition mode.
	 * If an inequality event is added, then the EventSet will be in the Inequality mode.
	 */
	public EventSet(){
		// The mode will be determined by the first element added into the EventSet.
	}
	
	/**
	 * Creates an EventSet in the Transition mode.
	 * @param transition
	 * 		The transition the EventSet should contain.
	 */
	public EventSet(Transition transition){
		_transition = transition;
	}
	
	/**
	 * Creates an EventSet in the Inequality mode.
	 * @param inequalities
	 * 		The list of inequalities that the EventSet should contain.
	 */
	public EventSet(Collection<? extends Event> inequalities){
		_inequalities = new ArrayList<InequalityVariable>();
		_inequalities.addAll(_inequalities);
	}
	
	/**
	 * Determines whether this EventSet represents a Transition.
	 * @return
	 * 		True if this EventSet represents a Transition; false otherwise.
	 */
	public boolean isTransition(){
		return _transition != null;
	}
	
	/**
	 * Determines whether this EventSet represents a set of Inequalities.
	 * @return
	 * 		True if this EventSet represents a set of inequalities; false otherwise.
	 */
	public boolean isInequalities(){
		return _inequalities != null;
	}
	
	/**
	 * Determines whether the EventSet represents a rate event.
	 * @return
	 * 		True if this EventSet represents a rate event; false otherwise.
	 */
	public boolean isRate(){
		return false;
	}
	
	/**
	 * Inserts an inequality event into the set of IneqaulityVariables when the EventSet is in
	 * the Inequality mode.
	 * @param e
	 * 
	 * @throws IllegalArgumentException
	 * 		Throws an IllegalArgumentException if in the Inequality mode and e
	 * 		is not an inequality event or if the EventSet is in the Transition mode.
	 */
	public void add(Event e){
		// Determine if the mode is the Inequality mode
		if(_inequalities != null){
			// We are in the inequality mode, now determine if the Event passed
			// is and inequality.
			if(e.isInequality()){
				// It is an inequality, so add it.
				_inequalities.add(e.getInequalityVariable());
			}
			else{
				// Tried to insert something other than an inequality into an
				// inequality list, so complain.
				throw new IllegalArgumentException("Cannot insert a non-inequality" +
						" into an EventSet of inequalities.");
			}
			return;
		}
		
		// We are not in the Inequality mode.
		// If we are also not in the Transition mode, then the new event determines
		// the mode.
		if(_transition == null){
			if(e.isInequality()){
				// The event is an inequality, so add it to the inequalities.
				// This also implies that the mode is the Inequality mode.
				_inequalities = new ArrayList<InequalityVariable>();
				_inequalities.add(e.getInequalityVariable());
			}
			else{
				// The event is a Transition, so store it. This also implies the
				// mode is the Transition mode.
				_transition = e.getTransition();
			}
			
			return;
		}
		
		// We are in the Transition mode. Nothing can be added in the transition mode.
		// yell.
		throw new IllegalArgumentException("Another event was attempted to be added" +
				"to an EventSet that already had a transition in it.");
	}
	
	/**
	 * Returns an iterator that returns the elements in the set as Event objects.
	 */
	public Iterator<Event> iterator(){
		return new EventSetIterator();
	}
	
	/**
	 * Clones the EventSet. Copies the internal objects by copying their reference. It does not make new instances
	 * of the contained objects.
	 */
	public EventSet clone(){
		
		// Create a new EventSet instance.
		EventSet newSet = new EventSet();
		
		// Determine whether or not the EventSet is in the Inequalty mode.
		if(_inequalities != null){
			// In the Inequality mode, we need to make a new ArrayList and copy the elements references over.
			newSet._inequalities = new ArrayList<InequalityVariable>();
			newSet._inequalities.addAll(this._inequalities);
		}
		
		else{
			// In this case we are in the Transition mode. Simple copy the transition over.
			newSet._transition = this._transition;
		}
		
		return newSet;
	}
	
	/**
	 * Removes an element from the EventSet.
	 * @param e
	 * 		The event to remove.
	 */
	public void remove(Event e){
		// If the event is a transition and is equal to the store transition
		// remove the stored transition.
		if(e.isTransition() && e.equals(_transition)){
			_transition = null;
			return;
		}
		
		// If the event is an inequality and the EventSet contains inequalities,
		// attempt to remove the event.
		if(_inequalities != null && e.isInequality()){
			_inequalities.remove(e.getInequalityVariable());
		}
	}
	
	/**
	 * Determines the number of elements in the EventSet.
	 * @return
	 * 		The number of elements in the EventSet.
	 */
	public int size(){
		if(_transition != null){
			// If we are in the Transition mode, the size is 1.
			return 1;
		}
		else{
			// If we are in the Inequality mode, the size is the 
			// number of inequalities.
			return _inequalities.size();
		}
	}
	
	/**
	 * Determines whether any elements are in the set.
	 * @return
	 * 		True if there is a least one element, false otherwise.
	 */
	public boolean isEmpty(){
		
		// If one of the member variables is not null (and contains elements),
		// the set is not empty.
		if(_transition != null || 
				(_inequalities != null && _inequalities.size() != 0)){
			return false;
		}
		return true;
	}
	
	/**
	 * Retrieve the transition that this EventSet represents.
	 * @return
	 * 		The transition that this EventSet represents or null if
	 * 		this EventSet does not represent a transition.
	 */
	public Transition getTransition(){
		return _transition;
	}
	
	/*
	 * (non-Javadoc)
	 * @see lpn.parser.Transition#toString()
	 */
	public String toString(){
		String result = "";
		
		// Check what type of events are contained in this event set.
		if(_transition != null){
			// This is a set of a singleton transition.
			result += "Transition Event Set = [" + _transition.getName();
		}
		else if (_inequalities != null){
			result += "Inequality Event Set = [" + _inequalities;
		}
		else{
			result += "Event Set = [";
		}
		
		result += "]";
		
		return result;
	}
	
	public String getName(){
		return toString();
	}
	
	/*
	 * -------------------------------------------------------------------------------------
	 *                                      Inner Class
	 * -------------------------------------------------------------------------------------
	 */
	
	/**
	 * This is the custom iterator for the EventSet class. It operates in one of two modes :
	 * the Transition mode or the Inequality mode depending on whether the EventSet that created
	 * it holds a Transition or a list of Inequalities. In the Transition mode the iterator
	 * will return the single Transition. In the Inequality mode, the Iterator will iterate
	 * through the Ineqaulity variables. All elements are returned packaged as Event objects.
	 * @author Andrew N. Fisher
	 *
	 */
	private class EventSetIterator implements Iterator<Event>{

		/*
		 * Abstraction Function : The Iterator operates in one of two modes: the
		 * Transition mode and the Inequality mode. If the _inequal variable
		 * is null, then the mode is the Transition mode, otherwise it is the
		 * Inequality mode.
		 */
		
		/*
		 * Representation Invariant : If the Iterator is created in a given mode,
		 * then it should stay in that mode. The mode is determined by whether
		 * the _ineq variable is null or not. Do not use the _tran variable to
		 * determine the mode. The _tran variable will be null in the Transition
		 * mode after returning it.
		 */
		
		// Stores the single transition if the Iterator is in the Transition mode.
		Transition _tran;
		
		// Stores the ArrayList<Inequality> objects iterator if the Iterator is in the
		// Inequality mode.
		Iterator<InequalityVariable> _inequal;
		
		/**
		 * The constructor initializes an Iterator in one of two modes : the Transition
		 * mode or the the Inequality mode. This mode is set once the Iterator is created.
		 */
		public EventSetIterator(){
			
			// Check to see in the EventSet is in a consitant state. It should only contain
			// A Transition or a list of InequalityVariables. It should not include both.
			if(_transition == null && _inequalities == null){
				throw new IllegalStateException("The EventSet has both a transition" +
						" and a set of inequalities.");
			}
			
			if(EventSet.this._inequalities != null){
				// The EventSet contains inequalities. So initialize the EventSetIterator in Inequality mode.
				_inequal = EventSet.this._inequalities.iterator();
			}
			else{
				// The EventSet contains a transition. So initialize the EventSetIterator in Transition mode.
				_tran = EventSet.this._transition;
			}
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			
			// Determine the mode the EventSetIterator is in. 
			if(_inequal != null){
				//A non-null _inequal variable indicates it is in the
				// Inequality mode, so pass the action to the _inequal iterator.
				return _inequal.hasNext();
			}
			
			// The Iterator is in the Transition mode. So determine if there is still a transition to return.
			return _tran != null;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Event next() {
			
			//Determine the mode the EventSetIterator is in.			
			if(_inequal != null){
				// The Iterator is in the Inequality mode, so pass the action to th _ineqaulities iterator.
				return new Event(_inequal.next());
			}
			
			// The Iterator is in the Transition mode. 
			if(_tran == null){
				// The transition has already been returned so complain.
				throw new NoSuchElementException("No more elements to return.");
			}
			
			// The Iterator is in the Transition mode and the transition has not be removed.
			// Remove the transition and return it.
			
			Transition tmpTran = _tran;
			
			_tran = null;
			
			return new Event(tmpTran);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			
			// Determine which mode is being operated in.
			if(_inequal == null){
				// We are in the Transition mode. This is not supported, so complain.
				throw new UnsupportedOperationException("The remove method is not supported when for the EventSet" +
						" iterator when in the Transition mode.");
			}
			else{
				_inequal.remove();
			}
		}
		
	}
	
}
