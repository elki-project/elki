package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ObjectHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Angle-Based Outlier Detection / Angle-Based Outlier Factor.
 * 
 * Fast-ABOD (approximateABOF) version.
 * 
 * H.-P. Kriegel, M. Schubert, and A. Zimek: Angle-Based Outlier Detection in
 * High-dimensional Data. In: Proc. 14th ACM SIGKDD Int. Conf. on Knowledge
 * Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008.
 * 
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * 
 * @param <V> Vector type
 */
@Title("Approximate ABOD: Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "H.-P. Kriegel, M. Schubert, and A. Zimek", title = "Angle-Based Outlier Detection in High-dimensional Data", booktitle = "Proc. 14th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008", url = "http://dx.doi.org/10.1145/1401890.1401946")
public class FastABOD<V extends NumberVector<?>> extends ABOD<V> {
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
  public FastABOD(SimilarityFunction<? super V, DoubleDistance> kernelFunction, int k) {
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
    // Build a kernel matrix, to make O(n^3) slightly less bad.
    SimilarityQuery<V, DoubleDistance> sq = db.getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();

    MeanVariance s = new MeanVariance();
    for (DBIDIter pA = ids.iter(); pA.valid(); pA.advance()) {
      s.reset();
      final double simAA = kernelMatrix.getSimilarity(pA, pA);

      // Choose the k-min nearest
      ComparableMaxHeap<DoubleDBIDPair> nn = new ComparableMaxHeap<>(k);
      for (DBIDIter nB = relation.iterDBIDs(); nB.valid(); nB.advance()) {
        if (DBIDUtil.equal(nB, pA)) {
          continue;
        }
        double simBB = kernelMatrix.getSimilarity(nB, nB);
        double simAB = kernelMatrix.getSimilarity(pA, nB);
        double sqdAB = simAA + simBB - simAB - simAB;
        if (!(sqdAB > 0.)) {
          continue;
        }
        if (nn.size() < k) {
          nn.add(DBIDUtil.newPair(sqdAB, nB));
        } else if (sqdAB < nn.peek().doubleValue()) {
          nn.replaceTopElement(DBIDUtil.newPair(sqdAB, nB));
        }
      }

      for (ObjectHeap.UnsortedIter<DoubleDBIDPair> iB = nn.unsortedIter(); iB.valid(); iB.advance()) {
        DoubleDBIDPair nB = iB.get();
        double sqdAB = nB.doubleValue();
        double simAB = kernelMatrix.getSimilarity(pA, nB);
        if (!(sqdAB > 0.)) {
          continue;
        }
        for (ObjectHeap.UnsortedIter<DoubleDBIDPair> iC = nn.unsortedIter(); iC.valid(); iC.advance()) {
          DoubleDBIDPair nC = iC.get();
          if (DBIDUtil.compare(nC, nB) < 0) {
            continue;
          }
          double sqdAC = nC.doubleValue();
          double simAC = kernelMatrix.getSimilarity(pA, nC);
          if (!(sqdAC > 0.)) {
            continue;
          }
          // Exploit bilinearity of scalar product:
          // <B-A, C-A> = <B, C-A> - <A,C-A>
          // = <B,C> - <B,A> - <A,C> + <A,A>
          // For computing variance, AA is a constant and can be ignored.
          double simBC = kernelMatrix.getSimilarity(nB, nC);
          double numerator = simBC - simAB - simAC; // + simAA;
          double val = numerator / (sqdAB * sqdAC);
          s.put(val, 1. / Math.sqrt(sqdAB * sqdAC));
        }
      }
      // Sample variance probably would be correct, but the ABOD publication
      // uses the naive variance.
      final double abof = s.getNaiveVariance();
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Angle-Based Outlier Degree", "abod-outlier", TypeUtil.DOUBLE, abodvalues, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends ABOD.Parameterizer<V> {
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
      final IntParameter kP = new IntParameter(K_ID);
      if (config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected FastABOD<V> makeInstance() {
      return new FastABOD<>(kernelFunction, k);
    }
  }
}
