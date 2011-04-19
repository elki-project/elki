package de.lmu.ifi.dbs.elki.workflow;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.visualization.gui.ResultVisualizer;

/**
 * The "output" step, where data is analyzed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Result
 * @apiviz.has ResultHandler
 */
public class OutputStep implements WorkflowStep {
  /**
   * Output handler.
   */
  private List<ResultHandler<Result>> resulthandlers = null;

  /**
   * Constructor.
   * 
   * @param resulthandlers Result handlers to use
   */
  public OutputStep(List<ResultHandler<Result>> resulthandlers) {
    super();
    this.resulthandlers = resulthandlers;
  }

  /**
   * Run the result handlers.
   * 
   * @param result Result to run on
   * @param db Database
   */
  public void runResultHandlers(Result result, Database db) {
    // Run result handlers
    for(ResultHandler<Result> resulthandler : resulthandlers) {
      resulthandler.processResult(db, result);
    }
  }

  /**
   * Get a default handler list containing a {@link ResultWriter}.
   * 
   * @return Result handler list
   */
  public static ArrayList<Class<? extends ResultHandler<Result>>> defaultWriter() {
    ArrayList<Class<? extends ResultHandler<Result>>> defaultHandlers = new ArrayList<Class<? extends ResultHandler<Result>>>(1);
    final Class<ResultHandler<Result>> rwcls = ClassGenericsUtil.uglyCrossCast(ResultWriter.class, ResultHandler.class);
    defaultHandlers.add(rwcls);
    return defaultHandlers;
  }

  /**
   * Get a default handler list containing a {@link ResultVisualizer}.
   * 
   * @return Result handler list
   */
  public static ArrayList<Class<? extends ResultHandler<Result>>> defaultVisualizer() {
    ArrayList<Class<? extends ResultHandler<Result>>> defaultHandlers = new ArrayList<Class<? extends ResultHandler<Result>>>(1);
    final Class<ResultHandler<Result>> rwcls = ClassGenericsUtil.uglyCrossCast(ResultVisualizer.class, ResultHandler.class);
    defaultHandlers.add(rwcls);
    return defaultHandlers;
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
     * Output handlers.
     */
    private List<ResultHandler<Result>> resulthandlers = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // result handlers
      final ObjectListParameter<ResultHandler<Result>> resultHandlerParam = new ObjectListParameter<ResultHandler<Result>>(OptionID.RESULT_HANDLER, ResultHandler.class);
      if(config.grab(resultHandlerParam)) {
        resulthandlers = resultHandlerParam.instantiateClasses(config);
      }
    }

    @Override
    protected OutputStep makeInstance() {
      return new OutputStep(resulthandlers);
    }
  }
}