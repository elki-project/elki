/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.projections;

import elki.data.Clustering;
import elki.database.ids.DoubleDBIDList;
import elki.math.scales.LinearScale;
import elki.visualization.VisualizerContext;
import elki.visualization.projector.Projector;
import elki.visualization.projector.SilhouettePlotProjector;
import elki.visualization.silhouette.SilhouettePlot;

/**
 * Silhouette projection.
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class SilhouetteProjection implements Projection {
  /**
   * The projector we were generated from.
   */
  SilhouettePlotProjector projector;

  /**
   * Constructor.
   *
   * @param silhouettePlotProjector Silhouette projector
   */
  public SilhouetteProjection(SilhouettePlotProjector silhouettePlotProjector) {
    super();
    this.projector = silhouettePlotProjector;
  }

  @Override
  public String getMenuName() {
    return "Silhouette Plot Projection";
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
   * Get or produce the actual Silhouette plot.
   *
   * @param context Context to use
   * @return Plot
   */
  public SilhouettePlot getSilhouettePlot(VisualizerContext context) {
    return projector.getSilhouettePlot(context);
  }

  /**
   * Get the underlying Clustering from which the silhouette values are
   * calculated.
   *
   * @return Clustering.
   */
  public Clustering<?> getResult() {
    return projector.getResult();
  }

  /**
   * Get the silhouette values
   * 
   * @return silhouette values
   */
  public DoubleDBIDList[] getSilhouetteValues() {
    return projector.getValues();
  }

  @Override
  public Projector getProjector() {
    return projector;
  }
}
