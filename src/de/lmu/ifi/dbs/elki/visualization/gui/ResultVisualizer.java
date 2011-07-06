package de.lmu.ifi.dbs.elki.visualization.gui;

import java.util.List;

import javax.swing.JFrame;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.OverviewPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerParameterizer;

/**
 * Handler to process and visualize a Result.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.composedOf VisualizerParameterizer
 * @apiviz.uses ResultWindow oneway
 */
public class ResultVisualizer implements ResultHandler {
  // TODO: move title/maxdim parameters into a layouter class.
  
  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(ResultVisualizer.class);

  /**
   * Parameter to specify the window title
   * <p>
   * Key: {@code -vis.window.title}
   * </p>
   * <p>
   * Default value: "ELKI Result Visualization"
   * </p>
   */
  public static final OptionID WINDOW_TITLE_ID = OptionID.getOrCreateOptionID("vis.window.title", "Title to use for visualization window.");

  /**
   * Parameter for the maximum number of dimensions,
   * 
   * <p>
   * Code: -vis.maxdim
   * </p>
   */
  public static final OptionID MAXDIM_ID = OptionID.getOrCreateOptionID("vis.maxdim", "Maximum number of dimensions to display.");

  /**
   * Stores the set title.
   */
  String title;

  /**
   * Default title
   */
  protected final static String DEFAULT_TITLE = "ELKI Result Visualization";

  /**
   * Stores the maximum number of dimensions to show.
   */
  int maxdim = OverviewPlot.MAX_DIMENSIONS_DEFAULT;

  /**
   * Visualization manager.
   */
  VisualizerParameterizer manager;

  /**
   * Constructor.
   * 
   * @param title
   * @param maxdim
   * @param manager
   */
  public ResultVisualizer(String title, int maxdim, VisualizerParameterizer manager) {
    super();
    this.title = title;
    this.maxdim = maxdim;
    this.manager = manager;
  }

  @Override
  public void processResult(final Database db, final Result result) {
    HierarchicalResult top = db;
    while (true) {
      List<Result> parents = top.getHierarchy().getParents(top);
      if (parents.size() > 0 && parents.get(0) instanceof HierarchicalResult) {
        top = (HierarchicalResult) parents.get(0);
      } else {
        break;
      }
    }
    ResultUtil.ensureClusteringResult(db, top);
    ResultUtil.ensureSelectionResult(db, db);

    final VisualizerContext context = manager.newContext(db, result);

    if(title == null) {
      title = VisualizerParameterizer.getTitle(db, result);
    }

    if(title == null) {
      title = DEFAULT_TITLE;
    }

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          ResultWindow window = new ResultWindow(title, db, result, maxdim, context);
          window.setVisible(true);
          window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
          window.update();
          window.showOverview();
        }
        catch(Throwable e) {
          logger.exception("Error in starting visualizer window.", e);
        }
      }
    });
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Stores the set title.
     */
    String title;

    /**
     * Stores the maximum number of dimensions to show.
     */
    int maxdim = OverviewPlot.MAX_DIMENSIONS_DEFAULT;

    /**
     * Visualization manager.
     */
    VisualizerParameterizer manager;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      StringParameter titleP = new StringParameter(WINDOW_TITLE_ID, true);
      if(config.grab(titleP)) {
        title = titleP.getValue();
      }

      IntParameter maxdimP = new IntParameter(MAXDIM_ID, new GreaterEqualConstraint(1), OverviewPlot.MAX_DIMENSIONS_DEFAULT);
      if(config.grab(maxdimP)) {
        maxdim = maxdimP.getValue();
      }
      manager = config.tryInstantiate(VisualizerParameterizer.class);
    }

    @Override
    protected ResultVisualizer makeInstance() {
      return new ResultVisualizer(title, maxdim, manager);
    }
  }
}