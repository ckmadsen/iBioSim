/*******************************************************************************
 *  
 * This file is part of iBioSim. Please visit <http://www.async.ece.utah.edu/ibiosim>
 * for the latest version of iBioSim.
 *
 * Copyright (C) 2017 University of Utah
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the Apache License. A copy of the license agreement is provided
 * in the file named "LICENSE.txt" included with this software distribution
 * and also available online at <http://www.async.ece.utah.edu/ibiosim/License>.
 *  
 *******************************************************************************/
package edu.utah.ece.async.ibiosim.analysis.simulation.hierarchical;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

import edu.utah.ece.async.ibiosim.analysis.properties.AnalysisProperties;
import edu.utah.ece.async.ibiosim.analysis.properties.SimulationProperties;
import edu.utah.ece.async.ibiosim.analysis.simulation.DynamicSimulation;
import edu.utah.ece.async.ibiosim.analysis.simulation.DynamicSimulation.SimulationType;
import edu.utah.ece.async.ibiosim.dataModels.util.dataparser.TSDParser;

/**
 * 
 *
 * @author Leandro Watanabe
 * @author Chris Myers
 * @author <a href="http://www.async.ece.utah.edu/ibiosim#Credits"> iBioSim Contributors </a>
 * @version %I%
 */
public class HierarchicalTestSuiteRunner
{
  static Set<String> unsupportedTags = new HashSet<String>(Arrays.asList("CSymbolDelay", "StoichiometryMath", "FastReaction", "AlgebraicRule"));
  static AnalysisProperties properties;
  
  /**
   * @param args
   *            %d = args[0] = path to cases %n = args[1] = case id %o =
   *            args[2] = output path
   */
  public static void main(String[] args)
  {

    if (args.length != 2)
    {
      System.out.println("Need two arguments: path to test cases and output path");
      return;
    }

    DynamicSimulation simulator = null;

    properties = new AnalysisProperties("", "", "" , false);
    
    String separator = (File.separator.equals("\\")) ? "\\\\" : File.separator;

    //    String[] casesNeedToChangeTimeStep = new String[] { "00028", "00080", "00128", "00173", "00194", "00196", "00197", "00198", "00200", "00201", "00269", "00274", "00400", "00460", "00276", "00278", "00279", "00870", "00872", "01159", "01160", "01161" };
    //
    //    for (String s : casesNeedToChangeTimeStep)
    //    {
    //      if (s.equals(testcase))
    //      {
    //        maxTimeStep = 0.001;
    //        break;
    //      }
    //    }

    int start = 1;
    int end = 1778;
    
    for(; start <= end; start++ )
    {
      String idcase = String.valueOf(start);
      String testcase = "00000".substring(0, 5-idcase.length()) + idcase;
      System.out.println("Running " + testcase);
      String root = args[0];
      properties.setRoot(root);

      properties.setId(testcase);
      
      properties.setModelFile(testcase + "-sbml-l3v1.xml");
      String filename = properties.getFilename();
      File file = new File(filename);
      if(!file.exists())
      {
        properties.setModelFile(testcase + "-sbml-l3v2.xml");
        filename = properties.getFilename();
        file = new File(filename);
        if(!file.exists())
        {
          System.out.println("Does not have L3V1 or L3V2 version.");
          continue;
        }
      }

      if(!filter(properties.getDirectory(), testcase))
      {
        continue;
      }

      
      String outputDirectory = args[1];

      properties.setOutDir(outputDirectory);
      
      String settingsFile = args[0] + separator + testcase + separator + testcase + "-settings.txt";

      simulator = new DynamicSimulation(SimulationType.HIERARCHICAL_RK);

      readSettings(settingsFile);


      try
      {

        simulator.simulate(properties);

        TSDParser tsdp = new TSDParser(outputDirectory + "run-1.tsd", true);
        tsdp.outputCSV(outputDirectory + testcase + ".csv");
        if(!checkResults(properties.getDirectory(), outputDirectory, testcase))
        {
          System.out.println("Case " + testcase + " is failing...");
        }
      }
      catch (Exception e1)
      {
        e1.printStackTrace();
      }
    }


  }


  private static boolean filter(String path, String testcase)
  {
    Scanner scanner = null;
    try {
      String filename = path+File.separator +testcase+"-model.m";
      scanner = new Scanner(new File(filename));
      while(scanner.hasNextLine())
      {
        String line = scanner.nextLine();

        if(line.startsWith("componentTags: "))
        {
          line = line.substring(15, line.length()).replace(" ","");
          String[] tags = line.split(",");
          for(String tag : tags)
          {
            if(unsupportedTags.contains(tag))
            {
              System.out.println("Does not support " + tag);
              return false;
            }
          }
          return true;
        }
      }

    } 
    catch (FileNotFoundException e) 
    {
      return false;
    }
    finally
    {
      if(scanner != null)
      {
        scanner.close();
      }
    }
    return false;
  }

  private static void readSettings(String filename)
  {

    
    File f = new File(filename);
    Properties p = new Properties();
    FileInputStream in;

    try
    {
      in = new FileInputStream(f);
      p.load(in);
      SimulationProperties simProperties= properties.getSimulationProperties();
      
      double timeLimit = Double.valueOf(p.getProperty("duration")) - Double.valueOf(p.getProperty("start"));
      double relativeError = Double.valueOf(p.getProperty("relative"));
      double absoluteError = Double.valueOf(p.getProperty("absolute"));
      int numSteps = Integer.valueOf(p.getProperty("steps"));
      
      
      simProperties.setTimeLimit(timeLimit);
      simProperties.setRelError(relativeError);
      simProperties.setAbsError(absoluteError);
      simProperties.setNumSteps(numSteps);
      
      for (String intSpecies : p.getProperty("variables").replaceAll(" ", "").split(","))
      {
        simProperties.addIntSpecies(intSpecies);
      }

      String quantityType = p.getProperty("concentration");
      simProperties.setPrinter_track_quantity(quantityType);
      double printInterval = timeLimit / numSteps;
      simProperties.setPrintInterval(printInterval);
    }
    catch (Exception e)
    {
      System.out.println("Could not find properties file.");
    }
  }

  private static DataTable buildDataTable(String filename)
  {
    DataTable table = new DataTable();
    try 
    {
      Scanner scanner = new Scanner(new File(filename));
      String line = scanner.nextLine().replaceAll(" ", "");
      String[] elements = line.split(",");
      for(String element : elements)
      {
        table.addElement(element);  
      }

      while(scanner.hasNextLine())
      {
        line = scanner.nextLine().replaceAll(" ", "");
        elements = line.split(",");

        for(int i = 0; i < elements.length; i++)
        {
          table.addDataEntry(i, Double.valueOf(elements[i]));
        }
      }
      scanner.close();
    } 
    catch (Exception e) 
    {
      return null; 
    }

    return table;
  }
  private static boolean checkResults(String inputDir, String outputDir, String testcase)
  {
    DataTable ref = buildDataTable(inputDir + File.separator + testcase + "-results.csv");
    DataTable sim = buildDataTable(outputDir + File.separator + testcase + ".csv");

    if(ref == null || sim == null)
    {
      return false;
    }
    return ref.compare(sim);
  }

  private static class DataTable
  {
    private Vector<String> elements;
    private ArrayList<ArrayList<Double>> data;

    DataTable()
    {
      elements = new Vector<String>();
      data = new ArrayList<ArrayList<Double>>();
    }

    void addElement(String element)
    {
      elements.add(element);
      data.add(new ArrayList<Double>());
    }

    void addDataEntry(int index, double value)
    {
      data.get(index).add(value);
    }

    int getIndex(String element)
    {
      return elements.indexOf(element);
    }

    boolean compare(DataTable cmp)
    {
      double absoluteError = properties.getSimulationProperties().getAbsError();
      double relativeError = properties.getSimulationProperties().getRelError();
      for(int i = 0; i < elements.size(); i++)
      {
        String element = elements.get(i);

        if(element.equals("time") || element.equals("Time"))
        {
          continue;
        }

        ArrayList<Double> y1 = data.get(i);
        int index = cmp.getIndex(element);

        if(index < 0)
        {
          return false;
        }
        ArrayList<Double> y2 = cmp.data.get(index);

        if(y1.size() != y2.size())
        {
          return false;
        }

        for(int j = 0; j < y1.size(); j++)
        {
          double x1 = y1.get(j);
          double x2 = y2.get(j);
          double diff = Math.abs(x1 - x2);
          double tolerance = absoluteError + relativeError*Math.abs(x1);
          if(diff > tolerance)
          {
            return false;
          }
        }
      }
      return true;
    }


  }
}
