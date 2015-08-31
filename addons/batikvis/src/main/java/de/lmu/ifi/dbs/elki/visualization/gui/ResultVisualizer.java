package de.lmu.ifi.dbs.elki.visualization.gui;

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

import javax.swing.JFrame;

import de.lmu.ifi.dbs.elki.gui.GUIUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizerParameterizer;

/**
 * Handler to process and visualize a Result.
 *
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 *
 * @apiviz.composedOf VisualizerParameterizer
 * @apiviz.uses ResultWindow oneway
 */
@Alias({ "visualizer", "vis", "ResultVisualizer" })
public class ResultVisualizer implements ResultHandler {
  /**
   * Get a logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ResultVisualizer.class);

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
  public ResultVisualizer(String title, VisualizerParameterizer manager, boolean single) {
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the window title
     * <p>
     * Key: {@code -vis.window.title}
     * </p>
     * <p>
     * Default value: "ELKI Result Visualization"
     * </p>
     */
    public static final OptionID WINDOW_TITLE_ID = new OptionID("vis.window.title", "Title to use for visualization window.");

    /**
     * Flag to set single display
     *
     * <p>
     * Key: -vis.single
     * </p>
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
      StringParameter titleP = new StringParameter(WINDOW_TITLE_ID);
      titleP.setOptional(true);
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
    protected ResultVisualizer makeInstance() {
      return new ResultVisualizer(title, manager, single);
    }
  }
}
