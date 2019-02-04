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
package de.lmu.ifi.dbs.elki.visualization.projections;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;

/**
 * OPTICS projection. This is not really needed, but a quick hack to have more
 * consistency in the visualizer API.
 *
 * @author Erich Schubert
 * @since 0.5.5
 */
public class OPTICSProjection implements Projection {
  /**
   * The projector we were generated from.
   */
  OPTICSProjector projector;

  /**
   * Constructor.
   *
   * @param opticsProjector OPTICS projector
   */
  public OPTICSProjection(OPTICSProjector opticsProjector) {
    super();
    this.projector = opticsProjector;
  }

  @Override
  public String getMenuName() {
    return "OPTICS Plot Projection";
  }

  @Override
  public int getInputDimensionality() {
    return -1;
  }

  @Override
  public LinearScale getScale(int d) {
    return null;
  }

  /**
   * Get or produce the actual OPTICS plot.
   *
   * @param context Context to use
   * @return Plot
   */
  public OPTICSPlot getOPTICSPlot(VisualizerContext context) {
    return projector.getOPTICSPlot(context);
  }

  /**
   * Get the OPTICS cluster order.
   *
   * @return Cluster oder result.
   */
  public ClusterOrder getResult() {
    return projector.getResult();
  }

  @Override
  public Projector getProjector() {
    return projector;
  }
}
