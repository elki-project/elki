package de.lmu.ifi.dbs.elki.workflow;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.AutomaticEvaluation;
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
   * 
   * @apiviz.exclude
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

  public HierarchicalResult getResult() {
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
      List<Class<? extends Evaluator>> def = new ArrayList<Class<? extends Evaluator>>(1);
      def.add(AutomaticEvaluation.class);
      // evaluator parameter
      final ObjectListParameter<Evaluator> evaluatorP = new ObjectListParameter<Evaluator>(OptionID.EVALUATOR, Evaluator.class);
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
}