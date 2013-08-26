package sbol;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sbml.libsbml.SBase;
import org.sbolstandard.core.DnaComponent;

import biomodel.annotation.AnnotationUtility;

public class SBOLSynthesisNode {
	private String id;
	private String type;
	private int nucleotideCount;
	private String signal;
	private String coverConstraint;
	private List<SBOLSynthesisGraph> matches;
	private List<Integer> matchBounds;
	private int coverIndex;
	private List<SBOLSynthesisNode> uncoveredNodes;
	
	public SBOLSynthesisNode(String type, SBase sbmlElement, SBOLFileManager fileManager) {
		id = sbmlElement.getId();
		this.type = type;
		processDNAComponents(sbmlElement, fileManager);
		coverIndex = -1;
	}
	
	public SBOLSynthesisNode(String type) {
		this.type = type;
		nucleotideCount = 0;
		coverIndex = -1;
	}
	
	public SBOLSynthesisNode() {
		type = "x";
		nucleotideCount = 0;
		coverIndex = -1;
	}
	
	public void processDNAComponents(SBase sbmlElement, SBOLFileManager fileManager) {
		List<DnaComponent> dnaComps = fileManager.resolveURIs(AnnotationUtility.parseSBOLAnnotation(sbmlElement));
		nucleotideCount = SBOLUtility.countNucleotides(dnaComps);
		Set<String> soFilterTypes = new HashSet<String>();
		if (type.equals("s"))
			soFilterTypes.add("coding sequence");
		else if (type.equals("n") || type.equals("i"))
			soFilterTypes.add("promoter");
		List<DnaComponent> signalComps = SBOLUtility.filterDNAComponents(dnaComps, soFilterTypes);
		if (signalComps.size() > 0)
			signal = signalComps.get(0).getURI().toString();
		else 
			signal = "";
	}
	
	public void setID(String id) {
		this.id = id;
	}
	
	public String getID() {
		if (id == null)
			id = "";
		return id;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public int getNucleotideCount() {
		return nucleotideCount;
	}
	
	public void setMatchBounds(List<Integer> matchBounds) {
		this.matchBounds = matchBounds;
	}
	
	public List<Integer> getMatchBounds() {
		if (matchBounds == null)
			matchBounds = new LinkedList<Integer>();
		return matchBounds;
	}
	
	public String getSignal() {
		if (signal == null)
			signal = "";
		return signal;
	}
	
	public void setUncoveredNodes(List<SBOLSynthesisNode> uncoveredNodes) {
		this.uncoveredNodes = uncoveredNodes;
	}
	
	public List<SBOLSynthesisNode> getUncoveredNodes() {
		if (uncoveredNodes == null)
			uncoveredNodes = new LinkedList<SBOLSynthesisNode>();
		return uncoveredNodes;
	}
	
	public void setCoverConstraint(String coverConstraint) {
		this.coverConstraint = coverConstraint;
	}
	
	public String getCoverConstraint() {
		if (coverConstraint == null)
			coverConstraint = "";
		return coverConstraint;
	}
	
	public void setMatches(List<SBOLSynthesisGraph> matches) {
		this.matches = matches;
	}
	
	public List<SBOLSynthesisGraph> getMatches() {
		if (matches == null)
			matches = new LinkedList<SBOLSynthesisGraph>();
		return matches;
	}
	
	public int getCoverIndex() {
		return coverIndex;
	}
	
	public SBOLSynthesisGraph getCover(int coverIndex) {
		if (coverIndex >= 0 && matches != null && coverIndex < matches.size())
			return matches.get(coverIndex);
		else
			return null;
	}
	
	public SBOLSynthesisGraph getCover() {
		if (matches != null && matches.size() > 0) {
			if (coverIndex < 0)
				return matches.get(0);
			else if (coverIndex < matches.size())
				return matches.get(coverIndex);
			else
				return null;
		} else
			return null;
	}
	
	public int getCoverBound() {
		if (matchBounds != null && matchBounds.size() > 0) {
			if (coverIndex < 0)
				return matchBounds.get(0);
			else if (coverIndex < matchBounds.size())
				return matchBounds.get(coverIndex);
			else
				return 0;
		} else
			return 0;
	}
	
	public void terminateBranch() {
		if (matches != null)
			coverIndex = matches.size() - 1;
	}
	
	public boolean branch() {
		if (matches != null) {
			coverIndex++;
			if (coverIndex < matches.size()) {
				return true;
			} else {
				coverIndex = -1;
				return false;
			}
		} else
			return false;
	}
	
	public void sortMatches() {
		if (matches != null && matchBounds != null && matches.size() == matchBounds.size())
			quickSortMatches(0, matches.size() - 1);
	}
	
	private void quickSortMatches(int left, int right) {
		if (left < right) {
			int pivot = left + (right - left)/2;
			pivot = partitionMatches(left, right, pivot);
			quickSortMatches(left, pivot - 1);
			quickSortMatches(pivot + 1, right);
		}
	}
	
	private int partitionMatches(int left, int right, int pivot) {
		int pivotValue = matchBounds.get(pivot);
		swapMatches(pivot, right);
		int store = left;
		for (int i = left; i < right; i++)
			if (matchBounds.get(i) < pivotValue) {
				swapMatches(store, i);
				store++;
			}
		swapMatches(store, right);
		return store;
	}
	
	private void swapMatches(int first, int second) {
		if (first != second) {
			matches.add(second - 1, matches.remove(first));
			matches.add(first, matches.remove(second));
			matchBounds.add(second - 1, matchBounds.remove(first));
			matchBounds.add(first, matchBounds.remove(second));
		}
	}
	
}
