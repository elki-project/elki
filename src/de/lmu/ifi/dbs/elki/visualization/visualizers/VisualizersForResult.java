package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.visualizers.adapter.AlgorithmAdapter;

/**
 * Utility class to determine the visualizers for a result class.
 * 
 * @author Erich Schubert
 */
public class VisualizersForResult extends AbstractParameterizable {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(VisualizersForResult.class);

  /**
   * (Result-to-visualization) Adapters
   */
  private Collection<AlgorithmAdapter<?>> adapters;
  
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
    for (AlgorithmAdapter<?> a: adapters){
      if (a.canVisualize(result)){
        // Note: this can throw an exception when setParameters() was not called!
        Collection<Visualizer> avis = a.getUsableVisualizers(context);
        //logger.debug("Got "+avis.size()+" visualizers from "+a.getClass().getName());
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
  private static Collection<AlgorithmAdapter<?>> collectAlgorithmAdapters() {
    ArrayList<AlgorithmAdapter<?>> algorithmAdapters = new ArrayList<AlgorithmAdapter<?>>();
    for (Class<?> c : InspectionUtil.findAllImplementations(AlgorithmAdapter.class, false)){
      try {
        AlgorithmAdapter<?> a = (AlgorithmAdapter<?>) c.newInstance();
        algorithmAdapters.add(a);
      } catch (Exception e) {
        logger.exception("Error instantiating AlgorithmAdapter "+c.getName(),e);
      }
    }
    return algorithmAdapters;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    
    for (AlgorithmAdapter<?> a : adapters){
      // parameterize if possible.
      if (a instanceof Parameterizable) {
        ((Parameterizable)a).setParameters(remainingParameters);
      }
      for (Visualizer v : a.getProvidedVisualizers()){
        List<String> leftoverParameters = v.setParameters(remainingParameters);
        remainingParameters.removeAll(leftoverParameters);
        // TODO: collect the usable parameters somehow!
        //addParameterizable(v);
      }
    }
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
