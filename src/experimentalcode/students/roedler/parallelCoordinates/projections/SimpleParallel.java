package experimentalcode.students.roedler.parallelCoordinates.projections;

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
import de.lmu.ifi.dbs.elki.result.BasicResult;

/**
 * Simple parallel projection
 * 
 * Scaled space: reordered, scaled and inverted. Lower dimensionality! [0:1]
 * Render space: not used here; no recentering needed.
 * 
 * @author Robert Rödler
 */
public class SimpleParallel extends BasicResult implements ProjectionParallel {
  /**
   * Number of dimensions
   */
  final int dims;

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
    dims = scales.length;
    visDims = dims;
    flags = new byte[dims];
    dimOrder = new int[dims];
    for(int i = 0; i < dimOrder.length; i++) {
      dimOrder[i] = i;
    }
  }

  @Override
  public LinearScale getScale(int d) {
    return scales[dimOrder[d]];
  }

  protected boolean inverted(int rawdim) {
    return (flags[rawdim] & FLAG_INVERTED) != FLAG_INVERTED;
  }

  @Override
  public boolean isInverted(int dim) {
    return inverted(dimOrder[dim]);
  }

  @Override
  public void setInverted(int dim, boolean bool) {
    if(bool) {
      flags[dimOrder[dim]] |= FLAG_INVERTED;
    }
    else {
      flags[dimOrder[dim]] &= ~FLAG_INVERTED;
    }
  }

  protected boolean hidden(int truedim) {
    return (flags[truedim] & FLAG_HIDDEN) == FLAG_HIDDEN;
  }

  @Override
  public boolean isVisible(int dim) {
    return !hidden(dimOrder[dim]);
  }

  @Override
  public void setVisible(boolean vis, int dim) {
    boolean prev = isVisible(dim);
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
    // TODO: signal change?
  }

  @Override
  public int getVisibleDimensions() {
    return visDims;
  }

  @Override
  public void toggleInverted(int dim) {
    flags[dimOrder[dim]] ^= FLAG_INVERTED;
  }

  @Override
  public int getFirstVisibleDimension() {
    for(int i = 0; i < dims; i++) {
      if(!hidden(dimOrder[i])) {
        return i;
      }
    }
    return 0;
  }

  @Override
  public int getLastVisibleDimension() {
    for(int i = dims - 1; i >= 0; i--) {
      if(!hidden(dimOrder[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getPrevVisibleDimension(int dim) {
    for(int i = dim - 1; i >= 0; i--) {
      if(!hidden(dimOrder[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getNextVisibleDimension(int dim) {
    for(int i = dim + 1; i < dims; i++) {
      if(!hidden(dimOrder[i])) {
        return i;
      }
    }
    return dim;
  }

  @Override
  public void swapDimensions(int a, int b) {
    int temp = dimOrder[a];
    dimOrder[a] = dimOrder[b];
    dimOrder[b] = temp;
  }

  @Override
  public void shiftDimension(int src, int dest) {
    if(src > dest) {
      int temp = dimOrder[src];
      System.arraycopy(dimOrder, src - 1, dimOrder, src, src - dest);
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
    for(int j = 0, o = 0; j < dims; j++) {
      if(hidden(j)) {
        continue;
      }
      int i = dimOrder[j];
      v[o] = scales[i].getScaled(data.doubleValue(i + 1));
      if(inverted(i)) {
        v[o] = 1 - v[o];
      }
      o++;
    }
    return v;
  }

  @Override
  public double[] fastProjectDataToRenderSpace(Vector data) {
    double[] v = new double[visDims];
    for(int j = 0, o = 0; j < dims; j++) {
      if(hidden(j)) {
        continue;
      }
      int i = dimOrder[j];
      v[o] = scales[i].getScaled(data.get(i));
      if(inverted(i)) {
        v[o] = 1 - v[o];
      }
      o++;
    }
    return v;
  }

  // @Override
  public Vector XXprojectScaledToRender(Vector v) {
    return YYprojectScaledToRender(v, true);
  }

  public Vector YYprojectScaledToRender(Vector v, boolean sort) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      if(isInverted(i)) {
        ret.set(i, v.get(i));
      }
      else {
        ret.set(i, 1 - v.get(i));
      }
    }
    if(sort) {
      return sortDims(ret);
    }
    else {
      return ret;
    }
  }

  // @Override
  public double XXprojectScaledToRender(int dim, double d) {
    if(isInverted(dim)) {
      return d;
    }
    else {
      return 1 - d;
    }
  }

  // @Override
  public Vector XXprojectRenderToScaled(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      if(isInverted(i)) {
        ret.set(i, v.get(i));
      }
      else {
        ret.set(i, 1 - v.get(i));
      }
    }
    return sortDims(ret);
  }

  // @Override
  public Vector XXprojectRelativeScaledToRender(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      ret.set(i, -v.get(i));
    }
    return sortDims(ret);
  }

  // @Override
  public Vector XXprojectRelativeRenderToScaled(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      ret.set(i, v.get(i));
    }
    return sortDims(ret);
  }

  // @Override
  public Vector XXprojectDataToRenderSpace(NumberVector<?, ?> data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  // @Override
  public Vector XXprojectDataToRenderSpace(NumberVector<?, ?> data, boolean sort) {
    return YYprojectScaledToRender(projectDataToScaledSpace(data), sort);
  }

  // @Override
  public Vector XXprojectDataToRenderSpace(Vector data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  // @Override
  public <NV extends NumberVector<NV, ?>> NV XXprojectRenderToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = projectRenderToScaled(v);
    double[] ds = vec.getArrayRef();
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getUnscaled(ds[d]);
    }
    return prototype.newNumberVector(vec.getArrayRef());
  }

  // @Override
  public Vector XXprojectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  // @Override
  public Vector XXprojectRelativeDataToRenderSpace(Vector data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  /*
   * @Override public <NV extends NumberVector<NV, ?>> NV
   * projectRelativeScaledToDataSpace(Vector v, NV prototype) { return null; }
   */

  // @Override
  public <NV extends NumberVector<NV, ?>> NV XXprojectRelativeRenderToDataSpace(Vector v, NV prototype) {
    final int dim = v.getDimensionality();
    Vector vec = projectRelativeRenderToScaled(v);
    double[] ds = vec.getArrayRef();
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      ds[d] = scales[d].getRelativeUnscaled(ds[d]);
    }
    return prototype.newNumberVector(vec.getArrayRef());
  }

  @Override
  public double projectDimension(int dim, double value) {
    double temp = scales[dimOrder[dim]].getScaled(value);
    if(isInverted(dimOrder[dim])) {
      return temp;
    }
    return 1 - temp;
  }

  @Override
  public double getXpos(int dim) {
    if(dim < 0 || dim > dims) {
      return -1;
    }
    int notvis = 0;
    if(isVisible(dimOrder[dim]) == false) {
      return -1.0;
    }
    for(int i = 0; i < dim; i++) {
      if(isVisible(dimOrder[i]) == false) {
        notvis++;
      }
    }
    return (dim - notvis) / (double) dims;
  }

  @Override
  public int getDimensionNumber(int pos) {
    return dimOrder[pos];
  }

  @Override
  public int getDimensionsPosition(int dim) {
    for(int i = 0; i < dimOrder.length; i++) {
      if(dimOrder[i] == dim) {
        return i;
      }
    }
    return -1;
  }

  protected Vector sortDims(Vector s) {
    Vector ret = new Vector(s.getDimensionality());
    for(int i = 0; i < s.getDimensionality(); i++) {
      ret.set(i, s.get(dimOrder[i]));
    }
    return ret;
  }

  @Override
  public int getInputDimensionality() {
    return dims;
  }

  @Override
  public Vector projectScaledToRender(Vector v) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRenderToScaled(Vector v) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRelativeScaledToRender(Vector v) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRelativeRenderToScaled(Vector v) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectDataToScaledSpace(NumberVector<?, ?> data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectDataToScaledSpace(Vector data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRelativeDataToScaledSpace(NumberVector<?, ?> data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRelativeDataToScaledSpace(Vector data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectDataToRenderSpace(Vector data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectScaledToDataSpace(Vector v, NV factory) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRenderToDataSpace(Vector v, NV prototype) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public Vector projectRelativeDataToRenderSpace(Vector data) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeScaledToDataSpace(Vector v, NV prototype) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeRenderToDataSpace(Vector v, NV prototype) {
    throw new UnsupportedOperationException("not yet implemented.");
  }
}