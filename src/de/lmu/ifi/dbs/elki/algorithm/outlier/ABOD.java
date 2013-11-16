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

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Angle-Based Outlier Detection
 * 
 * Outlier detection using variance analysis on angles, especially for high
 * dimensional data sets.
 * 
 * H.-P. Kriegel, M. Schubert, and A. Zimek: Angle-Based Outlier Detection in
 * High-dimensional Data. In: Proc. 14th ACM SIGKDD Int. Conf. on Knowledge
 * Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008.
 * 
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <V> Vector type
 */
@Title("ABOD: Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "H.-P. Kriegel, M. Schubert, and A. Zimek", title = "Angle-Based Outlier Detection in High-dimensional Data", booktitle = "Proc. 14th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008", url = "http://dx.doi.org/10.1145/1401890.1401946")
public class ABOD<V extends NumberVector<?>> extends AbstractDistanceBasedAlgorithm<V, DoubleDistance, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ABOD.class);

  /**
   * k parameter.
   */
  private int k;

  /**
   * Variable to store fast mode sampling value.
   */
  int sampleSize = 0;

  /**
   * Store the configured Kernel version.
   */
  private SimilarityFunction<? super V, DoubleDistance> kernelFunction;

  /**
   * Actual constructor, with parameters. Fast mode (sampling).
   * 
   * @param k k parameter
   * @param sampleSize sample size
   * @param kernelFunction Kernel function to use
   * @param distanceFunction Distance function
   */
  public ABOD(int k, int sampleSize, SimilarityFunction<? super V, DoubleDistance> kernelFunction, DistanceFunction<V, DoubleDistance> distanceFunction) {
    super(distanceFunction);
    this.k = k;
    this.sampleSize = sampleSize;
    this.kernelFunction = kernelFunction;
  }

  /**
   * Actual constructor, with parameters. Slow mode (exact).
   * 
   * @param k k parameter
   * @param kernelFunction kernel function to use
   * @param distanceFunction Distance function
   */
  public ABOD(int k, SimilarityFunction<? super V, DoubleDistance> kernelFunction, DistanceFunction<V, DoubleDistance> distanceFunction) {
    super(distanceFunction);
    this.k = k;
    this.sampleSize = 0;
    this.kernelFunction = kernelFunction;
  }

  /**
   * Main part of the algorithm. Exact version.
   * 
   * @param db Database
   * @param relation Relation to query
   * @return result
   */
  public OutlierResult getRanking(Database db, Relation<V> relation) {
    // Fix a static set of IDs
    DBIDs ids = relation.getDBIDs();
    SimilarityQuery<V, DoubleDistance> sq = db.getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    // Get kNN Query
    DistanceQuery<V, DoubleDistance> dq = db.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<V, DoubleDistance> knnQuery = db.getKNNQuery(dq, k);

    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();

    MeanVariance s = new MeanVariance();
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      s.reset();
      // Length of vector.
      double dAA = kernelMatrix.getSquaredDistance(it, it);

      KNNList<DoubleDistance> neighbors = knnQuery.getKNNForDBID(it, k);
      DBIDArrayIter n1 = neighbors.iter(), n2 = neighbors.iter();
      for(; n1.valid(); n1.advance()) {
        if(DBIDUtil.equal(n1, it)) {
          continue;
        }
        double dAB = kernelMatrix.getSquaredDistance(it, n1);
        if(!(dAB > 0.)) {
          continue;
        }
        for(n2.seek(n1.getOffset() + 1); n2.valid(); n2.advance()) {
          if(DBIDUtil.equal(n2, it)) {
            continue;
          }
          double dAC = kernelMatrix.getSquaredDistance(it, n2);
          if(dAC > 0.) {
            // Exploit bilinearity:
            // <B-A, C-A> = <B, C-A> - <A,C-A>
            // = <B,C> - <B,A> - <A,C> + <A,A>
            double numerator = kernelMatrix.getSimilarity(n1, n2) - dAB - dAC + dAA;
            double val = numerator / (dAB * dAC);
            double weight = 1. / Math.sqrt(dAB * dAC);
            s.put(val, weight);
          }
        }
      }
      // Sample variance probably would be correct, but the ABOD publication
      // uses the naive variance.
      double abof = s.getNaiveVariance();
      minmaxabod.put(abof);
      abodvalues.putDouble(it, abof);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Angle-based Outlier Degree", "abod-outlier", TypeUtil.DOUBLE, abodvalues, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Main part of the algorithm. Fast version.
   * 
   * @param db Database
   * @param relation Relation to use
   * @return result
   */
  public OutlierResult getFastRanking(Database db, Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    SimilarityQuery<V, DoubleDistance> sq = relation.getDatabase().getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    // Output storage.
    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();

    ComparableMaxHeap<DoubleDBIDPair> pq = new ComparableMaxHeap<>(relation.size());
    // get Candidate Ranking
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      // Storage for squared distances
      WritableDoubleDataStore sqDists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
      // Compute distances and nearest neighbors
      ComparableMinHeap<DoubleDBIDPair> nn = calcDistsandNN(relation, kernelMatrix, sampleSize, it, sqDists);

      // get normalization
      double[] counter = calcFastNormalization(it, sqDists, ids);
      // umsetzen von Pq zu list
      ModifiableDBIDs neighbors = DBIDUtil.newArray(nn.size());
      while(!nn.isEmpty()) {
        neighbors.add(nn.poll());
      }
      // getFilter
      double var = getAbofFilter(kernelMatrix, it, sqDists, counter[1], counter[0], neighbors);
      pq.add(DBIDUtil.newPair(var, it));
    }
    // refine Candidates
    DoubleMinHeap topscores = new DoubleMinHeap(k);
    MeanVariance s = new MeanVariance();
    while(!pq.isEmpty()) {
      // Stop refining
      if(topscores.size() >= k && pq.peek().doubleValue() > topscores.peek()) {
        break;
      }
      // double approx = pq.peek().getFirst();
      DBIDRef pA = pq.poll();
      s.reset();
      double dAA = kernelMatrix.getSquaredDistance(pA, pA);
      for(DBIDIter nB = relation.iterDBIDs(); nB.valid(); nB.advance()) {
        if(DBIDUtil.equal(nB, pA)) {
          continue;
        }
        double dAB = kernelMatrix.getSquaredDistance(pA, nB);
        if(!(dAB > 0.)) {
          continue;
        }
        for(DBIDIter nC = relation.iterDBIDs(); nC.valid(); nC.advance()) {
          if(DBIDUtil.equal(nC, pA) || DBIDUtil.compare(nC, nB) < 0) {
            continue;
          }
          double dAC = kernelMatrix.getSquaredDistance(pA, nC);
          if(dAC > 0.) {
            // Exploit bilinearity:
            // <B-A, C-A> = <B, C-A> - <A,C-A>
            // = <B,C> - <B,A> - <A,C> + <A,A>
            double numerator = kernelMatrix.getSimilarity(nB, nC) - dAB - dAC + dAA;
            double val = numerator / (dAB * dAC);
            double weight = 1. / Math.sqrt(dAB * dAC);
            s.put(val, weight);
          }
        }
      }
      // Sample variance probably would be correct, but the ABOD publication
      // uses the naive variance.
      double var = s.getNaiveVariance();
      // Store refined score:
      abodvalues.putDouble(pA, var);
      minmaxabod.put(var);
      // Update the heap tracking the top scores.
      if(topscores.size() < k) {
        topscores.add(var);
      }
      else {
        if(topscores.peek() > var) {
          topscores.replaceTopElement(var);
        }
      }
    }
    // Poll remaining candidates to transfer scores into output
    while(!pq.isEmpty()) {
      DoubleDBIDPair pair = pq.poll();
      abodvalues.putDouble(pair, pair.doubleValue());
      minmaxabod.put(pair.doubleValue());
    }
    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Angle-based Outlier Detection", "abod-outlier", TypeUtil.DOUBLE, abodvalues, ids);
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  private double[] calcFastNormalization(DBIDRef x, WritableDoubleDataStore sqdists, DBIDs ids) {
    double[] result = new double[2];

    double sum = 0;
    double sumF = 0;
    for(DBIDIter yKey = ids.iter(); yKey.valid(); yKey.advance()) {
      double sqdist = sqdists.doubleValue(yKey);
      if(sqdist > 0) {
        double tmp = 1 / Math.sqrt(sqdist);
        sum += tmp;
        sumF += (1 / sqdist) * tmp;
      }
    }
    double sofar = 0;
    double sofarF = 0;
    for(DBIDIter zKey = ids.iter(); zKey.valid(); zKey.advance()) {
      double sqdist = sqdists.doubleValue(zKey);
      if(sqdist > 0) {
        double tmp = 1 / Math.sqrt(sqdist);
        sofar += tmp;
        double rest = sum - sofar;
        result[0] += tmp * rest;

        sofarF += (1 / sqdist) * tmp;
        double restF = sumF - sofarF;
        result[1] += (1 / sqdist) * tmp * restF;
      }
    }
    return result;
  }

  private double getAbofFilter(KernelMatrix kernelMatrix, DBIDRef aKey, WritableDoubleDataStore dists, double fulCounter, double norm, DBIDs neighbors) {
    double dAA = kernelMatrix.getSimilarity(aKey, aKey);
    double sum = 0.0;
    double sqrSum = 0.0;
    double partCounter = 0;
    for(DBIDIter bKey = neighbors.iter(); bKey.valid(); bKey.advance()) {
      if(DBIDUtil.equal(bKey, aKey)) {
        continue;
      }
      double dAB = dists.doubleValue(bKey);
      if(!(dAB > 0.)) {
        continue;
      }
      for(DBIDIter cKey = neighbors.iter(); cKey.valid(); cKey.advance()) {
        if(DBIDUtil.equal(cKey, aKey) || DBIDUtil.compare(bKey, cKey) < 0) {
          continue;
        }
        double dAC = dists.doubleValue(cKey);
        if(dAC > 0.) {
          continue;
        }
        double div = dAB * dAC;
        double val = (dAA + kernelMatrix.getSimilarity(bKey, cKey) - dAC - dAB) / div;
        double sqrtNenner = Math.sqrt(div);
        sum += val * (1 / sqrtNenner);
        sqrSum += val * val * (1 / sqrtNenner);
        partCounter += (1 / (sqrtNenner * div));
      }
    }
    // TODO: Document the meaning / use of fulCounter, partCounter.
    double mu = (sum + (fulCounter - partCounter)) / norm;
    return (sqrSum / norm) - (mu * mu);
  }

  private double calcNumerator(KernelMatrix kernelMatrix, DBIDRef aKey, DBIDRef bKey, DBIDRef cKey) {
    return (kernelMatrix.getSimilarity(aKey, aKey) + kernelMatrix.getSimilarity(bKey, cKey) - kernelMatrix.getSimilarity(aKey, cKey) - kernelMatrix.getSimilarity(aKey, bKey));
  }

  /**
   * Compute distances to A and find the k nearest neighbors.
   * 
   * @param data Data relation
   * @param kernelMatrix Kernel matrix
   * @param sampleSize Sample size
   * @param aKey Query object
   * @param dists Distance output array
   * @return Heap of the nearest neighbors.
   */
  private ComparableMinHeap<DoubleDBIDPair> calcDistsandNN(Relation<V> data, KernelMatrix kernelMatrix, int sampleSize, DBIDRef aKey, WritableDoubleDataStore dists) {
    double asim = kernelMatrix.getSimilarity(aKey, aKey);
    ComparableMinHeap<DoubleDBIDPair> nn = new ComparableMinHeap<>(sampleSize);
    for(DBIDIter bKey = data.iterDBIDs(); bKey.valid(); bKey.advance()) {
      double sqDist = asim + kernelMatrix.getSimilarity(bKey, bKey) - kernelMatrix.getSimilarity(aKey, bKey);
      dists.putDouble(bKey, sqDist);
      // Update heap
      if(nn.size() < sampleSize) {
        nn.add(DBIDUtil.newPair(sqDist, bKey));
      }
      else {
        if(sqDist < nn.peek().doubleValue()) {
          nn.replaceTopElement(DBIDUtil.newPair(sqDist, bKey));
        }
      }
    }
    return nn;
  }

  /**
   * Get explanations for points in the database.
   * 
   * @param relation to get explanations for
   * @return String explanation
   */
  // TODO: this should be done by the result classes.
  public String getExplanations(Relation<V> relation) {
    SimilarityQuery<V, DoubleDistance> sq = relation.getDatabase().getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, relation.getDBIDs());
    // PQ for Outlier Ranking
    ComparableMaxHeap<DoubleDBIDPair> pq = new ComparableMaxHeap<>(relation.size());
    HashMap<DBID, DBIDs> explaintab = new HashMap<>();
    // test all objects
    MeanVariance s = new MeanVariance(), s2 = new MeanVariance();
    for(DBIDIter objKey = relation.iterDBIDs(); objKey.valid(); objKey.advance()) {
      s.reset();
      // Queue for the best explanation
      ComparableMinHeap<DoubleDBIDPair> explain = new ComparableMinHeap<>();
      // determine Object
      // for each pair of other objects
      for(DBIDIter key1 = relation.iterDBIDs(); key1.valid(); key1.advance()) {
        // Collect Explanation Vectors
        s2.reset();
        if(DBIDUtil.equal(objKey, key1)) {
          continue;
        }
        for(DBIDIter key2 = relation.iterDBIDs(); key2.valid(); key2.advance()) {
          if(DBIDUtil.equal(key2, key1) || DBIDUtil.equal(objKey, key2)) {
            continue;
          }
          double nenner = kernelMatrix.getSquaredDistance(objKey, key1) * kernelMatrix.getSquaredDistance(objKey, key2);
          if(nenner != 0) {
            double tmp = calcNumerator(kernelMatrix, objKey, key1, key2) / nenner;
            double sqr = Math.sqrt(nenner);
            s2.put(tmp, 1 / sqr);
          }
        }
        explain.add(DBIDUtil.newPair(s2.getSampleVariance(), key1));
        s.put(s2);
      }
      // build variance of the observed vectors
      pq.add(DBIDUtil.newPair(s.getSampleVariance(), objKey));
      //
      ModifiableDBIDs expList = DBIDUtil.newArray();
      expList.add(explain.poll());
      while(!explain.isEmpty()) {
        DBIDRef nextKey = explain.poll();
        if(DBIDUtil.equal(nextKey, objKey)) {
          continue;
        }
        double max = Double.MIN_VALUE;
        for(DBIDIter exp = expList.iter(); exp.valid(); exp.advance()) {
          if(DBIDUtil.equal(exp, objKey) || DBIDUtil.equal(nextKey, exp)) {
            continue;
          }
          double nenner = Math.sqrt(kernelMatrix.getSquaredDistance(objKey, nextKey)) * Math.sqrt(kernelMatrix.getSquaredDistance(objKey, exp));
          double angle = calcNumerator(kernelMatrix, objKey, nextKey, exp) / nenner;
          max = Math.max(angle, max);
        }
        if(max < 0.5) {
          expList.add(nextKey);
        }
      }
      explaintab.put(DBIDUtil.deref(objKey), expList);
    }
    StringBuilder buf = new StringBuilder();
    buf.append("Result: ABOD\n");
    int count = 0;
    while(!pq.isEmpty()) {
      if(count > 10) {
        break;
      }
      double factor = pq.peek().doubleValue();
      DBIDRef key = pq.poll();
      buf.append(relation.get(key)).append(' ');
      buf.append(count).append(" Factor=").append(factor).append(' ').append(key).append('\n');
      DBIDs expList = explaintab.get(key);
      generateExplanation(buf, relation, key, expList);
      count++;
    }
    return buf.toString();
  }

  private void generateExplanation(StringBuilder buf, Relation<V> data, DBIDRef key, DBIDs expList) {
    Vector vect1 = data.get(key).getColumnVector();
    for(DBIDIter iter = expList.iter(); iter.valid(); iter.advance()) {
      buf.append("Outlier: ").append(vect1).append('\n');
      Vector exp = data.get(iter).getColumnVector();
      buf.append("Most common neighbor: ").append(exp).append('\n');
      // determine difference Vector
      Vector vals = exp.minus(vect1);
      buf.append(vals).append('\n');
    }
  }

  /**
   * Run ABOD on the data set.
   * 
   * @param relation Relation to process
   * @return Outlier detection result
   */
  public OutlierResult run(Database db, Relation<V> relation) {
    if(sampleSize > 0) {
      return getFastRanking(db, relation);
    }
    else {
      return getRanking(db, relation);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, DoubleDistance> {
    /**
     * Parameter for k, the number of neighbors used in kNN queries.
     */
    public static final OptionID K_ID = new OptionID("abod.k", "Parameter k for kNN queries.");

    /**
     * Parameter for sample size to be used in fast mode.
     */
    public static final OptionID FAST_SAMPLE_ID = new OptionID("abod.samplesize", "Sample size to enable fast mode.");

    /**
     * Parameter for the kernel function.
     */
    public static final OptionID KERNEL_FUNCTION_ID = new OptionID("abod.kernelfunction", "Kernel function to use.");

    /**
     * The preprocessor used to materialize the kNN neighborhoods.
     */
    public static final OptionID PREPROCESSOR_ID = new OptionID("abod.knnquery", "Processor to compute the kNN neighborhoods.");

    /**
     * k Parameter.
     */
    protected int k = 0;

    /**
     * Sample size.
     */
    protected int sampleSize = 0;

    /**
     * Distance function.
     */
    protected PrimitiveSimilarityFunction<V, DoubleDistance> primitiveKernelFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID, 30);
      kP.addConstraint(new GreaterEqualConstraint(1));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      final IntParameter sampleSizeP = new IntParameter(FAST_SAMPLE_ID);
      sampleSizeP.addConstraint(new GreaterEqualConstraint(1));
      sampleSizeP.setOptional(true);
      if(config.grab(sampleSizeP)) {
        sampleSize = sampleSizeP.getValue();
      }
      final ObjectParameter<PrimitiveSimilarityFunction<V, DoubleDistance>> param = new ObjectParameter<>(KERNEL_FUNCTION_ID, PrimitiveSimilarityFunction.class, PolynomialKernelFunction.class);
      if(config.grab(param)) {
        primitiveKernelFunction = param.instantiateClass(config);
      }
    }

    @Override
    protected ABOD<V> makeInstance() {
      return new ABOD<>(k, sampleSize, primitiveKernelFunction, distanceFunction);
    }
  }
}
