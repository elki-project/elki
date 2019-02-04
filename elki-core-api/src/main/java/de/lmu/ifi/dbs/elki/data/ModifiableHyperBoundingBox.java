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
package de.lmu.ifi.dbs.elki.data;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;

/**
 * MBR class allowing modifications (as opposed to {@link HyperBoundingBox}).
 *
 * @author Marisa Thoma
 * @since 0.3
 */
public class ModifiableHyperBoundingBox extends HyperBoundingBox {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1;

  /**
   * Constructor.
   */
  public ModifiableHyperBoundingBox() {
    super();
  }

  /**
   * Derive a bounding box from a spatial object.
   *
   * @param hbb existing hyperboundingbox
   */
  public ModifiableHyperBoundingBox(SpatialComparable hbb) {
    super(SpatialUtil.getMin(hbb), SpatialUtil.getMax(hbb));
  }

  /**
   * Creates a ModifiableHyperBoundingBox for the given hyper points.
   *
   * @param min - the coordinates of the minimum hyper point
   * @param max - the coordinates of the maximum hyper point
   */
  public ModifiableHyperBoundingBox(double[] min, double[] max) {
    if(min.length != max.length) {
      throw new IllegalArgumentException("min/max need same dimensionality");
    }
    this.min = min;
    this.max = max;
  }

  /**
   * Create a ModifiableHyperBoundingBox with given min and max.
   *
   * @param dim Dimensionality
   * @param min Minimum in each dimension
   * @param max Maximum in each dimension
   */
  public ModifiableHyperBoundingBox(int dim, double min, double max) {
    super();
    this.min = new double[dim];
    this.max = new double[dim];
    Arrays.fill(this.min, min);
    Arrays.fill(this.max, max);
  }

  /**
   * Set the maximum bound in dimension <code>dimension</code> to value
   * <code>value</code>.
   *
   * @param dimension the dimension for which the coordinate should be set,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @param value the coordinate to set as upper bound for dimension
   *        <code>dimension</code>
   */
  public void setMax(int dimension, double value) {
    max[dimension] = value;
  }

  /**
   * Set the minimum bound in dimension <code>dimension</code> to value
   * <code>value</code>.
   *
   * @param dimension the dimension for which the lower bound should be set,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @param value the coordinate to set as lower bound for dimension
   *        <code>dimension</code>
   */
  public void setMin(int dimension, double value) {
    min[dimension] = value;
  }

  /**
   * Returns a reference to the minimum hyper point.
   *
   * @return the minimum hyper point
   */
  public double[] getMinRef() {
    return min;
  }

  /**
   * Returns the reference to the maximum hyper point.
   *
   * @return the maximum hyper point
   */
  public double[] getMaxRef() {
    return max;
  }

  /**
   * Set the bounding box to the same as some other spatial object.
   *
   * @param obj Spatial object to set to.
   */
  public void set(SpatialComparable obj) {
    final int dim = min.length;
    assert (obj.getDimensionality() == dim);
    if(obj instanceof ModifiableHyperBoundingBox) {
      ModifiableHyperBoundingBox ho = (ModifiableHyperBoundingBox) obj;
      System.arraycopy(ho.getMinRef(), 0, min, 0, dim);
      System.arraycopy(ho.getMaxRef(), 0, max, 0, dim);
      return;
    }
    for(int i = 0; i < dim; i++) {
      min[i] = obj.getMin(i);
      max[i] = obj.getMax(i);
    }
  }

  /**
   * Extend the bounding box by some other spatial object.
   *
   * @param obj Spatial object to extend with
   * @return true when the MBR changed.
   */
  public boolean extend(SpatialComparable obj) {
    final int dim = min.length;
    assert (obj.getDimensionality() == dim);
    boolean extended = false;
    for(int i = 0; i < dim; i++) {
      final double omin = obj.getMin(i);
      final double omax = obj.getMax(i);
      if(omin < min[i]) {
        min[i] = omin;
        extended = true;
      }
      if(omax > max[i]) {
        max[i] = omax;
        extended = true;
      }
    }
    return extended;
  }
}
