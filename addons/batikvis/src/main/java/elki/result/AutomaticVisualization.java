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
package elki.result;

import javax.swing.JFrame;

import elki.gui.GUIUtil;
import elki.logging.Logging;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.StringParameter;
import elki.visualization.VisualizerContext;
import elki.visualization.VisualizerParameterizer;
import elki.visualization.gui.ResultWindow;

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
@Alias({ "visualizer", "vis", "ResultVisualizer" })
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
  public void processNewResult(final Object result) {
    if(window == null) {
      if(title == null) {
        title = VisualizerParameterizer.getTitle(ResultUtil.findDatabase(result), result);
        if(title == null) {
          title = DEFAULT_TITLE;
        }
      }

      GUIUtil.setLookAndFeel();
      VisualizerContext context = manager.newContext(result);
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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new StringParameter(WINDOW_TITLE_ID) //
          .setOptional(true) //
          .grab(config, x -> title = x);
      new Flag(SINGLE_ID).grab(config, x -> single = x);
      manager = config.tryInstantiate(VisualizerParameterizer.class);
    }

    @Override
    public AutomaticVisualization make() {
      return new AutomaticVisualization(title, manager, single);
    }
  }
}
