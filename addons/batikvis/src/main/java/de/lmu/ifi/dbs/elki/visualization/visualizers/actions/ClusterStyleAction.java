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
package de.lmu.ifi.dbs.elki.visualization.visualizers.actions;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationMenuAction;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Actions to use clusterings for styling.
 *
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - SetStyleAction
 */
public class ClusterStyleAction implements VisFactory {
  /**
   * Constructor.
   */
  public ClusterStyleAction() {
    super();
  }

  @Override
  public void processNewResult(final VisualizerContext context, Object start) {
    VisualizationTree.findNewResults(context, start).filter(Clustering.class).forEach(c -> {
      if(VisualizationTree.findVis(context, c).filter(SetStyleAction.class).valid()) {
        return; // There already is a style button.
      }
      context.addVis(c, new SetStyleAction(c, context));
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    throw new AbortException("Should never be called.");
  }

  /**
   * Action to use a clustering as {@link ClusterStylingPolicy}.
   *
   * @author Erich Schubert
   */
  private static final class SetStyleAction implements VisualizationMenuAction {
    /**
     * Clustering to use
     */
    private final Clustering<?> c;

    /**
     * Visualization context.
     */
    private final VisualizerContext context;

    /**
     * Constructor.
     *
     * @param c Clustering
     * @param context Context
     */
    private SetStyleAction(Clustering<?> c, VisualizerContext context) {
      this.c = c;
      this.context = context;
    }

    @Override
    public void activate() {
      context.setStylingPolicy(new ClusterStylingPolicy(c, context.getStyleLibrary()));
    }

    @Override
    public String getMenuName() {
      return "Use as Styling Policy";
    }

    @Override
    public boolean enabled() {
      StylingPolicy sp = context.getStylingPolicy();
      return !(sp instanceof ClusterStylingPolicy) || //
          ((ClusterStylingPolicy) sp).getClustering() != c;
    }
  }
}
