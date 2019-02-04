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

import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;

/**
 * Abstract base projection class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractProjection implements Projection {
  /**
   * Scales in data set
   */
  final protected LinearScale[] scales;

  /**
   * Projector used
   */
  final private Projector p;

  /**
   * Constructor.
   *
   * @param p Projector
   * @param scales Scales to use
   */
  public AbstractProjection(Projector p, LinearScale[] scales) {
    super();
    this.p = p;
    this.scales = scales;
  }

  @Override
  public int getInputDimensionality() {
    return scales.length;
  }

  /**
   * Get the scales used, for rendering scales mostly.
   *
   * @param d Dimension
   * @return Scale used
   */
  @Override
  public LinearScale getScale(int d) {
    return scales[d];
  }

  @Override
  public String getMenuName() {
    return "Projection";
  }

  @Override
  public Projector getProjector() {
    return p;
  }
}