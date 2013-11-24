package analysis.main;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jlibsedml.Algorithm;
import org.jlibsedml.Annotation;
import org.jlibsedml.Libsedml;
import org.jlibsedml.Model;
import org.jlibsedml.SEDBase;
import org.jlibsedml.SEDMLDocument;
import org.jlibsedml.SedML;
import org.jlibsedml.SedMLError;
import org.jlibsedml.Simulation;
import org.jlibsedml.Task;
import org.jlibsedml.UniformTimeCourse;
import org.jlibsedml.XMLException;
import org.jlibsedml.modelsupport.SUPPORTED_LANGUAGE;

import lpn.gui.LHPNEditor;
import lpn.parser.Abstraction;
import lpn.parser.LhpnFile;
import lpn.parser.Translator;
import main.*;
import main.util.*;
import main.util.dataparser.DataParser;

import biomodel.gui.schematic.ModelEditor;
import biomodel.util.GlobalConstants;

import verification.AbstPane;
import graph.*;

/**
 * This class creates a GUI for the reb2sac program. It implements the
 * ActionListener class, the KeyListener class, the MouseListener class, and the
 * Runnable class. This allows the GUI to perform actions when buttons are
 * pressed, text is entered into a field, or the mouse is clicked. It also
 * allows this class to execute many reb2sac programs at the same time on
 * different threads.
 * 
 * @author Curtis Madsen
 */
public class AnalysisView extends JPanel implements ActionListener, Runnable, MouseListener {

	private static final long serialVersionUID = 3181014495993143825L;

	/*
	 * Radio Buttons that represent the different abstractions
	 */
	private JRadioButton none, abstraction, nary, ODE, monteCarlo, markov, fba;

	private JRadioButton sbml, dot, xhtml, lhpn; // Radio Buttons output option

	/*
	 * Radio Buttons for termination conditions
	 */
	private JRadioButton ge, gt, eq, le, lt;

	private JButton run, save; // The save and run button

	/*
	 * Text fields for changes in the abstraction
	 */
	private JTextField limit, interval, minStep, step, absErr, seed, runs, fileStem;

	private JComboBox intervalLabel;

	/*
	 * Labels for the changes in the abstraction
	 */
	private JLabel limitLabel, minStepLabel, stepLabel, errorLabel, seedLabel, runsLabel, fileStemLabel;

	/*
	 * Description of selected simulator
	 */
	private JLabel description, explanation;

	private Object[] preAbstractions = new Object[0];

	private Object[] loopAbstractions = new Object[0];

	private Object[] postAbstractions = new Object[0];

	private JComboBox simulators; // Combo Box for possible simulators

	private JLabel simulatorsLabel; // Label for possible simulators

	private JTextField rapid1, rapid2, qssa, maxCon, diffStoichAmp; // advanced options

	private JComboBox bifurcation;
	
	private JRadioButton mpde, meanPath, medianPath;
	
	private JRadioButton adaptive, nonAdaptive;
	/*
	 * advanced labels
	 */
	private JLabel rapidLabel1, rapidLabel2, qssaLabel, maxConLabel, diffStoichAmpLabel, 
		iSSATypeLabel, iSSAAdaptiveLabel, bifurcationLabel;

	private String sbmlFile, root; // sbml file and root directory

	public String getRootPath() {
		return root;
	}

	private Gui biomodelsim; // reference to the tstubd class

	private String simName; // simulation id

	private Log log; // the log

	private JTabbedPane simTab; // the simulation tab

	private ModelEditor modelEditor; // gcm editor

	private JCheckBox append;

	private JCheckBox concentrations, genRuns, genStats;

	private JLabel report;

	private boolean runFiles;

	private String separator;

	private String sbmlProp;

	private boolean change;

	private ArrayList<JFrame> frames;

	private Pattern stemPat = Pattern.compile("([a-zA-Z]|[0-9]|_)*");

	private JPanel propertiesPanel, advanced;

	private JList preAbs;

	private JList loopAbs;

	private JList postAbs;

	private JLabel preAbsLabel;

	private JLabel loopAbsLabel;

	private JLabel postAbsLabel;

	private JButton addPreAbs;

	private JButton rmPreAbs;

	private JButton editPreAbs;

	private JButton addLoopAbs;

	private JButton rmLoopAbs;

	private JButton editLoopAbs;

	private JButton addPostAbs;

	private JButton rmPostAbs;

	private JButton editPostAbs;

	private String modelFile;

	private AbstPane lhpnAbstraction;

	private JComboBox transientProperties;

	private JTextField backgroundField;

	private String selectedMarkovSim = null;

	private ArrayList<String> interestingSpecies = null;
	
	private SEDMLDocument sedmlDoc = null;

	private String sedmlFilename = "";

	/**
	 * This is the constructor for the GUI. It initializes all the input fields,
	 * puts them on panels, adds the panels to the frame, and then displays the
	 * GUI.
	 * 
	 * @param modelFile
	 */
	public AnalysisView(String sbmlFile, String sbmlProp, String root, Gui biomodelsim, String simName, Log log,
			JTabbedPane simTab, String open, String modelFile, AbstPane lhpnAbstraction,
			ArrayList<String> defaultInterestingSpecies) {
		if (File.separator.equals("\\")) {
			separator = "\\\\";
		}
		else {
			separator = File.separator;
		}

		this.interestingSpecies = new ArrayList<String>();

		this.biomodelsim = biomodelsim;
		this.sbmlFile = sbmlFile;
		this.sbmlProp = sbmlProp;
		this.root = root;
		this.simName = simName;
		this.log = log;
		this.simTab = simTab;
		this.lhpnAbstraction = lhpnAbstraction;
		change = false;
		frames = new ArrayList<JFrame>();
		String[] tempArray = modelFile.split(separator);
		this.modelFile = tempArray[tempArray.length - 1];
		
		// Creates the input fields for the changes in abstraction
		Preferences biosimrc = Preferences.userRoot();
		String[] odeSimulators = new String[6];
		odeSimulators[0] = "euler";
		odeSimulators[1] = "gear1";
		odeSimulators[2] = "gear2";
		odeSimulators[3] = "rk4imp";
		odeSimulators[4] = "rk8pd";
		odeSimulators[5] = "rkf45";
		explanation = new JLabel("Description Of Selected Simulator:     ");
		description = new JLabel("Embedded Runge-Kutta-Fehlberg (4, 5) method");
		simulators = new JComboBox(odeSimulators);
		simulators.setSelectedItem("rkf45");
		simulators.addActionListener(this);
		limit = new JTextField(biosimrc.get("biosim.sim.limit", ""), 39);
		interval = new JTextField(biosimrc.get("biosim.sim.interval", ""), 15);
		minStep = new JTextField(biosimrc.get("biosim.sim.min.step", ""), 15);
		step = new JTextField(biosimrc.get("biosim.sim.step", ""), 15);
		absErr = new JTextField(biosimrc.get("biosim.sim.error", ""), 15);
		int next = 1;
		String filename = "sim" + next;
		while (new File(root + separator + filename).exists()) {
			next++;
			filename = "sim" + next;
		}
		// dir = new JTextField(filename, 15);
		seed = new JTextField(biosimrc.get("biosim.sim.seed", ""), 15);
		runs = new JTextField(biosimrc.get("biosim.sim.runs", ""), 15);
		simulatorsLabel = new JLabel("Possible Simulators/Analyzers:");
		limitLabel = new JLabel("Time Limit:");
		String[] intervalChoices = { "Print Interval", "Minimum Print Interval", "Number Of Steps" };
		intervalLabel = new JComboBox(intervalChoices);
		intervalLabel.setSelectedItem(biosimrc.get("biosim.sim.useInterval", ""));
		minStepLabel = new JLabel("Minimum Time Step:");
		stepLabel = new JLabel("Maximum Time Step:");
		errorLabel = new JLabel("Absolute Error:");
		seedLabel = new JLabel("Random Seed:");
		runsLabel = new JLabel("Runs:");
		fileStem = new JTextField("", 15);
		fileStemLabel = new JLabel("Simulation ID:");
		JPanel inputHolder = new JPanel(new BorderLayout());
		JPanel inputHolderLeft;
		JPanel inputHolderRight;
		if (modelFile.contains(".lpn") || modelFile.contains(".gcm")) {
			inputHolderLeft = new JPanel(new GridLayout(11, 1));
			inputHolderRight = new JPanel(new GridLayout(11, 1));
		}
		else {
			inputHolderLeft = new JPanel(new GridLayout(10, 1));
			inputHolderRight = new JPanel(new GridLayout(10, 1));
		}
		inputHolderLeft.add(simulatorsLabel);
		inputHolderRight.add(simulators);
		inputHolderLeft.add(explanation);
		inputHolderRight.add(description);
		inputHolderLeft.add(limitLabel);
		inputHolderRight.add(limit);
		inputHolderLeft.add(intervalLabel);
		inputHolderRight.add(interval);
		inputHolderLeft.add(minStepLabel);
		inputHolderRight.add(minStep);
		inputHolderLeft.add(stepLabel);
		inputHolderRight.add(step);
		inputHolderLeft.add(errorLabel);
		inputHolderRight.add(absErr);
		inputHolderLeft.add(seedLabel);
		inputHolderRight.add(seed);
		inputHolderLeft.add(runsLabel);
		inputHolderRight.add(runs);
		inputHolderLeft.add(fileStemLabel);
		inputHolderRight.add(fileStem);
		if (modelFile.contains(".lpn")) {
			JLabel prop = new JLabel("Property:");
			String[] props = new String[] { "none" };
			LhpnFile lpn = new LhpnFile();
			lpn.load(root + separator + modelFile);
			String[] getProps = lpn.getProperties().toArray(new String[0]);
			props = new String[getProps.length + 1];
			props[0] = "none";
			for (int i = 0; i < getProps.length; i++) {
				props[i + 1] = getProps[i];
			}
			transientProperties = new JComboBox(props);
			transientProperties.setPreferredSize(new Dimension(5, 10));
			inputHolderLeft.add(prop);
			inputHolderRight.add(transientProperties);
		}
		inputHolder.add(inputHolderLeft, "West");
		inputHolder.add(inputHolderRight, "Center");
		JPanel topInputHolder = new JPanel();
		topInputHolder.add(inputHolder);

		// Creates the interesting species JList
		preAbs = new JList();
		loopAbs = new JList();
		postAbs = new JList();
		preAbsLabel = new JLabel("Preprocess abstraction methods:");
		loopAbsLabel = new JLabel("Main loop abstraction methods:");
		postAbsLabel = new JLabel("Postprocess abstraction methods:");
		JPanel absHolder = new JPanel(new BorderLayout());
		JPanel listOfAbsLabelHolder = new JPanel(new GridLayout(1, 3));
		JPanel listOfAbsHolder = new JPanel(new GridLayout(1, 3));
		JPanel listOfAbsButtonHolder = new JPanel(new GridLayout(1, 3));
		JScrollPane preAbsScroll = new JScrollPane();
		JScrollPane loopAbsScroll = new JScrollPane();
		JScrollPane postAbsScroll = new JScrollPane();
		preAbsScroll.setMinimumSize(new Dimension(260, 200));
		preAbsScroll.setPreferredSize(new Dimension(276, 132));
		preAbsScroll.setViewportView(preAbs);
		loopAbsScroll.setMinimumSize(new Dimension(260, 200));
		loopAbsScroll.setPreferredSize(new Dimension(276, 132));
		loopAbsScroll.setViewportView(loopAbs);
		postAbsScroll.setMinimumSize(new Dimension(260, 200));
		postAbsScroll.setPreferredSize(new Dimension(276, 132));
		postAbsScroll.setViewportView(postAbs);
		addPreAbs = new JButton("Add");
		rmPreAbs = new JButton("Remove");
		editPreAbs = new JButton("Edit");
		JPanel preAbsButtonHolder = new JPanel();
		preAbsButtonHolder.add(addPreAbs);
		preAbsButtonHolder.add(rmPreAbs);
		// preAbsButtonHolder.add(editPreAbs);
		addLoopAbs = new JButton("Add");
		rmLoopAbs = new JButton("Remove");
		editLoopAbs = new JButton("Edit");
		JPanel loopAbsButtonHolder = new JPanel();
		loopAbsButtonHolder.add(addLoopAbs);
		loopAbsButtonHolder.add(rmLoopAbs);
		// loopAbsButtonHolder.add(editLoopAbs);
		addPostAbs = new JButton("Add");
		rmPostAbs = new JButton("Remove");
		editPostAbs = new JButton("Edit");
		JPanel postAbsButtonHolder = new JPanel();
		postAbsButtonHolder.add(addPostAbs);
		postAbsButtonHolder.add(rmPostAbs);
		// postAbsButtonHolder.add(editPostAbs);
		listOfAbsLabelHolder.add(preAbsLabel);
		listOfAbsHolder.add(preAbsScroll);
		listOfAbsLabelHolder.add(loopAbsLabel);
		listOfAbsHolder.add(loopAbsScroll);
		listOfAbsLabelHolder.add(postAbsLabel);
		listOfAbsHolder.add(postAbsScroll);
		listOfAbsButtonHolder.add(preAbsButtonHolder);
		listOfAbsButtonHolder.add(loopAbsButtonHolder);
		listOfAbsButtonHolder.add(postAbsButtonHolder);
		absHolder.add(listOfAbsLabelHolder, "North");
		absHolder.add(listOfAbsHolder, "Center");
		absHolder.add(listOfAbsButtonHolder, "South");
		preAbs.setEnabled(false);
		loopAbs.setEnabled(false);
		postAbs.setEnabled(false);
		preAbs.addMouseListener(this);
		loopAbs.addMouseListener(this);
		postAbs.addMouseListener(this);
		preAbsLabel.setEnabled(false);
		loopAbsLabel.setEnabled(false);
		postAbsLabel.setEnabled(false);
		addPreAbs.setEnabled(false);
		rmPreAbs.setEnabled(false);
		editPreAbs.setEnabled(false);
		addPreAbs.addActionListener(this);
		rmPreAbs.addActionListener(this);
		editPreAbs.addActionListener(this);
		addLoopAbs.setEnabled(false);
		rmLoopAbs.setEnabled(false);
		editLoopAbs.setEnabled(false);
		addLoopAbs.addActionListener(this);
		rmLoopAbs.addActionListener(this);
		editLoopAbs.addActionListener(this);
		addPostAbs.setEnabled(false);
		rmPostAbs.setEnabled(false);
		editPostAbs.setEnabled(false);
		addPostAbs.addActionListener(this);
		rmPostAbs.addActionListener(this);
		editPostAbs.addActionListener(this);

		// Creates some abstraction options
		JPanel advancedGrid = new JPanel(new GridLayout(8, 4));
		advanced = new JPanel(new BorderLayout());
		
		rapidLabel1 = new JLabel("Rapid Equilibrium Condition 1:");
		rapid1 = new JTextField(biosimrc.get("biosim.sim.rapid1", ""), 15);
		rapidLabel2 = new JLabel("Rapid Equilibrium Condition 2:");
		rapid2 = new JTextField(biosimrc.get("biosim.sim.rapid2", ""), 15);
		qssaLabel = new JLabel("QSSA Condition:");
		qssa = new JTextField(biosimrc.get("biosim.sim.qssa", ""), 15);
		maxConLabel = new JLabel("Max Concentration Threshold:");
		maxCon = new JTextField(biosimrc.get("biosim.sim.concentration", ""), 15);
		diffStoichAmp = new JTextField("1.0", 15);
		diffStoichAmpLabel = new JLabel("Grid Diffusion Stoichiometry Amplification:");
		String [] options = { "1", "2" };

		mpde = new JRadioButton();
		mpde.setText("MPDE");
		mpde.addActionListener(this);
		meanPath = new JRadioButton();
		meanPath.setText("Mean Path");
		meanPath.addActionListener(this);
		medianPath = new JRadioButton();
		medianPath.setText("Median Path");
		medianPath.addActionListener(this);
		ButtonGroup iSSATypeButtons = new ButtonGroup();
		iSSATypeButtons.add(mpde);
		iSSATypeButtons.add(meanPath);
		iSSATypeButtons.add(medianPath);
		medianPath.setSelected(true);
		JPanel iSSAType = new JPanel(new GridLayout(1,3));
		iSSAType.add(mpde);
		iSSAType.add(meanPath);
		iSSAType.add(medianPath);
		iSSATypeLabel = new JLabel("iSSA Type:");

		adaptive = new JRadioButton();
		adaptive.setText("Adaptive");
		nonAdaptive = new JRadioButton();
		nonAdaptive.setText("Non-adaptive");
		ButtonGroup iSSAAdaptiveButtons = new ButtonGroup();
		iSSAAdaptiveButtons.add(adaptive);
		iSSAAdaptiveButtons.add(nonAdaptive);
		adaptive.setSelected(true);
		JPanel iSSAAdaptive = new JPanel(new GridLayout(1,2));
		iSSAAdaptive.add(adaptive);
		iSSAAdaptive.add(nonAdaptive);
		iSSAAdaptiveLabel = new JLabel("iSSA Adaptive:");
		
		bifurcation = new JComboBox(options);
		bifurcationLabel = new JLabel("Number of Paths to Follow with iSSA:");
		
		maxConLabel.setEnabled(false);
		maxCon.setEnabled(false);
		qssaLabel.setEnabled(false);
		qssa.setEnabled(false);
		rapidLabel1.setEnabled(false);
		rapid1.setEnabled(false);
		rapidLabel2.setEnabled(false);
		rapid2.setEnabled(false);
		diffStoichAmp.setEnabled(false);
		diffStoichAmpLabel.setEnabled(false);
		mpde.setEnabled(false);
		meanPath.setEnabled(false);
		medianPath.setEnabled(false);
		iSSATypeLabel.setEnabled(false);
		adaptive.setEnabled(false);
		nonAdaptive.setEnabled(false);
		iSSAAdaptiveLabel.setEnabled(false);
		bifurcation.setEnabled(false);
		bifurcationLabel.setEnabled(false);
		
		advancedGrid.add(rapidLabel1);
		advancedGrid.add(rapid1);
		advancedGrid.add(rapidLabel2);
		advancedGrid.add(rapid2);
		advancedGrid.add(qssaLabel);
		advancedGrid.add(qssa);
		advancedGrid.add(maxConLabel);
		advancedGrid.add(maxCon);
		advancedGrid.add(diffStoichAmpLabel);
		advancedGrid.add(diffStoichAmp);
		advancedGrid.add(iSSATypeLabel);
		advancedGrid.add(iSSAType);
		advancedGrid.add(iSSAAdaptiveLabel);
		advancedGrid.add(iSSAAdaptive);
		advancedGrid.add(bifurcationLabel);
		advancedGrid.add(bifurcation);
		JPanel advAbs = new JPanel(new BorderLayout());
		advAbs.add(absHolder, "Center");
		advAbs.add(advancedGrid, "South");
		advanced.add(advAbs, "North");
		// advanced.add(interestingPanel, "Center");

		// Sets up the radio buttons for Abstraction and Nary
		JLabel choose = new JLabel("Abstraction:");
		none = new JRadioButton("None");
		abstraction = new JRadioButton("Abstraction");
		nary = new JRadioButton("Logical Abstraction");
		ButtonGroup abs = new ButtonGroup();
		abs.add(none);
		abs.add(abstraction);
		abs.add(nary);
		none.setSelected(true);
		if (modelFile.contains(".lpn")) {
			nary.setEnabled(false);
		}
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel backgroundPanel = new JPanel();
		JLabel backgroundLabel = new JLabel("Model File:");
		backgroundField = new JTextField(this.modelFile);
		backgroundField.setEditable(false);
		backgroundPanel.add(backgroundLabel);
		backgroundPanel.add(backgroundField);
		JPanel absAndNaryPanel = new JPanel();
		absAndNaryPanel.add(choose);
		absAndNaryPanel.add(none);
		absAndNaryPanel.add(abstraction);
		absAndNaryPanel.add(nary);
		topPanel.add(backgroundPanel, BorderLayout.NORTH);
		topPanel.add(absAndNaryPanel, BorderLayout.SOUTH);
		none.addActionListener(this);
		abstraction.addActionListener(this);
		nary.addActionListener(this);

		// Sets up the radio buttons for ODE, Monte Carlo, and Markov
		JLabel choose2 = new JLabel("Simulation Type:");
		ODE = new JRadioButton("ODE");
		monteCarlo = new JRadioButton("Monte Carlo");
		markov = new JRadioButton("Markov");
		fba = new JRadioButton("FBA");
		ODE.setSelected(true);
		seed.setEnabled(true);
		seedLabel.setEnabled(true);
		runs.setEnabled(true);
		runsLabel.setEnabled(true);
		fileStem.setEnabled(true);
		fileStemLabel.setEnabled(true);
		minStep.setEnabled(true);
		minStepLabel.setEnabled(true);
		step.setEnabled(true);
		stepLabel.setEnabled(true);
		absErr.setEnabled(true);
		errorLabel.setEnabled(true);
		JPanel odeMonteAndMarkovPanel = new JPanel();
		odeMonteAndMarkovPanel.add(choose2);
		odeMonteAndMarkovPanel.add(ODE);
		odeMonteAndMarkovPanel.add(monteCarlo);
		odeMonteAndMarkovPanel.add(markov);
		odeMonteAndMarkovPanel.add(fba);
		ODE.addActionListener(this);
		monteCarlo.addActionListener(this);
		markov.addActionListener(this);
		fba.addActionListener(this);

		// Sets up the radio buttons for output option
		sbml = new JRadioButton("Model");
		dot = new JRadioButton("Network");
		xhtml = new JRadioButton("Browser");
		lhpn = new JRadioButton("LPN");
		sbml.setSelected(true);
		odeMonteAndMarkovPanel.add(sbml);
		odeMonteAndMarkovPanel.add(dot);
		odeMonteAndMarkovPanel.add(xhtml);
		//odeMonteAndMarkovPanel.add(lhpn);
		sbml.addActionListener(this);
		dot.addActionListener(this);
		xhtml.addActionListener(this);
		lhpn.addActionListener(this);
		ButtonGroup sim = new ButtonGroup();
		sim.add(ODE);
		sim.add(monteCarlo);
		sim.add(markov);
		sim.add(fba);
		sim.add(sbml);
		sim.add(dot);
		sim.add(xhtml);
		sim.add(lhpn);

		JPanel reportPanel = new JPanel();
		report = new JLabel("Options:");
		reportPanel.add(report);

		concentrations = new JCheckBox("Report Concentrations");
		concentrations.setEnabled(true);
		reportPanel.add(concentrations);
		concentrations.addActionListener(this);

		genRuns = new JCheckBox("Do Not Generate Runs");
		genRuns.setEnabled(true);
		reportPanel.add(genRuns);
		genRuns.addActionListener(this);

		append = new JCheckBox("Append Simulation Runs");
		append.setEnabled(true);
		reportPanel.add(append);
		append.addActionListener(this);
		
		genStats = new JCheckBox("Generate Statistics");
		genStats.setEnabled(true);
		reportPanel.add(genStats);
		genStats.addActionListener(this);

		// Puts all the radio buttons in a panel
		JPanel radioButtonPanel = new JPanel(new BorderLayout());
		radioButtonPanel.add(topPanel, "North");
		radioButtonPanel.add(odeMonteAndMarkovPanel, "Center");
		JPanel bottomPanel = new JPanel(new BorderLayout());
		// bottomPanel.add(overwritePanel, "North");
		bottomPanel.add(reportPanel, "South");
		radioButtonPanel.add(bottomPanel, "South");

		// Creates the main tabbed panel
		JPanel mainTabbedPanel = new JPanel(new BorderLayout());
		mainTabbedPanel.add(topInputHolder, "Center");
		mainTabbedPanel.add(radioButtonPanel, "North");

		// Creates the run button
		run = new JButton("Save and Run");
		save = new JButton("Save Parameters");
		JPanel runHolder = new JPanel();
		runHolder.add(run);
		run.addActionListener(this);
		run.setMnemonic(KeyEvent.VK_R);
		runHolder.add(save);
		save.addActionListener(this);
		save.setMnemonic(KeyEvent.VK_S);

		this.setLayout(new BorderLayout());
		this.add(mainTabbedPanel, "Center");
		runFiles = false;
		String[] searchForRunFiles = new File(root + separator + simName).list();
		for (String s : searchForRunFiles) {
			if (s.length() > 3 && s.substring(0, 4).equals("run-")) {
				runFiles = true;
			}
		}
		if (biosimrc.get("biosim.sim.abs", "").equals("None")) {
			none.doClick();
		}
		else if (biosimrc.get("biosim.sim.abs", "").equals("Abstraction")) {
			abstraction.doClick();
		}
		else {
			nary.doClick();
		}
		if (biosimrc.get("biosim.sim.type", "").equals("ODE")) {
			ODE.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
		}
		else if (biosimrc.get("biosim.sim.type", "").equals("Monte Carlo")) {
			monteCarlo.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
		}
		else if (biosimrc.get("biosim.sim.type", "").equals("Markov")) {
			markov.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
			if (!simulators.getSelectedItem().equals(biosimrc.get("biosim.sim.sim", ""))) {
				selectedMarkovSim = biosimrc.get("biosim.sim.sim", "");
			}
		}
		else if (biosimrc.get("biosim.sim.type", "").equals("FBA")) {
			fba.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
		}
		else if (biosimrc.get("biosim.sim.type", "").equals("SBML")) {
			sbml.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
			sbml.doClick();
		}
		else if (biosimrc.get("biosim.sim.type", "").equals("LPN")) {
			if (lhpn.isEnabled()) {
				lhpn.doClick();
				simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
				lhpn.doClick();
			}
		}
		else if (biosimrc.get("biosim.sim.type", "").equals("Network")) {
			dot.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
			dot.doClick();
		}
		else {
			xhtml.doClick();
			simulators.setSelectedItem(biosimrc.get("biosim.sim.sim", ""));
			xhtml.doClick();
		}
		if (open != null) {
			open(open);
		}
		sedmlFilename = root + separator + simName + separator + modelFile.replace(".xml","") + "-sedml.xml";
		loadSEDML();
	}

	/**
	 * This method performs different functions depending on what buttons are
	 * pushed and what input fields contain data.
	 */
	public void actionPerformed(ActionEvent e) {
		// if the none Radio Button is selected
		change = true;
		if (e.getSource() == none) {
			Button_Enabling.enableNoneOrAbs(ODE, monteCarlo, markov, fba, sbml, seed, seedLabel, runs, runsLabel,
					minStepLabel, minStep, stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel,
					interval, simulators, simulatorsLabel, explanation, description, none, rapid1, rapid2, qssa,
					maxCon, diffStoichAmp, rapidLabel1, rapidLabel2, qssaLabel, maxConLabel, diffStoichAmpLabel, 
					fileStem, fileStemLabel, preAbs, loopAbs,
					postAbs, preAbsLabel, loopAbsLabel, postAbsLabel, addPreAbs, rmPreAbs, editPreAbs, addLoopAbs,
					rmLoopAbs, editLoopAbs, addPostAbs, rmPostAbs, editPostAbs, lhpn);
			if (modelFile.contains(".lpn") || modelFile.contains(".s") || modelFile.contains(".inst")) {
				markov.setEnabled(true);
				lhpn.setEnabled(true);
			}
			if (!sbml.isSelected() && !xhtml.isSelected() && !dot.isSelected() && runFiles) {
				append.setEnabled(true);
				concentrations.setEnabled(true);
				genRuns.setEnabled(true);
				genStats.setEnabled(true);
				report.setEnabled(true);
				if (append.isSelected()) {
					limit.setEnabled(false);
					interval.setEnabled(false);
					limitLabel.setEnabled(false);
					intervalLabel.setEnabled(false);
				}
				else {
					limit.setEnabled(true);
					interval.setEnabled(true);
					limitLabel.setEnabled(true);
					intervalLabel.setEnabled(true);
				}
			}
		}
		// if the abstraction Radio Button is selected
		else if (e.getSource() == abstraction) {
			Button_Enabling.enableNoneOrAbs(ODE, monteCarlo, markov, fba, sbml, seed, seedLabel, runs, runsLabel,
					minStepLabel, minStep, stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel,
					interval, simulators, simulatorsLabel, explanation, description, none, rapid1, rapid2, qssa,
					maxCon, diffStoichAmp, rapidLabel1, rapidLabel2, qssaLabel, maxConLabel, diffStoichAmpLabel, 
					fileStem, fileStemLabel, preAbs, loopAbs,
					postAbs, preAbsLabel, loopAbsLabel, postAbsLabel, addPreAbs, rmPreAbs, editPreAbs, addLoopAbs,
					rmLoopAbs, editLoopAbs, addPostAbs, rmPostAbs, editPostAbs, lhpn);
			if (modelFile.contains(".lpn")) {
				markov.setEnabled(true);
				lhpn.setEnabled(true);
			}
			if (!sbml.isSelected() && !xhtml.isSelected() && !dot.isSelected() && runFiles) {
				append.setEnabled(true);
				concentrations.setEnabled(true);
				genRuns.setEnabled(true);
				genStats.setEnabled(true);
				report.setEnabled(true);
				if (append.isSelected()) {
					limit.setEnabled(false);
					interval.setEnabled(false);
					limitLabel.setEnabled(false);
					intervalLabel.setEnabled(false);
				}
				else {
					limit.setEnabled(true);
					interval.setEnabled(true);
					limitLabel.setEnabled(true);
					intervalLabel.setEnabled(true);
				}
			}
		}
		// if the nary Radio Button is selected
		else if (e.getSource() == nary) {
			Button_Enabling.enableNary(ODE, monteCarlo, markov, fba, seed, seedLabel, runs, runsLabel, minStepLabel,
					minStep, stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
					simulators, simulatorsLabel, explanation, description, rapid1, rapid2, qssa, maxCon, rapidLabel1,
					rapidLabel2, qssaLabel, maxConLabel, fileStem, fileStemLabel, preAbs, loopAbs, postAbs,
					preAbsLabel, loopAbsLabel, postAbsLabel, addPreAbs, rmPreAbs, editPreAbs, addLoopAbs, rmLoopAbs,
					editLoopAbs, addPostAbs, rmPostAbs, editPostAbs, lhpn, modelEditor);
			if (!sbml.isSelected() && !xhtml.isSelected() && !dot.isSelected() && runFiles) {
				append.setEnabled(true);
				concentrations.setEnabled(true);
				genRuns.setEnabled(true);
				genStats.setEnabled(true);
				report.setEnabled(true);
				if (append.isSelected()) {
					limit.setEnabled(false);
					interval.setEnabled(false);
					limitLabel.setEnabled(false);
					intervalLabel.setEnabled(false);
				}
				else {
					limit.setEnabled(true);
					interval.setEnabled(true);
					limitLabel.setEnabled(true);
					intervalLabel.setEnabled(true);
				}
			}
		}
		// if the ODE Radio Button is selected
		else if (e.getSource() == ODE) {
			Button_Enabling.enableODE(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel, step,
					errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, postAbs, abstraction);
			append.setEnabled(true);
			concentrations.setEnabled(true);
			genRuns.setEnabled(true);
			genStats.setEnabled(true);
			report.setEnabled(true);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the monteCarlo Radio Button is selected
		else if (e.getSource() == monteCarlo) {
			Button_Enabling.enableMonteCarlo(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel, step,
					errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, postAbs, abstraction, nary);
			if (runFiles) {
				append.setEnabled(true);
				concentrations.setEnabled(true);
				genRuns.setEnabled(true);
				genStats.setEnabled(true);
				report.setEnabled(true);
				if (append.isSelected()) {
					limit.setEnabled(false);
					interval.setEnabled(false);
					limitLabel.setEnabled(false);
					intervalLabel.setEnabled(false);
				}
				else {
					limit.setEnabled(true);
					interval.setEnabled(true);
					limitLabel.setEnabled(true);
					intervalLabel.setEnabled(true);
				}
			}
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the markov Radio Button is selected
		else if (e.getSource() == markov) {
			Button_Enabling.enableMarkov(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel, step,
					errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, modelEditor, postAbs, modelFile);
			append.setEnabled(false);
			concentrations.setEnabled(false);
			genRuns.setEnabled(false);
			genStats.setEnabled(false);
			report.setEnabled(false);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the sbml Radio Button is selected
		else if (e.getSource() == fba) {
			Button_Enabling.enableFBA(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
					step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, abstraction, nary, loopAbs, postAbs);
			append.setEnabled(false);
			concentrations.setEnabled(false);
			genRuns.setEnabled(false);
			genStats.setEnabled(false);
			absErr.setEnabled(true);
			report.setEnabled(false);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		else if (e.getSource() == sbml) {
			Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
					step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, abstraction, loopAbs, postAbs);
			append.setEnabled(false);
			concentrations.setEnabled(false);
			genRuns.setEnabled(false);
			genStats.setEnabled(false);
			report.setEnabled(false);
			absErr.setEnabled(false);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the dot Radio Button is selected
		else if (e.getSource() == dot) {
			Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
					step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, abstraction, loopAbs, postAbs);
			append.setEnabled(false);
			concentrations.setEnabled(false);
			genRuns.setEnabled(false);
			genStats.setEnabled(false);
			report.setEnabled(false);
			absErr.setEnabled(false);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the xhtml Radio Button is selected
		else if (e.getSource() == xhtml) {
			Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
					step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, abstraction, loopAbs, postAbs);
			append.setEnabled(false);
			concentrations.setEnabled(false);
			genRuns.setEnabled(false);
			genStats.setEnabled(false);
			report.setEnabled(false);
			absErr.setEnabled(false);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the lhpn Radio Button is selected
		else if (e.getSource() == lhpn) {
			Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
					step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
					explanation, description, fileStem, fileStemLabel, abstraction, loopAbs, postAbs);
			append.setEnabled(false);
			concentrations.setEnabled(false);
			genRuns.setEnabled(false);
			genStats.setEnabled(false);
			report.setEnabled(false);
			absErr.setEnabled(false);
			mpde.setEnabled(false);
			meanPath.setEnabled(false);
			medianPath.setEnabled(false);
			iSSATypeLabel.setEnabled(false);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		// if the add interesting species button is clicked
		// else if (e.getSource() == addIntSpecies) {
		// addInterstingSpecies();
		// }
		// if the add interesting species button is clicked
		// else if (e.getSource() == editIntSpecies) {
		// editInterstingSpecies();
		// }
		// if the remove interesting species button is clicked
		// else if (e.getSource() == removeIntSpecies) {
		// removeIntSpecies();
		// }
		// if the clear interesting species button is clicked
		// else if (e.getSource() == clearIntSpecies) {
		// int[] select = new int[interestingSpecies.length];
		// for (int i = 0; i < interestingSpecies.length; i++) {
		// select[i] = i;
		// }
		// //species.setSelectedIndices(select);
		// removeIntSpecies();
		// }
		// if the add termination conditions button is clicked
		/*
		 * else if (e.getSource() == addTermCond) { termConditions =
		 * Utility.add(termConditions, terminations, termCond, true, amountTerm,
		 * ge, gt, eq, lt, le, this); } // if the remove termination conditions
		 * button is clicked else if (e.getSource() == removeTermCond) {
		 * termConditions = Utility.remove(terminations, termConditions); } //
		 * if the clear termination conditions button is clicked else if
		 * (e.getSource() == clearTermCond) { termConditions = new Object[0];
		 * terminations.setListData(termConditions); }
		 */
		// if the simulators combo box is selected
		else if (e.getSource() == mpde) {
			nonAdaptive.setSelected(true);
			adaptive.setEnabled(false);
			nonAdaptive.setEnabled(false);
			iSSAAdaptiveLabel.setEnabled(false);
			bifurcation.setEnabled(false);
			bifurcationLabel.setEnabled(false);
		}
		else if (e.getSource() == meanPath) {
			adaptive.setEnabled(true);
			nonAdaptive.setEnabled(true);
			iSSAAdaptiveLabel.setEnabled(true);
			bifurcation.setEnabled(true);
			bifurcationLabel.setEnabled(true);
		}
		else if (e.getSource() == medianPath) {
			adaptive.setEnabled(true);
			nonAdaptive.setEnabled(true);
			iSSAAdaptiveLabel.setEnabled(true);
			bifurcation.setEnabled(true);
			bifurcationLabel.setEnabled(true);
		}
		else if (e.getSource() == simulators) {
			if (simulators.getItemCount() == 0) {
				description.setText("");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("euler")) {
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				absErr.setEnabled(false);
				errorLabel.setEnabled(false);
				description.setText("Euler method");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("gear1")) {
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				absErr.setEnabled(true);
				errorLabel.setEnabled(true);
				description.setText("Gear method, M=1");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("gear2")) {
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				absErr.setEnabled(true);
				errorLabel.setEnabled(true);
				description.setText("Gear method, M=2");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("rk4imp")) {
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				absErr.setEnabled(true);
				errorLabel.setEnabled(true);
				description.setText("Implicit 4th order Runge-Kutta at Gaussian points");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("rk8pd")) {
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				absErr.setEnabled(true);
				errorLabel.setEnabled(true);
				description.setText("Embedded Runge-Kutta Prince-Dormand (8,9) method");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("rkf45")) {
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				absErr.setEnabled(true);
				errorLabel.setEnabled(true);
				description.setText("Embedded Runge-Kutta-Fehlberg (4, 5) method");
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).equals("SSA-CR")) {
				description.setText("SSA Composition and Rejection Method");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).equals("SSA-Direct")) {
				description.setText("SSA-Direct Method (Java)");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).equals("Runge-Kutta-Fehlberg")) {
				description.setText("Runge-Kutta-Fehlberg Method (java)");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(true);
				absErr.setEnabled(true);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).equals("Hierarchical-RK")) {
				description.setText("Runge-Kutta-Fehlberg Method on Hierarchical Models (java)");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(true);
				absErr.setEnabled(true);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).equals("Hierarchical-Hybrid")) {
				description.setText("Hybrid SSA/ODE on Hierarchical Models (java)");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(true);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).contains("gillespie")) {
				description.setText("SSA-Direct Method");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).contains("SSA-Hierarchical")) {
				description.setText("SSA-Direct Method on Hierarchical Models (java)");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (((String) simulators.getSelectedItem()).contains("interactive")) {
				description.setText("Interactive SSA-Direct Method (java)");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("iSSA")) {
				description.setText("incremental SSA");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(true);
				meanPath.setEnabled(true);
				medianPath.setEnabled(true);
				iSSATypeLabel.setEnabled(true);
				if (mpde.isSelected()) {
					adaptive.setEnabled(false);
					nonAdaptive.setEnabled(false);
					iSSAAdaptiveLabel.setEnabled(false);
					bifurcation.setEnabled(false);
					bifurcationLabel.setEnabled(false);
				} else {
					adaptive.setEnabled(true);
					nonAdaptive.setEnabled(true);
					iSSAAdaptiveLabel.setEnabled(true);
					bifurcation.setEnabled(true);
					bifurcationLabel.setEnabled(true);
				}
			}
			else if (simulators.getSelectedItem().equals("emc-sim")) {
				description.setText("Monte Carlo sim with jump count as" + " independent variable");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("bunker")) {
				description.setText("Bunker's method");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("nmc")) {
				description.setText("Monte Carlo simulation with normally" + " distributed waiting time");
				minStep.setEnabled(true);
				minStepLabel.setEnabled(true);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("ctmc-transient")) {
				description.setText("Transient Distribution Analysis");
				minStep.setEnabled(false);
				minStepLabel.setEnabled(false);
				step.setEnabled(false);
				stepLabel.setEnabled(false);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				limitLabel.setEnabled(false);
				limit.setEnabled(false);
				intervalLabel.setEnabled(false);
				interval.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("atacs")) {
				description.setText("ATACS Analysis Tool");
				minStep.setEnabled(false);
				minStepLabel.setEnabled(false);
				step.setEnabled(false);
				stepLabel.setEnabled(false);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				limitLabel.setEnabled(false);
				limit.setEnabled(false);
				intervalLabel.setEnabled(false);
				interval.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("reachability-analysis")) {
				description.setText("State Space Exploration");
				minStep.setEnabled(false);
				minStepLabel.setEnabled(false);
				step.setEnabled(false);
				stepLabel.setEnabled(false);
				errorLabel.setEnabled(false);
				absErr.setEnabled(false);
				limitLabel.setEnabled(false);
				limit.setEnabled(false);
				intervalLabel.setEnabled(false);
				interval.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("steady-state-markov-chain-analysis")) {
				description.setText("Steady State Markov Chain Analysis");
				minStep.setEnabled(false);
				minStepLabel.setEnabled(false);
				step.setEnabled(false);
				stepLabel.setEnabled(false);
				errorLabel.setEnabled(true);
				absErr.setEnabled(true);
				limitLabel.setEnabled(false);
				limit.setEnabled(false);
				intervalLabel.setEnabled(false);
				interval.setEnabled(false);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
			else if (simulators.getSelectedItem().equals("transient-markov-chain-analysis")) {
				description.setText("Transient Markov Chain Analysis Using Uniformization");
				minStep.setEnabled(false);
				minStepLabel.setEnabled(false);
				step.setEnabled(true);
				stepLabel.setEnabled(true);
				errorLabel.setEnabled(true);
				absErr.setEnabled(true);
				limitLabel.setEnabled(true);
				limit.setEnabled(true);
				intervalLabel.setEnabled(true);
				interval.setEnabled(true);
				mpde.setEnabled(false);
				meanPath.setEnabled(false);
				medianPath.setEnabled(false);
				iSSATypeLabel.setEnabled(false);
				adaptive.setEnabled(false);
				nonAdaptive.setEnabled(false);
				iSSAAdaptiveLabel.setEnabled(false);
				bifurcation.setEnabled(false);
				bifurcationLabel.setEnabled(false);
			}
		}
		// if the Run button is clicked
		else if (e.getSource() == run) {
			boolean ignoreSweep = false;
			if (sbml.isSelected() || dot.isSelected() || xhtml.isSelected() || lhpn.isSelected()) {
				ignoreSweep = true;
			}
			String stem = "";
			if (!fileStem.getText().trim().equals("")) {
				if (!(stemPat.matcher(fileStem.getText().trim()).matches())) {
					JOptionPane.showMessageDialog(Gui.frame,
							"A file stem can only contain letters, numbers, and underscores.", "Invalid File Stem",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				stem += fileStem.getText().trim();
			}
			for (int i = 0; i < biomodelsim.getTab().getTabCount(); i++) {
				if (modelEditor != null) {
					if (biomodelsim.getTitleAt(i).equals(modelEditor.getRefFile())) {
						if (biomodelsim.getTab().getComponentAt(i) instanceof ModelEditor) {
							ModelEditor gcm = ((ModelEditor) (biomodelsim.getTab().getComponentAt(i)));
							if (gcm.isDirty()) {
								Object[] options = { "Yes", "No" };
								int value = JOptionPane
										.showOptionDialog(Gui.frame,
												"Do you want to save changes to " + modelEditor.getRefFile()
														+ " before running the simulation?", "Save Changes",
												JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
												options[0]);
								if (value == JOptionPane.YES_OPTION) {
									gcm.save("gcm");
								}
							}
						}
					}
				}
				else {
					if (biomodelsim.getTitleAt(i).equals(modelFile)) {
						if (biomodelsim.getTab().getComponentAt(i) instanceof LHPNEditor) {
							LHPNEditor lpn = ((LHPNEditor) (biomodelsim.getTab().getComponentAt(i)));
							if (lpn.isDirty()) {
								Object[] options = { "Yes", "No" };
								int value = JOptionPane
										.showOptionDialog(Gui.frame, "Do you want to save changes to " + modelFile
												+ " before running the simulation?", "Save Changes",
												JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
												options[0]);
								if (value == JOptionPane.YES_OPTION) {
									lpn.save();
								}
							}
						}
					}
				}
			}
			if (modelEditor != null) {
				modelEditor.saveParams(true, stem, ignoreSweep, simulators.getSelectedItem().toString());
			}
			else {
				if (!stem.equals("")) {
				}
				Translator t1 = new Translator();
				if (abstraction.isSelected()) {
					LhpnFile lhpnFile = new LhpnFile();
					lhpnFile.load(root + separator + modelFile);
					Abstraction abst = new Abstraction(lhpnFile, lhpnAbstraction);
					abst.abstractSTG(false);
					abst.save(root + separator + simName + separator + modelFile);
					if (transientProperties != null && !((String) transientProperties.getSelectedItem()).equals("none")) {
						t1.convertLPN2SBML(root + separator + simName + separator + modelFile,
								((String) transientProperties.getSelectedItem()));
					}
					else {
						t1.convertLPN2SBML(root + separator + simName + separator + modelFile, "");
					}
				}
				else {
					if (transientProperties != null && !((String) transientProperties.getSelectedItem()).equals("none")) {
						t1.convertLPN2SBML(root + separator + modelFile, ((String) transientProperties.getSelectedItem()));
					}
					else {
						t1.convertLPN2SBML(root + separator + modelFile, "");
					}
				}
				t1.setFilename(root + separator + simName + separator + stem + separator
						+ modelFile.replace(".lpn", ".xml"));
				t1.outputSBML();
				if (!stem.equals("")) {
					new File(root + separator + simName + separator + stem).mkdir();
					new AnalysisThread(this).start(stem, true);
				}
				else {
					new AnalysisThread(this).start(".", true);
				}
				emptyFrames();
			}
		}
		else if (e.getSource() == save) {
			boolean ignoreSweep = false;
			if (sbml.isSelected() || dot.isSelected() || xhtml.isSelected() || lhpn.isSelected()) {
				ignoreSweep = true;
			}
			if (modelEditor != null) {
				modelEditor.saveParams(false, "", ignoreSweep, simulators.getSelectedItem().toString());
			}
			save();
		}
		else if ((e.getSource() == addPreAbs) || (e.getSource() == addLoopAbs) || (e.getSource() == addPostAbs)) {
			JPanel addAbsPanel = new JPanel(new BorderLayout());
			JComboBox absList = new JComboBox();
			if (e.getSource() == addPreAbs)
				absList.addItem("complex-formation-and-sequestering-abstraction");
			// absList.addItem("species-sequestering-abstraction");
			absList.addItem("operator-site-reduction-abstraction");
			absList.addItem("absolute-activation/inhibition-generator");
			absList.addItem("absolute-inhibition-generator");
			absList.addItem("birth-death-generator");
			absList.addItem("birth-death-generator2");
			absList.addItem("birth-death-generator3");
			absList.addItem("birth-death-generator4");
			absList.addItem("birth-death-generator5");
			absList.addItem("birth-death-generator6");
			absList.addItem("birth-death-generator7");
			absList.addItem("degradation-stoichiometry-amplifier");
			absList.addItem("degradation-stoichiometry-amplifier2");
			absList.addItem("degradation-stoichiometry-amplifier3");
			absList.addItem("degradation-stoichiometry-amplifier4");
			absList.addItem("degradation-stoichiometry-amplifier5");
			absList.addItem("degradation-stoichiometry-amplifier6");
			absList.addItem("degradation-stoichiometry-amplifier7");
			absList.addItem("degradation-stoichiometry-amplifier8");
			absList.addItem("dimer-to-monomer-substitutor");
			absList.addItem("dimerization-reduction");
			absList.addItem("dimerization-reduction-level-assignment");
			absList.addItem("distribute-transformer");
			absList.addItem("enzyme-kinetic-qssa-1");
			absList.addItem("enzyme-kinetic-rapid-equilibrium-1");
			absList.addItem("enzyme-kinetic-rapid-equilibrium-2");
			absList.addItem("final-state-generator");
			absList.addItem("inducer-structure-transformer");
			absList.addItem("irrelevant-species-remover");
			absList.addItem("kinetic-law-constants-simplifier");
			absList.addItem("max-concentration-reaction-adder");
			absList.addItem("modifier-constant-propagation");
			absList.addItem("modifier-structure-transformer");
			absList.addItem("multiple-products-reaction-eliminator");
			absList.addItem("multiple-reactants-reaction-eliminator");
			absList.addItem("nary-order-unary-transformer");
			absList.addItem("nary-order-unary-transformer2");
			absList.addItem("nary-order-unary-transformer3");
			absList.addItem("operator-site-forward-binding-remover");
			absList.addItem("operator-site-forward-binding-remover2");
			absList.addItem("pow-kinetic-law-transformer");
			absList.addItem("ppta");
			absList.addItem("reversible-reaction-structure-transformer");
			absList.addItem("reversible-to-irreversible-transformer");
			absList.addItem("similar-reaction-combiner");
			absList.addItem("single-reactant-product-reaction-eliminator");
			absList.addItem("stoichiometry-amplifier");
			absList.addItem("stoichiometry-amplifier2");
			absList.addItem("stoichiometry-amplifier3");
			absList.addItem("stop-flag-generator");
			addAbsPanel.add(absList, "Center");
			String[] options = { "Add", "Cancel" };
			int value = JOptionPane.showOptionDialog(Gui.frame, addAbsPanel, "Add abstraction method",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (value == JOptionPane.YES_OPTION) {
				if (e.getSource() == addPreAbs) {
					Utility.add(preAbs, absList.getSelectedItem());
				}
				else if (e.getSource() == addLoopAbs) {
					Utility.add(loopAbs, absList.getSelectedItem());
				}
				else {
					Utility.add(postAbs, absList.getSelectedItem());
				}
			}
		}
		else if (e.getSource() == rmPreAbs) {
			Utility.remove(preAbs);
		}
		else if (e.getSource() == rmLoopAbs) {
			Utility.remove(loopAbs);
		}
		else if (e.getSource() == rmPostAbs) {
			Utility.remove(postAbs);
		}
		// if the remove ssa button is clicked
		/*
		 * else if (e.getSource() == removeSSA) { ssaList = Utility.remove(ssa,
		 * ssaList); } // if the remove sad button is clicked else if
		 * (e.getSource() == removeSAD) { sadList = Utility.remove(sad,
		 * sadList); } // if the new ssa button is clicked else if
		 * (e.getSource() == newSSA) { ssaList = new Object[0];
		 * ssa.setListData(ssaList); ssa.setEnabled(true);
		 * timeLabel.setEnabled(true); time.setEnabled(true);
		 * availSpecies.setEnabled(true); ssaMod.setEnabled(true);
		 * ssaModNum.setEnabled(true); addSSA.setEnabled(true);
		 * editSSA.setEnabled(true); removeSSA.setEnabled(true); } // if the new
		 * sad button is clicked else if (e.getSource() == newSAD) { sadList =
		 * new Object[0]; sad.setListData(sadList); TCid.setText("");
		 * desc.setText(""); cond.setText(""); } // if the new sad button is
		 * clicked else if (e.getSource() == newProp) { props = new Object[0];
		 * properties.setListData(props); prop.setText(""); value.setText(""); }
		 * // if the remove properties button is clicked else if (e.getSource()
		 * == removeProp) { props = Utility.remove(properties, props); } // if
		 * the add properties button is clicked else if (e.getSource() ==
		 * addProp) { if (prop.getText().trim().equals("")) {
		 * JOptionPane.showMessageDialog(Gui.frame,
		 * "Enter a option into the option field!", "Must Enter an Option",
		 * JOptionPane.ERROR_MESSAGE); return; } if
		 * (value.getText().trim().equals("")) {
		 * JOptionPane.showMessageDialog(Gui.frame,
		 * "Enter a value into the value field!", "Must Enter a Value",
		 * JOptionPane.ERROR_MESSAGE); return; } String add =
		 * prop.getText().trim() + "=" + value.getText().trim(); JList
		 * addPropery = new JList(); Object[] adding = { add };
		 * addPropery.setListData(adding); addPropery.setSelectedIndex(0); props
		 * = Utility.add(props, properties, addPropery, false, null, null, null,
		 * null, null, null, this); }
		 */
		else if (e.getSource() == append) {
			if (append.isSelected()) {
				limit.setEnabled(false);
				interval.setEnabled(false);
				limitLabel.setEnabled(false);
				intervalLabel.setEnabled(false);
			}
			else {
				limit.setEnabled(true);
				interval.setEnabled(true);
				limitLabel.setEnabled(true);
				intervalLabel.setEnabled(true);
			}
			Random rnd = new Random();
			seed.setText("" + rnd.nextInt());
			int cut = 0;
			String[] getFilename = sbmlProp.split(separator);
			for (int i = 0; i < getFilename[getFilename.length - 1].length(); i++) {
				if (getFilename[getFilename.length - 1].charAt(i) == '.') {
					cut = i;
				}
			}
			String propName = sbmlProp.substring(0, sbmlProp.length() - getFilename[getFilename.length - 1].length())
					+ getFilename[getFilename.length - 1].substring(0, cut) + ".properties";
			try {
				if (new File(propName).exists()) {
					Properties getProps = new Properties();
					FileInputStream load = new FileInputStream(new File(propName));
					getProps.load(load);
					load.close();
					if (getProps.containsKey("monte.carlo.simulation.time.limit")) {
						minStep.setText(getProps.getProperty("monte.carlo.simulation.min.time.step"));
						step.setText(getProps.getProperty("monte.carlo.simulation.time.step"));
						limit.setText(getProps.getProperty("monte.carlo.simulation.time.limit"));
						interval.setText(getProps.getProperty("monte.carlo.simulation.print.interval"));
					}
				}
			}
			catch (Exception e1) {
				JOptionPane.showMessageDialog(Gui.frame, "Unable to restore time limit and print interval.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		/*
		 * else if (e.getActionCommand().contains("box")) { int num =
		 * Integer.parseInt(e.getActionCommand().substring(3)) - 1; if
		 * (!((JCheckBox) speciesInt.get(num).get(0)).isSelected()) { for (int i
		 * = 2; i < speciesInt.get(num).size(); i++) {
		 * speciesInt.get(num).get(i).setEnabled(false); } } else { if
		 * (gcmEditor == null ||
		 * !(gcmEditor.getGCM().getBiochemicalSpecies().contains(((JTextField)
		 * speciesInt.get(num).get(1)).getText()) || gcmEditor
		 * .getGCM().getInputSpecies().contains(((JTextField)
		 * speciesInt.get(num).get(1)).getText()))) { for (int i = 2; i <
		 * speciesInt.get(num).size(); i++) {
		 * speciesInt.get(num).get(i).setEnabled(true); } } } } else if
		 * (e.getActionCommand().contains("text")) { int num =
		 * Integer.parseInt(e.getActionCommand().substring(4)) - 1;
		 * editNumThresholds(num); speciesPanel.revalidate();
		 * speciesPanel.repaint(); }
		 */
	}

	/**
	 * If the run button is pressed, this method starts a new thread for the
	 * simulation.
	 * 
	 * @param refresh
	 */
	public void run(String direct, boolean refresh) {
		double timeLimit = 100.0;
		double printInterval = 1.0;
		double minTimeStep = 0.0;
		double timeStep = 1.0;
		double absError = 1.0e-9;
		String outDir = "";
		long rndSeed = 314159;
		int run = 1;
		try {
			timeLimit = Double.parseDouble(limit.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Time Limit Field.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			if (((String) intervalLabel.getSelectedItem()).contains("Print Interval")) {
				printInterval = Double.parseDouble(interval.getText().trim());
				if (printInterval < 0) {
					JOptionPane.showMessageDialog(Gui.frame,
							"Must Enter A Positive Number Into The Print Interval Field.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				else if (printInterval == 0 && !((String) intervalLabel.getSelectedItem()).contains("Minimum")) {
					JOptionPane.showMessageDialog(Gui.frame,
							"Must Enter A Positive Number Into The Print Interval Field.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			else {
				printInterval = Integer.parseInt(interval.getText().trim());
				if (printInterval <= 0) {
					JOptionPane.showMessageDialog(Gui.frame,
							"Must Enter A Positive Number Into The Number of Steps Field.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		catch (Exception e1) {
			if (((String) intervalLabel.getSelectedItem()).contains("Print Interval")) {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Print Interval Field.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			else {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter An Integer Into The Number Of Steps Field.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		String sim = (String) simulators.getSelectedItem();
		if (step.getText().trim().equals("inf") && !sim.equals("euler")) {
			timeStep = Double.MAX_VALUE;
		}
		else if (step.getText().trim().equals("inf") && sim.equals("euler")) {
			JOptionPane.showMessageDialog(Gui.frame, "Cannot Select An Infinite Time Step With Euler Simulation.",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else {
			try {
				// if (step.isEnabled()) {
				timeStep = Double.parseDouble(step.getText().trim());
				// }
			}
			catch (Exception e1) {
				// if (step.isEnabled()) {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Time Step Field.", "Error",
						JOptionPane.ERROR_MESSAGE);
				// }
				return;
			}
		}
		try {
			minTimeStep = Double.parseDouble(minStep.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Minimum Time Step Field.",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			// if (absErr.isEnabled()) {
			absError = Double.parseDouble(absErr.getText().trim());
			// }
		}
		catch (Exception e1) {
			// if (absErr.isEnabled()) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Absolute Error Field.",
					"Error", JOptionPane.ERROR_MESSAGE);
			// }
			return;
		}
		if (direct.equals(".")) {
			outDir = simName;
		}
		else {
			outDir = simName + separator + direct;
		}
		try {
			// if (seed.isEnabled()) {
			rndSeed = Long.parseLong(seed.getText().trim());
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter An Integer Into The Random Seed Field.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		Preferences biosimrc = Preferences.userRoot();
		try {
			// if (runs.isEnabled()) {
			run = Integer.parseInt(runs.getText().trim());
			if (run < 0) {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Positive Integer Into The Runs Field."
						+ "\nProceding With Default:   " + biosimrc.get("biosim.sim.runs", ""), "Error",
						JOptionPane.ERROR_MESSAGE);
				run = Integer.parseInt(biosimrc.get("biosim.sim.runs", ""));
			}
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Positive Integer Into The Runs Field.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (!runs.isEnabled()) {
			for (String runs : new File(root + separator + outDir).list()) {
				if (runs.length() >= 4) {
					String end = "";
					for (int j = 1; j < 5; j++) {
						end = runs.charAt(runs.length() - j) + end;
					}
					if (end.equals(".tsd") || end.equals(".dat") || end.equals(".csv")) {
						if (runs.contains("run-")) {
							run = Math.max(run, Integer.parseInt(runs.substring(4, runs.length() - end.length())));
						}
					}
				}
			}
		}
		
		String printer_id;
		if (genRuns.isSelected()) {
			printer_id = "null.printer";
		}
		else {
			printer_id = "tsd.printer";
		}
		
		String printer_track_quantity = "amount";
		if (concentrations.isSelected()) {
			printer_track_quantity = "concentration";
		}
		
		String generate_statistics = "false";
		if (genStats.isSelected()) {
			generate_statistics = "true";
		}
		
		String[] intSpecies = getInterestingSpecies();
		String selectedButtons = "";
		double rap1 = 0.1;
		double rap2 = 0.1;
		double qss = 0.1;
		int con = 15;
		double stoichAmp = 1.0;
		
		try {
			// if (rapid1.isEnabled()) {
			rap1 = Double.parseDouble(rapid1.getText().trim());
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The"
					+ " Rapid Equilibrium Condition 1 Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			// if (rapid2.isEnabled()) {
			rap2 = Double.parseDouble(rapid2.getText().trim());
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The"
					+ " Rapid Equilibrium Condition 2 Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			// if (qssa.isEnabled()) {
			qss = Double.parseDouble(qssa.getText().trim());
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The QSSA Condition Field.",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			// if (maxCon.isEnabled()) {
			con = Integer.parseInt(maxCon.getText().trim());
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter An Integer Into The Max"
					+ " Concentration Threshold Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			if (abstraction.isSelected())
				stoichAmp = Double.parseDouble(diffStoichAmp.getText().trim());
			else
				stoichAmp = 1;
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "You must enter a double into the shoich."
					+ " amp. field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (none.isSelected() && ODE.isSelected()) {
			selectedButtons = "none_ODE";
		}
		else if (none.isSelected() && monteCarlo.isSelected()) {
			selectedButtons = "none_monteCarlo";
		}
		else if (abstraction.isSelected() && ODE.isSelected()) {
			selectedButtons = "abs_ODE";
		}
		else if (abstraction.isSelected() && monteCarlo.isSelected()) {
			selectedButtons = "abs_monteCarlo";
		}
		else if (nary.isSelected() && monteCarlo.isSelected()) {
			selectedButtons = "nary_monteCarlo";
		}
		else if (none.isSelected() && fba.isSelected()) {
			selectedButtons = "none_fba";
		}
		else if (nary.isSelected() && markov.isSelected()) {
			selectedButtons = "nary_markov";
		}
		else if (none.isSelected() && markov.isSelected()) {
			selectedButtons = "none_markov";
		}
		else if (abstraction.isSelected() && markov.isSelected()) {
			selectedButtons = "abs_markov";
		}
		else if (none.isSelected() && sbml.isSelected()) {
			selectedButtons = "none_sbml";
		}
		else if (abstraction.isSelected() && sbml.isSelected()) {
			selectedButtons = "abs_sbml";
		}
		else if (nary.isSelected() && sbml.isSelected()) {
			selectedButtons = "nary_sbml";
		}
		else if (none.isSelected() && dot.isSelected()) {
			selectedButtons = "none_dot";
		}
		else if (none.isSelected() && lhpn.isSelected()) {
			selectedButtons = "none_lhpn";
		}
		else if (abstraction.isSelected() && dot.isSelected()) {
			selectedButtons = "abs_dot";
		}
		else if (nary.isSelected() && dot.isSelected()) {
			selectedButtons = "nary_dot";
		}
		else if (none.isSelected() && xhtml.isSelected()) {
			selectedButtons = "none_xhtml";
		}
		else if (abstraction.isSelected() && xhtml.isSelected()) {
			selectedButtons = "abs_xhtml";
		}
		else if (nary.isSelected() && xhtml.isSelected()) {
			selectedButtons = "nary_xhtml";
		}
		else if (nary.isSelected() && lhpn.isSelected()) {
			selectedButtons = "nary_lhpn";
		}
		else if (abstraction.isSelected() && lhpn.isSelected()) {
			selectedButtons = "abs_lhpn";
		}
		int cut = 0;
		String simProp = sbmlProp;
		boolean saveTopLevel = false;
		if (!direct.equals(".")) {
			simProp = simProp.substring(0, simProp.length()
					- simProp.split(separator)[simProp.split(separator).length - 1].length())
					+ direct
					+ separator
					+ simProp.substring(simProp.length()
							- simProp.split(separator)[simProp.split(separator).length - 1].length());
			saveTopLevel = true;
		}
		String[] getFilename = simProp.split(separator);
		for (int i = 0; i < getFilename[getFilename.length - 1].length(); i++) {
			if (getFilename[getFilename.length - 1].charAt(i) == '.') {
				cut = i;
			}
		}
		String propName = simProp.substring(0, simProp.length() - getFilename[getFilename.length - 1].length())
				+ getFilename[getFilename.length - 1].substring(0, cut) + ".properties";
		String topLevelProps = null;
		if (saveTopLevel) {
			topLevelProps = sbmlProp.substring(0, sbmlProp.length() - getFilename[getFilename.length - 1].length())
					+ getFilename[getFilename.length - 1].substring(0, cut) + ".properties";
		}
		log.addText("Creating properties file:\n" + propName + "\n");
		final JButton cancel = new JButton("Cancel");
		final JFrame running = new JFrame("Progress");
		WindowListener w = new WindowListener() {
			public void windowClosing(WindowEvent arg0) {
				cancel.doClick();
				running.dispose();
			}

			public void windowOpened(WindowEvent arg0) {
			}

			public void windowClosed(WindowEvent arg0) {
			}

			public void windowIconified(WindowEvent arg0) {
			}

			public void windowDeiconified(WindowEvent arg0) {
			}

			public void windowActivated(WindowEvent arg0) {
			}

			public void windowDeactivated(WindowEvent arg0) {
			}
		};
		running.addWindowListener(w);
		JPanel text = new JPanel();
		JPanel progBar = new JPanel();
		JPanel button = new JPanel();
		JPanel all = new JPanel(new BorderLayout());
		JLabel label;
		if (!direct.equals(".")) {
			label = new JLabel("Running " + simName + " " + direct);
		}
		else {
			label = new JLabel("Running " + simName);
		}
		// int steps;
		double runTime;
		if (((String) intervalLabel.getSelectedItem()).contains("Print Interval")) {
			if (simulators.getSelectedItem().equals("iSSA")) { 
				runTime = timeLimit;
			}
			else {
				runTime = timeLimit * run;
			}
		}
		else {
			if (simulators.getSelectedItem().equals("iSSA")) { 
				runTime = timeLimit;
			}
			else {
				runTime = timeLimit * run;
			}
		}
		JProgressBar progress = new JProgressBar(0, 100);
		progress.setStringPainted(true);
		progress.setValue(0);
		text.add(label);
		progBar.add(progress);
		button.add(cancel);
		all.add(text, "North");
		all.add(progBar, "Center");
		all.add(button, "South");
		running.setContentPane(all);
		running.pack();
		Dimension screenSize;
		try {
			Toolkit tk = Toolkit.getDefaultToolkit();
			screenSize = tk.getScreenSize();
		}
		catch (AWTError awe) {
			screenSize = new Dimension(640, 480);
		}
		Dimension frameSize = running.getSize();

		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		int x = screenSize.width / 2 - frameSize.width / 2;
		int y = screenSize.height / 2 - frameSize.height / 2;
		running.setLocation(x, y);
		running.setVisible(true);
		running.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		Run runProgram = new Run(this);
		cancel.addActionListener(runProgram);
		biomodelsim.getExitButton().addActionListener(runProgram);
		// saveSAD(outDir);
		int numPaths = Integer.parseInt((String)(bifurcation.getSelectedItem()));
		runProgram.createProperties(timeLimit, ((String) (intervalLabel.getSelectedItem())), printInterval,
				minTimeStep, timeStep, absError, ".", rndSeed, run, numPaths, intSpecies, printer_id, printer_track_quantity, 
				generate_statistics, simProp.split(separator), selectedButtons, this, simProp, rap1, rap2, qss, con, 
				stoichAmp, preAbs, loopAbs, postAbs, lhpnAbstraction, mpde.isSelected(), meanPath.isSelected(), 
				medianPath.isSelected(), adaptive.isSelected(), nonAdaptive.isSelected());
		try {
			Properties getProps = new Properties();
			FileInputStream load = new FileInputStream(new File(propName));
			getProps.load(load);
			load.close();
			getProps.setProperty("selected.simulator", sim);
			if (transientProperties != null) {
				getProps.setProperty("selected.property", (String) transientProperties.getSelectedItem());
			}
			if (!fileStem.getText().trim().equals("")) {
				getProps.setProperty("file.stem", fileStem.getText().trim());
			}
			if (monteCarlo.isSelected() || ODE.isSelected()) {
				if (append.isSelected()) {
					String[] searchForRunFiles = new File(root + separator + outDir).list();
					int start = 1;
					for (String s : searchForRunFiles) {
						if (s.length() > 3 && s.substring(0, 4).equals("run-")
								&& new File(root + separator + outDir + separator + s).isFile()) {
							String getNumber = s.substring(4, s.length());
							String number = "";
							for (int i = 0; i < getNumber.length(); i++) {
								if (Character.isDigit(getNumber.charAt(i))) {
									number += getNumber.charAt(i);
								}
								else {
									break;
								}
							}
							start = Math.max(Integer.parseInt(number), start);
						}
						else if (s.length() > 3
								&& new File(root + separator + outDir + separator + s).isFile()
								&& (s.equals("mean.tsd") || s.equals("standard_deviation.tsd") || s
										.equals("variance.tsd"))) {
							new File(root + separator + outDir + separator + s).delete();
						}
					}
					getProps.setProperty("monte.carlo.simulation.start.index", (start + 1) + "");
				}
				else {
					String[] searchForRunFiles = new File(root + separator + outDir).list();
					for (String s : searchForRunFiles) {
						if (s.length() > 3 && s.substring(0, 4).equals("run-")
								&& new File(root + separator + outDir + separator + s).isFile()) {
							new File(root + separator + outDir + separator + s).delete();
						}
					}
					getProps.setProperty("monte.carlo.simulation.start.index", "1");
				}
			}
			FileOutputStream store = new FileOutputStream(new File(propName));
			getProps.store(store, getFilename[getFilename.length - 1].substring(0, cut) + " Properties");
			store.close();
			if (saveTopLevel) {
				store = new FileOutputStream(new File(topLevelProps));
				getProps.store(store, getFilename[getFilename.length - 1].substring(0, cut) + " Properties");
				store.close();
			}
			//saveSEDML();
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Gui.frame, "Unable to add properties to property file.", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		if (monteCarlo.isSelected() || ODE.isSelected()) {
			File[] files = new File(root + separator + outDir).listFiles();
			for (File f : files) {
				if (f.getName().contains("mean.") || f.getName().contains("standard_deviation.")
						|| f.getName().contains("variance.")) {
					f.delete();
				}
			}
		}
		int exit;
		if (!direct.equals(".")) {
			String lpnProperty = "";
			if (transientProperties != null) {
				if (!((String) transientProperties.getSelectedItem()).equals("none")) {
					lpnProperty = ((String) transientProperties.getSelectedItem());
				}
			}
			exit = runProgram.execute(simProp, fba, sbml, dot, xhtml, lhpn, Gui.frame, ODE, monteCarlo, sim, printer_id,
					printer_track_quantity, root + separator + simName, nary, 1, intSpecies, log, biomodelsim, simTab,
					root, progress, simName + " " + direct, modelEditor, direct, timeLimit, runTime, modelFile,
					lhpnAbstraction, abstraction, lpnProperty, absError, timeStep, printInterval, run, rndSeed,
					refresh, label, running);
		}
		else {
			String lpnProperty = "";
			if (transientProperties != null) {
				if (!((String) transientProperties.getSelectedItem()).equals("none")) {
					lpnProperty = ((String) transientProperties.getSelectedItem());
				}
			}
			exit = runProgram.execute(simProp, fba, sbml, dot, xhtml, lhpn, Gui.frame, ODE, monteCarlo, sim, printer_id,
					printer_track_quantity, root + separator + simName, nary, 1, intSpecies, log, biomodelsim, simTab,
					root, progress, simName, modelEditor, null, timeLimit, runTime, modelFile, lhpnAbstraction,
					abstraction, lpnProperty, absError, timeStep, printInterval, run, rndSeed, refresh, label, running);
		}
		if (nary.isSelected() && modelEditor == null && !sim.contains("markov-chain-analysis") && !lhpn.isSelected()
				&& exit == 0) {
			String d = null;
			if (!direct.equals(".")) {
				d = direct;
			}
			new Nary_Run(this, ge, gt, eq, lt, le, simulators, simProp.split(separator), simProp, fba, sbml, dot, xhtml,
					lhpn, nary, ODE, monteCarlo, timeLimit, ((String) (intervalLabel.getSelectedItem())),
					printInterval, minTimeStep, timeStep, root + separator + simName, rndSeed, run, printer_id,
					printer_track_quantity, intSpecies, rap1, rap2, qss, con, log, root + separator + outDir
							+ separator + "user-defined.dat", biomodelsim, simTab, root, d, modelFile, abstraction,
					lhpnAbstraction, absError);
		}
		running.setCursor(null);
		running.dispose();
		biomodelsim.getExitButton().removeActionListener(runProgram);
		String[] searchForRunFiles = new File(root + separator + outDir).list();
		for (String s : searchForRunFiles) {
			if (s.length() > 3 && s.substring(0, 4).equals("run-")) {
				runFiles = true;
			}
		}
		if (monteCarlo.isSelected()) {
			append.setEnabled(true);
			concentrations.setEnabled(true);
			genRuns.setEnabled(true);
			genStats.setEnabled(true);
			report.setEnabled(true);
			if (append.isSelected()) {
				limit.setEnabled(false);
				interval.setEnabled(false);
				limitLabel.setEnabled(false);
				intervalLabel.setEnabled(false);
			}
			else {
				limit.setEnabled(true);
				interval.setEnabled(true);
				limitLabel.setEnabled(true);
				intervalLabel.setEnabled(true);
			}
		}
		if (append.isSelected()) {
			Random rnd = new Random();
			seed.setText("" + rnd.nextInt());
		}
		for (int i = 0; i < biomodelsim.getTab().getTabCount(); i++) {
			if (biomodelsim.getTab().getComponentAt(i) instanceof Graph) {
				((Graph) biomodelsim.getTab().getComponentAt(i)).refresh();
			}
		}
	}

	public void emptyFrames() {
		for (JFrame f : frames) {
			f.dispose();
		}
	}

	/**
	 * Invoked when the mouse is double clicked in the interesting species
	 * JLists or termination conditions JLists. Adds or removes the selected
	 * interesting species or termination conditions.
	 */
	public void mouseClicked(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * Saves the simulate options.
	 */
	public void save() {
		double timeLimit = 100.0;
		double printInterval = 1.0;
		double minTimeStep = 0.0;
		double timeStep = 1.0;
		double absError = 1.0e-9;
		long rndSeed = 314159;
		int run = 1;
		try {
			timeLimit = Double.parseDouble(limit.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Time Limit Field.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			if (((String) intervalLabel.getSelectedItem()).contains("Print Interval")) {
				printInterval = Double.parseDouble(interval.getText().trim());
			}
			else {
				printInterval = Integer.parseInt(interval.getText().trim());
			}
		}
		catch (Exception e1) {
			if (((String) intervalLabel.getSelectedItem()).contains("Print Interval")) {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Print Interval Field.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			else {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter An Integer Into The Number Of Steps Field.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		if (step.getText().trim().equals("inf")) {
			timeStep = Double.MAX_VALUE;
		}
		else {
			try {
				timeStep = Double.parseDouble(step.getText().trim());
			}
			catch (Exception e1) {
				if (step.isEnabled()) {
					JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Time Step Field.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
		try {
			minTimeStep = Double.parseDouble(minStep.getText().trim());
		}
		catch (Exception e1) {
			if (minStep.isEnabled()) {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Time Step Field.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		try {
			// if (absErr.isEnabled()) {
			absError = Double.parseDouble(absErr.getText().trim());
			// }
		}
		catch (Exception e1) {
			// if (absErr.isEnabled()) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The Absolute Error Field.",
					"Error", JOptionPane.ERROR_MESSAGE);
			// }
			return;
		}
		try {
			// if (seed.isEnabled()) {
			rndSeed = Long.parseLong(seed.getText().trim());
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter An Integer Into The Random Seed Field.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			// if (runs.isEnabled()) {
			run = Integer.parseInt(runs.getText().trim());
			if (run < 0) {
				JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Positive Integer Into The Runs Field."
						+ "\nProceding With Default:  1", "Error", JOptionPane.ERROR_MESSAGE);
				run = 1;
			}
			// }
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Positive Integer Into The Runs Field.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String printer_id;
		if (genRuns.isSelected()) {
			printer_id = "null.printer";
		}
		else {
			printer_id = "tsd.printer";
		}
		String printer_track_quantity = "amount";
		if (concentrations.isSelected()) {
			printer_track_quantity = "concentration";
		}
		
		String generate_statistics = "false";
		if (genStats.isSelected())
			generate_statistics = "true";
		String[] intSpecies = getInterestingSpecies();
		String selectedButtons = "";
		double rap1 = 0.1;
		double rap2 = 0.1;
		double qss = 0.1;
		int con = 15;
		double stoichAmp = 1.0;
		
		try {
			rap1 = Double.parseDouble(rapid1.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The"
					+ " Rapid Equilibrium Condition 1 Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			rap2 = Double.parseDouble(rapid2.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The"
					+ " Rapid Equilibrium Condition 2 Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			qss = Double.parseDouble(qssa.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter A Real Number Into The QSSA Condition Field.",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			con = Integer.parseInt(maxCon.getText().trim());
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter An Integer Into The Max"
					+ " Concentration Threshold Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			if (abstraction.isSelected())
				stoichAmp = Double.parseDouble(diffStoichAmp.getText().trim());
			else
				stoichAmp = 1;
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(Gui.frame, "Must Enter a Double Into the Stoich."
					+ " Amp. Field.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (none.isSelected() && ODE.isSelected()) {
			selectedButtons = "none_ODE";
		}
		else if (none.isSelected() && monteCarlo.isSelected()) {
			selectedButtons = "none_monteCarlo";
		}
		else if (abstraction.isSelected() && ODE.isSelected()) {
			selectedButtons = "abs_ODE";
		}
		else if (abstraction.isSelected() && monteCarlo.isSelected()) {
			selectedButtons = "abs_monteCarlo";
		}
		else if (nary.isSelected() && monteCarlo.isSelected()) {
			selectedButtons = "nary_monteCarlo";
		}
		else if (none.isSelected() && fba.isSelected()) {
			selectedButtons = "none_fba";
		}
		else if (nary.isSelected() && markov.isSelected()) {
			selectedButtons = "nary_markov";
		}
		else if (none.isSelected() && markov.isSelected()) {
			selectedButtons = "none_markov";
		}
		else if (abstraction.isSelected() && markov.isSelected()) {
			selectedButtons = "abs_markov";
		}
		else if (none.isSelected() && sbml.isSelected()) {
			selectedButtons = "none_sbml";
		}
		else if (abstraction.isSelected() && sbml.isSelected()) {
			selectedButtons = "abs_sbml";
		}
		else if (nary.isSelected() && sbml.isSelected()) {
			selectedButtons = "nary_sbml";
		}
		else if (none.isSelected() && dot.isSelected()) {
			selectedButtons = "none_dot";
		}
		else if (none.isSelected() && lhpn.isSelected()) {
			selectedButtons = "none_lhpn";
		}
		else if (abstraction.isSelected() && dot.isSelected()) {
			selectedButtons = "abs_dot";
		}
		else if (nary.isSelected() && dot.isSelected()) {
			selectedButtons = "nary_dot";
		}
		else if (none.isSelected() && xhtml.isSelected()) {
			selectedButtons = "none_xhtml";
		}
		else if (abstraction.isSelected() && xhtml.isSelected()) {
			selectedButtons = "abs_xhtml";
		}
		else if (nary.isSelected() && xhtml.isSelected()) {
			selectedButtons = "nary_xhtml";
		}
		else if (nary.isSelected() && lhpn.isSelected()) {
			selectedButtons = "nary_lhpn";
		}
		else if (abstraction.isSelected() && lhpn.isSelected()) {
			selectedButtons = "abs_lhpn";
		}
		Run runProgram = new Run(this);
		int cut = 0;
		String[] getFilename = sbmlProp.split(separator);
		for (int i = 0; i < getFilename[getFilename.length - 1].length(); i++) {
			if (getFilename[getFilename.length - 1].charAt(i) == '.') {
				cut = i;
			}
		}
		String propName = sbmlProp.substring(0, sbmlProp.length() - getFilename[getFilename.length - 1].length())
				+ getFilename[getFilename.length - 1].substring(0, cut) + ".properties";
		log.addText("Creating properties file1:\n" + propName + "\n");
		int numPaths = Integer.parseInt((String)(bifurcation.getSelectedItem()));
		runProgram.createProperties(timeLimit, ((String) (intervalLabel.getSelectedItem())), printInterval,
				minTimeStep, timeStep, absError, ".", rndSeed, run, numPaths, intSpecies, printer_id, printer_track_quantity, 
				generate_statistics, sbmlProp.split(separator), selectedButtons, this, sbmlProp, rap1, rap2, qss, con, 
				stoichAmp, preAbs, loopAbs, postAbs, lhpnAbstraction, mpde.isSelected(), meanPath.isSelected(), 
				medianPath.isSelected(), adaptive.isSelected(), nonAdaptive.isSelected());
		try {
			Properties getProps = new Properties();
			FileInputStream load = new FileInputStream(new File(propName));
			getProps.load(load);
			load.close();
			getProps.setProperty("selected.simulator", (String) simulators.getSelectedItem());
			if (transientProperties != null) {
				getProps.setProperty("selected.property", (String) transientProperties.getSelectedItem());
			}
			if (!fileStem.getText().trim().equals("")) {
				getProps.setProperty("file.stem", fileStem.getText().trim());
			}
			FileOutputStream store = new FileOutputStream(new File(propName));
			getProps.store(store, getFilename[getFilename.length - 1].substring(0, cut) + " Properties");
			store.close();
			//saveSEDML();
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Gui.frame, "Unable to add properties to property file.", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		change = false;
	}
	
	private void loadSEDML() {
		File sedmlFile = new File(sedmlFilename);
		if (sedmlFile.exists()) {
			try {
				sedmlDoc = Libsedml.readDocument(sedmlFile);
				sedmlDoc.validate();
				if(sedmlDoc.hasErrors()) {
					List<SedMLError> errors = sedmlDoc.getErrors();
					for (int i = 0; i < errors.size(); i++) {
						SedMLError error = errors.get(i);
						System.out.println(error.getMessage());
					}
					//return;
				}
				SedML sedml = sedmlDoc.getSedMLModel();
				List<Simulation> simulations = sedml.getSimulations();
				if (simulations.size() > 0) {
					UniformTimeCourse simulation = (UniformTimeCourse) simulations.get(0);
					//KisaoTerm kisaoTerm = KisaoOntology.getInstance().getTermById(simulation.getAlgorithm().getKisaoID());
					Annotation annotation = getSEDBaseAnnotation(simulation,"printInterval");
					if (annotation==null) {
						intervalLabel.setSelectedItem("Number Of Steps");
						interval.setText(""+simulation.getNumberOfPoints());
					} else {
						Element element = annotation.getAnnotationElementsList().get(0);
						if (element.getAttribute("Print_Interval")!=null) {
							intervalLabel.setSelectedItem("Print Interval");
							interval.setText(element.getAttributeValue("Print_Interval"));
						} else {
							intervalLabel.setSelectedItem("Minimum Print Interval");
							interval.setText(element.getAttributeValue("Minimum_Print_Interval"));
						}
					}
					limit.setText(""+simulation.getOutputEndTime());
				}
			} catch (XMLException e) {
				JOptionPane.showMessageDialog(Gui.frame, "Unable to load SED-ML file!", "Error Loading SED-ML File",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void saveSEDML() {
		double timeLimit = Double.parseDouble(limit.getText().trim());
		double printInterval = Double.parseDouble(interval.getText().trim());
		int numberOfSteps;
		if (((String)intervalLabel.getSelectedItem()).equals("Number Of Steps")) {
			numberOfSteps = Integer.parseInt(interval.getText().trim());
		} else {
			numberOfSteps = (int)Math.floor(timeLimit / printInterval) + 1;
		}
		if (sedmlDoc == null) {
			sedmlDoc = new SEDMLDocument();
		}
		SedML sedml = sedmlDoc.getSedMLModel();
		List<Simulation> simulations = sedml.getSimulations();
		UniformTimeCourse simulation;
		if (simulations.size() > 0) {
			simulation = (UniformTimeCourse) simulations.get(0);
			simulation.setOutputEndTime(timeLimit);
			simulation.setNumberOfPoints(numberOfSteps);
			simulation.setAlgorithm(getAlgorithm());
		} else {
			Algorithm algo = getAlgorithm();
			simulation = new UniformTimeCourse("simId", "", 0, 0, timeLimit, numberOfSteps, algo);
			sedml.addSimulation(simulation);
			Model model = new Model("modelId", "", SUPPORTED_LANGUAGE.SBML_GENERIC.getURN(), modelFile);
			sedml.addModel(model);
			Task task = new Task(simName, "", model.getId(), simulation.getId());
			sedml.addTask(task);
		} 
		Annotation annotation = getSEDBaseAnnotation(simulation,"printInterval");
		if (annotation!=null) {
			simulation.removeAnnotation(annotation);
		}
		if (!((String)intervalLabel.getSelectedItem()).equals("Number Of Steps")) {
			Element para = new Element("printInterval");
			para.setAttribute(((String)intervalLabel.getSelectedItem()).replace(" ","_"),interval.getText().trim());
			para.setNamespace(Namespace.getNamespace("ibiosim", "http://www.fakeuri.com"));
			Annotation ann = new Annotation(para);
			simulation.addAnnotation(ann);
		}
		File sedmlFile = new File(sedmlFilename);
		sedmlDoc.writeDocument(sedmlFile);
		/*
		ASTNode root = Libsedml.parseFormulaString("x");
		DataGenerator dgx = new DataGenerator("dg1", "dg1name", root);
		SBMLSupport support = new SBMLSupport();
		Variable var = new Variable("x", "x",task1.getId(),
		support.getXPathForSpecies("x"));
		// now add this variable to the data generator:
		dgx.addVariable(var);
		// and add the data generator to the document:
		sedml.addDataGenerator(dgx);
		Variable vartime = new Variable("time", "time",task1.getId(),VariableSymbol.TIME);
		Plot2D plot1 = new Plot2D("basicPlot", "basic Plot");
		Curve cv1 = new Curve("curve1ID","",false,false,vartime.getId(),dgx.getId());
		//Curve cv2 = new Curve("curve2ID","",false,false,vartime.getId(),dgy.getId());
		plot1.addCurve(cv1);
		//plot1.addCurve(cv2);
		sedml.addOutput(plot1);
		for (SedMLError err:doc.validate()){
			 System.out.println(err.getMessage());
		}
		*/
	}
	
	private Algorithm getAlgorithm() {
		if (ODE.isEnabled()) {
			if (((String) simulators.getSelectedItem()).contains("euler")) {
				return new Algorithm(GlobalConstants.KISAO_EULER);
			}
			else if (((String) simulators.getSelectedItem()).contains("rk8pd")) {
				return new Algorithm(GlobalConstants.KISAO_RUNGE_KUTTA_PRINCE_DORMAND);
			}
			else if (((String) simulators.getSelectedItem()).contains("rkf45") ||
					((String) simulators.getSelectedItem()).contains("Runge-Kutta-Fehlberg")) {
				return new Algorithm(GlobalConstants.KISAO_RUNGE_KUTTA_FEHLBERG);
			}
		} else if (monteCarlo.isEnabled()) {
			if (((String) simulators.getSelectedItem()).contains("gillespie")) {
				return new Algorithm(GlobalConstants.KISAO_GILLESPIE_DIRECT);
			}
			else if (((String) simulators.getSelectedItem()).equals("SSA-CR")) {
				return new Algorithm(GlobalConstants.KISAO_SSA_CR);
			}
			Algorithm algorithm = new Algorithm(GlobalConstants.KISAO_MONTE_CARLO);
			Element para = new Element("analysis");
			para.setAttribute("method",((String)simulators.getSelectedItem()).replace(" ","_"));
			para.setNamespace(Namespace.getNamespace("ibiosim", "http://www.fakeuri.com"));
			Annotation ann = new Annotation(para);
			algorithm.addAnnotation(ann);
			return algorithm;
		} 
		Algorithm algorithm = new Algorithm(GlobalConstants.KISAO_GENERIC);
		Element para = new Element("analysis");
		para.setAttribute("method",((String)simulators.getSelectedItem()).replace(" ","_"));
		para.setNamespace(Namespace.getNamespace("ibiosim", "http://www.fakeuri.com"));
		Annotation ann = new Annotation(para);
		algorithm.addAnnotation(ann);
		return algorithm;
	}

	@SuppressWarnings("unused")
	private void setAlgorithm(Algorithm algorithm) {
		if (algorithm.getKisaoID().equals(GlobalConstants.KISAO_EULER)) {
			ODE.setEnabled(true);
			simulators.setSelectedItem("euler");
		} else if (algorithm.getKisaoID().equals(GlobalConstants.KISAO_RUNGE_KUTTA_PRINCE_DORMAND)) {
			ODE.setEnabled(true);
			simulators.setSelectedItem("rk8pd");
		} else if (algorithm.getKisaoID().equals(GlobalConstants.KISAO_RUNGE_KUTTA_FEHLBERG)) {
			ODE.setEnabled(true);
			// check annotation
			simulators.setSelectedItem("rkf45");
			simulators.setSelectedItem("Runge-Kutta-Fehlberg");
		} else if (algorithm.getKisaoID().equals(GlobalConstants.KISAO_GILLESPIE_DIRECT)) {
			monteCarlo.setEnabled(true);
			simulators.setSelectedItem("gillespie");
		} else if (algorithm.getKisaoID().equals(GlobalConstants.KISAO_SSA_CR)) {
			monteCarlo.setEnabled(true);
			simulators.setSelectedItem("SSA-CR");
		} else if (algorithm.getKisaoID().equals(GlobalConstants.KISAO_MONTE_CARLO)) {
			monteCarlo.setEnabled(true);
			//Annotation annotation = getSEDBaseAnnotation(algorithm,"analysis");
			simulators.setSelectedItem("SSA-CR");
			/*
			Algorithm algorithm = new Algorithm(GlobalConstants.KISAO_GENERIC);
			Element para = new Element("analysis");
			para.setAttribute("method",((String)simulators.getSelectedItem()).replace(" ","_"));
			para.setNamespace(Namespace.getNamespace("ibiosim", "http://www.fakeuri.com"));
			Annotation ann = new Annotation(para);
			algorithm.addAnnotation(ann);
			return algorithm;
			*/
		}
	}

	private Annotation getSEDBaseAnnotation(SEDBase sedBase,String name) {
		@SuppressWarnings("deprecation")
		List<Annotation> annotations = sedBase.getAnnotation();
		for (int i = 0; i < annotations.size(); i++) {
			Annotation annotation = annotations.get(i);
			List<Element> elements = annotation.getAnnotationElementsList();
			if (elements.size()>0 && elements.get(0).getName().equals(name)) {
				return annotation;
			}
		}
		return null;
	}
	/**
	 * Loads the simulate options.
	 */
	public void open(String openFile) {
		Properties load = new Properties();
		try {
			if (!openFile.equals("")) {
				FileInputStream in = new FileInputStream(new File(openFile));
				load.load(in);
				in.close();
				ArrayList<String> loadProperties = new ArrayList<String>();
				for (Object key : load.keySet()) {
					String type = key.toString().substring(0, key.toString().indexOf('.'));
					if (type.equals("gcm")) {
						loadProperties.add(key.toString() + "=" + load.getProperty(key.toString()));
					}
					else if (key.equals("reb2sac.abstraction.method.0.1")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.1").equals("enzyme-kinetic-qssa-1")) {
							loadProperties.add("reb2sac.abstraction.method.0.1="
									+ load.getProperty("reb2sac.abstraction.method.0.1"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.0.2")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.2").equals(
								"reversible-to-irreversible-transformer")) {
							loadProperties.add("reb2sac.abstraction.method.0.2="
									+ load.getProperty("reb2sac.abstraction.method.0.2"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.0.3")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.3").equals(
								"multiple-products-reaction-eliminator")) {
							loadProperties.add("reb2sac.abstraction.method.0.3="
									+ load.getProperty("reb2sac.abstraction.method.0.3"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.0.4")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.4").equals(
								"multiple-reactants-reaction-eliminator")) {
							loadProperties.add("reb2sac.abstraction.method.0.4="
									+ load.getProperty("reb2sac.abstraction.method.0.4"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.0.5")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.5").equals(
								"single-reactant-product-reaction-eliminator")) {
							loadProperties.add("reb2sac.abstraction.method.0.5="
									+ load.getProperty("reb2sac.abstraction.method.0.5"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.0.6")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.6").equals("dimer-to-monomer-substitutor")) {
							loadProperties.add("reb2sac.abstraction.method.0.6="
									+ load.getProperty("reb2sac.abstraction.method.0.6"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.0.7")) {
						if (!load.getProperty("reb2sac.abstraction.method.0.7").equals("inducer-structure-transformer")) {
							loadProperties.add("reb2sac.abstraction.method.0.7="
									+ load.getProperty("reb2sac.abstraction.method.0.7"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.1.1")) {
						if (!load.getProperty("reb2sac.abstraction.method.1.1")
								.equals("modifier-structure-transformer")) {
							loadProperties.add("reb2sac.abstraction.method.1.1="
									+ load.getProperty("reb2sac.abstraction.method.1.1"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.1.2")) {
						if (!load.getProperty("reb2sac.abstraction.method.1.2").equals("modifier-constant-propagation")) {
							loadProperties.add("reb2sac.abstraction.method.1.2="
									+ load.getProperty("reb2sac.abstraction.method.1.2"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.1")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.1").equals(
								"operator-site-forward-binding-remover")) {
							loadProperties.add("reb2sac.abstraction.method.2.1="
									+ load.getProperty("reb2sac.abstraction.method.2.1"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.3")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.3").equals(
								"enzyme-kinetic-rapid-equilibrium-1")) {
							loadProperties.add("reb2sac.abstraction.method.2.3="
									+ load.getProperty("reb2sac.abstraction.method.2.3"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.4")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.4").equals("irrelevant-species-remover")) {
							loadProperties.add("reb2sac.abstraction.method.2.4="
									+ load.getProperty("reb2sac.abstraction.method.2.4"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.5")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.5").equals("inducer-structure-transformer")) {
							loadProperties.add("reb2sac.abstraction.method.2.5="
									+ load.getProperty("reb2sac.abstraction.method.2.5"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.6")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.6").equals("modifier-constant-propagation")) {
							loadProperties.add("reb2sac.abstraction.method.2.6="
									+ load.getProperty("reb2sac.abstraction.method.2.6"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.7")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.7").equals("similar-reaction-combiner")) {
							loadProperties.add("reb2sac.abstraction.method.2.7="
									+ load.getProperty("reb2sac.abstraction.method.2.7"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.8")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.8").equals("modifier-constant-propagation")) {
							loadProperties.add("reb2sac.abstraction.method.2.8="
									+ load.getProperty("reb2sac.abstraction.method.2.8"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.2.2")) {
						if (!load.getProperty("reb2sac.abstraction.method.2.2").equals("dimerization-reduction")
								&& !load.getProperty("reb2sac.abstraction.method.2.2").equals(
										"dimerization-reduction-level-assignment")) {
							loadProperties.add("reb2sac.abstraction.method.2.2="
									+ load.getProperty("reb2sac.abstraction.method.2.2"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.3.1")) {
						if (!load.getProperty("reb2sac.abstraction.method.3.1").equals(
								"kinetic-law-constants-simplifier")
								&& !load.getProperty("reb2sac.abstraction.method.3.1").equals(
										"reversible-to-irreversible-transformer")
								&& !load.getProperty("reb2sac.abstraction.method.3.1").equals(
										"nary-order-unary-transformer")) {
							loadProperties.add("reb2sac.abstraction.method.3.1="
									+ load.getProperty("reb2sac.abstraction.method.3.1"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.3.2")) {
						if (!load.getProperty("reb2sac.abstraction.method.3.2").equals(
								"kinetic-law-constants-simplifier")
								&& !load.getProperty("reb2sac.abstraction.method.3.2").equals(
										"modifier-constant-propagation")) {
							loadProperties.add("reb2sac.abstraction.method.3.2="
									+ load.getProperty("reb2sac.abstraction.method.3.2"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.3.3")) {
						if (!load.getProperty("reb2sac.abstraction.method.3.3").equals("absolute-inhibition-generator")) {
							loadProperties.add("reb2sac.abstraction.method.3.3="
									+ load.getProperty("reb2sac.abstraction.method.3.3"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.3.4")) {
						if (!load.getProperty("reb2sac.abstraction.method.3.4").equals("final-state-generator")) {
							loadProperties.add("reb2sac.abstraction.method.3.4="
									+ load.getProperty("reb2sac.abstraction.method.3.4"));
						}
					}
					else if (key.equals("reb2sac.abstraction.method.3.5")) {
						if (!load.getProperty("reb2sac.abstraction.method.3.5").equals("stop-flag-generator")) {
							loadProperties.add("reb2sac.abstraction.method.3.5="
									+ load.getProperty("reb2sac.abstraction.method.3.5"));
						}
					}
					else if (key.equals("reb2sac.nary.order.decider")) {
						if (!load.getProperty("reb2sac.nary.order.decider").equals("distinct")) {
							loadProperties.add("reb2sac.nary.order.decider="
									+ load.getProperty("reb2sac.nary.order.decider"));
						}
					}
					else if (key.equals("simulation.printer")) {
						if (!load.getProperty("simulation.printer").equals("tsd.printer")) {
							loadProperties.add("simulation.printer=" + load.getProperty("simulation.printer"));
						}
					}
					else if (key.equals("simulation.printer.tracking.quantity")) {
						if (!load.getProperty("simulation.printer.tracking.quantity").equals("amount")) {
							loadProperties.add("simulation.printer.tracking.quantity="
									+ load.getProperty("simulation.printer.tracking.quantity"));
						}
					}
					else if (((String) key).length() > 27
							&& ((String) key).substring(0, 28).equals("reb2sac.interesting.species.")) {
					}
					else if (key.equals("reb2sac.rapid.equilibrium.condition.1")) {
					}
					else if (key.equals("reb2sac.rapid.equilibrium.condition.2")) {
					}
					else if (key.equals("reb2sac.qssa.condition.1")) {
					}
					else if (key.equals("reb2sac.operator.max.concentration.threshold")) {
					}
					else if (key.equals("reb2sac.diffusion.stoichiometry.amplification.value")){
					}
					else if (key.equals("reb2sac.iSSA.number.paths")){
					}
					else if (key.equals("reb2sac.iSSA.type")){
					}
					else if (key.equals("reb2sac.iSSA.adaptive")){
					}
					else if (key.equals("ode.simulation.time.limit")) {
					}
					else if (key.equals("ode.simulation.print.interval")) {
					}
					else if (key.equals("ode.simulation.number.steps")) {
					}
					else if (key.equals("ode.simulation.min.time.step")) {
					}
					else if (key.equals("ode.simulation.time.step")) {
					}
					else if (key.equals("ode.simulation.absolute.error")) {
					}
					else if (key.equals("ode.simulation.out.dir")) {
					}
					else if (key.equals("monte.carlo.simulation.time.limit")) {
					}
					else if (key.equals("monte.carlo.simulation.print.interval")) {
					}
					else if (key.equals("monte.carlo.simulation.number.steps")) {
					}
					else if (key.equals("monte.carlo.simulation.min.time.step")) {
					}
					else if (key.equals("monte.carlo.simulation.time.step")) {
					}
					else if (key.equals("monte.carlo.simulation.random.seed")) {
					}
					else if (key.equals("monte.carlo.simulation.runs")) {
					}
					else if (key.equals("monte.carlo.simulation.out.dir")) {
					}
					else if (key.equals("simulation.run.termination.decider")) {
					}
					else if (key.equals("computation.analysis.sad.path")) {
					}
					else if (key.equals("simulation.time.series.species.level.file")) {
					}
					else if (key.equals("reb2sac.simulation.method")) {
					}
					else if (key.equals("reb2sac.abstraction.method")) {
					}
					else if (key.equals("selected.simulator")) {
					}
					else if (key.equals("file.stem")) {
					}
					else if (((String) key).length() > 36
							&& ((String) key).substring(0, 37).equals("simulation.run.termination.condition.")) {
					}
					else if (((String) key).length() > 37
							&& ((String) key).substring(0, 38).equals("reb2sac.absolute.inhibition.threshold.")) {
					}
					else if (((String) key).length() > 27
							&& ((String) key).substring(0, 28).equals("reb2sac.concentration.level.")) {
					}
					else if (((String) key).length() > 19
							&& ((String) key).substring(0, 20).equals("reb2sac.final.state.")) {
					}
					else if (key.equals("reb2sac.analysis.stop.enabled")) {
					}
					else if (key.equals("reb2sac.analysis.stop.rate")) {
					}
					else if (key.equals("monte.carlo.simulation.start.index")) {
					}
					else if (key.equals("abstraction.interesting") && lhpnAbstraction != null) {
						String intVars = load.getProperty("abstraction.interesting");
						String[] array = intVars.split(" ");
						for (String s : array) {
							if (!s.equals("")) {
								lhpnAbstraction.addIntVar(s);
							}
						}
					}
					else if (key.equals("abstraction.factor") && lhpnAbstraction != null) {
						lhpnAbstraction.factorField.setText(load.getProperty("abstraction.factor"));
					}
					else if (key.equals("abstraction.iterations") && lhpnAbstraction != null) {
						lhpnAbstraction.iterField.setText(load.getProperty("abstraction.iterations"));
					}
					else if (key.toString().startsWith("abstraction.transform")) {
						continue;
					}
					else {
						loadProperties.add(key + "=" + load.getProperty((String) key));
					}
				}
				HashMap<Integer, String> preOrder = new HashMap<Integer, String>();
				HashMap<Integer, String> loopOrder = new HashMap<Integer, String>();
				HashMap<Integer, String> postOrder = new HashMap<Integer, String>();
				HashMap<String, Boolean> containsXform = new HashMap<String, Boolean>();
				boolean containsAbstractions = false;
				if (lhpnAbstraction != null) {
					for (String s : lhpnAbstraction.transforms) {
						if (load.containsKey("abstraction.transform." + s)) {
							if (load.getProperty("abstraction.transform." + s).contains("preloop")) {
								Pattern prePattern = Pattern.compile("preloop(\\d+)");
								Matcher intMatch = prePattern.matcher(load.getProperty("abstraction.transform." + s));
								if (intMatch.find()) {
									Integer index = Integer.parseInt(intMatch.group(1));
									preOrder.put(index, s);
								}
								else {
									lhpnAbstraction.addPreXform(s);
								}
							}
							else {
								lhpnAbstraction.preAbsModel.removeElement(s);
							}
							if (load.getProperty("abstraction.transform." + s).contains("mainloop")) {
								Pattern loopPattern = Pattern.compile("mainloop(\\d+)");
								Matcher intMatch = loopPattern.matcher(load.getProperty("abstraction.transform." + s));
								if (intMatch.find()) {
									Integer index = Integer.parseInt(intMatch.group(1));
									loopOrder.put(index, s);
								}
								else {
									lhpnAbstraction.addLoopXform(s);
								}
							}
							else {
								lhpnAbstraction.loopAbsModel.removeElement(s);
							}
							if (load.getProperty("abstraction.transform." + s).contains("postloop")) {
								Pattern postPattern = Pattern.compile("postloop(\\d+)");
								Matcher intMatch = postPattern.matcher(load.getProperty("abstraction.transform." + s));
								if (intMatch.find()) {
									Integer index = Integer.parseInt(intMatch.group(1));
									postOrder.put(index, s);
								}
								else {
									lhpnAbstraction.addPostXform(s);
								}
							}
							else {
								lhpnAbstraction.postAbsModel.removeElement(s);
							}
						}
						else if (containsAbstractions && !containsXform.get(s)) {
							lhpnAbstraction.preAbsModel.removeElement(s);
							lhpnAbstraction.loopAbsModel.removeElement(s);
							lhpnAbstraction.postAbsModel.removeElement(s);
						}
					}
					if (preOrder.size() > 0) {
						lhpnAbstraction.preAbsModel.removeAllElements();
					}
					for (Integer j = 0; j < preOrder.size(); j++) {
						lhpnAbstraction.preAbsModel.addElement(preOrder.get(j));
					}
					if (loopOrder.size() > 0) {
						lhpnAbstraction.loopAbsModel.removeAllElements();
					}
					for (Integer j = 0; j < loopOrder.size(); j++) {
						lhpnAbstraction.loopAbsModel.addElement(loopOrder.get(j));
					}
					if (postOrder.size() > 0) {
						lhpnAbstraction.postAbsModel.removeAllElements();
					}
					for (Integer j = 0; j < postOrder.size(); j++) {
						lhpnAbstraction.postAbsModel.addElement(postOrder.get(j));
					}
					lhpnAbstraction.preAbs.setListData(lhpnAbstraction.preAbsModel.toArray());
					lhpnAbstraction.loopAbs.setListData(lhpnAbstraction.loopAbsModel.toArray());
					lhpnAbstraction.postAbs.setListData(lhpnAbstraction.postAbsModel.toArray());
				}
				if (load.getProperty("reb2sac.abstraction.method").equals("none")) {
					none.setSelected(true);
					Button_Enabling.enableNoneOrAbs(ODE, monteCarlo, markov, fba, sbml, seed, seedLabel, runs, runsLabel,
							minStepLabel, minStep, stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel,
							interval, simulators, simulatorsLabel, explanation, description, none, rapid1, rapid2, qssa,
							maxCon, diffStoichAmp, rapidLabel1, rapidLabel2, qssaLabel, maxConLabel, diffStoichAmpLabel, 
							fileStem, fileStemLabel, preAbs, loopAbs,
							postAbs, preAbsLabel, loopAbsLabel, postAbsLabel, addPreAbs, rmPreAbs, editPreAbs, addLoopAbs,
							rmLoopAbs, editLoopAbs, addPostAbs, rmPostAbs, editPostAbs, lhpn);
					if (modelFile.contains(".lpn")) {
						markov.setEnabled(true);
						lhpn.setEnabled(true);
					}
				}
				else if (load.getProperty("reb2sac.abstraction.method").equals("abs")) {
					abstraction.setSelected(true);
					Button_Enabling.enableNoneOrAbs(ODE, monteCarlo, markov, fba, sbml, seed, seedLabel, runs, runsLabel,
							minStepLabel, minStep, stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel,
							interval, simulators, simulatorsLabel, explanation, description, none, rapid1, rapid2, qssa,
							maxCon, diffStoichAmp, rapidLabel1, rapidLabel2, qssaLabel, maxConLabel, diffStoichAmpLabel, 
							fileStem, fileStemLabel, preAbs, loopAbs,
							postAbs, preAbsLabel, loopAbsLabel, postAbsLabel, addPreAbs, rmPreAbs, editPreAbs, addLoopAbs,
							rmLoopAbs, editLoopAbs, addPostAbs, rmPostAbs, editPostAbs, lhpn);
					if (modelFile.contains(".lpn")) {
						markov.setEnabled(true);
						lhpn.setEnabled(true);
					}
				}
				else {
					nary.setSelected(true);
					Button_Enabling.enableNary(ODE, monteCarlo, markov, fba, seed, seedLabel, runs, runsLabel, minStepLabel,
							minStep, stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
							simulators, simulatorsLabel, explanation, description, rapid1, rapid2, qssa, maxCon,
							rapidLabel1, rapidLabel2, qssaLabel, maxConLabel, fileStem, fileStemLabel, preAbs, loopAbs,
							postAbs, preAbsLabel, loopAbsLabel, postAbsLabel, addPreAbs, rmPreAbs, editPreAbs,
							addLoopAbs, rmLoopAbs, editLoopAbs, addPostAbs, rmPostAbs, editPostAbs, lhpn, modelEditor);
				}
				if (load.containsKey("ode.simulation.absolute.error")) {
					absErr.setText(load.getProperty("ode.simulation.absolute.error"));
				}
				else {
					absErr.setText("1.0E-9");
				}
				if (load.containsKey("monte.carlo.simulation.time.step")) {
					step.setText(load.getProperty("monte.carlo.simulation.time.step"));
				}
				else {
					step.setText("inf");
				}
				if (load.containsKey("monte.carlo.simulation.min.time.step")) {
					minStep.setText(load.getProperty("monte.carlo.simulation.min.time.step"));
				}
				else {
					minStep.setText("0");
				}
				if (load.containsKey("monte.carlo.simulation.time.limit")) {
					limit.setText(load.getProperty("monte.carlo.simulation.time.limit"));
				}
				else {
					limit.setText("100.0");
				}
				if (load.containsKey("monte.carlo.simulation.print.interval")) {
					intervalLabel.setSelectedItem("Print Interval");
					interval.setText(load.getProperty("monte.carlo.simulation.print.interval"));
				}
				else if (load.containsKey("monte.carlo.simulation.minimum.print.interval")) {
					intervalLabel.setSelectedItem("Minimum Print Interval");
					interval.setText(load.getProperty("monte.carlo.simulation.minimum.print.interval"));
				}
				else if (load.containsKey("monte.carlo.simulation.number.steps")) {
					intervalLabel.setSelectedItem("Number Of Steps");
					interval.setText(load.getProperty("monte.carlo.simulation.number.steps"));
				}
				else {
					interval.setText("1.0");
				}
				if (load.containsKey("monte.carlo.simulation.random.seed")) {
					seed.setText(load.getProperty("monte.carlo.simulation.random.seed"));
				}
				if (load.containsKey("monte.carlo.simulation.runs")) {
					runs.setText(load.getProperty("monte.carlo.simulation.runs"));
				}
				if (load.containsKey("simulation.time.series.species.level.file")) {
					// usingSSA.doClick();
				}
				else {
					description.setEnabled(true);
					explanation.setEnabled(true);
					simulators.setEnabled(true);
					simulatorsLabel.setEnabled(true);
					if (!nary.isSelected()) {
						ODE.setEnabled(true);
					}
					else {
						markov.setEnabled(true);
					}
				}
				if (load.containsKey("simulation.printer.tracking.quantity")) {
					if (load.getProperty("simulation.printer.tracking.quantity").equals("concentration")) {
						concentrations.doClick();
					}
				}
				if (load.containsKey("simulation.printer")) {
					if (load.getProperty("simulation.printer").equals("null.printer")) {
						genRuns.doClick();
					}
				}
				if (load.containsKey("reb2sac.simulation.method")) {
					if (load.getProperty("reb2sac.simulation.method").equals("ODE")) {
						ODE.setSelected(true);
						if (load.containsKey("ode.simulation.time.limit")) {
							limit.setText(load.getProperty("ode.simulation.time.limit"));
						}
						if (load.containsKey("ode.simulation.print.interval")) {
							intervalLabel.setSelectedItem("Print Interval");
							interval.setText(load.getProperty("ode.simulation.print.interval"));
						}
						if (load.containsKey("ode.simulation.minimum.print.interval")) {
							intervalLabel.setSelectedItem("Minimum Print Interval");
							interval.setText(load.getProperty("ode.simulation.minimum.print.interval"));
						}
						else if (load.containsKey("ode.simulation.number.steps")) {
							intervalLabel.setSelectedItem("Number Of Steps");
							interval.setText(load.getProperty("ode.simulation.number.steps"));
						}
						if (load.containsKey("ode.simulation.time.step")) {
							step.setText(load.getProperty("ode.simulation.time.step"));
						}
						if (load.containsKey("ode.simulation.min.time.step")) {
							minStep.setText(load.getProperty("ode.simulation.min.time.step"));
						}
						Button_Enabling.enableODE(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
								step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators,
								simulatorsLabel, explanation, description, fileStem, fileStemLabel, postAbs,
								abstraction);
						if (load.containsKey("selected.simulator")) {
							simulators.setSelectedItem(load.getProperty("selected.simulator"));
						}
						if (load.containsKey("file.stem")) {
							fileStem.setText(load.getProperty("file.stem"));
						}
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("monteCarlo")) {
						monteCarlo.setSelected(true);
						if (runFiles) {
							// overwrite.setEnabled(true);
							append.setEnabled(true);
							// choose3.setEnabled(true);
						}
						Button_Enabling.enableMonteCarlo(seed, seedLabel, runs, runsLabel, minStepLabel, minStep,
								stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
								simulators, simulatorsLabel, explanation, description, fileStem, fileStemLabel,
								postAbs, abstraction, nary);
						if (load.containsKey("selected.simulator")) {
							String simId = load.getProperty("selected.simulator");
							if (simId.equals("mpde")) {
								simulators.setSelectedItem("iSSA");
								mpde.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("mean_path")) {
								simulators.setSelectedItem("iSSA");
								meanPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("median_path")) {
								simulators.setSelectedItem("iSSA");
								medianPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("mean_path-bifurcation")) {
								simulators.setSelectedItem("iSSA");
								meanPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("2");
							} else if (simId.equals("median_path-bifurcation")) {
								simulators.setSelectedItem("iSSA");
								medianPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("2");
							} else if (simId.equals("mean_path-adaptive")) {
								simulators.setSelectedItem("iSSA");
								meanPath.doClick();
								adaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("median_path-adaptive")) {
								simulators.setSelectedItem("iSSA");
								medianPath.doClick();
								adaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("mean_path-adaptive-bifurcation")) {
								simulators.setSelectedItem("iSSA");
								meanPath.doClick();
								adaptive.doClick();
								bifurcation.setSelectedItem("2");
							} else if (simId.equals("median_path-adaptive-bifurcation")) {
								simulators.setSelectedItem("iSSA");
								medianPath.doClick();
								adaptive.doClick();
								bifurcation.setSelectedItem("2");
							} else if (simId.equals("mean_path-event")) {
								simulators.setSelectedItem("iSSA");
								meanPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("median_path-event")) {
								simulators.setSelectedItem("iSSA");
								medianPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("1");
							} else if (simId.equals("mean_path-event-bifurcation")) {
								simulators.setSelectedItem("iSSA");
								meanPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("2");
							} else if (simId.equals("median_path-event-bifurcation")) {
								simulators.setSelectedItem("iSSA");
								medianPath.doClick();
								nonAdaptive.doClick();
								bifurcation.setSelectedItem("2");
							} else {
								simulators.setSelectedItem(simId);
							}
						}
						if (load.containsKey("file.stem")) {
							fileStem.setText(load.getProperty("file.stem"));
						}
						absErr.setEnabled(false);
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("markov")) {
						markov.setSelected(true);
						Button_Enabling.enableMarkov(seed, seedLabel, runs, runsLabel, minStepLabel, minStep,
								stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
								simulators, simulatorsLabel, explanation, description, fileStem, fileStemLabel,
								modelEditor, postAbs, modelFile);
						if (load.containsKey("selected.simulator")) {
							selectedMarkovSim = load.getProperty("selected.simulator");
							simulators.setSelectedItem(selectedMarkovSim);
						}
						absErr.setEnabled(false);
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("FBA")) {
						fba.doClick();
						Button_Enabling.enableFBA(seed, seedLabel, runs, runsLabel, minStepLabel, minStep, stepLabel,
								step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval, simulators, simulatorsLabel,
								explanation, description, fileStem, fileStemLabel, abstraction, nary, loopAbs, postAbs);
						//absErr.setEnabled(false);
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("SBML")) {
						sbml.setSelected(true);
						Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep,
								stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
								simulators, simulatorsLabel, explanation, description, fileStem, fileStemLabel,
								abstraction, loopAbs, postAbs);
						absErr.setEnabled(false);
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("Network")) {
						dot.setSelected(true);
						Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep,
								stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
								simulators, simulatorsLabel, explanation, description, fileStem, fileStemLabel,
								abstraction, loopAbs, postAbs);
						absErr.setEnabled(false);
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("Browser")) {
						xhtml.setSelected(true);
						Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep,
								stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
								simulators, simulatorsLabel, explanation, description, fileStem, fileStemLabel,
								abstraction, loopAbs, postAbs);
						absErr.setEnabled(false);
					}
					else if (load.getProperty("reb2sac.simulation.method").equals("LPN")) {
						lhpn.setSelected(true);
						Button_Enabling.enableSbmlDotAndXhtml(seed, seedLabel, runs, runsLabel, minStepLabel, minStep,
								stepLabel, step, errorLabel, absErr, limitLabel, limit, intervalLabel, interval,
								simulators, simulatorsLabel, explanation, description, fileStem, fileStemLabel,
								abstraction, loopAbs, postAbs);
						absErr.setEnabled(false);
					}
				}
				if (load.containsKey("reb2sac.abstraction.method")) {
					if (load.getProperty("reb2sac.abstraction.method").equals("none")) {
						none.setSelected(true);
						abstraction.setSelected(false);
						nary.setSelected(false);
					}
					else if (load.getProperty("reb2sac.abstraction.method").equals("abs")) {
						none.setSelected(false);
						abstraction.setSelected(true);
						nary.setSelected(false);
					}
					else if (load.getProperty("reb2sac.abstraction.method").equals("nary")) {
						none.setSelected(false);
						abstraction.setSelected(false);
						nary.setSelected(true);
					}
				}
				if (load.containsKey("selected.property")) {
					if (transientProperties != null) {
						transientProperties.setSelectedItem(load.getProperty("selected.property"));
					}
				}
				ArrayList<String> getLists = new ArrayList<String>();
				int i = 1;
				while (load.containsKey("simulation.run.termination.condition." + i)) {
					getLists.add(load.getProperty("simulation.run.termination.condition." + i));
					i++;
				}
				// termConditions = getLists.toArray();
				// terminations.setListData(termConditions);
				getLists = new ArrayList<String>();
				i = 1;
				while (load.containsKey("reb2sac.interesting.species." + i)) {
					String species = load.getProperty("reb2sac.interesting.species." + i);
					int j = 2;
					String interesting = " ";
					if (load.containsKey("reb2sac.concentration.level." + species + ".1")) {
						interesting += load.getProperty("reb2sac.concentration.level." + species + ".1");
					}
					while (load.containsKey("reb2sac.concentration.level." + species + "." + j)) {
						interesting += "," + load.getProperty("reb2sac.concentration.level." + species + "." + j);
						j++;
					}
					if (!interesting.equals(" ")) {
						species += interesting;
					}
					getLists.add(species);
					i++;
				}
				for (String s : getLists) {
					String[] split1 = s.split(" ");

					// load the species and its thresholds into the list of
					// interesting species
					String speciesAndThresholds = split1[0];

					if (split1.length > 1)
						speciesAndThresholds += " " + split1[1];

					interestingSpecies.add(speciesAndThresholds);
				}

				getLists = new ArrayList<String>();
				i = 1;
				while (load.containsKey("gcm.abstraction.method." + i)) {
					getLists.add(load.getProperty("gcm.abstraction.method." + i));
					i++;
				}
				i = 1;
				while (load.containsKey("reb2sac.abstraction.method.1." + i)) {
					getLists.add(load.getProperty("reb2sac.abstraction.method.1." + i));
					i++;
				}
				preAbstractions = getLists.toArray();
				preAbs.setListData(preAbstractions);

				getLists = new ArrayList<String>();
				i = 1;
				while (load.containsKey("reb2sac.abstraction.method.2." + i)) {
					getLists.add(load.getProperty("reb2sac.abstraction.method.2." + i));
					i++;
				}
				loopAbstractions = getLists.toArray();
				loopAbs.setListData(loopAbstractions);

				getLists = new ArrayList<String>();
				i = 1;
				while (load.containsKey("reb2sac.abstraction.method.3." + i)) {
					getLists.add(load.getProperty("reb2sac.abstraction.method.3." + i));
					i++;
				}
				postAbstractions = getLists.toArray();
				postAbs.setListData(postAbstractions);

				if (load.containsKey("reb2sac.rapid.equilibrium.condition.1")) {
					rapid1.setText(load.getProperty("reb2sac.rapid.equilibrium.condition.1"));
				}
				if (load.containsKey("reb2sac.rapid.equilibrium.condition.2")) {
					rapid2.setText(load.getProperty("reb2sac.rapid.equilibrium.condition.2"));
				}
				if (load.containsKey("reb2sac.qssa.condition.1")) {
					qssa.setText(load.getProperty("reb2sac.qssa.condition.1"));
				}
				if (load.containsKey("reb2sac.operator.max.concentration.threshold")) {
					maxCon.setText(load.getProperty("reb2sac.operator.max.concentration.threshold"));
				}
				if (load.containsKey("reb2sac.diffusion.stoichiometry.amplification.value")) {
					diffStoichAmp.setText(load.getProperty("reb2sac.diffusion.stoichiometry.amplification.value"));
				}
				if (load.containsKey("reb2sac.iSSA.number.paths")) {
					bifurcation.setSelectedItem(load.getProperty("reb2sac.iSSA.number.paths"));
				}
				if (load.containsKey("reb2sac.iSSA.type")) {
					String type = load.getProperty("reb2sac.iSSA.type");
					if (type.equals("mpde")) {
						mpde.doClick();
					} else if (type.equals("medianPath")) {
						medianPath.doClick();
					} else {
						meanPath.doClick();
					}
				}
				if (load.containsKey("reb2sac.iSSA.adaptive")) {
					String type = load.getProperty("reb2sac.iSSA.adaptive");
					if (type.equals("true")) {
						adaptive.doClick();
					} else {
						nonAdaptive.doClick();
					} 
				}
			}
			else {
				if (load.containsKey("selected.simulator")) {
					simulators.setSelectedItem(load.getProperty("selected.simulator"));
				}
				if (load.containsKey("file.stem")) {
					fileStem.setText(load.getProperty("file.stem"));
				}
				if (load.containsKey("simulation.printer.tracking.quantity")) {
					if (load.getProperty("simulation.printer.tracking.quantity").equals("concentration")) {
						concentrations.doClick();
					}
				}
				if (load.containsKey("simulation.printer")) {
					if (load.getProperty("simulation.printer").equals("null.printer")) {
						genRuns.doClick();
					}
				}
			}
			change = false;
		}
		catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(Gui.frame, "Unable to load properties file!", "Error Loading Properties",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public Graph createGraph(String open) {
		String outDir = root + separator + simName;
		String printer_id;
		printer_id = "tsd.printer";
		String printer_track_quantity = "amount";
		if (concentrations.isSelected()) {
			printer_track_quantity = "concentration";
		}
		// int[] index = species.getSelectedIndices();
		// species.setSelectedIndices(index);
		return new Graph(this, printer_track_quantity, simName + " simulation results", printer_id, outDir, "time",
				biomodelsim, open, log, null, true, false);
	}

	public JButton getRunButton() {
		return run;
	}

	public JButton getSaveButton() {
		return save;
	}

	// Reports which gcm abstraction options are selected
	public ArrayList<String> getGcmAbstractions() {
		ArrayList<String> gcmAbsList = new ArrayList<String>();
		ListModel preAbsList = preAbs.getModel();
		for (int i = 0; i < preAbsList.getSize(); i++) {
			String abstractionOption = (String) preAbsList.getElementAt(i);
			if (abstractionOption.equals("complex-formation-and-sequestering-abstraction")
					|| abstractionOption.equals("operator-site-reduction-abstraction"))
				// ||
				// abstractionOption.equals("species-sequestering-abstraction"))
				gcmAbsList.add(abstractionOption);
		}
		return gcmAbsList;
	}

	// Reports if any reb2sac abstraction options are selected
	public boolean reb2sacAbstraction() {
		ListModel preAbsList = preAbs.getModel();
		for (int i = 0; i < preAbsList.getSize(); i++) {
			String abstractionOption = (String) preAbsList.getElementAt(i);
			if (!abstractionOption.equals("complex-formation-and-sequestering-abstraction")
					&& !abstractionOption.equals("operator-site-reduction-abstraction"))
				// &&
				// !abstractionOption.equals("species-sequestering-abstraction"))
				return true;
		}
		ListModel loopAbsList = loopAbs.getModel();
		if (loopAbsList.getSize() > 0)
			return true;
		ListModel postAbsList = postAbs.getModel();
		if (postAbsList.getSize() > 0)
			return true;
		return false;
	}

	/*
	public void setSbml(SBML_Editor sbml) {
		sbmlEditor = sbml;
	}
*/
	
	public void setGcm(ModelEditor gcm) {
		modelEditor = gcm;
		if (nary.isSelected()) {
			lhpn.setEnabled(true);
		}
		if (markov.isSelected()) {
			simulators.removeAllItems();
			simulators.addItem("steady-state-markov-chain-analysis");
			simulators.addItem("transient-markov-chain-analysis");
			simulators.addItem("reachability-analysis");
			simulators.addItem("atacs");
			simulators.addItem("ctmc-transient");
			if (selectedMarkovSim != null) {
				simulators.setSelectedItem(selectedMarkovSim);
			}
		}
		change = false;
	}


	public void setSim(String newSimName) {
		sbmlProp = root + separator + newSimName + separator
				+ sbmlFile.split(separator)[sbmlFile.split(separator).length - 1];
		simName = newSimName;
	}

	public boolean hasChanged() {
		return change;
	}

	public void addLhpnAbstraction(AbstPane pane) {
		lhpnAbstraction = pane;
	}


	public String[] getInterestingSpecies() {
		return interestingSpecies.toArray(new String[0]);
	}

	public ArrayList<String> getInterestingSpeciesAsArrayList() {
		return interestingSpecies;
	}

	/**
	 * adds a string with the species ID and its threshold values to the
	 * arraylist of interesting species
	 * 
	 * @param speciesAndThresholds
	 */
	public void addInterestingSpecies(String speciesAndThresholds) {
		String species = speciesAndThresholds.split(" ")[0];
		for (int i = 0; i < interestingSpecies.size(); i++) {
			if (interestingSpecies.get(i).split(" ")[0].equals(species)) {
				interestingSpecies.set(i, speciesAndThresholds);
				return;
			}
		}
		interestingSpecies.add(speciesAndThresholds);
	}

	/**
	 * removes a string with the species ID and its threshold values from the
	 * arraylist of interesting species
	 * 
	 * @param species
	 */
	public void removeInterestingSpecies(String species) {
		for (int i = 0; i < interestingSpecies.size(); i++) {
			if (interestingSpecies.get(i).split(" ")[0].equals(species)) {
				interestingSpecies.remove(i);
				return;
			}
		}
	}

	/*
	 * private String[] getAllSpecies() { ArrayList<String> species = new
	 * ArrayList<String>(); for (Object s : allSpecies) { species.add((String)
	 * s); } for (int i = 0; i < speciesInt.size(); i++) { String spec =
	 * ((JTextField) speciesInt.get(i).get(1)).getText(); if
	 * (species.contains(spec)) { species.set(species.indexOf(spec), spec + " "
	 * + getLine(i)); } } String[] speciesArray = species.toArray(new
	 * String[0]); for (int i = 1; i < speciesArray.length; i++) { String index
	 * = (String) speciesArray[i]; int j = i; while ((j > 0) && ((String)
	 * speciesArray[j - 1]).compareToIgnoreCase(index) > 0) { speciesArray[j] =
	 * speciesArray[j - 1]; j = j - 1; } speciesArray[j] = index; } return
	 * speciesArray; }
	 * 
	 * private void editNumThresholds(int num) { try { ArrayList<Component>
	 * specs = speciesInt.get(num); Component[] panels =
	 * speciesPanel.getComponents(); int boxes = Integer.parseInt((String)
	 * ((JComboBox) specs.get(2)).getSelectedItem()); if ((specs.size() - 3) <
	 * boxes) { for (int i = 0; i < boxes; i++) { try { specs.get(i + 3); }
	 * catch (Exception e1) { JTextField temp = new JTextField(""); ((JPanel)
	 * panels[num + 1]).add(temp); specs.add(temp); } } } else { try { if (boxes
	 * > 0) { while (true) { specs.remove(boxes + 3); ((JPanel) panels[num +
	 * 1]).remove(boxes + 3); } } else if (boxes == 0) { while (true) {
	 * specs.remove(3); ((JPanel) panels[num + 1]).remove(3); } } } catch
	 * (Exception e1) { } } int max = 0; for (int i = 0; i < speciesInt.size();
	 * i++) { max = Math.max(max, speciesInt.get(i).size()); } if (((JPanel)
	 * panels[0]).getComponentCount() < max) { for (int i = 0; i < max - 3; i++)
	 * { try { ((JPanel) panels[0]).getComponent(i + 3); } catch (Exception e) {
	 * ((JPanel) panels[0]).add(new JLabel("Threshold " + (i + 1))); } } } else
	 * { try { while (true) { ((JPanel) panels[0]).remove(max); } } catch
	 * (Exception e) { } } for (int i = 1; i < panels.length; i++) { JPanel sp =
	 * (JPanel) panels[i]; for (int j = sp.getComponentCount() - 1; j >= 3; j--)
	 * { if (sp.getComponent(j) instanceof JLabel) { sp.remove(j); } } if (max >
	 * sp.getComponentCount()) { for (int j = sp.getComponentCount(); j < max;
	 * j++) { sp.add(new JLabel()); } } else { for (int j =
	 * sp.getComponentCount() - 3; j >= max; j--) { sp.remove(j); } } } } catch
	 * (Exception e) { } }
	 */

	public Graph createProbGraph(String open) {
		String outDir = root + separator + simName;
		String printer_id;
		printer_id = "tsd.printer";
		String printer_track_quantity = "amount";
		if (concentrations.isSelected()) {
			printer_track_quantity = "concentration";
		}
		return new Graph(this, printer_track_quantity, simName + " simulation results", printer_id, outDir, "time",
				biomodelsim, open, log, null, false, false);
	}

	public void run(ArrayList<AnalysisThread> threads, ArrayList<String> dirs, ArrayList<String> levelOne, String stem) {
		for (AnalysisThread thread : threads) {
			try {
				thread.join();
			}
			catch (InterruptedException e) {
			}
		}
		if (!dirs.isEmpty()
				&& new File(root + separator + simName + separator + stem + dirs.get(0) + separator + "sim-rep.txt")
						.exists()) {
			ArrayList<String> dataLabels = new ArrayList<String>();
			ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double>>();
			String spec = dirs.get(0).split("=")[0];
			dataLabels.add(spec);
			data.add(new ArrayList<Double>());
			for (String prefix : levelOne) {
				double val = Double.parseDouble(prefix.split("=")[1].split("_")[0]);
				data.get(0).add(val);
				for (String d : dirs) {
					if (d.startsWith(prefix)) {
						String suffix = d.replace(prefix, "");
						ArrayList<String> vals = new ArrayList<String>();
						try {
							Scanner s = new Scanner(new File(root + separator + simName + separator + stem + d
									+ separator + "sim-rep.txt"));
							while (s.hasNextLine()) {
								String[] ss = s.nextLine().split(" ");
								if (ss[0].equals("The") && ss[1].equals("total") && ss[2].equals("termination")
										&& ss[3].equals("count:") && ss[4].equals("0")) {
								}
								if (vals.size() == 0) {
									for (String add : ss) {
										vals.add(add + suffix);
									}
								}
								else {
									for (int i = 0; i < ss.length; i++) {
										vals.set(i, vals.get(i) + " " + ss[i]);
									}
								}
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						double total = 0;
						int i = 0;
						if (vals.get(0).split(" ")[0].startsWith("#total")) {
							total = Double.parseDouble(vals.get(0).split(" ")[1]);
							i = 1;
						}
						for (; i < vals.size(); i++) {
							int index;
							if (dataLabels.contains(vals.get(i).split(" ")[0])) {
								index = dataLabels.indexOf(vals.get(i).split(" ")[0]);
							}
							else {
								dataLabels.add(vals.get(i).split(" ")[0]);
								data.add(new ArrayList<Double>());
								index = dataLabels.size() - 1;
							}
							if (total == 0) {
								data.get(index).add(Double.parseDouble(vals.get(i).split(" ")[1]));
							}
							else {
								data.get(index).add(100 * ((Double.parseDouble(vals.get(i).split(" ")[1])) / total));
							}
						}
					}
				}
			}
			DataParser constData = new DataParser(dataLabels, data);
			constData.outputTSD(root + separator + simName + separator + "sim-rep.tsd");
			for (int i = 0; i < simTab.getComponentCount(); i++) {
				if (simTab.getComponentAt(i).getName().equals("TSD Graph")) {
					if (simTab.getComponentAt(i) instanceof Graph) {
						((Graph) simTab.getComponentAt(i)).refresh();
					}
				}
			}
		}
	}

	public void run() {
	}

	public JPanel getAdvanced() {
		JPanel constructPanel = new JPanel(new BorderLayout());
		constructPanel.add(advanced, "Center");
		JButton runButton = new JButton("Save and Run");
		JButton saveButton = new JButton("Save Parameters");
		JPanel runHolder = new JPanel();
		runHolder.add(runButton);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				run.doClick();
			}
		});
		runButton.setMnemonic(KeyEvent.VK_R);
		runHolder.add(saveButton);
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save.doClick();
			}
		});
		saveButton.setMnemonic(KeyEvent.VK_S);
		// JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		// runHolder, null);
		// splitPane.setDividerSize(0);
		// constructPanel.add(splitPane, "South");
		return constructPanel;
	}

	public JPanel getProperties() {
		JPanel constructPanel = new JPanel(new BorderLayout());
		constructPanel.add(propertiesPanel, "Center");
		JButton runButton = new JButton("Save and Run");
		JButton saveButton = new JButton("Save Parameters");
		JPanel runHolder = new JPanel();
		runHolder.add(runButton);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				run.doClick();
			}
		});
		runButton.setMnemonic(KeyEvent.VK_R);
		runHolder.add(saveButton);
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save.doClick();
			}
		});
		saveButton.setMnemonic(KeyEvent.VK_S);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, runHolder, null);
		splitPane.setDividerSize(0);
		constructPanel.add(splitPane, "South");
		return constructPanel;
	}

	public String getProperty() {
		if (transientProperties != null) {
			if (!((String) transientProperties.getSelectedItem()).equals("none")) {
				return ((String) transientProperties.getSelectedItem());
			}
			else {
				return "";
			}
		}
		else {
			return null;
		}
	}

	public String getSimName() {
		return simName;
	}

	public String getSimID() {
		return fileStem.getText().trim();
	}

	public String getSimPath() {
		if (!fileStem.getText().trim().equals("")) {
			return root + separator + simName + separator + fileStem.getText().trim();
		}
		else {
			return root + separator + simName;
		}
	}

	public void updateBackgroundFile(String updatedFile) {
		backgroundField.setText(updatedFile);
	}

	public String getBackgroundFile() {
		return backgroundField.getText();
	}

	public void updateProperties() {
		if (transientProperties != null && modelFile.contains(".lpn")) {
			Object selected = transientProperties.getSelectedItem();
			String[] props = new String[] { "none" };
			LhpnFile lpn = new LhpnFile();
			lpn.load(root + separator + modelFile);
			String[] getProps = lpn.getProperties().toArray(new String[0]);
			props = new String[getProps.length + 1];
			props[0] = "none";
			for (int i = 0; i < getProps.length; i++) {
				props[i + 1] = getProps[i];
			}
			transientProperties.removeAllItems();
			for (String s : props) {
				transientProperties.addItem(s);
			}
			transientProperties.setSelectedItem(selected);
		}
	}
}