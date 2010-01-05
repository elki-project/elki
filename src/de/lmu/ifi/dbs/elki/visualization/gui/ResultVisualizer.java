package de.lmu.ifi.dbs.elki.visualization.gui;

import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizersForResult;

/**
 * Handler to process and visualize a Result.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class ResultVisualizer extends AbstractParameterizable implements ResultHandler<DatabaseObject, Result> {
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(ResultVisualizer.class);

  /**
   * OptionID for {@link #WINDOW_TITLE_PARAM}
   */
  public static final OptionID WINDOW_TITLE_ID = OptionID.getOrCreateOptionID("vis.window.title", "Title to use for visualization window.");

  /**
   * Parameter to specify the window title
   * <p>
   * Key: {@code -vis.window.title}
   * </p>
   * <p>
   * Default value: "ELKI Result Visualization"
   * </p>
   */
  protected final PatternParameter WINDOW_TITLE_PARAM = new PatternParameter(WINDOW_TITLE_ID, "ELKI Result Visualization");
  
  /**
   * Stores the set title.
   */
  private String title;

  /**
   * Visualization manager.
   */
  VisualizersForResult manager = new VisualizersForResult();
  
  /**
   * Constructor
   */
  public ResultVisualizer() {
    super();
    addOption(WINDOW_TITLE_PARAM);
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
    
    ResultWindow window = new ResultWindow(title, db, mr);
    window.addVisualizations(vs);
    window.setVisible(true);
    window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);    
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
    
    title = WINDOW_TITLE_PARAM.getValue();
    
    remainingParameters = manager.setParameters(remainingParameters);
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
