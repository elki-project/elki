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
package elki.outlier.anglebased;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.math.DoubleMinMax;
import elki.math.MeanVariance;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.similarity.Similarity;
import elki.similarity.kernel.KernelMatrix;
import elki.similarity.kernel.LinearKernel;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Fast-ABOD (approximateABOF) version of
 * Angle-Based Outlier Detection / Angle-Based Outlier Factor.
 * <p>
 * Note: the minimum k is 3. The 2 nearest neighbors yields one 1 angle, which
 * implies a constant 0 variance everywhere.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Matthias Schubert, Arthur Zimek<br>
 * Angle-Based Outlier Detection in High-dimensional Data<br>
 * Proc. 14th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining (KDD'08)
 *
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * @since 0.6.0
 *
 * @param <V> Vector type
 */
@Title("Approximate ABOD: Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "Hans-Peter Kriegel, Matthias Schubert, Arthur Zimek", //
    title = "Angle-Based Outlier Detection in High-dimensional Data", //
    booktitle = "Proc. 14th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining (KDD'08)", //
    url = "https://doi.org/10.1145/1401890.1401946", //
    bibkey = "DBLP:conf/kdd/KriegelSZ08")
public class FastABOD<V extends NumberVector> extends ABOD<V> {
  /**
   * Number of nearest neighbors.
   */
  protected int k;

  /**
   * Constructor for Angle-Based Outlier Detection (ABOD).
   *
   * @param kernelFunction kernel function to use
   * @param k Number of nearest neighbors
   */
  public FastABOD(Similarity<? super V> kernelFunction, int k) {
    super(kernelFunction);
    this.k = k;
  }

  /**
   * Run Fast-ABOD on the data set.
   *
   * @param relation Relation to process
   * @return Outlier detection result
   */
  @Override
  public OutlierResult run(Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();
    if(kernelFunction.getClass() == LinearKernel.class) {
      if(!kNNABOD(relation, ids, abodvalues, minmaxabod)) {
        // Fallback, if we do not have an index.
        fastABOD(relation, ids, abodvalues, minmaxabod);
      }
    }
    else {
      fastABOD(relation, ids, abodvalues, minmaxabod);
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Angle-Based Outlier Degree", relation.getDBIDs(), abodvalues);
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Simpler kNN based, can use more indexing.
   *
   * @param relation Data relation
   * @param ids IDs
   * @param abodvalues Score storage
   * @param minmaxabod Min/max storage
   * @return {@code true} if kNN were available and usable.
   */
  private boolean kNNABOD(Relation<V> relation, DBIDs ids, WritableDoubleDataStore abodvalues, DoubleMinMax minmaxabod) {
    int k1 = k + 1; // We will get the query point back by the knnq.
    DistanceQuery<V> dq = new QueryBuilder<>(relation, SquaredEuclideanDistance.STATIC).distanceQuery();
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(dq).optimizedOnly().kNNByDBID(k1);
    boolean squared = true;
    if(knnq == null) {
      dq = new QueryBuilder<>(relation, EuclideanDistance.STATIC).distanceQuery();
      if((knnq = new QueryBuilder<>(dq).optimizedOnly().kNNByDBID(k1)) == null) {
        return false;
      }
      squared = false;
    }
    SimilarityQuery<V> lk = new QueryBuilder<>(relation, (Similarity<? super V>) LinearKernel.STATIC).similarityQuery();

    MeanVariance s = new MeanVariance();
    for(DBIDIter pA = ids.iter(); pA.valid(); pA.advance()) {
      KNNList nl = knnq.getKNN(pA, k1);
      double simAA = lk.similarity(pA, pA);

      s.reset();
      DoubleDBIDListIter iB = nl.iter(), iC = nl.iter();
      for(; iB.valid(); iB.advance()) {
        double dAB = iB.doubleValue();
        double simAB = lk.similarity(pA, iB);
        if(!(dAB > 0.)) {
          continue;
        }
        for(iC.seek(iB.getOffset() + 1); iC.valid(); iC.advance()) {
          double dAC = iC.doubleValue();
          double simAC = lk.similarity(pA, iC);
          if(!(dAC > 0.)) {
            continue;
          }
          // Exploit bilinearity of scalar product:
          // <B-A, C-A> = <B, C-A> - <A,C-A>
          // = <B,C> - <B,A> - <A,C> + <A,A>
          double simBC = lk.similarity(iB, iC);
          double numerator = simBC - simAB - simAC + simAA;
          if(squared) {
            double div = 1. / (dAB * dAC);
            s.put(numerator * div, Math.sqrt(div));
          }
          else {
            double sqrtdiv = 1. / (dAB * dAC);
            s.put(numerator * sqrtdiv * sqrtdiv, sqrtdiv);
          }
        }
      }
      final double abof = s.getCount() > 0 ? s.getPopulationVariance() : Double.POSITIVE_INFINITY;
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }
    return true;
  }

  /**
   * Full kernel-based version.
   *
   * @param relation Data relation
   * @param ids IDs
   * @param abodvalues Score storage
   * @param minmaxabod Min/max storage
   */
  private void fastABOD(Relation<V> relation, DBIDs ids, WritableDoubleDataStore abodvalues, DoubleMinMax minmaxabod) {
    // Build a kernel matrix, to make O(n^3) slightly less bad.
    SimilarityQuery<V> sq = new QueryBuilder<>(relation, kernelFunction).similarityQuery();
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    MeanVariance s = new MeanVariance();
    KNNHeap nn = DBIDUtil.newHeap(k);
    for(DBIDIter pA = ids.iter(); pA.valid(); pA.advance()) {
      final double simAA = kernelMatrix.getSimilarity(pA, pA);

      // Choose the k-min nearest
      nn.clear();
      for(DBIDIter nB = relation.iterDBIDs(); nB.valid(); nB.advance()) {
        if(DBIDUtil.equal(nB, pA)) {
          continue;
        }
        double simBB = kernelMatrix.getSimilarity(nB, nB);
        double simAB = kernelMatrix.getSimilarity(pA, nB);
        double sqdAB = simAA + simBB - simAB - simAB;
        if(!(sqdAB > 0.)) {
          continue;
        }
        nn.insert(sqdAB, nB);
      }
      KNNList nl = nn.toKNNList();

      s.reset();
      DoubleDBIDListIter iB = nl.iter(), iC = nl.iter();
      for(; iB.valid(); iB.advance()) {
        double sqdAB = iB.doubleValue();
        double simAB = kernelMatrix.getSimilarity(pA, iB);
        if(!(sqdAB > 0.)) {
          continue;
        }
        for(iC.seek(iB.getOffset() + 1); iC.valid(); iC.advance()) {
          double sqdAC = iC.doubleValue();
          double simAC = kernelMatrix.getSimilarity(pA, iC);
          if(!(sqdAC > 0.)) {
            continue;
          }
          // Exploit bilinearity of scalar product:
          // <B-A, C-A> = <B, C-A> - <A,C-A>
          // = <B,C> - <B,A> - <A,C> + <A,A>
          double simBC = kernelMatrix.getSimilarity(iB, iC);
          double numerator = simBC - simAB - simAC + simAA;
          double div = 1. / (sqdAB * sqdAC);
          s.put(numerator * div, Math.sqrt(div));
        }
      }
      final double abof = s.getCount() > 0 ? s.getPopulationVariance() : Double.POSITIVE_INFINITY;
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends ABOD.Par<V> {
    /**
     * Parameter for the nearest neighbors.
     */
    public static final OptionID K_ID = new OptionID("fastabod.k", "Number of nearest neighbors to use for ABOD.");

    /**
     * Number of neighbors.
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(new GreaterEqualConstraint(3)) //
          .grab(config, x -> k = x);
    }

    @Override
    public FastABOD<V> make() {
      return new FastABOD<>(kernelFunction, k);
    }
  }
}
