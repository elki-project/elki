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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Star-based strategy to pick reference points.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class StarBasedReferencePoints implements ReferencePointsHeuristic {
  /**
   * Exclude the center vector.
   */
  protected boolean nocenter;

  /**
   * Scaling factor.
   */
  protected double scale;

  /**
   * Constructor.
   * 
   * @param nocenter Do not include center point
   * @param scale Scaling factor
   */
  public StarBasedReferencePoints(boolean nocenter, double scale) {
    super();
    this.nocenter = nocenter;
    this.scale = scale;
  }

  @Override
  public Collection<? extends NumberVector> getReferencePoints(Relation<? extends NumberVector> db) {
    int dim = RelationUtil.dimensionality(db);

    // Compute minimum, maximum and centroid
    double[] centroid = new double[dim];
    double[] min = new double[dim];
    double[] max = new double[dim];
    for(int d = 0; d < dim; d++) {
      centroid[d] = 0;
      min[d] = Double.MAX_VALUE;
      max[d] = -Double.MAX_VALUE;
    }
    for(DBIDIter iditer = db.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector obj = db.get(iditer);
      for(int d = 0; d < dim; d++) {
        double val = obj.doubleValue(d);
        centroid[d] += val;
        min[d] = Math.min(min[d], val);
        max[d] = Math.max(max[d], val);
      }
    }
    // finish centroid, scale min, max
    for(int d = 0; d < dim; d++) {
      centroid[d] = centroid[d] / db.size();
      min[d] = (min[d] - centroid[d]) * scale + centroid[d];
      max[d] = (max[d] - centroid[d]) * scale + centroid[d];
    }

    ArrayList<DoubleVector> result = new ArrayList<>(2 * dim + 1);
    if(!nocenter) {
      result.add(DoubleVector.wrap(centroid));
    }
    // Plus axis end points through centroid
    for(int i = 0; i < dim; i++) {
      double[] vec = centroid.clone();
      vec[i] = min[i];
      result.add(DoubleVector.wrap(vec));
      vec = centroid.clone();
      vec[i] = max[i];
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
    /**
     * Parameter to specify the grid resolution.
     */
    public static final OptionID NOCENTER_ID = new OptionID("star.nocenter", "Do not use the center as extra reference point.");

    /**
     * Parameter to specify the extra scaling of the space, to allow
     * out-of-data-space reference points.
     */
    public static final OptionID SCALE_ID = new OptionID("star.scale", "Scale the reference points by the given factor. This can be used to obtain reference points outside the used data space.");

    /**
     * Holds the value of {@link #NOCENTER_ID}.
     */
    protected boolean nocenter;

    /**
     * Holds the value of {@link #SCALE_ID}.
     */
    protected double scale;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag nocenterF = new Flag(NOCENTER_ID);
      if(config.grab(nocenterF)) {
        nocenter = nocenterF.getValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID, 1.0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(scaleP)) {
        scale = scaleP.getValue();
      }
    }

    @Override
    protected StarBasedReferencePoints makeInstance() {
      return new StarBasedReferencePoints(nocenter, scale);
    }
  }
}
