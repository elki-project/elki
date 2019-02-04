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

/**
 * Projections that have specialized methods to only compute the first two
 * dimensions of the projection.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @has - - - CanvasSize
 */
public interface Projection2D extends Projection {
  /**
   * Project a data vector from data space to rendering space.
   *
   * @param data vector in data space
   * @return vector in rendering space
   */
  double[] fastProjectDataToRenderSpace(double[] data);

  /**
   * Project a data vector from data space to rendering space.
   *
   * @param data vector in data space
   * @return vector in rendering space
   */
  double[] fastProjectDataToRenderSpace(NumberVector data);

  /**
   * Project a data vector from data space to scaled space.
   *
   * @param data vector in data space
   * @return vector in scaled space
   */
  double[] fastProjectDataToScaledSpace(double[] data);

  /**
   * Project a data vector from data space to scaled space.
   *
   * @param data vector in data space
   * @return vector in scaled space
   */
  double[] fastProjectDataToScaledSpace(NumberVector data);

  /**
   * Project a vector from scaled space to rendering space.
   *
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  double[] fastProjectScaledToRenderSpace(double[] v);

  /**
   * Project a data vector from rendering space to data space.
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @return vector in data space
   */
  double[] fastProjectRenderToDataSpace(double x, double y);

  /**
   * Project a vector from rendering space to scaled space.
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @return vector in scaled space
   */
  double[] fastProjectRenderToScaledSpace(double x, double y);

  /**
   * Project a data vector from data space to rendering space.
   *
   * @param data vector in data space
   * @return vector in rendering space
   */
  double[] fastProjectRelativeDataToRenderSpace(double[] data);

  /**
   * Project a data vector from data space to rendering space.
   *
   * @param data vector in data space
   * @return vector in rendering space
   */
  double[] fastProjectRelativeDataToRenderSpace(NumberVector data);

  /**
   * Project a vector from scaled space to rendering space.
   *
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  double[] fastProjectRelativeScaledToRenderSpace(double[] v);

  // FIXME: add missing relative projection functions

  /**
   * Estimate the viewport requirements
   *
   * @return Canvas size obtained from projecting scale endpoints
   */
  CanvasSize estimateViewport();

  /**
   * Get a bit set of dimensions that are visible.
   *
   * @return Bit set, first dimension is bit 0.
   */
  long[] getVisibleDimensions2D();
}
