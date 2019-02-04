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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;

/**
 * Dimension-selecting 1D projection.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class Simple1D extends AbstractSimpleProjection implements Projection1D {
  /**
   * Our dimension, starting with 0
   */
  final int dnum;

  /**
   * Simple 1D projection using scaling only.
   *
   * @param p Projector
   * @param scales Scales to use
   * @param dnum Dimension (starting at 0)
   */
  public Simple1D(Projector p, LinearScale[] scales, int dnum) {
    super(p, scales);
    this.dnum = dnum;
  }

  @Override
  public double fastProjectDataToRenderSpace(double[] data) {
    return (scales[dnum].getScaled(data[dnum]) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectDataToRenderSpace(NumberVector data) {
    return (scales[dnum].getScaled(data.doubleValue(dnum)) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectScaledToRender(double[] v) {
    return (v[dnum] - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(double[] data) {
    return (scales[dnum].getScaled(data[dnum]) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(NumberVector data) {
    return (data.doubleValue(dnum) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeScaledToRender(double[] v) {
    return v[dnum] * SCALE;
  }

  @Override
  protected double[] rearrange(double[] v) {
    final double[] r = new double[v.length];
    r[0] = v[dnum];
    if(dnum > 0) {
      System.arraycopy(v, 0, r, 1, dnum);
    }
    if(dnum + 1 < v.length) {
      System.arraycopy(v, dnum + 1, r, dnum + 1, v.length - (dnum + 1));
    }
    return r;
  }

  @Override
  protected double[] dearrange(double[] v) {
    final double[] r = new double[v.length];
    if(dnum > 0) {
      System.arraycopy(v, 1, r, 0, dnum);
    }
    r[dnum] = v[0];
    if(dnum + 1 < v.length) {
      System.arraycopy(v, dnum + 1, r, dnum + 1, v.length - (dnum + 1));
    }
    return r;
  }


  @Override
  public String getMenuName() {
    return "Axis";
  }
}