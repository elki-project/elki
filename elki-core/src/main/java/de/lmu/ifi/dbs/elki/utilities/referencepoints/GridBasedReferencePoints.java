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
package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Grid-based strategy to pick reference points.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class GridBasedReferencePoints implements ReferencePointsHeuristic {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GridBasedReferencePoints.class);

  /**
   * Holds the grid resolution.
   */
  protected int gridres;

  /**
   * Holds the grid scale.
   */
  protected double gridscale;

  /**
   * Constructor.
   * 
   * @param gridres Grid resolution
   * @param gridscale Grid scaling
   */
  public GridBasedReferencePoints(int gridres, double gridscale) {
    super();
    this.gridres = gridres;
    this.gridscale = gridscale;
  }

  @Override
  public Collection<? extends NumberVector> getReferencePoints(Relation<? extends NumberVector> db) {
    double[][] minmax = RelationUtil.computeMinMax(db);
    final int dim = minmax[0].length;

    // Compute mean from minmax.
    double[] mean = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax[0][d] + minmax[1][d]) * .5;
    }

    if(gridres <= 0) {
      LOG.warning("Grid of resolution " + gridres + " will have a single point only.");
      ArrayList<DoubleVector> result = new ArrayList<>(1);
      result.add(DoubleVector.wrap(mean));
      return result;
    }
    final int grids = gridres + 1;
    final int gridpoints = MathUtil.ipowi(grids, dim);
    if(gridpoints < 0) {
      throw new AbortException("Grids with more than 2^31 are not supported, or meaningful.");
    }
    if(gridpoints > db.size()) {
      LOG.warning("Grid has " + gridpoints + " points, but you only have " + db.size() + " observations.");
    }
    ArrayList<DoubleVector> result = new ArrayList<>(gridpoints);
    double[] delta = new double[dim];
    for(int d = 0; d < dim; d++) {
      delta[d] = (minmax[1][d] - minmax[0][d]) / gridres;
    }

    double halfgrid = gridres * .5;
    for(int i = 0; i < gridpoints; i++) {
      double[] vec = new double[dim];
      int acc = i;
      for(int d = 0; d < dim; d++) {
        int coord = acc % grids;
        acc /= grids;
        vec[d] = mean[d] + (coord - halfgrid) * delta[d] * gridscale;
      }
      result.add(DoubleVector.wrap(vec));
    }
    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    // TODO: add "grid sampling" option.

    /**
     * Parameter to specify the grid resolution.
     */
    public static final OptionID GRID_ID = new OptionID("grid.size", "The number of partitions in each dimension. Points will be placed on the edges of the grid, except for a grid size of 0, where only the mean is generated as reference point.");

    /**
     * Parameter to specify the extra scaling of the space, to allow
     * out-of-data-space reference points.
     */
    public static final OptionID GRID_SCALE_ID = new OptionID("grid.scale", "Scale the grid by the given factor. This can be used to obtain reference points outside the used data space.");

    /**
     * Holds the value of {@link #GRID_ID}.
     */
    protected int gridres;

    /**
     * Holds the value of {@link #GRID_SCALE_ID}.
     */
    protected double gridscale;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter gridP = new IntParameter(GRID_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(gridP)) {
        gridres = gridP.getValue();
      }

      DoubleParameter gridscaleP = new DoubleParameter(GRID_SCALE_ID, 1.0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(gridscaleP)) {
        gridscale = gridscaleP.getValue();
      }
    }

    @Override
    protected GridBasedReferencePoints makeInstance() {
      return new GridBasedReferencePoints(gridres, gridscale);
    }
  }
}
