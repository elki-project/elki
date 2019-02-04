/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.workflow;

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.AutomaticEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "evaluation" step, where data is analyzed.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - Evaluator
 * @assoc - - - Result
 */
public class EvaluationStep implements WorkflowStep {
  /**
   * Evaluators to run.
   */
  private List<? extends Evaluator> evaluators = null;

  /**
   * Result.
   */
  private Result stepresult;

  /**
   * Constructor.
   *
   * @param evaluators
   */
  public EvaluationStep(List<? extends Evaluator> evaluators) {
    super();
    this.evaluators = evaluators;
  }

  public void runEvaluators(ResultHierarchy hier, Database db) {
    // Currently only serves indication purposes.
    stepresult = new BasicResult("Evaluation Step", "evaluation-step");
    // Run evaluation helpers
    if(evaluators != null) {
      new Evaluation(hier, evaluators).update(db);
    }
  }

  /**
   * Class to handle running the evaluators on a database instance.
   *
   * @author Erich Schubert
   */
  private static class Evaluation implements ResultListener {
    /**
     * Evaluators to run.
     */
    private List<? extends Evaluator> evaluators;

    /**
     * Result hierarchy
     */
    private ResultHierarchy hier;

    /**
     * Constructor.
     *
     * @param hier Result hierarchy
     * @param evaluators Evaluators
     */
    public Evaluation(ResultHierarchy hier, List<? extends Evaluator> evaluators) {
      this.hier = hier;
      this.evaluators = evaluators;

      hier.addResultListener(this);
    }

    /**
     * Update on a particular result.
     *
     * @param r Result
     */
    public void update(Result r) {
      for(Evaluator evaluator : evaluators) {
        Thread.currentThread().setName(evaluator.toString());
        evaluator.processNewResult(hier, r);
      }
    }

    @Override
    public void resultAdded(Result child, Result parent) {
      // Re-run evaluators on result
      update(child);
    }

    @Override
    public void resultChanged(Result current) {
      // Ignore for now. TODO: re-evaluate?
    }

    @Override
    public void resultRemoved(Result child, Result parent) {
      // TODO: Implement
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Evaluators to run
     */
    private List<Evaluator> evaluators = null;

    /**
     * Parameter ID to specify the evaluators to run.
     */
    public static final OptionID EVALUATOR_ID = new OptionID("evaluator", "Class to evaluate the results with.");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      List<Class<? extends Evaluator>> def = Arrays.asList(AutomaticEvaluation.class);
      // evaluator parameter
      ObjectListParameter<Evaluator> evaluatorP = new ObjectListParameter<>(EVALUATOR_ID, Evaluator.class);
      evaluatorP.setDefaultValue(def);
      if(config.grab(evaluatorP)) {
        evaluators = evaluatorP.instantiateClasses(config);
      }
    }

    @Override
    protected EvaluationStep makeInstance() {
      return new EvaluationStep(evaluators);
    }
  }

  /**
   * Return the result.
   *
   * @return Result
   */
  public Result getResult() {
    return stepresult;
  }
}
