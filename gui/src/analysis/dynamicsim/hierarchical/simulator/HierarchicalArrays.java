package analysis.dynamicsim.hierarchical.simulator;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Constraint;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.EventAssignment;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.Variable;
import org.sbml.jsbml.ext.arrays.ArraysSBasePlugin;
import org.sbml.jsbml.ext.arrays.Dimension;
import org.sbml.jsbml.ext.arrays.Index;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.Submodel;

import analysis.dynamicsim.hierarchical.util.Evaluator;
import analysis.dynamicsim.hierarchical.util.HierarchicalUtilities;
import analysis.dynamicsim.hierarchical.util.Setup;
import analysis.dynamicsim.hierarchical.util.arrays.ArraysObject;
import analysis.dynamicsim.hierarchical.util.arrays.IndexObject;

public abstract class HierarchicalArrays extends HierarchicalReplacement
{

	protected enum SetupType
	{
		PARAMETER, SPECIES, COMPARTMENT, ASSIGNMENT_RULE, RATE_RULE, EVENT, CONSTRAINT, INITIAL_ASSIGNMENT, REACTION, EVENT_ASSIGNMENT;
	}

	public HierarchicalArrays(String SBMLFileName, String rootDirectory, String outputDirectory, int runs, double timeLimit, double maxTimeStep,
			double minTimeStep, JProgressBar progress, double printInterval, double stoichAmpValue, JFrame running, String[] interestingSpecies,
			String quantityType, String abstraction) throws IOException, XMLStreamException
	{
		super(SBMLFileName, rootDirectory, outputDirectory, runs, timeLimit, maxTimeStep, minTimeStep, progress, printInterval, stoichAmpValue,
				running, interestingSpecies, quantityType, abstraction);
	}

	protected String getIndexedSpeciesReference(ModelState modelstate, String reaction, String species, int[] reactionIndices)
	{

		String id = species;

		IndexObject index = modelstate.getIndexObjects().get(reaction + "__" + species);

		if (index == null)
		{
			return species;
		}

		Map<Integer, ASTNode> speciesAttribute = index.getAttributes().get("species");

		if (speciesAttribute == null)
		{
			return species;
		}

		Map<String, Integer> dimensionIdMap = new HashMap<String, Integer>();

		for (int i = 0; i < reactionIndices.length; i++)
		{
			dimensionIdMap.put("d" + i, reactionIndices[i]);
		}

		for (int i = 0; i < speciesAttribute.size(); i++)
		{
			id = id
					+ "["
					+ (int) Evaluator.evaluateExpressionRecursive(modelstate, speciesAttribute.get(i), false, getCurrentTime(), null, dimensionIdMap,
							getReplacements()) + "]";
		}

		return id;
	}

	/**
	 * 
	 */
	protected void setupArrayedModels()
	{
		ArraysSBasePlugin arrays;

		Model model = getDocument().getModel();

		CompModelPlugin comp = (CompModelPlugin) model.getPlugin("comp");

		for (Submodel sub : comp.getListOfSubmodels())
		{

			arrays = (ArraysSBasePlugin) sub.getExtension("arrays");

			if (arrays != null)
			{
				addValue(arrays, model, sub.getId());

				ModelState arrayedState = getSubmodels().remove(sub.getId());

				getArrayModels().put(sub.getId(), arrayedState);
			}
		}
	}

	protected void setupArrays(ModelState modelstate, String id, SBase sbase, SetupType type)
	{

		ArraysSBasePlugin plugin = (ArraysSBasePlugin) sbase.getExtension("arrays");

		if (plugin == null)
		{
			return;
		}

		if (plugin.isSetListOfDimensions())
		{

			modelstate.addArrayedObject(id);

			for (Dimension dimension : plugin.getListOfDimensions())
			{
				modelstate.addDimension(id, dimension.getSize(), dimension.getArrayDimension());
			}
		}

		for (Index index : plugin.getListOfIndices())
		{
			modelstate.addIndex(id, index.getReferencedAttribute(), index.getArrayDimension(), index.getMath());
		}

	}

	protected void setupArrayValue(ModelState modelstate, Variable variable, SetupType type)
	{
		int size = modelstate.getDimensionCount(variable.getId());

		if (size <= 0)
		{
			return;
		}

		double value = modelstate.getVariableToValue(getReplacements(), variable.getId());
		int[] sizes = new int[size];
		int[] indices = new int[sizes.length];

		for (ArraysObject obj : modelstate.getDimensionObjects().get(variable.getId()))
		{
			sizes[obj.getArrayDim()] = (int) modelstate.getVariableToValue(getReplacements(), obj.getSize()) - 1;
		}

		setupArrayValue(modelstate, variable, variable.getId(), value, sizes, indices, type);
	}

	protected void setupArrayObject(ModelState modelstate, String id, SBase sbase, SetupType type)
	{
		int size = modelstate.getDimensionCount(id);

		if (size <= 0)
		{
			return;
		}

		int[] sizes = new int[size];
		int[] indices = new int[sizes.length];

		for (ArraysObject obj : modelstate.getDimensionObjects().get(id))
		{
			sizes[obj.getArrayDim()] = (int) modelstate.getVariableToValue(getReplacements(), obj.getSize()) - 1;
		}

		setupArrayObject(modelstate, sbase, id, sizes, indices, type);

	}

	protected void setupArrayEventAssignments(ModelState modelstate, Event event, String eventId)
	{
		for (EventAssignment assignment : event.getListOfEventAssignments())
		{
			if (assignment.isSetMetaId() && modelstate.isDeletedByMetaID(assignment.getMetaId()))
			{
				continue;
			}

			String assignmentId = eventId + "_" + assignment.getVariable();

			setupArrays(modelstate, assignmentId, assignment, SetupType.EVENT_ASSIGNMENT);

			if (!modelstate.isArrayedObject(assignmentId))
			{
				Setup.setupEventAssignment(modelstate, assignment.getVariable(), event.getId(), assignment.getMath(), assignment, getModels(),
						getIbiosimFunctionDefinitions());
			}
		}
	}

	protected void setupSpeciesReferenceArrays(ModelState modelstate, Reaction reaction)
	{
		for (SpeciesReference reactant : reaction.getListOfReactants())
		{
			setupArrays(modelstate, reaction.getId(), reactant);
		}
		for (SpeciesReference product : reaction.getListOfProducts())
		{
			setupArrays(modelstate, reaction.getId(), product);
		}
		for (ModifierSpeciesReference modifier : reaction.getListOfModifiers())
		{
			setupArrays(modelstate, reaction.getId(), modifier);
		}
	}

	/**
	 * 
	 * @param arrays
	 * @param model
	 * @param id
	 */
	private void addValue(ArraysSBasePlugin arrays, Model model, String id)
	{
		ModelState state = getSubmodels().get(id);
		int[] sizes = new int[arrays.getDimensionCount()];
		for (Dimension dim : arrays.getListOfDimensions())
		{
			if (model.getParameter(dim.getSize()) == null)
			{
				return;
			}
			sizes[dim.getArrayDimension()] = (int) model.getParameter(dim.getSize()).getValue() - 1;
		}

		setupArrayValue(state, id, sizes, new int[sizes.length]);
	}

	private void setupArrays(ModelState modelstate, String reactionId, SimpleSpeciesReference specRef)
	{
		ArraysSBasePlugin plugin = (ArraysSBasePlugin) specRef.getExtension("arrays");

		if (plugin == null)
		{
			return;
		}

		String id = specRef.isSetId() ? specRef.getId() : reactionId + "__" + specRef.getSpecies();

		for (Dimension dimension : plugin.getListOfDimensions())
		{
			modelstate.addArrayedObject(id);
			modelstate.addDimension(id, dimension.getSize(), dimension.getArrayDimension());
		}

		for (Index index : plugin.getListOfIndices())
		{
			modelstate.addIndex(id, index.getReferencedAttribute(), index.getArrayDimension(), index.getMath());
		}
	}

	private void setupArrayObject(ModelState modelstate, SBase sbase, String id, int[] sizes, int[] indices, SetupType type)
	{
		ASTNode clone;
		String variable;
		while (sizes[sizes.length - 1] >= indices[indices.length - 1])
		{
			switch (type)
			{
			case CONSTRAINT:
				Constraint constraint = (Constraint) sbase;
				clone = constraint.getMath().clone();
				HierarchicalUtilities.replaceDimensionIds(clone, indices);
				Setup.setupSingleConstraint(modelstate, clone, getModels(), getIbiosimFunctionDefinitions());
				break;
			case RATE_RULE:
				RateRule rateRule = (RateRule) sbase;
				clone = rateRule.getMath().clone();
				variable = HierarchicalUtilities.getIndexedObject(modelstate, id, rateRule.getVariable(), "variable", indices, getReplacements());
				HierarchicalUtilities.replaceDimensionIds(clone, indices);
				Setup.setupSingleRateRule(modelstate, variable, clone, getModels(), getIbiosimFunctionDefinitions());
				break;
			case ASSIGNMENT_RULE:
				AssignmentRule assignRule = (AssignmentRule) sbase;
				clone = assignRule.getMath().clone();
				variable = HierarchicalUtilities.getIndexedObject(modelstate, id, assignRule.getVariable(), "variable", indices, getReplacements());
				HierarchicalUtilities.replaceDimensionIds(clone, indices);
				Setup.setupSingleAssignmentRule(modelstate, variable, clone, getModels(), getIbiosimFunctionDefinitions());
				break;
			case INITIAL_ASSIGNMENT:
				InitialAssignment init = (InitialAssignment) sbase;
				clone = init.getMath().clone();
				variable = HierarchicalUtilities.getIndexedObject(modelstate, id, init.getVariable(), "symbol", indices, getReplacements());
				HierarchicalUtilities.replaceDimensionIds(clone, indices);
				HierarchicalUtilities.replaceSelector(modelstate, getReplacements(), clone);
				modelstate.getInitAssignment().put(variable, clone);
				break;
			}

			indices[0]++;
			for (int i = 0; i < indices.length - 1; i++)
			{
				if (indices[i] > sizes[i])
				{
					indices[i] = 0;
					indices[i + 1]++;
				}
			}
		}
	}

	private void setupArrayValue(ModelState modelstate, Variable variable, String id, double value, int[] sizes, int[] indices, SetupType type)
	{
		while (sizes[sizes.length - 1] >= indices[indices.length - 1])
		{
			String newId = HierarchicalUtilities.getArrayedID(modelstate, id, indices);
			switch (type)
			{
			case PARAMETER:
				Setup.setupSingleParameter(modelstate, (Parameter) variable, newId);
				break;
			case SPECIES:
				Setup.setupSingleSpecies(modelstate, (Species) variable, newId, getModels(), getReplacements());
				break;
			case COMPARTMENT:
				Setup.setupSingleCompartment(modelstate, (Compartment) variable, newId, getReplacements());
				break;
			}

			indices[0]++;
			for (int i = 0; i < indices.length - 1; i++)
			{
				if (indices[i] > sizes[i])
				{
					indices[i] = 0;
					indices[i + 1]++;
				}
			}
		}
	}

	private void setupArrayValue(ModelState state, String id, int[] sizes, int[] indices)
	{
		String newId = id;

		for (int i = indices.length - 1; i >= 0; i--)
		{
			newId = newId + "_" + indices[i];
		}
		ModelState newState = state.clone();
		newState.setID(newId);
		getSubmodels().put(newId, newState);

		if (Arrays.equals(sizes, indices))
		{
			return;
		}

		indices[0]++;
		for (int i = 0; i < indices.length - 1; i++)
		{
			if (indices[i] > sizes[i])
			{
				indices[i] = 0;
				indices[i + 1]++;
			}
		}
		setupArrayValue(state, id, sizes, indices);
	}
}
