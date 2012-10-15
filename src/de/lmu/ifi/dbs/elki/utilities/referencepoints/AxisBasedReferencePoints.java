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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Strategy to pick reference points by placing them on the axis ends.
 * 
 * This strategy produces n+2 reference points that lie on the edges of the
 * surrounding cube.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class AxisBasedReferencePoints<V extends NumberVector<?>> implements ReferencePointsHeuristic<V> {
  /**
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -axisref.scale}
   * </p>
   */
  public static final OptionID SPACE_SCALE_ID = OptionID.getOrCreateOptionID("axisref.scale", "Scale the data space extension by the given factor.");

  /**
   * Holds the value of {@link #SPACE_SCALE_ID}.
   */
  protected double spacescale;

  /**
   * Constructor.
   * 
   * @param spacescale Extra scaling
   */
  public AxisBasedReferencePoints(double spacescale) {
    super();
    this.spacescale = spacescale;
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Relation<T> db) {
    Relation<V> database = DatabaseUtil.relationUglyVectorCast(db);
    Pair<V, V> minmax = DatabaseUtil.computeMinMax(database);
    NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(database);

    int dim = RelationUtil.dimensionality(db);

    // Compute mean and extend from minmax.
    double[] mean = new double[dim];
    double[] delta = new double[dim];
    for (int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.doubleValue(d) + minmax.second.doubleValue(d)) * .5;
      delta[d] = spacescale * (minmax.second.doubleValue(d) - mean[d]);
    }

    ArrayList<V> result = new ArrayList<V>(2 + dim);

    double[] vec = new double[dim];
    // Use min and max
    for (int d = 0; d < dim; d++) {
      vec[d] = mean[d] - delta[d];
    }
    result.add(factory.newNumberVector(vec));
    for (int d = 0; d < dim; d++) {
      vec[d] = mean[d] + delta[d];
    }
    result.add(factory.newNumberVector(vec));

    // Plus axis end points:
    for (int i = 0; i < dim; i++) {
      for (int d = 0; d < dim; d++) {
        if (d != i) {
          vec[d] = mean[d] - delta[d];
        } else {
          vec[d] = mean[d] + delta[d];
        }
      }
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
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Holds the value of {@link #SPACE_SCALE_ID}.
     */
    protected double spacescale = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter spacescaleP = new DoubleParameter(SPACE_SCALE_ID, 1.0);
      spacescaleP.addConstraint(new GreaterEqualConstraint(0.0));
      if (config.grab(spacescaleP)) {
        spacescale = spacescaleP.getValue();
      }
    }

    @Override
    protected AxisBasedReferencePoints<V> makeInstance() {
      return new AxisBasedReferencePoints<V>(spacescale);
    }
  }
}
