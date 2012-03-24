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

/**
 * Projection to parallel coordinates that allows reordering and inversion of
 * axes.
 * 
 * Note: when using this projection, pay attention to the two different schemes
 * of dimension numbering: by input dimension and by axis position.
 * 
 * @author Robert Rödler
 * @author Erich Schubert
 */
public interface ProjectionParallel extends Projection {
  /**
   * Get inversion flag of axis.
   * 
   * @param axis Axis (reordered) position
   * @return Inversion flag
   */
  public boolean isAxisInverted(int axis);

  /**
   * Set inversion flag of axis.
   * 
   * @param axis Axis (reordered) position
   * @param bool Value of inversion flag
   */
  public void setAxisInverted(int axis, boolean bool);

  /**
   * Toggle inverted flag of axis.
   * 
   * @param axis Axis (reordered) position
   */
  public void toggleAxisInverted(int axis);

  /**
   * Get inversion flag of dimension.
   * 
   * @param truedim Dimension in original numbering
   * @return Inversion flag
   */
  public boolean isDimInverted(int truedim);

  /**
   * Set inversion flag of a dimension.
   * 
   * @param truedim Dimension in original numbering
   * @param bool Value of inversion flag
   */
  public void setDimInverted(int truedim, boolean bool);

  /**
   * Toggle inverted flag of dimension.
   * 
   * @param truedim Dimension in original numbering
   */
  public void toggleDimInverted(int truedim);

  /**
   * Get scale for the given axis
   * 
   * @param axis Axis (reordered) position
   * @return Axis scale
   */
  public LinearScale getAxisScale(int axis);

  /**
   * Test whether the current axis is visible
   * 
   * @param axis Axis (reordered) position
   * @return Visibility of axis
   */
  public boolean isAxisVisible(int axis);

  /**
   * Set the visibility of the axis.
   * 
   * @param axis Axis number
   * @param vis Visibility status
   */
  public void setAxisVisible(int axis, boolean vis);

  /**
   * Toggle visibility of the axis.
   * 
   * @param axis Axis number
   */
  public void toggleAxisVisible(int axis);

  /**
   * Get the number of visible dimension.
   * 
   * @return Number of visible dimensions
   */
  public int getVisibleDimensions();

  /**
   * Exchange axes A and B
   * @param a First axis
   * @param b Second axis
   */
  public void swapAxes(int a, int b);

  /**
   * shift a dimension to another position
   * 
   * @param dim dimension to shift
   * @param rn new position
   */
  public void moveAxis(int axis, int rn);

  /**
   * Get the dimension for the given axis number
   * 
   * @param axis Axis number
   * @return Dimension
   */
  public int getDimForAxis(int axis);

  /**
   * Get the dimension for the given visible axis
   * 
   * @param axis Axis number (visible axes only)
   * @return Dimension
   */
  public int getDimForVisibleAxis(int axis);

  /**
   * Fast project a vector from data to render space
   * 
   * @param v Input vector
   * @return Vector with reordering, inversions and scales applied.
   */
  public double[] fastProjectDataToRenderSpace(double[] v);

  /**
   * Fast project a vector from data to render space
   * 
   * @param v Input vector
   * @return Vector with reordering, inversions and scales applied.
   */
  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> v);

  /**
   * Project the value of a single axis to its display value
   * 
   * @param value Input value
   * @param axis Axis to use for scaling and inversion
   * @return Transformed value
   */
  public double fastProjectDataToRenderSpace(double value, int axis);

  /**
   * Project a display value back to the original data space
   * 
   * @param value transformed value
   * @param axis Axis to use for scaling and inversion
   * @return Original value
   */
  public double fastProjectRenderToDataSpace(double value, int axis);

  /**
   * Find the axis assinged to the given dimension.
   * 
   * @param truedim Dimension
   * @return Axis number
   */
  public int getAxisForDim(int truedim);
}