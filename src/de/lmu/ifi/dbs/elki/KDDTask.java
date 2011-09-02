package de.lmu.ifi.dbs.elki;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;
import de.lmu.ifi.dbs.elki.workflow.InputStep;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.elki.datasource.DatabaseConnection DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.composedOf InputStep
 * @apiviz.composedOf AlgorithmStep
 * @apiviz.composedOf EvaluationStep
 * @apiviz.composedOf OutputStep
 */
public class KDDTask implements Parameterizable {
  /**
   * The settings used, for settings reporting.
   */
  private Collection<Pair<Object, Parameter<?, ?>>> settings;

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
   * The result object.
   */
  private HierarchicalResult result;

  /**
   * Constructor.
   * 
   * @param inputStep
   * @param algorithmStep
   * @param evaluationStep
   * @param outputStep
   * @param settings
   */
  public KDDTask(InputStep inputStep, AlgorithmStep algorithmStep, EvaluationStep evaluationStep, OutputStep outputStep, Collection<Pair<Object, Parameter<?, ?>>> settings) {
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
   * 
   * @throws IllegalStateException on execution errors
   */
  public void run() throws IllegalStateException {
    // Input step
    Database db = inputStep.getDatabase();

    // Algorithms - Data Mining Step
    result = algorithmStep.runAlgorithms(db);

    // TODO: this could be nicer
    result.getHierarchy().add(result, new SettingsResult(settings));

    // Evaluation
    evaluationStep.runEvaluators(result, db);

    // Output / Visualization
    outputStep.runResultHandlers(result);
  }

  /**
   * Get the algorithms result.
   * 
   * @return the result
   */
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
    InputStep inputStep = null;

    AlgorithmStep algorithmStep = null;

    EvaluationStep evaluationStep = null;

    Collection<Pair<Object, Parameter<?, ?>>> settings = null;

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