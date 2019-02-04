/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Utility class to determine the visualizers for a result class.
 *
 * You <em>really</em> should use the parameterization API to configure this
 * class. Manually populating the factory collection is cumbersome, and the
 * parameterization API takes care of this.
 *
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @since 0.3
 *
 * @opt nodefillcolor LemonChiffon
 * @navhas - create - VisualizerContext
 * @navhas - configure * VisualizationProcessor
 */
public class VisualizerParameterizer {
  /**
   * Get a logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(VisualizerParameterizer.class);

  /**
   * Default sample size to visualize.
   */
  public static final int DEFAULT_SAMPLE_SIZE = 10000;

  /**
   * Style library to use.
   */
  private StyleLibrary stylelib;

  /**
   * Projections and visualization factories.
   */
  private Collection<VisualizationProcessor> factories;

  /**
   * Sample size
   */
  private int samplesize = -1;

  /**
   * Random seed for sampling.
   *
   * FIXME: make parameterizable.
   */
  private RandomFactory rnd = RandomFactory.DEFAULT;

  /**
   * Constructor.
   *
   * @param samplesize
   * @param stylelib Style library
   * @param factories Factories to use
   */
  public VisualizerParameterizer(int samplesize, StyleLibrary stylelib, Collection<VisualizationProcessor> factories) {
    super();
    this.samplesize = samplesize;
    this.stylelib = stylelib;
    this.factories = factories;
  }

  /**
   * Make a new visualization context
   *
   * @param hier Result hierarchy
   * @param start Starting result
   * @return New context
   */
  public VisualizerContext newContext(ResultHierarchy hier, Result start) {
    Collection<Relation<?>> rels = ResultUtil.filterResults(hier, Relation.class);
    for(Relation<?> rel : rels) {
      if(samplesize == 0) {
        continue;
      }
      if(!ResultUtil.filterResults(hier, rel, SamplingResult.class).isEmpty()) {
        continue;
      }
      if(rel.size() > samplesize) {
        SamplingResult sample = new SamplingResult(rel);
        sample.setSample(DBIDUtil.randomSample(sample.getSample(), samplesize, rnd));
        ResultUtil.addChildResult(rel, sample);
      }
    }
    return new VisualizerContext(hier, start, stylelib, factories);
  }

  /**
   * Try to automatically generate a title for this.
   *
   * @param db Database
   * @param result Result object
   * @return generated title
   */
  public static String getTitle(Database db, Result result) {
    List<TrackedParameter> settings = new ArrayList<>();
    for(SettingsResult sr : SettingsResult.getSettingsResults(result)) {
      settings.addAll(sr.getSettings());
    }
    String algorithm = null;
    String distance = null;
    String dataset = null;

    for(TrackedParameter setting : settings) {
      Parameter<?> param = setting.getParameter();
      OptionID option = param.getOptionID();
      String value = param.isDefined() ? param.getValueAsString() : null;
      if(option.equals(AlgorithmStep.Parameterizer.ALGORITHM_ID)) {
        algorithm = value;
      }
      if(option.equals(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID)) {
        distance = value;
      }
      if(option.equals(FileBasedDatabaseConnection.Parameterizer.INPUT_ID)) {
        dataset = value;
      }
    }
    StringBuilder buf = new StringBuilder();
    if(algorithm != null) {
      buf.append(shortenClassname(algorithm.split(",")[0], '.'));
    }
    if(distance != null) {
      if(buf.length() > 0) {
        buf.append(" using ");
      }
      buf.append(shortenClassname(distance, '.'));
    }
    if(dataset != null) {
      if(buf.length() > 0) {
        buf.append(" on ");
      }
      buf.append(shortenClassname(dataset, File.separatorChar));
    }
    if(buf.length() > 0) {
      return buf.toString();
    }
    return null;
  }

  /**
   * Shorten the class name.
   *
   * @param nam Class name
   * @param c Splitting character
   * @return Shortened name
   */
  protected static String shortenClassname(String nam, char c) {
    final int lastdot = nam.lastIndexOf(c);
    if(lastdot >= 0) {
      nam = nam.substring(lastdot + 1);
    }
    return nam;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to get the style properties file.
     * <p>
     * Included stylesheets:
     * <ul>
     * <li>classic</li>
     * <li>default</li>
     * <li>greyscale</li>
     * <li>neon</li>
     * <li>presentation</li>
     * <li>print</li>
     * </ul>
     * These are {@code *.properties} files in the package
     * {@code de.lmu.ifi.dbs.elki.visualization.style}.
     */
    public static final OptionID STYLELIB_ID = new OptionID("visualizer.stylesheet", "Style properties file to use, included properties: classic, default, greyscale, neon, presentation, print");

    /**
     * Parameter to enable visualizers
     */
    public static final OptionID ENABLEVIS_ID = new OptionID("vis.enable", "Visualizers to enable by default.");

    /**
     * Parameter to set the sampling level
     */
    public static final OptionID SAMPLING_ID = new OptionID("vis.sampling", "Maximum number of objects to visualize by default (for performance reasons).");

    /**
     * Style library
     */
    protected StyleLibrary stylelib = null;

    /**
     * Pattern to enable visualizers
     */
    protected Pattern enableVisualizers = null;

    /**
     * Visualizer factories
     */
    protected Collection<VisualizationProcessor> factories = null;

    /**
     * Sampling size
     */
    protected int samplesize = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter samplingP = new IntParameter(SAMPLING_ID, DEFAULT_SAMPLE_SIZE) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_MINUSONE_INT);
      if(config.grab(samplingP)) {
        samplesize = samplingP.intValue();
      }
      StringParameter stylelibP = new StringParameter(STYLELIB_ID, PropertiesBasedStyleLibrary.DEFAULT_SCHEME_FILENAME);
      if(config.grab(stylelibP)) {
        String filename = stylelibP.getValue();
        try {
          stylelib = new PropertiesBasedStyleLibrary(filename, filename);
        }
        catch(AbortException e) {
          config.reportError(new WrongParameterValueException(stylelibP, filename, e.getMessage(), e));
        }
      }
      PatternParameter enablevisP = new PatternParameter(ENABLEVIS_ID) //
          .setOptional(true);
      if(config.grab(enablevisP) && !"all".equals(enablevisP.getValueAsString())) {
        enableVisualizers = enablevisP.getValue();
      }
      MergedParameterization merged = new MergedParameterization(config);
      factories = collectFactorys(merged, enableVisualizers);
    }

    /**
     * Collect and instantiate all visualizer factories.
     *
     * @param config Parameterization
     * @param filter Filter
     * @return List of all adapters found.
     */
    private static <O> Collection<VisualizationProcessor> collectFactorys(MergedParameterization config, Pattern filter) {
      ArrayList<VisualizationProcessor> factories = new ArrayList<>();
      for(Class<?> c : ELKIServiceRegistry.findAllImplementations(VisualizationProcessor.class)) {
        if(filter != null && !filter.matcher(c.getCanonicalName()).find()) {
          continue;
        }
        try {
          config.rewind();
          VisualizationProcessor a = ClassGenericsUtil.tryInstantiate(VisualizationProcessor.class, c, config);
          factories.add(a);
        }
        catch(Throwable e) {
          if(LOG.isDebugging()) {
            LOG.exception("Error instantiating visualization processor " + c.getName(), e.getCause());
          }
          else {
            LOG.warning("Error instantiating visualization processor " + c.getName() + ": " + e.getMessage());
          }
        }
      }
      return factories;
    }

    @Override
    protected VisualizerParameterizer makeInstance() {
      return new VisualizerParameterizer(samplesize, stylelib, factories);
    }
  }
}
