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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;

/**
 * Dimension-selecting 1D projection.
 * 
 * @author Erich Schubert
 */
public class Simple1D extends AbstractSimpleProjection implements Projection1D {
  /**
   * Our dimension, starting with 0
   */
  final int dnum;

  /**
   * Simple 1D projection using scaling only.
   * 
   * @param scales Scales to use
   * @param dnum Dimension (starting at 1)
   */
  public Simple1D(LinearScale[] scales, int dnum) {
    super(scales);
    this.dnum = dnum - 1;
  }

  @Override
  public double fastProjectDataToRenderSpace(Vector data) {
    return (scales[dnum].getScaled(data.get(dnum)) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectDataToRenderSpace(NumberVector<?, ?> data) {
    return (scales[dnum].getScaled(data.doubleValue(dnum + 1)) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectScaledToRender(Vector v) {
    return (v.get(dnum) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(Vector data) {
    return (scales[dnum].getScaled(data.get(dnum)) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return (data.doubleValue(dnum) - 0.5) * SCALE;
  }

  @Override
  public double fastProjectRelativeScaledToRender(Vector v) {
    return v.get(dnum) * SCALE;
  }

  @Override
  protected Vector rearrange(Vector v) {
    final double[] s = v.getArrayRef();
    final double[] r = new double[s.length];
    r[0] = s[dnum];
    if(dnum > 0) {
      System.arraycopy(s, 0, r, 1, dnum);
    }
    if(dnum + 1 < s.length) {
      System.arraycopy(s, dnum + 1, r, dnum + 1, s.length - (dnum + 1));
    }
    return new Vector(r);
  }

  @Override
  protected Vector dearrange(Vector v) {
    final double[] s = v.getArrayRef();
    final double[] r = new double[s.length];
    if(dnum > 0) {
      System.arraycopy(s, 1, r, 0, dnum);
    }
    r[dnum] = s[0];
    if(dnum + 1 < s.length) {
      System.arraycopy(s, dnum + 1, r, dnum + 1, s.length - (dnum + 1));
    }
    return new Vector(r);
  }
}