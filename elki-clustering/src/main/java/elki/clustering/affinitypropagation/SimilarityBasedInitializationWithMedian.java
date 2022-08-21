/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.query.QueryBuilder;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;
import elki.similarity.Similarity;
import elki.similarity.kernel.LinearKernel;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Similarity based initialization.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <O> Object type
 */
public class SimilarityBasedInitializationWithMedian<O> implements AffinityPropagationInitialization<O> {
  /**
   * Similarity function.
   */
  Similarity<? super O> similarity;

  /**
   * Quantile to use.
   */
  double quantile;

  /**
   * Constructor.
   * 
   * @param similarity Similarity function
   * @param quantile Quantile
   */
  public SimilarityBasedInitializationWithMedian(Similarity<? super O> similarity, double quantile) {
    super();
    this.similarity = similarity;
    this.quantile = quantile;
  }

  @Override
  public double[][] getSimilarityMatrix(Relation<O> relation, ArrayDBIDs ids) {
    final int size = ids.size();
    SimilarityQuery<O> sq = new QueryBuilder<>(relation, similarity).similarityQuery();
    double[][] mat = new double[size][size];
    double[] flat = new double[(size * (size - 1)) >> 1];
    DBIDArrayIter i1 = ids.iter(), i2 = ids.iter();
    // Compute self-similarities first, for centering:
    for(int i = 0; i < size; i++, i1.advance()) {
      mat[i][i] = sq.similarity(i1, i1) * .5;
    }
    i1.seek(0);
    int j = 0;
    for(int i = 0; i < size; i++, i1.advance()) {
      final double[] mati = mat[i]; // Probably faster access.
      i2.seek(i + 1);
      for(int k = i + 1; k < size; k++, i2.advance()) {
        mati[k] = sq.similarity(i1, i2) - mati[i] - mat[k][k];
        mat[k][i] = mati[k]; // symmetry.
        flat[j++] = mati[k];
      }
    }
    assert j == (size * (size - 1)) >>> 1;
    double median = QuickSelect.quantile(flat, quantile);
    // On the diagonal, we place the median
    for(int i = 0; i < size; i++) {
      mat[i][i] = median;
    }
    return mat;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return similarity.getInputTypeRestriction();
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
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for the similarity function.
     */
    public static final OptionID SIMILARITY_ID = new OptionID("ap.similarity", "Similarity function to use.");

    /**
     * Similarity function.
     */
    Similarity<? super O> similarity;

    /**
     * Quantile to use.
     */
    double quantile;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Similarity<? super O>>(SIMILARITY_ID, Similarity.class, LinearKernel.class) //
          .grab(config, x -> similarity = x);
      new DoubleParameter(QUANTILE_ID, .5) //
          .grab(config, x -> quantile = x);
    }

    @Override
    public SimilarityBasedInitializationWithMedian<O> make() {
      return new SimilarityBasedInitializationWithMedian<>(similarity, quantile);
    }
  }
}
