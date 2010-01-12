package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.visualizers.adapter.AlgorithmAdapter;

/**
 * Utility class to determine the visualizers for a result class.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class VisualizersForResult extends AbstractParameterizable {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(VisualizersForResult.class);

  /**
   * (Result-to-visualization) Adapters
   */
  private Collection<AlgorithmAdapter> adapters;

  /**
   * Visualizer instances.
   */
  private Collection<Visualizer> visualizers;

  /**
   * Constructor. No parameters: AbstractParameterizable
   */
  public VisualizersForResult() {
    super();
    this.adapters = collectAlgorithmAdapters();
    this.visualizers = new ArrayList<Visualizer>();
  }

  /**
   * Process a particular result.
   * 
   * @param db Database context
   * @param result Result
   */
  public void processResult(Database<? extends DatabaseObject> db, MultiResult result) {
    VisualizerContext context = new VisualizerContext(db, result);

    // Collect all visualizers.
    for(AlgorithmAdapter a : adapters) {
      if(a.canVisualize(context)) {
        // Note: this can throw an exception when setParameters() was not
        // called!
        Collection<Visualizer> avis = a.getUsableVisualizers(context);
        // logger.debug("Got "+avis.size()+" visualizers from "+a.getClass().getName());
        this.visualizers.addAll(avis);
      }
    }
  }

  /**
   * Get the visualizers found.
   * 
   * @return Visualizers found for result
   */
  public Collection<Visualizer> getVisualizers() {
    // TODO: copy? it's cheap because it's small.
    return visualizers;
  }

  /**
   * Collect and instantiate all adapters.
   * 
   * @return List of all adapters found.
   */
  private static Collection<AlgorithmAdapter> collectAlgorithmAdapters() {
    ArrayList<AlgorithmAdapter> algorithmAdapters = new ArrayList<AlgorithmAdapter>();
    for(Class<?> c : InspectionUtil.findAllImplementations(AlgorithmAdapter.class, false)) {
      try {
        AlgorithmAdapter a = (AlgorithmAdapter) c.newInstance();
        algorithmAdapters.add(a);
      }
      catch(Exception e) {
        logger.exception("Error instantiating AlgorithmAdapter " + c.getName(), e);
      }
    }
    return algorithmAdapters;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    List<String> usedParameters = new ArrayList<String>();
    for(AlgorithmAdapter a : adapters) {
      // parameterize if possible.
      if(a instanceof Parameterizable) {
        ((Parameterizable) a).setParameters(remainingParameters);
      }
      for(Visualizer v : a.getProvidedVisualizers()) {
        v.setParameters(remainingParameters);
        usedParameters.addAll(v.getParameters());
        // TODO: collect the usable parameters somehow!
        // addParameterizable(v);
      }
    }
    // hack, this should be done with parameter understanding...
    remainingParameters.removeAll(usedParameters);

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Try to automatically generate a title for this.
   * 
   * @param db Database
   * @param result Result object
   * @return generated title
   */
  public String getTitle(Database<? extends DatabaseObject> db, MultiResult result) {
    List<Pair<Parameterizable, Option<?>>> settings = ResultUtil.getGlobalAssociation(result, AssociationID.META_SETTINGS);
    String algorithm = null;
    String distance = null;
    String dataset = null;

    for(Pair<Parameterizable, Option<?>> setting : settings) {
      try {
        if(setting.second.equals(OptionID.ALGORITHM)) {
          algorithm = setting.second.getValue().toString();
        }
        if(setting.second.equals(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID)) {
          distance = setting.second.getValue().toString();
        }
        if(setting.second.equals(FileBasedDatabaseConnection.INPUT_ID)) {
          dataset = setting.second.getValue().toString();
        }
      }
      catch(UnusedParameterException e) {
        // Do nothing, the parameter wasn't set.
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
