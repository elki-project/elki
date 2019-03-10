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
package elki.clustering.affinitypropagation;

import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.distance.distancefunction.minkowski.SquaredEuclideanDistance;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance based initialization.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <O> Object type
 */
public class DistanceBasedInitializationWithMedian<O> implements AffinityPropagationInitialization<O> {
  /**
   * Distance function.
   */
  Distance<? super O> distance;

  /**
   * Quantile to use.
   */
  double quantile;

  /**
   * Constructor.
   * 
   * @param distance Similarity function
   * @param quantile Quantile
   */
  public DistanceBasedInitializationWithMedian(Distance<? super O> distance, double quantile) {
    super();
    this.distance = distance;
    this.quantile = quantile;
  }

  @Override
  public double[][] getSimilarityMatrix(Database db, Relation<O> relation, ArrayDBIDs ids) {
    final int size = ids.size();
    DistanceQuery<O> dq = db.getDistanceQuery(relation, distance);
    double[][] mat = new double[size][size];
    double[] flat = new double[(size * (size - 1)) >> 1];
    DBIDArrayIter i1 = ids.iter(), i2 = ids.iter();
    for(int i = 0, j = 0; i < size; i++, i1.advance()) {
      double[] mati = mat[i];
      i2.seek(i + 1);
      for(int k = i + 1; k < size; k++, i2.advance()) {
        mati[k] = -dq.distance(i1, i2);
        mat[k][i] = mati[k]; // symmetry.
        flat[j] = mati[k];
        j++;
      }
    }
    double median = QuickSelect.quantile(flat, quantile);
    // On the diagonal, we place the median
    for(int i = 0; i < size; i++) {
      mat[i][i] = median;
    }
    return mat;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distance.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("ap.distance", "Distance function to use.");

    /**
     * istance function.
     */
    Distance<? super O> distance;

    /**
     * Quantile to use.
     */
    double quantile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Distance<? super O>> param = new ObjectParameter<>(DISTANCE_ID, Distance.class, SquaredEuclideanDistance.class);
      if(config.grab(param)) {
        distance = param.instantiateClass(config);
      }

      DoubleParameter quantileP = new DoubleParameter(QUANTILE_ID, .5);
      if(config.grab(quantileP)) {
        quantile = quantileP.doubleValue();
      }
    }

    @Override
    protected DistanceBasedInitializationWithMedian<O> makeInstance() {
      return new DistanceBasedInitializationWithMedian<>(distance, quantile);
    }
  }
}
