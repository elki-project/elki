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
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Simple parallel projection
 * 
 * Scaled space: reordered, scaled and inverted. Lower dimensionality! [0:1]
 * Render space: not used here; no recentering needed.
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 */
public class SimpleParallel extends BasicResult implements ProjectionParallel {
  /**
   * Number of visible dimensions
   */
  int visDims;

  /**
   * Flags for the dimensions
   */
  byte[] flags;

  /**
   * Ordering of dimensions
   */
  int[] dimOrder;

  /**
   * Scales
   */
  private LinearScale[] scales;

  /**
   * Flag for visibility
   */
  final static byte FLAG_HIDDEN = 1;

  /**
   * Flag for inverted dimensions
   * 
   * TODO: handle inversions via scales?
   */
  final static byte FLAG_INVERTED = 2;

  /**
   * Constructor.
   * 
   * @param scales Scales to use
   */
  public SimpleParallel(LinearScale[] scales) {
    super("Parallel projection", "parallel-projection");
    this.scales = scales;
    visDims = scales.length;
    flags = new byte[scales.length];
    dimOrder = new int[scales.length];
    for(int i = 0; i < dimOrder.length; i++) {
      dimOrder[i] = i;
    }
  }

  @Override
  public LinearScale getScale(int dim) {
    return scales[dim];
  }

  @Override
  public boolean isAxisInverted(int axis) {
    return isDimInverted(dimOrder[axis]);
  }

  @Override
  public void setAxisInverted(int axis, boolean bool) {
    setDimInverted(dimOrder[axis], bool);
  }

  @Override
  public void toggleAxisInverted(int axis) {
    toggleDimInverted(dimOrder[axis]);
  }

  @Override
  public boolean isDimInverted(int truedim) {
    return (flags[truedim] & FLAG_INVERTED) == FLAG_INVERTED;
  }

  @Override
  public void setDimInverted(int truedim, boolean bool) {
    if(bool) {
      flags[truedim] |= FLAG_INVERTED;
    }
    else {
      flags[truedim] &= ~FLAG_INVERTED;
    }
  }

  @Override
  public void toggleDimInverted(int truedim) {
    flags[truedim] ^= FLAG_INVERTED;
  }

  @Override
  public LinearScale getAxisScale(int axis) {
    return scales[dimOrder[axis]];
  }

  protected boolean isDimHidden(int truedim) {
    return (flags[truedim] & FLAG_HIDDEN) == FLAG_HIDDEN;
  }

  @Override
  public boolean isAxisVisible(int dim) {
    return !isDimHidden(dimOrder[dim]);
  }

  @Override
  public void setAxisVisible(int dim, boolean vis) {
    boolean prev = isAxisVisible(dim);
    if(prev == vis) {
      return;
    }
    if(vis) {
      flags[dimOrder[dim]] &= ~FLAG_HIDDEN;
      visDims++;
    }
    else {
      flags[dimOrder[dim]] |= FLAG_HIDDEN;
      visDims--;
    }
  }

  @Override
  public void toggleAxisVisible(int dim) {
    boolean prev = isAxisVisible(dim);
    if(!prev) {
      flags[dimOrder[dim]] &= ~FLAG_HIDDEN;
      visDims++;
    }
    else {
      flags[dimOrder[dim]] |= FLAG_HIDDEN;
      visDims--;
    }
  }

  @Override
  public int getVisibleDimensions() {
    return visDims;
  }

  @Override
  public int getDimForAxis(int pos) {
    return dimOrder[pos];
  }

  @Override
  public int getDimForVisibleAxis(int pos) {
    for(int i = 0; i < scales.length; i++) {
      if (isDimHidden(dimOrder[i])) {
        continue;
      }
      if (pos == 0) {
        return dimOrder[i];
      }
      pos--;
    }
    return -1;
  }

  @Override
  public void swapAxes(int a, int b) {
    int temp = dimOrder[a];
    dimOrder[a] = dimOrder[b];
    dimOrder[b] = temp;
  }

  @Override
  public void moveAxis(int src, int dest) {
    if(src > dest) {
      int temp = dimOrder[src];
      System.arraycopy(dimOrder, dest, dimOrder, dest + 1, src - dest);
      dimOrder[dest] = temp;
    }
    else if(src < dest) {
      int temp = dimOrder[src];
      System.arraycopy(dimOrder, src + 1, dimOrder, src, dest - src);
      dimOrder[dest - 1] = temp;
    }
  }

  @Override
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> data) {
    double[] v = new double[visDims];
    for(int j = 0, o = 0; j < scales.length; j++) {
      if(isDimHidden(j)) {
        continue;
      }
      int i = dimOrder[j];
      v[o] = scales[i].getScaled(data.doubleValue(i + 1));
      if(!isDimInverted(i)) {
        v[o] = 1 - v[o];
      }
      v[o] *= StyleLibrary.SCALE;
      o++;
    }
    return v;
  }

  @Override
  public double[] fastProjectDataToRenderSpace(double[] data) {
    double[] v = new double[visDims];
    for(int j = 0, o = 0; j < scales.length; j++) {
      if(isDimHidden(j)) {
        continue;
      }
      int i = dimOrder[j];
      v[o] = scales[i].getScaled(data[i]);
      if(!isDimInverted(i)) {
        v[o] = 1 - v[o];
      }
      v[o] *= StyleLibrary.SCALE;
      o++;
    }
    return v;
  }

  @Override
  public double fastProjectRenderToDataSpace(double v, int projdim) {
    int truedim = dimOrder[projdim];
    v /= StyleLibrary.SCALE;
    if(!isDimInverted(truedim)) {
      v = 1 - v;
    }
    return scales[truedim].getUnscaled(v);
  }

  @Override
  public double fastProjectDataToRenderSpace(double value, int dim) {
    double temp = scales[dimOrder[dim]].getScaled(value);
    temp *= StyleLibrary.SCALE;
    if(!isAxisInverted(dimOrder[dim])) {
      return temp;
    }
    return 1 - temp;
  }

  @Override
  public int getAxisForDim(int truedim) {
    for(int i = 0; i < dimOrder.length; i++) {
      if(dimOrder[i] == truedim) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getInputDimensionality() {
    return scales.length;
  }
}