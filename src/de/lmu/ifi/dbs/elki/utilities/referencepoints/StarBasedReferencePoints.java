package de.lmu.ifi.dbs.elki.utilities.referencepoints;

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

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Star-based strategy to pick reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Object type
 */
public class StarBasedReferencePoints<V extends NumberVector<V, ?>> implements ReferencePointsHeuristic<V> {
  /**
   * Parameter to specify the grid resolution.
   * <p>
   * Key: {@code -star.nocenter}
   * </p>
   */
  public static final OptionID NOCENTER_ID = OptionID.getOrCreateOptionID("star.nocenter", "Do not use the center as extra reference point.");

  /**
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -star.scale}
   * </p>
   */
  public static final OptionID SCALE_ID = OptionID.getOrCreateOptionID("star.scale", "Scale the reference points by the given factor. This can be used to obtain reference points outside the used data space.");

  /**
   * Holds the value of {@link #NOCENTER_ID}.
   */
  protected boolean nocenter;

  /**
   * Holds the value of {@link #SCALE_ID}.
   */
  protected double scale;

  /**
   * Constructor.
   * 
   * @param nocenter
   * @param scale
   */
  public StarBasedReferencePoints(boolean nocenter, double scale) {
    super();
    this.nocenter = nocenter;
    this.scale = scale;
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Relation<T> db) {
    Relation<V> database = DatabaseUtil.relationUglyVectorCast(db);
    V factory = DatabaseUtil.assumeVectorField(database).getFactory();

    int dim = DatabaseUtil.dimensionality(db);

    // Compute minimum, maximum and centroid
    double[] centroid = new double[dim];
    double[] min = new double[dim];
    double[] max = new double[dim];
    for(int d = 0; d < dim; d++) {
      centroid[d] = 0;
      min[d] = Double.MAX_VALUE;
      max[d] = -Double.MAX_VALUE;
    }
    for(DBID objID : database.iterDBIDs()) {
      V obj = database.get(objID);
      for(int d = 0; d < dim; d++) {
        double val = obj.doubleValue(d + 1);
        centroid[d] += val;
        min[d] = Math.min(min[d], val);
        max[d] = Math.max(max[d], val);
      }
    }
    // finish centroid, scale min, max
    for(int d = 0; d < dim; d++) {
      centroid[d] = centroid[d] / database.size();
      min[d] = (min[d] - centroid[d]) * scale + centroid[d];
      max[d] = (max[d] - centroid[d]) * scale + centroid[d];
    }

    ArrayList<V> result = new ArrayList<V>(2 * dim + 1);
    if(!nocenter) {
      result.add(factory.newNumberVector(centroid));
    }
    // Plus axis end points through centroid
    double[] vec = new double[dim];
    for(int i = 0; i < dim; i++) {
      for(int d = 0; d < dim; d++) {
        if(d != i) {
          vec[d] = centroid[d];
        }
      }
      vec[i] = min[i];
      result.add(factory.newNumberVector(vec));
      vec[i] = max[i];
      result.add(factory.newNumberVector(vec));
    }

    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
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

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID, new GreaterEqualConstraint(0.0), 1.0);
      if(config.grab(scaleP)) {
        scale = scaleP.getValue();
      }
    }

    @Override
    protected StarBasedReferencePoints<V> makeInstance() {
      return new StarBasedReferencePoints<V>(nocenter, scale);
    }
  }
}