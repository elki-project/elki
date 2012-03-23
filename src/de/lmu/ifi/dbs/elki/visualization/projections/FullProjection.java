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
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Full vector space projections.
 * 
 * These rather portable projections offer a large choice of functions, at the
 * cost of often being a bit slower than the low level functions.
 * 
 * Note: this interface and methods may be removed, unless there is a clear use
 * case for them as opposed to always using the low-level fast projections.
 * 
 * @author Erich Schubert
 */
public interface FullProjection extends Projection {
  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public Vector projectScaledToRender(Vector v);

  /**
   * Project a vector from rendering space to scaled space.
   * 
   * @param v vector in rendering space
   * @return vector in scaled space
   */
  public Vector projectRenderToScaled(Vector v);

  /**
   * Project a relative vector from scaled space to rendering space.
   * 
   * @param v relative vector in scaled space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeScaledToRender(Vector v);

  /**
   * Project a relative vector from rendering space to scaled space.
   * 
   * @param v relative vector in rendering space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeRenderToScaled(Vector v);

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public Vector projectDataToScaledSpace(NumberVector<?, ?> data);

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public Vector projectDataToScaledSpace(Vector data);

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeDataToScaledSpace(NumberVector<?, ?> data);

  /**
   * Project a relative data vector from data space to scaled space.
   * 
   * @param data relative vector in data space
   * @return relative vector in scaled space
   */
  public Vector projectRelativeDataToScaledSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public Vector projectDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public Vector projectDataToRenderSpace(Vector data);

  /**
   * Project a vector from scaled space to data space.
   * 
   * @param <NV> Vector type
   * @param v vector in scaled space
   * @param factory Object factory
   * @return vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectScaledToDataSpace(Vector v, NV factory);

  /**
   * Project a vector from rendering space to data space.
   * 
   * @param <NV> Vector type
   * @param v vector in rendering space
   * @param prototype Object factory
   * @return vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectRenderToDataSpace(Vector v, NV prototype);

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a relative data vector from data space to rendering space.
   * 
   * @param data relative vector in data space
   * @return relative vector in rendering space
   */
  public Vector projectRelativeDataToRenderSpace(Vector data);

  /**
   * Project a relative vector from scaled space to data space.
   * 
   * @param <NV> Vector type
   * @param v relative vector in scaled space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectRelativeScaledToDataSpace(Vector v, NV prototype);

  /**
   * Project a relative vector from rendering space to data space.
   * 
   * @param <NV> Vector type
   * @param v relative vector in rendering space
   * @param prototype Object factory
   * @return relative vector in data space
   */
  public <NV extends NumberVector<NV, ?>> NV projectRelativeRenderToDataSpace(Vector v, NV prototype);
}
