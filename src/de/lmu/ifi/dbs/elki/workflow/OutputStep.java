package de.lmu.ifi.dbs.elki.workflow;
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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.visualization.gui.ResultVisualizer;

/**
 * The "output" step, where data is analyzed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Result
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
   * @param result Result to run on
   */
  public void runResultHandlers(HierarchicalResult result) {
    // Run result handlers
    for(ResultHandler resulthandler : resulthandlers) {
      resulthandler.processNewResult(result, result);
    }
  }

  /**
   * Set the default handler to the {@link ResultWriter}.
   */
  public static void setDefaultHandlerWriter() {
    defaultHandlers = new ArrayList<Class<? extends ResultHandler>>(1);
    defaultHandlers.add(ResultWriter.class);
  }

  /**
   * Set the default handler to the {@link ResultVisualizer}.
   */
  public static void setDefaultHandlerVisualizer() {
    defaultHandlers = new ArrayList<Class<? extends ResultHandler>>(1);
    defaultHandlers.add(ResultVisualizer.class);
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

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // result handlers
      final ObjectListParameter<ResultHandler> resultHandlerParam = new ObjectListParameter<ResultHandler>(OptionID.RESULT_HANDLER, ResultHandler.class);
      if (defaultHandlers != null) {
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