package de.lmu.ifi.dbs.elki.workflow;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
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
 * 
 * @param <O> database object type
 */
public class OutputStep<O extends DatabaseObject> implements WorkflowStep {
  /**
   * Output handler.
   */
  private List<ResultHandler<O, Result>> resulthandlers = null;

  /**
   * Constructor.
   * 
   * @param resulthandlers Result handlers to use
   */
  public OutputStep(List<ResultHandler<O, Result>> resulthandlers) {
    super();
    this.resulthandlers = resulthandlers;
  }

  /**
   * Run the result handlers.
   * 
   * @param result Result to run on
   * @param db Database
   * @param normalizationUndo Flag to undo normalization
   * @param normalization Normalization
   */
  public void runResultHandlers(Result result, Database<O> db, boolean normalizationUndo, Normalization<O> normalization) {
    // Run result handlers
    for(ResultHandler<O, Result> resulthandler : resulthandlers) {
      if(normalizationUndo) {
        resulthandler.setNormalization(normalization);
      }
      resulthandler.processResult(db, result);
    }
  }

  /**
   * Get a default handler list containing a {@link ResultWriter}.
   * 
   * @param <O> Object type
   * @return Result handler list
   */
  public static <O extends DatabaseObject> ArrayList<Class<? extends ResultHandler<O, Result>>> defaultWriter() {
    ArrayList<Class<? extends ResultHandler<O, Result>>> defaultHandlers = new ArrayList<Class<? extends ResultHandler<O, Result>>>(1);
    final Class<ResultHandler<O, Result>> rwcls = ClassGenericsUtil.uglyCrossCast(ResultWriter.class, ResultHandler.class);
    defaultHandlers.add(rwcls);
    return defaultHandlers;
  }

  /**
   * Get a default handler list containing a {@link ResultVisualizer}.
   * 
   * @param <O> Object type
   * @return Result handler list
   */
  public static <O extends DatabaseObject> ArrayList<Class<? extends ResultHandler<O, Result>>> defaultVisualizer() {
    ArrayList<Class<? extends ResultHandler<O, Result>>> defaultHandlers = new ArrayList<Class<? extends ResultHandler<O, Result>>>(1);
    final Class<ResultHandler<O, Result>> rwcls = ClassGenericsUtil.uglyCrossCast(ResultVisualizer.class, ResultHandler.class);
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
  public static class Parameterizer<O extends DatabaseObject> extends AbstractParameterizer {
    /**
     * Output handlers.
     */
    private List<ResultHandler<O, Result>> resulthandlers = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // result handlers
      final ObjectListParameter<ResultHandler<O, Result>> resultHandlerParam = new ObjectListParameter<ResultHandler<O, Result>>(OptionID.RESULT_HANDLER, ResultHandler.class);
      if(config.grab(resultHandlerParam)) {
        resulthandlers = resultHandlerParam.instantiateClasses(config);
      }
    }

    @Override
    protected OutputStep<O> makeInstance() {
      return new OutputStep<O>(resulthandlers);
    }
  }
}