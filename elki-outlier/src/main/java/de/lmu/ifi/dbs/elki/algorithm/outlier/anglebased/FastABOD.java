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
package de.lmu.ifi.dbs.elki.algorithm.outlier.anglebased;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.LinearKernelFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

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
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.FastABOD", "fastabod" })
public class FastABOD<V extends NumberVector> extends ABOD<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FastABOD.class);

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
  public FastABOD(SimilarityFunction<? super V> kernelFunction, int k) {
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
  public OutlierResult run(Database db, Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();
    if(kernelFunction.getClass() == LinearKernelFunction.class) {
      if(!kNNABOD(db, relation, ids, abodvalues, minmaxabod)) {
        // Fallback, if we do not have an index.
        fastABOD(db, relation, ids, abodvalues, minmaxabod);
      }
    }
    else {
      fastABOD(db, relation, ids, abodvalues, minmaxabod);
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Angle-Based Outlier Degree", "abod-outlier", abodvalues, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Simpler kNN based, can use more indexing.
   *
   * @param db Database
   * @param relation Data relation
   * @param ids IDs
   * @param abodvalues Score storage
   * @param minmaxabod Min/max storage
   * @return {@code true} if kNN were available and usable.
   */
  private boolean kNNABOD(Database db, Relation<V> relation, DBIDs ids, WritableDoubleDataStore abodvalues, DoubleMinMax minmaxabod) {
    DistanceQuery<V> dq = db.getDistanceQuery(relation, SquaredEuclideanDistanceFunction.STATIC);
    KNNQuery<V> knnq = db.getKNNQuery(dq, DatabaseQuery.HINT_OPTIMIZED_ONLY);
    boolean squared = true;
    if(knnq == null) {
      dq = db.getDistanceQuery(relation, EuclideanDistanceFunction.STATIC);
      knnq = db.getKNNQuery(dq, DatabaseQuery.HINT_OPTIMIZED_ONLY);
      if(knnq == null) {
        return false;
      }
      squared = false;
    }
    SimilarityQuery<V> lk = db.getSimilarityQuery(relation, LinearKernelFunction.STATIC);
    int k1 = k + 1; // We will get the query point back by the knnq.

    MeanVariance s = new MeanVariance();
    for(DBIDIter pA = ids.iter(); pA.valid(); pA.advance()) {
      KNNList nl = knnq.getKNNForDBID(pA, k1);
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
            s.put(numerator * div, FastMath.sqrt(div));
          }
          else {
            double sqrtdiv = 1. / (dAB * dAC);
            s.put(numerator * sqrtdiv * sqrtdiv, sqrtdiv);
          }
        }
      }
      final double abof = s.getNaiveVariance();
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }
    return true;
  }

  /**
   * Full kernel-based version.
   *
   * @param db Database
   * @param relation Data relation
   * @param ids IDs
   * @param abodvalues Score storage
   * @param minmaxabod Min/max storage
   */
  private void fastABOD(Database db, Relation<V> relation, DBIDs ids, WritableDoubleDataStore abodvalues, DoubleMinMax minmaxabod) {
    // Build a kernel matrix, to make O(n^3) slightly less bad.
    SimilarityQuery<V> sq = db.getSimilarityQuery(relation, kernelFunction);
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
          s.put(numerator * div, FastMath.sqrt(div));
        }
      }
      final double abof = s.getNaiveVariance();
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends ABOD.Parameterizer<V> {
    /**
     * Parameter for the nearest neighbors.
     */
    public static final OptionID K_ID = new OptionID("fastabod.k", "Number of nearest neighbors to use for ABOD.");

    /**
     * Number of neighbors.
     */
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(new GreaterEqualConstraint(3));
      if(config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected FastABOD<V> makeInstance() {
      return new FastABOD<>(kernelFunction, k);
    }
  }
}
