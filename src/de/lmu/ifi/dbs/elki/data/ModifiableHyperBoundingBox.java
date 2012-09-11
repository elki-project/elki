package de.lmu.ifi.dbs.elki.data;

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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

/**
 * MBR class allowing modifications (as opposed to {@link HyperBoundingBox}).
 * 
 * @author Marisa Thoma
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
   * Uses the references to the fields in <code>hbb</code> as <code>min</code>,
   * <code>max</code> fields. Thus, this constructor indirectly provides a way
   * to modify the fields of a {@link HyperBoundingBox}.
   * 
   * FIXME: that isn't really nice and should be handled with care.
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
   * Set the maximum bound in dimension <code>dimension</code> to value
   * <code>value</code>.
   * 
   * @param dimension the dimension for which the coordinate should be set,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @param value the coordinate to set as upper bound for dimension
   *        <code>dimension</code>
   */
  public void setMax(int dimension, double value) {
    max[dimension - 1] = value;
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
    max[dimension - 1] = value;
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
   * Extend the bounding box by some other spatial object.
   * 
   * @param obj Spatial object to extend with
   * @return true when the MBR changed.
   */
  public boolean extend(SpatialComparable obj) {
    final int dim = min.length;
    assert (!LoggingConfiguration.DEBUG || (obj.getDimensionality() == dim));
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