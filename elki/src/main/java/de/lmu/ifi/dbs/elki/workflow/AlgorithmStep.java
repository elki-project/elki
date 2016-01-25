package de.lmu.ifi.dbs.elki.workflow;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "algorithms" step, where data is analyzed.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.has Algorithm
 * @apiviz.has Result
 * @apiviz.uses Database
 */
public class AlgorithmStep implements WorkflowStep {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(AlgorithmStep.class);

  /**
   * Holds the algorithm to run.
   */
  private List<Algorithm> algorithms;

  /**
   * The algorithm output
   */
  private Result stepresult;

  /**
   * Constructor.
   *
   * @param algorithms
   */
  public AlgorithmStep(List<Algorithm> algorithms) {
    super();
    this.algorithms = algorithms;
  }

  /**
   * Run algorithms.
   *
   * @param database Database
   * @return Algorithm result
   */
  public Result runAlgorithms(Database database) {
    ResultHierarchy hier = database.getHierarchy();
    if(LOG.isStatistics()) {
      boolean first = true;
      for(Iter<Result> it = hier.iterDescendants(database); it.valid(); it.advance()) {
        if(!(it.get() instanceof Index)) {
          continue;
        }
        if(first) {
          LOG.statistics("Index statistics before running algorithms:");
          first = false;
        }
        ((Index) it.get()).logStatistics();
      }
    }
    stepresult = new BasicResult("Algorithm Step", "algorithm-step");
    for(Algorithm algorithm : algorithms) {
      Thread.currentThread().setName(algorithm.toString());
      Duration duration = LOG.isStatistics() ? LOG.newDuration(algorithm.getClass().getName() + ".runtime").begin() : null;
      Result res = algorithm.run(database);
      if(duration != null) {
        LOG.statistics(duration.end());
      }
      if(LOG.isStatistics()) {
        boolean first = true;
        for(Iter<Result> it = hier.iterDescendants(database); it.valid(); it.advance()) {
          if(!(it.get() instanceof Index)) {
            continue;
          }
          if(first) {
            LOG.statistics("Index statistics after running algorithm " + algorithm.toString() + ":");
            first = false;
          }
          ((Index) it.get()).logStatistics();
        }
      }
      if(res != null) {
        // Make sure the result is attached, but usually this is a noop:
        hier.add(database, res);
      }
    }
    return stepresult;
  }

  /**
   * Get the result.
   *
   * @return Result.
   */
  public Result getResult() {
    return stepresult;
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
     * Enable logging of performance data
     */
    protected boolean time = false;

    /**
     * Holds the algorithm to run.
     */
    protected List<Algorithm> algorithms;

    /**
     * Flag to allow verbose messages while running the application.
     * <p>
     * Key: {@code -time}
     * </p>
     */
    public static final OptionID TIME_ID = new OptionID("time", "Enable logging of runtime data. Do not combine with more verbose logging, since verbose logging can significantly impact performance.");

    /**
     * Parameter to specify the algorithm to run.
     * <p>
     * Key: {@code -algorithm}
     * </p>
     */
    public static final OptionID ALGORITHM_ID = new OptionID("algorithm", "Algorithm to run.");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Time parameter
      final Flag timeF = new Flag(TIME_ID);
      if(config.grab(timeF)) {
        time = timeF.getValue();
      }
      // parameter algorithm
      final ObjectListParameter<Algorithm> ALGORITHM_PARAM = new ObjectListParameter<>(ALGORITHM_ID, Algorithm.class);
      if(config.grab(ALGORITHM_PARAM)) {
        algorithms = ALGORITHM_PARAM.instantiateClasses(config);
      }
    }

    @Override
    protected AlgorithmStep makeInstance() {
      if(time) {
        LoggingConfiguration.setStatistics();
      }
      return new AlgorithmStep(algorithms);
    }
  }
}
