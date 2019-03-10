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
package elki.application;

import elki.Algorithm;
import elki.KDDTask;
import elki.utilities.Alias;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.workflow.OutputStep;

/**
 * Basic command line application for Knowledge Discovery in Databases use
 * cases. It allows running unsupervised {@link Algorithm}s to run on any
 * {@link elki.datasource.DatabaseConnection DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * @since 0.3
 * 
 * @composed - - - KDDTask
 */
@Alias({ "cli", "kddtask" })
public class KDDCLIApplication extends AbstractApplication {
  /**
   * The KDD Task to perform.
   */
  KDDTask task;

  /**
   * Constructor.
   * 
   * @param task Task to run
   */
  public KDDCLIApplication(KDDTask task) {
    super();
    this.task = task;
  }

  @Override
  public void run() {
    task.run();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * The KDD Task to perform.
     */
    protected KDDTask task;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      task = config.tryInstantiate(KDDTask.class);
    }

    @Override
    protected KDDCLIApplication makeInstance() {
      return new KDDCLIApplication(task);
    }
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    OutputStep.setDefaultHandlerWriter();
    runCLIApplication(KDDCLIApplication.class, args);
  }
}
