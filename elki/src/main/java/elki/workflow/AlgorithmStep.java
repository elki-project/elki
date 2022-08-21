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

import java.util.List;

import elki.Algorithm;
import elki.database.Database;
import elki.index.Index;
import elki.logging.Logging;
import elki.logging.LoggingConfiguration;
import elki.logging.statistics.Duration;
import elki.result.Metadata;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "algorithms" step, where data is analyzed.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - Algorithm
 * @has - - - Result
 * @assoc - - - Database
 */
public class AlgorithmStep implements WorkflowStep {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(AlgorithmStep.class);

  /**
   * Holds the algorithm to run.
   */
  private List<? extends Algorithm> algorithms;

  /**
   * The algorithm output
   */
  private Object stepresult;

  /**
   * Constructor.
   *
   * @param algorithms
   */
  public AlgorithmStep(List<? extends Algorithm> algorithms) {
    super();
    this.algorithms = algorithms;
  }

  /**
   * Run algorithms.
   *
   * @param database Database
   * @return Algorithm result
   */
  public Object runAlgorithms(Database database) {
    if(LOG.isStatistics()) {
      boolean first = true;
      for(It<Index> it = Metadata.hierarchyOf(database).iterDescendants().filter(Index.class); it.valid(); it.advance()) {
        if(first) {
          LOG.statistics("Index statistics before running algorithms:");
          first = false;
        }
        it.get().logStatistics();
      }
    }
    stepresult = new Object();
    Metadata.of(stepresult).setLongName("Algorithm Step");
    for(Algorithm algorithm : algorithms) {
      Thread.currentThread().setName(algorithm.toString());
      Duration duration = LOG.isStatistics() ? LOG.newDuration(algorithm.getClass().getName() + ".runtime").begin() : null;
      Object res = algorithm.autorun(database);
      if(duration != null) {
        LOG.statistics(duration.end());
      }
      if(LOG.isStatistics()) {
        boolean first = true;
        for(It<Index> it = Metadata.hierarchyOf(database).iterDescendants().filter(Index.class); it.valid(); it.advance()) {
          if(first) {
            LOG.statistics("Index statistics after running algorithm " + algorithm.toString() + ":");
            first = false;
          }
          it.get().logStatistics();
        }
      }
      if(res != null) {
        // Make sure the result is attached, but usually this is a noop:
        Metadata.hierarchyOf(database).addChild(res);
      }
    }
    Thread.currentThread().setName("AlgorithmStep finished.");
    return stepresult;
  }

  /**
   * Get the result.
   *
   * @return Result.
   */
  public Object getResult() {
    return stepresult;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Enable logging of performance data
     */
    protected boolean time = false;

    /**
     * Holds the algorithm to run.
     */
    protected List<? extends Algorithm> algorithms;

    /**
     * Flag to allow verbose messages while running the application.
     */
    public static final OptionID TIME_ID = new OptionID("time", "Enable logging of runtime data. Do not combine with more verbose logging, since verbose logging can significantly impact performance.");

    /**
     * Parameter to specify the algorithm to run.
     */
    public static final OptionID ALGORITHM_ID = Algorithm.Utils.ALGORITHM_ID;

    @Override
    public void configure(Parameterization config) {
      new Flag(TIME_ID).grab(config, x -> time = x);
      // parameter algorithm
      new ObjectListParameter<Algorithm>(ALGORITHM_ID, Algorithm.class) //
          .grab(config, x -> algorithms = x);
    }

    @Override
    public AlgorithmStep make() {
      if(time) {
        LoggingConfiguration.setStatistics();
      }
      return new AlgorithmStep(algorithms);
    }
  }
}
