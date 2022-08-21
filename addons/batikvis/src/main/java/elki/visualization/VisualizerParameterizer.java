/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import elki.Algorithm;
import elki.database.Database;
import elki.database.ids.DBIDUtil;
import elki.database.relation.Relation;
import elki.datasource.FileBasedDatabaseConnection;
import elki.logging.Logging;
import elki.result.ResultUtil;
import elki.result.SamplingResult;
import elki.result.SettingsResult;
import elki.result.SettingsResult.SettingInformation;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.ELKIServiceRegistry;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.MergedParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.PatternParameter;
import elki.utilities.optionhandling.parameters.StringParameter;
import elki.utilities.random.RandomFactory;
import elki.visualization.style.PropertiesBasedStyleLibrary;
import elki.visualization.style.StyleLibrary;
import elki.workflow.AlgorithmStep;

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
   * Pattern to show additional visualizers
   */
  private Pattern showVisualizers = null;

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
   * @param showVisualizers Visualizers to force visible (may be null)
   */
  public VisualizerParameterizer(int samplesize, StyleLibrary stylelib, Collection<VisualizationProcessor> factories, Pattern showVisualizers) {
    super();
    this.samplesize = samplesize;
    this.stylelib = stylelib;
    this.factories = factories;
    this.showVisualizers = showVisualizers;
  }

  /**
   * Make a new visualization context
   *
   * @param start Starting result
   * @return New context
   */
  public VisualizerContext newContext(Object start) {
    Collection<Relation<?>> rels = ResultUtil.filterResults(start, Relation.class);
    for(Relation<?> rel : rels) {
      if(samplesize == 0) {
        continue;
      }
      if(!ResultUtil.filterResults(rel, SamplingResult.class).isEmpty()) {
        continue;
      }
      if(rel.size() > samplesize) {
        SamplingResult sample = new SamplingResult(rel);
        sample.setSample(DBIDUtil.randomSample(sample.getSample(), samplesize, rnd));
        ResultUtil.addChildResult(rel, sample);
      }
    }
    VisualizerContext ctx = new VisualizerContext(start, stylelib, factories);
    // Make additional visualizers visible
    if(showVisualizers != null) {
      for(It<VisualizationTask> iter2 = ctx.getVisHierarchy().iterAll().filter(VisualizationTask.class); iter2.valid(); iter2.advance()) {
        if(!iter2.get().isVisible() && showVisualizers.matcher(iter2.get().getMenuName()).find()) {
          iter2.get().visibility(true);
        }
      }
    }
    return ctx;
  }

  /**
   * Try to automatically generate a title for this.
   *
   * @param db Database
   * @param result Result object
   * @return generated title
   */
  public static String getTitle(Database db, Object result) {
    List<SettingInformation> settings = new ArrayList<>();
    for(SettingsResult sr : SettingsResult.getSettingsResults(result)) {
      settings.addAll(sr.getSettings());
    }
    String algorithm = null;
    String distance = null;
    String dataset = null;

    for(SettingInformation setting : settings) {
      if(setting.name.equals(AlgorithmStep.Par.ALGORITHM_ID.getName())) {
        algorithm = setting.value;
      }
      if(setting.name.equals(Algorithm.Utils.DISTANCE_FUNCTION_ID.getName())) {
        distance = setting.value;
      }
      if(setting.name.equals(FileBasedDatabaseConnection.Par.INPUT_ID.getName())) {
        dataset = setting.value;
      }
    }
    StringBuilder buf = new StringBuilder(200);
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
    return buf.length() > 0 ? buf.toString() : null;
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
  public static class Par implements Parameterizer {
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
     * {@code elki.visualization.style}.
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
     * Parameter to show visualizers hidden by default
     */
    public static final OptionID SHOWVIS_ID = new OptionID("vis.show", "Visualizers to force visible, that would be hidden by default.");

    /**
     * Style library
     */
    protected StyleLibrary stylelib = null;

    /**
     * Pattern to enable visualizers
     */
    protected Pattern enableVisualizers = null;

    /**
     * Pattern to show additional visualizers
     */
    protected Pattern showVisualizers = null;

    /**
     * Visualizer factories
     */
    protected Collection<VisualizationProcessor> factories = null;

    /**
     * Sampling size
     */
    protected int samplesize = -1;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(SAMPLING_ID, DEFAULT_SAMPLE_SIZE) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_MINUSONE_INT) //
          .grab(config, x -> samplesize = x);
      StringParameter stylelibP = new StringParameter(STYLELIB_ID, PropertiesBasedStyleLibrary.DEFAULT_SCHEME_FILENAME);
      stylelibP.grab(config, filename -> {
        try {
          stylelib = new PropertiesBasedStyleLibrary(filename, filename);
        }
        catch(AbortException e) {
          config.reportError(new WrongParameterValueException(stylelibP, filename, e.getMessage(), e));
        }
      });
      new PatternParameter(ENABLEVIS_ID) //
          .setOptional(true) //
          .grab(config, x -> enableVisualizers = x);
      new PatternParameter(SHOWVIS_ID) //
          .setOptional(true) //
          .grab(config, x -> showVisualizers = x);
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
    public VisualizerParameterizer make() {
      return new VisualizerParameterizer(samplesize, stylelib, factories, showVisualizers);
    }
  }
}
