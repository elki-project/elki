package de.lmu.ifi.dbs.elki.workflow;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
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
 * 
 * @param <O> database object type
 */
public class EvaluationStep<O extends DatabaseObject> implements WorkflowStep {
  /**
   * Evaluators to run
   */
  private List<Evaluator<O>> evaluators = null;
  
  /**
   * The result we last processed.
   */
  private HierarchicalResult result;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EvaluationStep(Parameterization config) {
    super();
    config = config.descend(this);
    // evaluator parameter
    final ObjectListParameter<Evaluator<O>> EVALUATOR_PARAM = new ObjectListParameter<Evaluator<O>>(OptionID.EVALUATOR, Evaluator.class, true);
    if(config.grab(EVALUATOR_PARAM)) {
      evaluators = EVALUATOR_PARAM.instantiateClasses(config);
    }
  }

  public void runEvaluators(HierarchicalResult r, Database<O> db, boolean normalizationUndo, Normalization<O> normalization) {
    // Run evaluation helpers
    if(evaluators != null) {
      new Evaluation(db, r.getHierarchy(), normalizationUndo, normalization, evaluators).update(r);
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
     * Normalization
     */
    private Normalization<O> normalization;

    /**
     * Normalization undo flag.
     */
    private boolean normalizationUndo;

    /**
     * Database
     */
    private Database<O> database;

    /**
     * Evaluators to run.
     */
    private List<Evaluator<O>> evaluators;

    /**
     * Result hierarchy.
     */
    private ResultHierarchy hierarchy;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param hierarchy Result Hierarchy
     * @param normalizationUndo Normalization undo flag
     * @param normalization Normalization
     * @param evaluators Evaluators
     */
    public Evaluation(Database<O> database, ResultHierarchy hierarchy, boolean normalizationUndo, Normalization<O> normalization, List<Evaluator<O>> evaluators) {
      this.database = database;
      this.hierarchy = hierarchy;
      this.normalizationUndo = normalizationUndo;
      this.normalization = normalization;
      this.evaluators = evaluators;
      
      hierarchy.addResultListener(this);
    }

    /**
     * Update on a particular result.
     * 
     * @param r Result
     */
    public void update(Result r) {
      for(Evaluator<O> evaluator : evaluators) {
        if(normalizationUndo) {
          evaluator.setNormalization(normalization);
        }
        evaluator.processResult(database, r, hierarchy);
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
}