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
import de.lmu.ifi.dbs.elki.visualization.projections.AbstractProjection;

/**
 * Simple parallel projection
 * 
 * @author Robert Rödler
 */
public class SimpleParallel extends AbstractProjection implements ProjectionParallel {
  /**
   * Number of dimensions
   */
  final protected int dims;

  /**
   * visible dimensions
   */
  int visDims;

  /**
   * which dimensions are visible
   */
  boolean[] isVisible;

  /**
   * dimension order
   */
  int[] dimOrder;

  /**
   * dimension inverted?
   */
  boolean[] inverted;

  /**
   * Constructor.
   * 
   * @param scales Scales to use
   */
  public SimpleParallel(LinearScale[] scales) {
    super(scales);
    this.dims = scales.length;
    this.visDims = dims;
    isVisible = new boolean[dims];
    for(int i = 0; i < isVisible.length; i++) {
      isVisible[i] = true;
    }
    dimOrder = new int[dims];
    for(int i = 0; i < dimOrder.length; i++) {
      dimOrder[i] = i;
    }
    inverted = new boolean[dims];
    for(int i = 0; i < inverted.length; i++) {
      inverted[i] = false;
    }

  }

  @Override
  public int getFirstVisibleDimension() {
    for(int i = 0; i < dims; i++) {
      if(isVisible(dimOrder[i])) {
        return i;
      }
    }
    return 0;
  }

  @Override
  public int getVisibleDimensions() {
    return visDims;
  }

  @Override
  public LinearScale getScale(int d) {
    return scales[d];
  }

  @Override
  public boolean isVisible(int dim) {
    return isVisible[dimOrder[dim]];
  }

  @Override
  public void setVisible(boolean vis, int dim) {
    isVisible[dimOrder[dim]] = vis;
    if(vis == false) {
      visDims--;
    }
    else {
      visDims++;
    }
    // calcAxisPositions();
  }

  @Override
  public Vector projectScaledToRender(Vector v) {
    return projectScaledToRender(v, true);
  }

  @Override
  public Vector projectScaledToRender(Vector v, boolean sort) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      if(inverted[i]) {
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

  @Override
  public double projectScaledToRender(int dim, double d) {
    if(inverted[dim]) {
      return d;
    }
    else {
      return 1 - d;
    }
  }

  @Override
  public Vector projectRenderToScaled(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      if(inverted[i]) {
        ret.set(i, v.get(i));
      }
      else {
        ret.set(i, 1 - v.get(i));
      }
    }
    return sortDims(ret);
  }

  @Override
  public Vector projectRelativeScaledToRender(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      ret.set(i, -v.get(i));
    }
    return sortDims(ret);
  }

  @Override
  public Vector projectRelativeRenderToScaled(Vector v) {
    Vector ret = new Vector(v.getDimensionality());
    for(int i = 0; i < v.getDimensionality(); i++) {
      ret.set(i, v.get(i));
    }
    return sortDims(ret);
  }

  @Override
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  @Override
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data, boolean sort) {
    return projectScaledToRender(projectDataToScaledSpace(data), sort);
  }

  @Override
  public Vector projectDataToRenderSpace(Vector data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRenderToDataSpace(Vector v, NV prototype) {
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

  @Override
  public Vector projectRelativeDataToRenderSpace(NumberVector<?, ?> data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  @Override
  public Vector projectRelativeDataToRenderSpace(Vector data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  /*
   * @Override public <NV extends NumberVector<NV, ?>> NV
   * projectRelativeScaledToDataSpace(Vector v, NV prototype) { return null; }
   */

  @Override
  public <NV extends NumberVector<NV, ?>> NV projectRelativeRenderToDataSpace(Vector v, NV prototype) {
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
    if(inverted[dimOrder[dim]]) {
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
    if(isVisible[dimOrder[dim]] == false) {
      return -1.0;
    }
    for(int i = 0; i < dim; i++) {
      if(isVisible[dimOrder[i]] == false) {
        notvis++;
      }
    }
    return (dim - notvis) / (double) dims;
  }

  @Override
  public int getLastVisibleDimension() {
    for(int i = (isVisible.length - 1); i >= 0; i--) {
      if(isVisible[dimOrder[i]] == true) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getLastVisibleDimension(int dim) {
    for(int i = dim - 1; i >= 0; i--) {
      if(isVisible[dimOrder[i]] == true) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void swapDimensions(int a, int b) {
    int temp = dimOrder[a];
    dimOrder[a] = dimOrder[b];
    dimOrder[b] = temp;

  }

  @Override
  public void shiftDimension(int dim, int rn) {
    if(dim > rn) {
      int temp = dimOrder[dim];

      for(int i = dim; i > rn; i--) {
        dimOrder[i] = dimOrder[i - 1];
      }
      dimOrder[rn] = temp;
    }
    else if(dim < rn) {
      int temp = dimOrder[dim];

      for(int i = dim; i < rn - 1; i++) {
        dimOrder[i] = dimOrder[i + 1];
      }
      dimOrder[rn - 1] = temp;
    }
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

  @Override
  public Vector sortDims(Vector s) {
    Vector ret = new Vector(s.getDimensionality());
    for(int i = 0; i < s.getDimensionality(); i++) {
      ret.set(i, s.get(dimOrder[i]));
    }
    return ret;
  }

  @Override
  public int getNextVisibleDimension(int dim) {
    for(int i = dim + 1; i < dims; i++) {
      if(isVisible[dimOrder[i]] == true) {
        return i;
      }
    }
    return dim;
  }

  @Override
  public void setInverted(int dim) {
    inverted[dimOrder[dim]] = !inverted[dimOrder[dim]];

  }

  @Override
  public void setInverted(int dim, boolean bool) {
    inverted[dimOrder[dim]] = bool;

  }

  @Override
  public boolean isInverted(int dim) {
    return inverted[dimOrder[dim]];
  }

  @Override
  public LinearScale getLinearScale(int dim) {
    return scales[dim];
  }

  @Override
  public double getDimensions() {
    return dims;
  }
}