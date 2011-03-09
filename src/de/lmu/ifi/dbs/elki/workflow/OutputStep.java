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
   * Parameterization method.
   * 
   * @param <O> Object type
   * @param config Parameterization
   * 
   * @return Output step
   */
  public static <O extends DatabaseObject> OutputStep<O> parameterize(Parameterization config) {
    return parameterize(config, null);
  }

  /**
   * Parameterization method with default handlers.
   * 
   * @param <O> Object type
   * @param config Parameterization
   * @param defaultHandlers Default handlers
   * 
   * @return Output step
   */
  public static <O extends DatabaseObject> OutputStep<O> parameterize(Parameterization config, ArrayList<Class<? extends ResultHandler<O, Result>>> defaultHandlers) {
    // result handlers
    final ObjectListParameter<ResultHandler<O, Result>> RESULT_HANDLER_PARAM = new ObjectListParameter<ResultHandler<O, Result>>(OptionID.RESULT_HANDLER, ResultHandler.class);
    if(defaultHandlers != null) {
      RESULT_HANDLER_PARAM.setDefaultValue(defaultHandlers);
    }
    List<ResultHandler<O, Result>> resulthandlers = null;
    if(config.grab(RESULT_HANDLER_PARAM)) {
      resulthandlers = RESULT_HANDLER_PARAM.instantiateClasses(config);
    }
    if(config.hasErrors()) {
      return null;
    }
    return new OutputStep<O>(resulthandlers);
  }
  
  /**
   * Get a default handler list containing a {@link #ResultWriter}.
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
   * Get a default handler list containing a {@link #ResultVisualizer}.
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
}