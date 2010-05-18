package de.lmu.ifi.dbs.elki.workflow;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "evaluation" step, where data is analyzed.
 * 
 * @author Erich Schubert
 *
 * @param <O> database object type
 */
public class EvaluationStep<O extends DatabaseObject> implements Parameterizable {
  /**
   * Evaluators to run
   */
  private List<Evaluator<O>> evaluators = null;
  
  /**
   * The algorithm output
   */
  private MultiResult result = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EvaluationStep(Parameterization config) {
    super();
    // evaluator parameter
    final ObjectListParameter<Evaluator<O>> EVALUATOR_PARAM = new ObjectListParameter<Evaluator<O>>(OptionID.EVALUATOR, Evaluator.class, true);
    if(config.grab(EVALUATOR_PARAM)) {
      evaluators = EVALUATOR_PARAM.instantiateClasses(config);
    }
  }
  
  public MultiResult runEvaluators(MultiResult r, Database<O> db, boolean normalizationUndo, Normalization<O> normalization) {
    result = r;
    // Run evaluation helpers
    if(evaluators != null) {
      for(Evaluator<O> evaluator : evaluators) {
        if(normalizationUndo) {
          evaluator.setNormalization(normalization);
        }
        result = evaluator.processResult(db, result);
      }
    }
    return result;
  }

  /**
   * Get the algorithm result.
   * 
   * @return Algorithm result.
   */
  public MultiResult getResult() {
    return result;
  }
}