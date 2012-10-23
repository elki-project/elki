package de.lmu.ifi.dbs.elki.visualization.projections;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;

/**
 * OPTICS projection. This is not really needed, but a quick hack to have more
 * consistency in the visualizer API.
 * 
 * @author Erich Schubert
 */
public class OPTICSProjection<D extends Distance<D>> extends AbstractHierarchicalResult implements Projection {
  /**
   * The projector we were generated from.
   */
  OPTICSProjector<D> projector;

  /**
   * Constructor.
   *
   * @param opticsProjector OPTICS projector
   */
  public OPTICSProjection(OPTICSProjector<D> opticsProjector) {
    super();
    this.projector = opticsProjector;
  }

  @Override
  public String getLongName() {
    return "OPTICS projection";
  }

  @Override
  public String getShortName() {
    return "OPTICSproj";
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
  public OPTICSPlot<D> getOPTICSPlot(VisualizerContext context) {
    return projector.getOPTICSPlot(context);
  }

  /**
   * Get the OPTICS cluster order.
   * 
   * @return Cluster oder result.
   */
  public ClusterOrderResult<D> getResult() {
    return projector.getResult();
  }
}
