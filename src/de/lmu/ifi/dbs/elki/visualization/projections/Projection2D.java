package de.lmu.ifi.dbs.elki.visualization.projections;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Projections that have specialized methods to only compute the first two
 * dimensions of the projection.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public interface Projection2D extends Projection {
  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectDataToRenderSpace(Vector data);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> data);

  /**
   * Project a vector from scaled space to rendering space.
   * 
   * @param v vector in scaled space
   * @return vector in rendering space
   */
  public double[] fastProjectScaledToRender(Vector v);

  /**
   * Project a data vector from data space to rendering space.
   * 
   * @param data vector in data space
   * @return vector in rendering space
   */
  public double[] fastProjectRelativeDataToRenderSpace(Vector data);

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
  public double[] fastProjectRelativeScaledToRender(Vector v);

  /**
   * Estimate the viewport requirements
   * 
   * @return MinMax for x and y obtained from projecting scale endpoints
   */
  public Pair<DoubleMinMax, DoubleMinMax> estimateViewport();

  /**
   * Get a SVG transformation string to bring the contents into the unit cube.
   * 
   * @param margin extra margin to add.
   * @param width Width
   * @param height Height
   * @return transformation string.
   */
  public String estimateTransformString(double margin, double width, double height);

  /**
   * Get a bit set of dimensions that are visible.
   * 
   * @return Bit set, first dimension is bit 0.
   */
  public BitSet getVisibleDimensions2D();
}