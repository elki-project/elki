package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Utility class to determine the visualizers for a result class.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has VisualizerContext oneway - - creates
 * @apiviz.uses VisFactory oneway - n «configure»
 * @apiviz.uses StyleLibrary oneway - - «configure»
 * @apiviz.uses Result oneway - - processes
 */
public class VisualizerParameterizer<O extends DatabaseObject> implements Parameterizable {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(VisualizerParameterizer.class);

  /**
   * Option ID for the style properties to use, {@link #STYLELIB_PARAM}
   */
  public final static OptionID STYLELIB_ID = OptionID.getOrCreateOptionID("visualizer.stylesheet", "Style properties file to use");

  /**
   * Parameter to get the style properties file.
   * 
   * <p>
   * Key: -visualizer.stylesheet
   * 
   * Default: default properties file
   * </p>
   */
  private StringParameter STYLELIB_PARAM = new StringParameter(STYLELIB_ID, PropertiesBasedStyleLibrary.DEFAULT_SCHEME_FILENAME);

  /**
   * Default pattern for visualizer disabling.
   */
  public final static String DEFAULT_HIDEVIS = "^experimentalcode\\..*";

  /**
   * Option ID for the visualizers to disable
   */
  public final static OptionID HIDEVIS_ID = OptionID.getOrCreateOptionID("vis.hide", "Visualizers to not show by default. Use 'none' to not hide any by default.");

  /**
   * Parameter to disable visualizers
   * 
   * <p>
   * Key: -vis.hide
   * 
   * Default: default properties file
   * </p>
   */
  private PatternParameter HIDEVIS_PARAM = new PatternParameter(HIDEVIS_ID, DEFAULT_HIDEVIS);

  /**
   * Style library to use.
   */
  private StyleLibrary stylelib;

  /**
   * (Result-to-visualization) Adapters
   */
  private Collection<VisFactory<O>> adapters;

  /**
   * Visualizer disabling pattern
   */
  private Pattern hideVisualizers = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public VisualizerParameterizer(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(STYLELIB_PARAM)) {
      String filename = STYLELIB_PARAM.getValue();
      try {
        stylelib = new PropertiesBasedStyleLibrary(filename, "Command line style");
      }
      catch(AbortException e) {
        config.reportError(new WrongParameterValueException(STYLELIB_PARAM, filename, e));
      }
    }
    if(config.grab(HIDEVIS_PARAM)) {
      if(!"none".equals(HIDEVIS_PARAM.getValueAsString())) {
        hideVisualizers = HIDEVIS_PARAM.getValue();
      }
    }
    MergedParameterization merged = new MergedParameterization(config);
    this.adapters = collectAlgorithmAdapters(merged);
  }

  /**
   * Process a particular result.
   * 
   * @param context Database context
   * @param result Result
   */
  public void processResult(VisualizerContext<O> context, Result result) {
    // Collect all visualizers.
    for(VisFactory<O> a : adapters) {
      try {
        a.addVisualizers(context, result);
      }
      catch(Throwable e) {
        logger.warning("AlgorithmAdapter " + a.getClass().getCanonicalName() + " failed:", e);
      }
    }
  }

  /**
   * Make a new visualization context
   * 
   * @param db Database
   * @param result Base result
   * @return New context
   */
  public VisualizerContext<O> newContext(Database<O> db, HierarchicalResult result) {
    VisualizerContext<O> context = new VisualizerContext<O>(db, result);
    context.setStyleLibrary(stylelib);
    context.put(VisualizerContext.HIDE_PATTERN, hideVisualizers);
    processResult(context, result);
    return context;
  }

  /**
   * Collect and instantiate all adapters.
   * 
   * @param config Parameterization
   * @return List of all adapters found.
   */
  @SuppressWarnings("unchecked")
  private static <O extends DatabaseObject> Collection<VisFactory<O>> collectAlgorithmAdapters(Parameterization config) {
    ArrayList<VisFactory<O>> algorithmAdapters = new ArrayList<VisFactory<O>>();
    for(Class<?> c : InspectionUtil.cachedFindAllImplementations(VisFactory.class)) {
      try {
        VisFactory<O> a = ClassGenericsUtil.tryInstantiate(VisFactory.class, c, config);
        algorithmAdapters.add(a);
      }
      catch(Throwable e) {
        logger.exception("Error instantiating visualization factory " + c.getName(), e);
      }
    }
    return algorithmAdapters;
  }

  /**
   * Try to automatically generate a title for this.
   * 
   * @param db Database
   * @param result Result object
   * @return generated title
   */
  public static String getTitle(Database<? extends DatabaseObject> db, Result result) {
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
}