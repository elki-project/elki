package de.lmu.ifi.dbs.elki.math.dimensionsimilarity;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.outlier.meta.HiCS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Use the statistical tests as used by HiCS to arrange dimensions.
 *
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br />
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br />
 * Proceedings of the 2013 ACM International Conference on Management of Data
 * (SIGMOD), New York City, NY, 2013.
 * </p>
 *
 * Based on:
 * <p>
 * F. Keller, E. Müller, and K. Böhm.<br />
 * HiCS: High Contrast Subspaces for Density-Based Outlier Ranking. <br />
 * In ICDE, pages 1037–1048, 2012.
 * </p>
 *
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", //
booktitle = "Proc. of the 2013 ACM International Conference on Management of Data (SIGMOD)", //
url = "http://dx.doi.org/10.1145/2463676.2463696")
public class HiCSDimensionSimilarity implements DimensionSimilarity<NumberVector> {
  /**
   * Monte-Carlo iterations
   */
  private int m = 50;

  /**
   * Alpha threshold
   */
  private double alpha = 0.1;

  /**
   * Statistical test to use
   */
  private GoodnessOfFitTest statTest;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param statTest Test function
   * @param m Number of monte-carlo iterations
   * @param alpha Alpha threshold
   * @param rnd Random source
   */
  public HiCSDimensionSimilarity(GoodnessOfFitTest statTest, int m, double alpha, RandomFactory rnd) {
    super();
    this.statTest = statTest;
    this.m = m;
    this.alpha = alpha;
    this.rnd = rnd;
  }

  @Override
  public void computeDimensionSimilarites(Relation<? extends NumberVector> relation, DBIDs subset, DimensionSimilarityMatrix matrix) {
    final Random random = rnd.getSingleThreadedRandom();
    final int dim = matrix.size();

    // FIXME: only compute indexes necessary.
    ArrayList<ArrayDBIDs> subspaceIndex = buildOneDimIndexes(relation, subset, matrix);

    // compute two-element sets of subspaces
    for(int x = 0; x < dim; x++) {
      final int i = matrix.dim(x);
      for(int y = x + 1; y < dim; y++) {
        final int j = matrix.dim(y);
        matrix.set(x, y, calculateContrast(relation, subset, subspaceIndex.get(x), subspaceIndex.get(y), i, j, random));
      }
    }
  }

  /**
   * Calculates "index structures" for every attribute, i.e. sorts a
   * ModifiableArray of every DBID in the database for every dimension and
   * stores them in a list
   *
   * @param relation Relation to index
   * @param ids IDs to use
   * @param matrix Matrix (for dimension subset)
   * @return List of sorted objects
   */
  private ArrayList<ArrayDBIDs> buildOneDimIndexes(Relation<? extends NumberVector> relation, DBIDs ids, DimensionSimilarityMatrix matrix) {
    final int dim = matrix.size();
    ArrayList<ArrayDBIDs> subspaceIndex = new ArrayList<>(dim);

    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    for(int i = 0; i < dim; i++) {
      ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(ids);
      comp.setDimension(matrix.dim(i));
      amDBIDs.sort(comp);
      subspaceIndex.add(amDBIDs);
    }

    return subspaceIndex;
  }

  /**
   * Calculates the actual contrast of a given subspace
   *
   * @param relation Data relation
   * @param subset Subset to process
   * @param subspaceIndex1 Index of first subspace
   * @param subspaceIndex2 Index of second subspace
   * @param dim1 First dimension
   * @param dim2 Second dimension
   * @param random Random generator
   * @return Contrast
   */
  private double calculateContrast(Relation<? extends NumberVector> relation, DBIDs subset, ArrayDBIDs subspaceIndex1, ArrayDBIDs subspaceIndex2, int dim1, int dim2, Random random) {
    final double alpha1 = Math.sqrt(alpha);
    final int windowsize = (int) (relation.size() * alpha1);

    // TODO: speed up by keeping marginal distributions prepared.
    // Instead of doing the random switch, do half-half.
    double deviationSum = 0.0;
    for(int i = 0; i < m; i++) {
      // Randomly switch dimensions
      final int cdim1;
      ArrayDBIDs cindex1, cindex2;
      if(random.nextDouble() > .5) {
        cdim1 = dim1;
        cindex1 = subspaceIndex1;
        cindex2 = subspaceIndex2;
      }
      else {
        cdim1 = dim2;
        cindex1 = subspaceIndex2;
        cindex2 = subspaceIndex1;
      }
      // Build the sample
      DBIDArrayIter iter = cindex2.iter();
      HashSetModifiableDBIDs conditionalSample = DBIDUtil.newHashSet();
      iter.seek(random.nextInt(subset.size() - windowsize));
      for(int k = 0; k < windowsize && iter.valid(); k++, iter.advance()) {
        conditionalSample.add(iter);
      }
      // Project the data
      double[] fullValues = new double[subset.size()];
      double[] sampleValues = new double[conditionalSample.size()];
      {
        int l = 0, s = 0;
        // Note: we use the sorted index sets.
        for(DBIDIter id = cindex1.iter(); id.valid(); id.advance(), l++) {
          final double val = relation.get(id).doubleValue(cdim1);
          fullValues[l] = val;
          if(conditionalSample.contains(id)) {
            sampleValues[s] = val;
            s++;
          }
        }
        assert (s == conditionalSample.size());
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if(Double.isNaN(contrast)) {
        i--;
        continue;
      }
      deviationSum += contrast;
    }
    return deviationSum / m;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Statistical test to use
     */
    private GoodnessOfFitTest statTest;

    /**
     * Holds the value of
     * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.meta.HiCS.Parameterizer#M_ID}
     * .
     */
    private int m = 50;

    /**
     * Holds the value of
     * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.meta.HiCS.Parameterizer#ALPHA_ID}
     * .
     */
    private double alpha = 0.1;

    /**
     * Random generator.
     */
    private RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter mP = new IntParameter(HiCS.Parameterizer.M_ID, 50);
      mP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(mP)) {
        m = mP.intValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(HiCS.Parameterizer.ALPHA_ID, 0.1);
      alphaP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      final ObjectParameter<GoodnessOfFitTest> testP = new ObjectParameter<>(HiCS.Parameterizer.TEST_ID, GoodnessOfFitTest.class, KolmogorovSmirnovTest.class);
      if(config.grab(testP)) {
        statTest = testP.instantiateClass(config);
      }

      final RandomParameter rndP = new RandomParameter(HiCS.Parameterizer.SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected HiCSDimensionSimilarity makeInstance() {
      return new HiCSDimensionSimilarity(statTest, m, alpha, rnd);
    }
  }
}
