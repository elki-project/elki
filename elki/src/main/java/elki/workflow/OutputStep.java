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
package elki.workflow;

import java.util.ArrayList;
import java.util.List;

import elki.database.Database;
import elki.result.ResultHandler;
import elki.result.ResultWriter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "output" step, where data is analyzed.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - ResultHierarchy
 * @has - - - ResultHandler
 */
public class OutputStep implements WorkflowStep {
  /**
   * Output handler.
   */
  private List<? extends ResultHandler> resulthandlers = null;

  /**
   * Constructor.
   *
   * @param resulthandlers Result handlers to use
   */
  public OutputStep(List<? extends ResultHandler> resulthandlers) {
    super();
    this.resulthandlers = resulthandlers;
  }

  /**
   * Run the result handlers.
   *
   * @param db Database
   */
  public void runResultHandlers(Database db) {
    // Run result handlers
    for(ResultHandler resulthandler : resulthandlers) {
      Thread.currentThread().setName(resulthandler.toString());
      resulthandler.processNewResult(db);
    }
    Thread.currentThread().setName("OutputStep finished.");
  }

  /**
   * Set the default handler to the {@link ResultWriter}.
   */
  public static void setDefaultHandlerWriter() {
    defaultHandlers = new ArrayList<>(1);
    defaultHandlers.add(ResultWriter.class);
  }

  /**
   * Set the default handler to the Batik addon visualizer, if available.
   */
  @SuppressWarnings("unchecked")
  public static void setDefaultHandlerVisualizer() {
    defaultHandlers = new ArrayList<>(1);
    Class<? extends ResultHandler> clz;
    try {
      clz = (Class<? extends ResultHandler>) Thread.currentThread().getContextClassLoader() //
          .loadClass("elki.result.AutomaticVisualization");
    }
    catch(ClassNotFoundException e) {
      clz = ResultWriter.class;
    }
    defaultHandlers.add(clz);
  }

  protected static ArrayList<Class<? extends ResultHandler>> defaultHandlers = null;

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Output handlers.
     */
    private List<? extends ResultHandler> resulthandlers = null;

    /**
     * Parameter to specify the result handler classes.
     */
    public static final OptionID RESULT_HANDLER_ID = new OptionID("resulthandler", "Result handler class.");

    /**
     * OptionID for the application output file/folder.
     */
    public static final OptionID OUTPUT_ID = new OptionID("out", //
        "Directory name (or name of an existing file) to write the obtained results in. " + //
            "If this parameter is omitted, per default the output will sequentially be given to STDOUT.");

    @Override
    public void configure(Parameterization config) {
      new ObjectListParameter<ResultHandler>(RESULT_HANDLER_ID, ResultHandler.class) //
          .setDefaultValue(defaultHandlers) //
          .grab(config, x -> resulthandlers = x);
    }

    @Override
    public OutputStep make() {
      return new OutputStep(resulthandlers);
    }
  }
}
