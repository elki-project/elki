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

import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;

/**
 * Abstract base class for "simple" projections.
 *
 * Simple projections use the given scaling and dimension selection only.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractSimpleProjection extends AbstractFullProjection {
  /**
   * Constructor.
   *
   * @param p Projector
   * @param scales Scales to use
   */
  public AbstractSimpleProjection(Projector p, LinearScale[] scales) {
    super(p, scales);
  }

  @Override
  public double[] projectScaledToRender(double[] v) {
    v = rearrange(v);
    VMath.minusEquals(v, .5);
    v = flipSecondEquals(v);
    VMath.timesEquals(v, SCALE);
    return v;
  }

  @Override
  public double[] projectRenderToScaled(double[] v) {
    v = VMath.times(v, INVSCALE);
    v = flipSecondEquals(v);
    VMath.plusEquals(v, .5);
    v = dearrange(v);
    return v;
  }

  @Override
  public double[] projectRelativeScaledToRender(double[] v) {
    v = rearrange(v);
    v = flipSecondEquals(v);
    VMath.timesEquals(v, SCALE);
    return v;
  }

  @Override
  public double[] projectRelativeRenderToScaled(double[] v) {
    v = VMath.times(v, INVSCALE);
    v = flipSecondEquals(v);
    v = dearrange(v);
    return v;
  }

  /**
   * Flip the y axis.
   *
   * @param v double[]
   * @return modified v
   */
  protected double[] flipSecondEquals(double[] v) {
    if(v.length > 1) {
      v[1] *= -1;
    }
    return v;
  }

  /**
   * Method to rearrange components.
   *
   * @param v double[] to rearrange
   * @return rearranged copy
   */
  protected abstract double[] rearrange(double[] v);

  /**
   * Undo the rearrangement of components.
   *
   * @param v double[] to undo the rearrangement
   * @return rearranged-undone copy
   */
  protected abstract double[] dearrange(double[] v);
}