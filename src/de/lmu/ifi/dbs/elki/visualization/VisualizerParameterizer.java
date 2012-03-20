package de.lmu.ifi.dbs.elki.visualization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.projector.ProjectorFactory;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;

/**
 * Utility class to determine the visualizers for a result class.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.landmark
 * @apiviz.has VisualizerContext oneway - - «create»
 * @apiviz.uses VisFactory oneway - n «configure»
 */
public class VisualizerParameterizer implements Parameterizable {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(VisualizerParameterizer.class);

  /**
   * Parameter to get the style properties file.
   * 
   * <p>
   * Key: -visualizer.stylesheet
   * 
   * Default: default properties file
   * </p>
   */
  public final static OptionID STYLELIB_ID = OptionID.getOrCreateOptionID("visualizer.stylesheet", "Style properties file to use");

  /**
   * Default pattern for visualizer enabling.
   */
  public final static String DEFAULT_ENABLEVIS = "^" + Pattern.quote(VisualizerParameterizer.class.getPackage().getName()) + "\\..*";

  /**
   * Parameter to enable visualizers
   * 
   * <p>
   * Key: -vis.enable
   * 
   * Default: ELKI core
   * </p>
   */
  public final static OptionID ENABLEVIS_ID = OptionID.getOrCreateOptionID("vis.enable", "Visualizers to enable by default.");

  /**
   * Parameter to set the sampling level
   * 
   * <p>
   * Key: -vis.sampling
   * </p>
   */
  public final static OptionID SAMPLING_ID = OptionID.getOrCreateOptionID("vis.sampling", "Maximum number of objects to visualize by default (for performance reasons).");

  /**
   * Style library to use.
   */
  private StyleLibrary stylelib;

  /**
   * (Result-to-visualization) Adapters
   */
  private Collection<VisFactory> factories;

  /**
   * Projectors to use.
   */
  private Collection<ProjectorFactory> projectors;

  /**
   * Sample size
   */
  private int samplesize = -1;

  /**
   * Random seed for sampling.
   * 
   * FIXME: make parameterizable.
   */
  private long seed = new Random().nextLong();

  /**
   * Constructor.
   * 
   * @param samplesize
   * @param stylelib Style library
   * @param projectors Projectors
   * @param factories Factories to use
   * @param hideVisualizers Visualizer hiding pattern
   */
  public VisualizerParameterizer(int samplesize, StyleLibrary stylelib, Collection<ProjectorFactory> projectors, Collection<VisFactory> factories, Pattern hideVisualizers) {
    super();
    this.samplesize = samplesize;
    this.stylelib = stylelib;
    this.projectors = projectors;
    this.factories = factories;
  }

  /**
   * Make a new visualization context
   * 
   * @param result Base result
   * @return New context
   */
  public VisualizerContext newContext(HierarchicalResult result) {
    VisualizerContext context = new VisualizerContext(result, stylelib, projectors, factories);
    if(samplesize > 0) {
      Iterator<Relation<?>> iter = ResultUtil.filteredResults(result, Relation.class);
      while(iter.hasNext()) {
        Relation<?> rel = iter.next();
        if(ResultUtil.filteredResults(rel, SamplingResult.class).hasNext()) {
          continue;
        }
        int size = rel.size();
        if(size > samplesize) {
          SamplingResult sample = new SamplingResult(rel);
          sample.setSample(DBIDUtil.randomSample(sample.getSample(), samplesize, seed));
          ResultUtil.addChildResult(rel, sample);
        }
      }
    }
    return context;
  }

  /**
   * Try to automatically generate a title for this.
   * 
   * @param db Database
   * @param result Result object
   * @return generated title
   */
  public static String getTitle(Database db, Result result) {
    List<Pair<Object, Parameter<?, ?>>> settings = new ArrayList<Pair<Object, Parameter<?, ?>>>();
    for(SettingsResult sr : ResultUtil.getSettingsResults(result)) {
      settings.addAll(sr.getSettings());
    }
    String algorithm = null;
    String distance = null;
    String dataset = null;

    for(Pair<Object, Parameter<?, ?>> setting : settings) {
      if(setting.second.equals(OptionID.ALGORITHM)) {
        algorithm = setting.second.getValue().toString();
      }
      if(setting.second.equals(AbstractDistanceBasedAlgorithm.DISTANCE_FUNCTION_ID)) {
        distance = setting.second.getValue().toString();
      }
      if(setting.second.equals(FileBasedDatabaseConnection.INPUT_ID)) {
        dataset = setting.second.getValue().toString();
      }
    }
    StringBuilder buf = new StringBuilder();
    if(algorithm != null) {
      // shorten the algorithm
      if(algorithm.contains(".")) {
        algorithm = algorithm.substring(algorithm.lastIndexOf(".") + 1);
      }
      buf.append(algorithm);
    }
    if(distance != null) {
      // shorten the distance
      if(distance.contains(".")) {
        distance = distance.substring(distance.lastIndexOf(".") + 1);
      }
      if(buf.length() > 0) {
        buf.append(" using ");
      }
      buf.append(distance);
    }
    if(dataset != null) {
      // shorten the data set filename
      if(dataset.contains(File.separator)) {
        dataset = dataset.substring(dataset.lastIndexOf(File.separator) + 1);
      }
      if(buf.length() > 0) {
        buf.append(" on ");
      }
      buf.append(dataset);
    }
    if(buf.length() > 0) {
      return buf.toString();
    }
    return null;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected StyleLibrary stylelib = null;

    protected Pattern enableVisualizers = null;

    protected Collection<VisFactory> factories = null;

    protected Collection<ProjectorFactory> projectors = null;

    protected int samplesize = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter samplingP = new IntParameter(SAMPLING_ID, new GreaterEqualConstraint(-1), 10000);
      if(config.grab(samplingP)) {
        samplesize = samplingP.getValue();
      }
      StringParameter stylelibP = new StringParameter(STYLELIB_ID, PropertiesBasedStyleLibrary.DEFAULT_SCHEME_FILENAME);
      if(config.grab(stylelibP)) {
        String filename = stylelibP.getValue();
        try {
          stylelib = new PropertiesBasedStyleLibrary(filename, "Command line style");
        }
        catch(AbortException e) {
          config.reportError(new WrongParameterValueException(stylelibP, filename, e));
        }
      }
      PatternParameter enablevisP = new PatternParameter(ENABLEVIS_ID, DEFAULT_ENABLEVIS);
      if(config.grab(enablevisP)) {
        if(!"all".equals(enablevisP.getValueAsString())) {
          enableVisualizers = enablevisP.getValue();
        }
      }
      MergedParameterization merged = new MergedParameterization(config);
      projectors = collectProjectorFactorys(merged, enableVisualizers);
      factories = collectVisFactorys(merged, enableVisualizers);
    }

    /**
     * Collect and instantiate all projector factories.
     * 
     * @param config Parameterization
     * @param filter Filter
     * @return List of all adapters found.
     */
    private static <O> Collection<ProjectorFactory> collectProjectorFactorys(MergedParameterization config, Pattern filter) {
      ArrayList<ProjectorFactory> factories = new ArrayList<ProjectorFactory>();
      for(Class<?> c : InspectionUtil.cachedFindAllImplementations(ProjectorFactory.class)) {
        if(filter != null && !filter.matcher(c.getCanonicalName()).find()) {
          continue;
        }
        try {
          config.rewind();
          ProjectorFactory a = ClassGenericsUtil.tryInstantiate(ProjectorFactory.class, c, config);
          factories.add(a);
        }
        catch(Throwable e) {
          if(logger.isDebugging()) {
            logger.exception("Error instantiating visualization factory " + c.getName(), e.getCause());
          }
          else {
            logger.warning("Error instantiating visualization factory " + c.getName() + ": " + e.getMessage());
          }
        }
      }
      return factories;
    }

    /**
     * Collect and instantiate all visualizer factories.
     * 
     * @param config Parameterization
     * @param filter Filter
     * @return List of all adapters found.
     */
    private static <O> Collection<VisFactory> collectVisFactorys(MergedParameterization config, Pattern filter) {
      ArrayList<VisFactory> factories = new ArrayList<VisFactory>();
      for(Class<?> c : InspectionUtil.cachedFindAllImplementations(VisFactory.class)) {
        if(filter != null && !filter.matcher(c.getCanonicalName()).find()) {
          continue;
        }
        try {
          config.rewind();
          VisFactory a = ClassGenericsUtil.tryInstantiate(VisFactory.class, c, config);
          factories.add(a);
        }
        catch(Throwable e) {
          if(logger.isDebugging()) {
            logger.exception("Error instantiating visualization factory " + c.getName(), e.getCause());
          }
          else {
            logger.warning("Error instantiating visualization factory " + c.getName() + ": " + e.getMessage());
          }
        }
      }
      return factories;
    }

    @Override
    protected VisualizerParameterizer makeInstance() {
      return new VisualizerParameterizer(samplesize, stylelib, projectors, factories, enableVisualizers);
    }
  }
}