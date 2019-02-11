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
package de.lmu.ifi.dbs.elki.result;

import javax.swing.JFrame;

import de.lmu.ifi.dbs.elki.gui.GUIUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizerParameterizer;
import de.lmu.ifi.dbs.elki.visualization.gui.ResultWindow;

/**
 * Handler to process and visualize a Result.
 *
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @since 0.3
 *
 * @composed - - - VisualizerParameterizer
 * @navassoc - - - ResultWindow
 */
@Alias({ "visualizer", "vis", "ResultVisualizer", "de.lmu.ifi.dbs.elki.visualization.gui.ResultVisualizer" })
@Priority(Priority.IMPORTANT + 5)
public class AutomaticVisualization implements ResultHandler {
  /**
   * Get a logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AutomaticVisualization.class);

  /**
   * Stores the set title.
   */
  String title;

  /**
   * Default title
   */
  protected static final String DEFAULT_TITLE = "ELKI Result Visualization";

  /**
   * Visualization manager.
   */
  VisualizerParameterizer manager;

  /**
   * Single view mode
   */
  boolean single;

  /**
   * Current result window.
   */
  ResultWindow window;

  /**
   * Constructor.
   *
   * @param title Window title
   * @param manager Parameterization manager for visualizers
   * @param single Flag to indicat single-view mode.
   */
  public AutomaticVisualization(String title, VisualizerParameterizer manager, boolean single) {
    super();
    this.title = title;
    this.manager = manager;
    this.single = single;
  }

  @Override
  public void processNewResult(final ResultHierarchy hier, final Result result) {
    if(window == null) {
      if(title == null) {
        title = VisualizerParameterizer.getTitle(ResultUtil.findDatabase(hier), result);
        if(title == null) {
          title = DEFAULT_TITLE;
        }
      }

      GUIUtil.setLookAndFeel();
      VisualizerContext context = manager.newContext(hier, result);
      window = new ResultWindow(title, context, single);
    }

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          window.setVisible(true);
          window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
        catch(Throwable e) {
          LOG.exception("Error in starting visualizer window.", e);
        }
      }
    });
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the window title
     */
    public static final OptionID WINDOW_TITLE_ID = new OptionID("vis.window.title", "Title to use for visualization window.");

    /**
     * Flag to set single display
     */
    public static final OptionID SINGLE_ID = new OptionID("vis.window.single", "Embed visualizers in a single window, not using thumbnails and detail views.");

    /**
     * Stores the set title.
     */
    String title;

    /**
     * Visualization manager.
     */
    VisualizerParameterizer manager;

    /**
     * Single view mode.
     */
    boolean single = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      StringParameter titleP = new StringParameter(WINDOW_TITLE_ID) //
          .setOptional(true);
      if(config.grab(titleP)) {
        title = titleP.getValue();
      }
      Flag singleF = new Flag(SINGLE_ID);
      if(config.grab(singleF)) {
        single = singleF.isTrue();
      }
      manager = config.tryInstantiate(VisualizerParameterizer.class);
    }

    @Override
    protected AutomaticVisualization makeInstance() {
      return new AutomaticVisualization(title, manager, single);
    }
  }
}
