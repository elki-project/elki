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
package de.lmu.ifi.dbs.elki.algorithm.clustering.affinitypropagation;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
  DistanceFunction<? super O> distance;

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
  public DistanceBasedInitializationWithMedian(DistanceFunction<? super O> distance, double quantile) {
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
    DistanceFunction<? super O> distance;

    /**
     * Quantile to use.
     */
    double quantile;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> param = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class, SquaredEuclideanDistanceFunction.class);
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
