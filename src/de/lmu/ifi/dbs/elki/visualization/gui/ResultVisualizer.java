package de.lmu.ifi.dbs.elki.visualization.gui;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizersForResult;

/**
 * Handler to process and visualize a Result.
 * 
 * @author Erich Schubert
 */
public class ResultVisualizer extends AbstractParameterizable implements ResultHandler<DatabaseObject, Result> {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(ResultVisualizer.class);

  /**
   * Visualization manager.
   */
  VisualizersForResult manager = new VisualizersForResult();
  
  /**
   * Constructor
   */
  public ResultVisualizer() {
    super();
    addParameterizable(manager);
  }

  @Override
  public void processResult(Database<DatabaseObject> db, Result result) throws IllegalStateException {
    MultiResult mr = ResultUtil.ensureMultiResult(result);
    manager.processResult(db, mr);
    Collection<Visualizer> vs = manager.getVisualizers();
    if (vs.size() == 0) {
      logger.error("No visualizers found for result!");
      return;
    }
    
    ResultWindow window = new ResultWindow(db, mr);
    window.addVisualizations(vs);
    window.setVisible(true);
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<DatabaseObject> normalization) {
    // TODO: handle normalizations
    logger.warning("Normalizations not yet supported in " + ResultVisualizer.class.getName());
  }
  
  @Override
  public String shortDescription() {
    return "Visualize a Result from ELKI.";
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    
    remainingParameters = manager.setParameters(remainingParameters);
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
