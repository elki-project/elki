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
 * Abstract base class for full projections.
 *
 * Note: the full projection API may be removed at some point, unless we find a
 * clear use case that cannot be done by the low level fast projections.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractFullProjection extends AbstractProjection implements FullProjection {
  /**
   * Constructor.
   *
   * @param p Projector
   * @param scales Scales
   */
  public AbstractFullProjection(Projector p, LinearScale[] scales) {
    super(p, scales);
  }

  /**
   * Project a data vector from data space to scaled space.
   *
   * @param data vector in data space
   * @return vector in scaled space
   */
  @Override
  public double[] projectDataToScaledSpace(NumberVector data) {
    final int dim = data.getDimensionality();
    double[] vec = new double[dim];
    for(int d = 0; d < dim; d++) {
      vec[d] = scales[d].getScaled(data.doubleValue(d));
    }
    return vec;
  }

  /**
   * Project a data vector from data space to scaled space.
   *
   * @param data vector in data space
   * @return vector in scaled space
   */
  @Override
  public double[] projectDataToScaledSpace(double[] data) {
    final int dim = data.length;
    double[] dst = new double[dim];
    for(int d = 0; d < dim; d++) {
      dst[d] = scales[d].getScaled(data[d]);
    }
    return dst;
  }

  /**
   * Project a relative data vector from data space to scaled space.
   *
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  @Override
  public double[] projectRelativeDataToScaledSpace(NumberVector data) {
    final int dim = data.getDimensionality();
    double[] vec = new double[dim];
    for(int d = 0; d < dim; d++) {
      vec[d] = scales[d].getRelativeScaled(data.doubleValue(d));
    }
    return vec;
  }

  /**
   * Project a relative data vector from data space to scaled space.
   *
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  @Override
  public double[] projectRelativeDataToScaledSpace(double[] data) {
    final int dim = data.length;
    double[] dst = new double[dim];
    for(int d = 0; d < dim; d++) {
      dst[d] = scales[d].getRelativeScaled(data[d]);
    }
    return dst;
  }

  /**
   * Project a data vector from data space to rendering space.
   *
   * @param data vector in data space
   * @return vector in rendering space
   */
  @Override
  public double[] projectDataToRenderSpace(NumberVector data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  /**
   * Project a data vector from data space to rendering space.
   *
   * @param data vector in data space
   * @return vector in rendering space
   */
  @Override
  public double[] projectDataToRenderSpace(double[] data) {
    return projectScaledToRender(projectDataToScaledSpace(data));
  }

  /**
   * Project a relative data vector from data space to rendering space.
   *
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  @Override
  public double[] projectRelativeDataToRenderSpace(NumberVector data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  /**
   * Project a relative data vector from data space to rendering space.
   *
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  @Override
  public double[] projectRelativeDataToRenderSpace(double[] data) {
    return projectRelativeScaledToRender(projectRelativeDataToScaledSpace(data));
  }

  /**
   * Project a vector from scaled space to data space.
   *
   * @param <NV> Vector type
   * @param v vector in scaled space
   * @param factory Object factory
   * @return vector in data space
   */
  @Override
  public <NV extends NumberVector> NV projectScaledToDataSpace(double[] v, NumberVector.Factory<NV> factory) {
    final int dim = v.length;
    double[] vec = new double[dim];
    for(int d = 0; d < dim; d++) {
      vec[d] = scales[d].getUnscaled(v[d]);
    }
    return factory.newNumberVector(vec);
  }

  /**
   * Project a vector from rendering space to data space.
   *
   * @param <NV> Vector type
   * @param v vector in rendering space
   * @param prototype Object factory
   * @return vector in data space
   */
  @Override
  public <NV extends NumberVector> NV projectRenderToDataSpace(double[] v, NumberVector.Factory<NV> prototype) {
    final int dim = v.length;
    double[] vec = projectRenderToScaled(v);
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      vec[d] = scales[d].getUnscaled(vec[d]);
    }
    return prototype.newNumberVector(vec);
  }

  /**
   * Project a relative vector from scaled space to data space.
   *
   * @param <NV> Vector type
   * @param v relative vector in scaled space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  @Override
  public <NV extends NumberVector> NV projectRelativeScaledToDataSpace(double[] v, NumberVector.Factory<NV> prototype) {
    final int dim = v.length;
    double[] vec = new double[dim];
    for(int d = 0; d < dim; d++) {
      vec[d] = scales[d].getRelativeUnscaled(v[d]);
    }
    return prototype.newNumberVector(vec);
  }

  /**
   * Project a relative vector from rendering space to data space.
   *
   * @param <NV> Vector type
   * @param v relative vector in rendering space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  @Override
  public <NV extends NumberVector> NV projectRelativeRenderToDataSpace(double[] v, NumberVector.Factory<NV> prototype) {
    final int dim = v.length;
    double[] vec = projectRelativeRenderToScaled(v);
    // Not calling {@link #projectScaledToDataSpace} to avoid extra copy of
    // vector.
    for(int d = 0; d < dim; d++) {
      vec[d] = scales[d].getRelativeUnscaled(vec[d]);
    }
    return prototype.newNumberVector(vec);
  }
}
