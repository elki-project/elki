package de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic;

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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

public class IDOS<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(IDOS.class);

  /**
   * The distance function to determine the distance between
   * database objects.
   */
  public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("idos.distfunction", "Distance function.");


  /**
   * Parameter to specify the neighborhood size to use for the averaging.
   */
  public static final OptionID KR_ID = new OptionID("idos.kr", "Reference set size.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * used for the GED computation.
   */
  public static final OptionID KC_ID = new OptionID("idos.kc", "Context set size (ID estimation).");


  /**
   * Storage for the computed IDs
   */
  private WritableDoubleDataStore intDims = null;


  /**
   * Holds the value of {@link #KC_ID}.
   */
  protected int k_c = 20;

  /**
   * Maximum of kr and kc
   */
  protected int kmax = 100;

  /**
   * Distance function.
   */
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Holds the value of {@link #KC_ID}.
   */
  protected int k_r = 0;

  /**
   * Constructor.
   *
   * @param kc the context set size for the ID computation
   * @param kr the neighborhood size to use in score computation
   * @param distanceFunction the distance function to use
   */
  public IDOS(int kc, int kr, DistanceFunction<O> distanceFunction) {
    super(distanceFunction);
    this.kmax = Math.max(kc, kr);
    this.k_c = kc;
    this.distanceFunction = distanceFunction;
    this.k_r = kr;
  }


  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("IDOS", 2) : null;
    KNNQuery<O> knnQ = DatabaseUtil.precomputedKNNQuery(database, relation, getDistanceFunction(), kmax);
    return doRunInTime(relation.getDBIDs(), knnQ, stepprog);
  }



  /**
   * Performs the IDOS algorithm on the given database and
   *
   * @param ids Object ids
   * @param knnQ the precomputed neighborhood of the objects
   * @param stepprog Progress logger
   * @return Outlier result
   */
  protected OutlierResult doRunInTime(DBIDs ids, KNNQuery<O> knnQ, StepProgress stepprog) {
    intDims  = computeIDs(ids, knnQ);
    if (stepprog != null) {
      stepprog.beginStep(2, "Computing Scores...", LOG);
    }
    FiniteProgress progressLDMs = LOG.isVerbose() ? new FiniteProgress("ID Outlier Scores for objects", ids.size(), LOG) : null;
    WritableDoubleDataStore ldms = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax idosminmax = new DoubleMinMax();
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if (k_r > 1){
        Mean id_r = new Mean();
        final double id_q = intDims.doubleValue(iter);
        final KNNList neighbors = knnQ.getKNNForDBID(iter, k_r);
        int lv = 0;
        for (DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          final double id = intDims.doubleValue(neighbor);
          id_r.put(1.0 / id);
          if (lv == k_r){
            break;
          }
        }
        final double idos = id_q * id_r.getMean();
        idosminmax.put(idos);
        ldms.putDouble(iter, idos);
        if (progressLDMs != null) {
          progressLDMs.incrementProcessed(LOG);
        }
      }
      else{
        final double id_q = intDims.doubleValue(iter);
        idosminmax.put(id_q);
        ldms.putDouble(iter, id_q);
        if (progressLDMs != null) {
          progressLDMs.incrementProcessed(LOG);
        }
      }
    }
    if (progressLDMs != null) {
      progressLDMs.ensureCompleted(LOG);
    }
    if(stepprog != null) {
      stepprog.setCompleted(LOG);
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Intrinsic Dimensionality Outlier Score", "idos", ldms, ids);
    OutlierScoreMeta scoreMeta;
    if (k_r > 1){
      scoreMeta = new QuotientOutlierScoreMeta(idosminmax.getMin(), idosminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    }
    else{
      scoreMeta = new BasicOutlierScoreMeta(idosminmax.getMin(), idosminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    }
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }


  /**
   * Compute a single ID form distances to the context sets objects
   * @param neighbors distances of the context set
   * @return ID
   */
  double evalID(double[] neighbors) {
    double d = 0.0;
    final int n = neighbors.length;
    if(n < 2){
      return 0.0;
    }
    if(n < 100){
      double id = 0.0;
      final int m = 1;
      double sum = 0.0;
      int p = 0;
      double w = neighbors[m];
      int sumk = 0;
      for(int i=m; i < n; i++){
        for (; p < i; p++){
          sum += Math.log(neighbors[p] / w);
        }
        id += -1.0 * sum;
        sumk += i;
        if(i < n-1){
          final double w2 = neighbors[i+1];
          sum += i * Math.log(w / w2);
          w = w2;
        }
      }
      d =  (double)sumk / id;
    }
    else{
      final double w = neighbors[n-1];
      double sum = 0.0;
      for (int i = 0; i < n-1; ++i) {
        sum += Math.log(neighbors[i] / w);
      }
      d = -1.0 * (double)(n-1) / sum;
    }
    return d;
  }

  /**
   * Computes all IDs
   * @param ids the datasets DBIDs
   * @param knnQ the KNN query
   * @return WritableDoubleDataStore with all IDs
   */
  protected WritableDoubleDataStore computeIDs(DBIDs ids,  KNNQuery<O> knnQ) {
  	WritableDoubleDataStore intDims = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress intDimsProgress = LOG.isVerbose() ? new FiniteProgress("Intrinsic dimensionality", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
      double[] dists = new double[k_c-1];
      int pos = 0;
      KNNList nn = knnQ.getKNNForDBID(iter, k_c);
      for (DoubleDBIDListIter neighbor = nn.iter(); neighbor.valid(); neighbor.advance()){
        final double ndist = neighbor.doubleValue();
        if (ndist > 0.0){
          dists[pos++] = ndist;
        }
        if (pos >= (k_c-1)){
          break;
        }
      }
      if (pos < (k_c-1)){
        dists = Arrays.copyOf(dists, pos);
      }
      final double id = evalID(dists);
      intDims.putDouble(iter, id);
      if(intDimsProgress != null) {
        intDimsProgress.incrementProcessed(LOG);
      }
    }
    if(intDimsProgress != null) {
      intDimsProgress.ensureCompleted(LOG);
    }
    return intDims;
  }


  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation type;
    type = distanceFunction.getInputTypeRestriction();
    return TypeUtil.array(type);
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
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * kNN for the context set (ID computation).
     */
    protected int k_c = 20;

    /**
     * kNN for the reference set.
     */
    protected int k_r = 20;

    /**
     * Distance function.
     */
    protected DistanceFunction<O> distfunc;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pKc = new IntParameter(KC_ID);
      pKc.addConstraint(new GreaterEqualConstraint(5));
      if(config.grab(pKc)) {
        k_c = pKc.getValue();
      }

      final IntParameter pKr = new IntParameter(KR_ID);
      pKr.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(pKr)) {
        k_r = pKr.getValue();
      }


      ObjectParameter<DistanceFunction<O>> distP = AbstractDistanceBasedAlgorithm.makeParameterDistanceFunction(EuclideanDistanceFunction.class, DistanceFunction.class);
      if (config.grab(distP)) {
        distfunc = distP.instantiateClass(config);
      }
    }

    @Override
    protected IDOS<O> makeInstance() {
      return new IDOS<>(k_c, k_r, distfunc);
    }
  }
}
