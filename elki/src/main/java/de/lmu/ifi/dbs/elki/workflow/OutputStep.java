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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * The "output" step, where data is analyzed.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.uses ResultHierarchy
 * @apiviz.has ResultHandler
 */
public class OutputStep implements WorkflowStep {
  /**
   * Output handler.
   */
  private List<ResultHandler> resulthandlers = null;

  /**
   * Constructor.
   *
   * @param resulthandlers Result handlers to use
   */
  public OutputStep(List<ResultHandler> resulthandlers) {
    super();
    this.resulthandlers = resulthandlers;
  }

  /**
   * Run the result handlers.
   *
   * @param hier Result to run on
   * @param db Database
   */
  public void runResultHandlers(ResultHierarchy hier, Database db) {
    // Run result handlers
    for(ResultHandler resulthandler : resulthandlers) {
      Thread.currentThread().setName(resulthandler.toString());
      resulthandler.processNewResult(hier, db);
    }
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
      clz = (Class<? extends ResultHandler>) ClassLoader.getSystemClassLoader().loadClass(//
      "de.lmu.ifi.dbs.elki.result.AutomaticVisualization");
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Output handlers.
     */
    private List<ResultHandler> resulthandlers = null;

    /**
     * Parameter to specify the result handler classes.
     *
     * Key:
     * <p>
     * {@code -resulthandler}
     * </p>
     */
    public static final OptionID RESULT_HANDLER_ID = new OptionID("resulthandler", "Result handler class.");

    /**
     * OptionID for the application output file/folder.
     *
     * Key:
     * <p>
     * {@code -out}
     * </p>
     */
    public static final OptionID OUTPUT_ID = new OptionID("out", //
    "Directory name (or name of an existing file) to write the obtained results in. " + //
    "If this parameter is omitted, per default the output will sequentially be given to STDOUT.");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // result handlers
      final ObjectListParameter<ResultHandler> resultHandlerParam = new ObjectListParameter<>(RESULT_HANDLER_ID, ResultHandler.class);
      if(defaultHandlers != null) {
        resultHandlerParam.setDefaultValue(defaultHandlers);
      }
      if(config.grab(resultHandlerParam)) {
        resulthandlers = resultHandlerParam.instantiateClasses(config);
      }
    }

    @Override
    protected OutputStep makeInstance() {
      return new OutputStep(resulthandlers);
    }
  }
}
