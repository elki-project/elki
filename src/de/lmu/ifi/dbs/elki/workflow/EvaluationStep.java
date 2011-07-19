package de.lmu.ifi.dbs.elki.workflow;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "evaluation" step, where data is analyzed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Evaluator
 * @apiviz.has Result
 * @apiviz.uses Result
 */
public class EvaluationStep implements WorkflowStep {
  /**
   * Evaluators to run
   */
  private List<Evaluator> evaluators = null;

  /**
   * The result we last processed.
   */
  private HierarchicalResult result;

  /**
   * Constructor.
   * 
   * @param evaluators
   */
  public EvaluationStep(List<Evaluator> evaluators) {
    super();
    this.evaluators = evaluators;
  }

  public void runEvaluators(HierarchicalResult r, Database db) {
    // Run evaluation helpers
    if(evaluators != null) {
      new Evaluation(db, evaluators).update(r);
    }
    this.result = r;
  }

  /**
   * Class to handle running the evaluators on a database instance.
   * 
   * @author Erich Schubert
   */
  public class Evaluation implements ResultListener {
    /**
     * Database
     */
    private Database database;

    /**
     * Evaluators to run.
     */
    private List<Evaluator> evaluators;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param evaluators Evaluators
     */
    public Evaluation(Database database, List<Evaluator> evaluators) {
      this.database = database;
      this.evaluators = evaluators;

      database.getHierarchy().addResultListener(this);
    }

    /**
     * Update on a particular result.
     * 
     * @param r Result
     */
    public void update(Result r) {
      for(Evaluator evaluator : evaluators) {
        /*
         * if(normalizationUndo) { evaluator.setNormalization(normalization); }
         */
        evaluator.processNewResult(database, r);
      }
    }

    @Override
    public void resultAdded(Result child, @SuppressWarnings("unused") Result parent) {
      // Re-run evaluators on result
      update(child);
    }

    @SuppressWarnings("unused")
    @Override
    public void resultChanged(Result current) {
      // Ignore for now. TODO: re-evaluate?
    }

    @SuppressWarnings("unused")
    @Override
    public void resultRemoved(Result child, Result parent) {
      // TODO: Implement
    }
  }

  public Result getResult() {
    return result;
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
     * Evaluators to run
     */
    private List<Evaluator> evaluators = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // evaluator parameter
      final ObjectListParameter<Evaluator> ealuatorP = new ObjectListParameter<Evaluator>(OptionID.EVALUATOR, Evaluator.class, true);
      if(config.grab(ealuatorP)) {
        evaluators = ealuatorP.instantiateClasses(config);
      }
    }

    @Override
    protected EvaluationStep makeInstance() {
      return new EvaluationStep(evaluators);
    }
  }
}