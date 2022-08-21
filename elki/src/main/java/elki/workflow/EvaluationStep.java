/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.workflow;

import java.util.Arrays;
import java.util.List;

import elki.database.Database;
import elki.evaluation.AutomaticEvaluation;
import elki.evaluation.Evaluator;
import elki.result.Metadata;
import elki.result.ResultListener;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectListParameter;

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
  private Object stepresult;

  /**
   * Constructor.
   *
   * @param evaluators
   */
  public EvaluationStep(List<? extends Evaluator> evaluators) {
    super();
    this.evaluators = evaluators;
  }

  public void runEvaluators(Database db) {
    // Currently only serves indication purposes.
    stepresult = new Object();
    Metadata.of(stepresult).setLongName("Evaluation Step");
    // Run evaluation helpers
    if(evaluators != null) {
      new Evaluation(evaluators, db);
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
     * Constructor.
     *
     * @param evaluators Evaluators
     */
    public Evaluation(List<? extends Evaluator> evaluators, Database db) {
      this.evaluators = evaluators;
      Metadata.of(db).addResultListener(this);
      update(db);
    }

    /**
     * Update on a particular result.
     *
     * @param r Result
     */
    public void update(Object r) {
      for(Evaluator evaluator : evaluators) {
        Thread.currentThread().setName(evaluator.toString());
        evaluator.processNewResult(r);
      }
      Thread.currentThread().setName("EvaluationStep finished.");
    }

    @Override
    public void resultAdded(Object child, Object parent) {
      // Re-run evaluators on result
      update(child);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Evaluators to run
     */
    private List<? extends Evaluator> evaluators = null;

    /**
     * Parameter ID to specify the evaluators to run.
     */
    public static final OptionID EVALUATOR_ID = new OptionID("evaluator", "Class to evaluate the results with.");

    @Override
    public void configure(Parameterization config) {
      new ObjectListParameter<Evaluator>(EVALUATOR_ID, Evaluator.class) //
          .setDefaultValue(Arrays.asList(AutomaticEvaluation.class)) //
          .grab(config, x -> evaluators = x);
    }

    @Override
    public EvaluationStep make() {
      return new EvaluationStep(evaluators);
    }
  }

  /**
   * Return the result.
   *
   * @return Result
   */
  public Object getResult() {
    return stepresult;
  }
}
