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
package de.lmu.ifi.dbs.elki;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;
import de.lmu.ifi.dbs.elki.workflow.InputStep;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * KDDTask encapsulates the common workflow of an <i>unsupervised</i> knowledge
 * discovery task.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @composed - - - InputStep
 * @composed - - - AlgorithmStep
 * @composed - - - EvaluationStep
 * @composed - - - OutputStep
 */
public class KDDTask {
  /**
   * The settings used, for settings reporting.
   */
  private Collection<TrackedParameter> settings;

  /**
   * The data input step
   */
  private InputStep inputStep;

  /**
   * The algorithm (data mining) step.
   */
  private AlgorithmStep algorithmStep;

  /**
   * The evaluation step.
   */
  private EvaluationStep evaluationStep;

  /**
   * The output/visualization step
   */
  private OutputStep outputStep;

  /**
   * The result hierarchy.
   */
  private ResultHierarchy hier;

  /**
   * Constructor.
   *
   * @param inputStep
   * @param algorithmStep
   * @param evaluationStep
   * @param outputStep
   * @param settings
   */
  public KDDTask(InputStep inputStep, AlgorithmStep algorithmStep, EvaluationStep evaluationStep, OutputStep outputStep, Collection<TrackedParameter> settings) {
    super();
    this.inputStep = inputStep;
    this.algorithmStep = algorithmStep;
    this.evaluationStep = evaluationStep;
    this.outputStep = outputStep;
    this.settings = settings;
  }

  /**
   * Method to run the specified algorithm using the specified database
   * connection.
   */
  public void run() {
    // Input step
    Database db = inputStep.getDatabase();
    hier = db.getHierarchy();

    // Algorithms - Data Mining Step
    algorithmStep.runAlgorithms(db);

    // TODO: this could be nicer
    hier.add(db, new SettingsResult(settings));

    // Evaluation
    evaluationStep.runEvaluators(hier, db);

    // Output / Visualization
    outputStep.runResultHandlers(hier, db);
  }

  /**
   * Get the algorithms result hierarchy.
   *
   * @return the result hierarchy
   */
  public ResultHierarchy getResultHierarchy() {
    return hier;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    InputStep inputStep = null;

    AlgorithmStep algorithmStep = null;

    EvaluationStep evaluationStep = null;

    Collection<TrackedParameter> settings = null;

    OutputStep outputStep = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Track the key parameters for reporting the settings.
      TrackParameters track = new TrackParameters(config);

      inputStep = track.tryInstantiate(InputStep.class);
      algorithmStep = track.tryInstantiate(AlgorithmStep.class);
      evaluationStep = track.tryInstantiate(EvaluationStep.class);

      // We don't include output parameters
      settings = track.getAllParameters();
      // configure output with the original parameterization
      outputStep = config.tryInstantiate(OutputStep.class);
    }

    @Override
    protected KDDTask makeInstance() {
      return new KDDTask(inputStep, algorithmStep, evaluationStep, outputStep, settings);
    }
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    KDDCLIApplication.runCLIApplication(KDDCLIApplication.class, args);
  }
}