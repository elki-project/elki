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
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;

/**
 * Projection to parallel coordinates.
 * 
 * @author Robert Rödler
 */
public interface ProjectionParallel extends Projection {
  public boolean isVisible(int dim);

  public void setVisible(boolean vis, int dim);

  public int getVisibleDimensions();

  public int getFirstVisibleDimension();

  public double projectDimension(int dim, double value);

  public int getLastVisibleDimension();

  public int getPrevVisibleDimension(int dim);

  public int getNextVisibleDimension(int dim);

  public void swapDimensions(int a, int b);

  /**
   * shift a dimension to another position
   * 
   * @param dim dimension to shift
   * @param rn new position
   */
  public void shiftDimension(int dim, int rn);

  public int getDimensionNumber(int pos);

  public void toggleInverted(int dim);

  public void setInverted(int dim, boolean bool);

  public boolean isInverted(int dim);
  
  public double[] fastProjectDataToRenderSpace(Vector v);

  public double[] fastProjectDataToRenderSpace(NumberVector<?, ?> v);

  //public double projectScaledToRender(int dim, double d);

  //public Vector projectDataToRenderSpace(NumberVector<?, ?> data, boolean sort);

  public int findDimensionPosition(int dim);

  @Deprecated
  double getXpos(int dim);
}