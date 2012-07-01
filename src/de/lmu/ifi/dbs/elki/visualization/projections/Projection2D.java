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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Projections that have specialized methods to only compute the first two
 * dimensions of the projection.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 *
 * @apiviz.has CanvasSize
 */
public interface Projection2D extends Projection {
  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectDataToRenderSpace(double[] data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public double[] fastProjectDataToScaledSpace(double[] data);

  /**
   * Project a data vector from data space to scaled space.
   * 
   * @param data vector in data space
   * @return vector in scaled space
   */
  public double[] fastProjectDataToScaledSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double[] fastProjectScaledToRenderSpace(double[] v);

  /**
   * Project a data vector from rendering space to data space.
   * 
   * @param data vector in rendering space
   * @return vector in data space
   */
  public double[] fastProjectRenderToDataSpace(double[] data);

  /**
   * Project a data vector from rendering space to data space.
   * 
   * @param data vector in rendering space
   * @param prototype Prototype to create vector from
   * @return vector in data space
   */
  // public <V extends NumberVector<V, ?>> V fastProjectRenderToDataSpace(double[] data, V prototype);

  /**
   * Project a vector from rendering space to scaled space.
   * 
   * @param v vector in rendering space
   * @return vector in scaled space
   */
  public double[] fastProjectRenderToScaledSpace(double[] v);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeDataToRenderSpace(double[] data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeScaledToRenderSpace(double[] v);

  // FIXME: add missing relative projection functions
  
  /**
   * Estimate the viewport requirements
   * 
   * @return Canvas size obtained from projecting scale endpoints
   */
  public CanvasSize estimateViewport();

  /**
   * Get a bit set of dimensions that are visible.
   * 
   * @return Bit set, first dimension is bit 0.
   */
  public BitSet getVisibleDimensions2D();
}
