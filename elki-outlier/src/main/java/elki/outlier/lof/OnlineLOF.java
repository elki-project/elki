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
package elki.outlier.lof;

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.preprocessed.knn.*;
import elki.logging.Logging;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.utilities.pairs.Pair;

/**
 * Incremental version of the {@link LOF} Algorithm, supports insertions and
 * removals.
 *
 * @author Elke Achtert
 * @since 0.4.0
 *
 * @navhas - updates - FlexibleLOF.LOFResult
 * @composed - - - LOFKNNListener
 */
// TODO: related to publication?
public class OnlineLOF<O> extends FlexibleLOF<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OnlineLOF.class);

  /**
   * Constructor.
   *
   * @param krefer The number of neighbors for reference
   * @param kreach The number of neighbors for reachability distance
   * @param neighborhoodDistance the neighborhood distance function
   * @param reachabilityDistance the reachability distance function
   */
  public OnlineLOF(int krefer, int kreach, Distance<? super O> neighborhoodDistance, Distance<? super O> reachabilityDistance) {
    super(krefer, kreach, neighborhoodDistance, reachabilityDistance);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)} and adds a {@link LOFKNNListener} to
   * the preprocessors.
   */
  @Override
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("OnlineLOF", 3) : null;

    Pair<Pair<KNNSearcher<DBIDRef>, KNNSearcher<DBIDRef>>, Pair<RKNNSearcher<DBIDRef>, RKNNSearcher<DBIDRef>>> queries = getKNNAndRkNNQueries(relation, stepprog);
    KNNSearcher<DBIDRef> kNNRefer = queries.getFirst().getFirst();
    KNNSearcher<DBIDRef> kNNReach = queries.getFirst().getSecond();
    RKNNSearcher<DBIDRef> rkNNRefer = queries.getSecond().getFirst();
    RKNNSearcher<DBIDRef> rkNNReach = queries.getSecond().getSecond();

    LOFResult<O> lofResult = super.doRunInTime(relation.getDBIDs(), kNNRefer, kNNReach, stepprog);
    lofResult.setRkNNRefer(rkNNRefer);
    lofResult.setRkNNReach(rkNNReach);

    // add listener
    KNNListener l = new LOFKNNListener(lofResult);
    ((MaterializeKNNPreprocessor<?>) ((PreprocessorKNNQuery) lofResult.getKNNRefer()).getPreprocessor()).addKNNListener(l);
    ((MaterializeKNNPreprocessor<?>) ((PreprocessorKNNQuery) lofResult.getKNNReach()).getPreprocessor()).addKNNListener(l);

    return lofResult.getResult();
  }

  /**
   * Get the kNN and rkNN queries for the algorithm.
   *
   * @param relation Data
   * @param stepprog Progress logger
   * @return the kNN and rkNN queries
   */
  private Pair<Pair<KNNSearcher<DBIDRef>, KNNSearcher<DBIDRef>>, Pair<RKNNSearcher<DBIDRef>, RKNNSearcher<DBIDRef>>> getKNNAndRkNNQueries(Relation<O> relation, StepProgress stepprog) {
    DistanceQuery<O> drefQ = new QueryBuilder<>(relation, referenceDistance).distanceQuery();
    KNNSearcher<DBIDRef> kNNRefer = new QueryBuilder<>(drefQ).optimizedOnly().kNNByDBID(krefer);
    RKNNSearcher<DBIDRef> rkNNRefer = new QueryBuilder<>(drefQ).optimizedOnly().rKNNByDBID(krefer);

    // No optimized kNN query or RkNN query - use a preprocessor!
    if(kNNRefer == null || rkNNRefer == null) {
      if(stepprog != null) {
        stepprog.beginStep(1, "Materializing neighborhood w.r.t. reference neighborhood distance function.", LOG);
      }
      MaterializeKNNAndRKNNPreprocessor<O> preproc = new MaterializeKNNAndRKNNPreprocessor<>(relation, referenceDistance, krefer);
      kNNRefer = preproc.kNNByDBID(drefQ, krefer, 0);
      rkNNRefer = preproc.rkNNByDBID(drefQ, krefer, 0);
      // add as index
      Metadata.hierarchyOf(relation).addChild(preproc);
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(1, "Optimized neighborhood w.r.t. reference neighborhood distance function provided by database.", LOG);
      }
    }

    DistanceQuery<O> dreachQ = new QueryBuilder<>(relation, reachabilityDistance).distanceQuery();
    KNNSearcher<DBIDRef> kNNReach = new QueryBuilder<>(dreachQ).optimizedOnly().kNNByDBID(kreach);
    RKNNSearcher<DBIDRef> rkNNReach = new QueryBuilder<>(dreachQ).optimizedOnly().rKNNByDBID(kreach);
    if(kNNReach == null || rkNNReach == null) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Materializing neighborhood w.r.t. reachability distance function.", LOG);
      }
      MaterializeKNNAndRKNNPreprocessor<O> preproc = new MaterializeKNNAndRKNNPreprocessor<>(relation, reachabilityDistance, kreach);
      kNNReach = preproc.kNNByDBID(dreachQ, kreach, 0);
      rkNNReach = preproc.rkNNByDBID(dreachQ, kreach, 0);
      // add as index
      Metadata.hierarchyOf(relation).addChild(preproc);
    }

    Pair<KNNSearcher<DBIDRef>, KNNSearcher<DBIDRef>> kNNPair = new Pair<>(kNNRefer, kNNReach);
    Pair<RKNNSearcher<DBIDRef>, RKNNSearcher<DBIDRef>> rkNNPair = new Pair<>(rkNNRefer, rkNNReach);
    return new Pair<>(kNNPair, rkNNPair);
  }

  /**
   * Encapsulates a listener for changes of kNNs used in the online LOF
   * algorithm.
   *
   * @author Elke Achtert
   */
  private class LOFKNNListener implements KNNListener {
    /**
     * Holds the first event of one of the both preprocessors. The online
     * algorithm is waiting until both events have been received, i.e. until
     * both preprocessors are up to date.
     */
    private KNNChangeEvent firstEventReceived;

    /**
     * Holds the result of a former run of the LOF algorithm.
     */
    private LOFResult<O> lofResult;

    /**
     * Constructs a listener for the LOF algorithm.
     *
     * @param lofResult the result of a former run of the LOF algorithm
     */
    public LOFKNNListener(LOFResult<O> lofResult) {
      this.lofResult = lofResult;
    }

    @Override
    public void kNNsChanged(KNNChangeEvent e) {
      AbstractMaterializeKNNPreprocessor<?> p1 = ((PreprocessorKNNQuery) lofResult.getKNNRefer()).getPreprocessor();
      AbstractMaterializeKNNPreprocessor<?> p2 = ((PreprocessorKNNQuery) lofResult.getKNNReach()).getPreprocessor();

      if(firstEventReceived == null) {
        if(e.getSource().equals(p1) && e.getSource().equals(p2)) {
          kNNsChanged(e, e);
        }
        else {
          firstEventReceived = e;
        }
      }
      else {
        if(e.getSource().equals(p1) && firstEventReceived.getSource().equals(p2)) {
          kNNsChanged(e, firstEventReceived);
          firstEventReceived = null;
        }
        else if(e.getSource().equals(p2) && firstEventReceived.getSource().equals(p1)) {
          kNNsChanged(firstEventReceived, e);
          firstEventReceived = null;
        }
        else {
          throw new UnsupportedOperationException("Event sources do not fit!");
        }
      }
    }

    /**
     * Invoked after the events of both preprocessors have been received, i.e.
     * until both preprocessors are up to date.
     *
     * @param e1 the change event of the first preprocessor
     * @param e2 the change event of the second preprocessor
     */
    private void kNNsChanged(KNNChangeEvent e1, KNNChangeEvent e2) {
      if(!e1.getType().equals(e2.getType())) {
        throw new UnsupportedOperationException("Event types do not fit: " + e1.getType() + " != " + e2.getType());
      }
      if(!e1.getObjects().equals(e2.getObjects())) {
        throw new UnsupportedOperationException("Objects do not fit: " + e1.getObjects() + " != " + e2.getObjects());
      }

      if(e1.getType().equals(KNNChangeEvent.Type.DELETE)) {
        kNNsRemoved(e1.getObjects(), e1.getUpdates(), e2.getUpdates(), lofResult);
      }
      else if(e1.getType().equals(KNNChangeEvent.Type.INSERT)) {
        kNNsInserted(e1.getObjects(), e1.getUpdates(), e2.getUpdates(), lofResult);
      }
      else {
        throw new UnsupportedOperationException("Unsupported event type: " + e1.getType());
      }
    }

    /**
     * Invoked after kNNs have been inserted and updated, updates the result.
     *
     * @param insertions the ids of the newly inserted neighborhoods
     * @param updates1 the ids of the updated neighborhood w.r.t. the
     *        neighborhood distance function
     * @param updates2 the ids of the updated neighborhood w.r.t. the
     *        reachability distance function
     * @param lofResult the result of the former LOF run
     */
    private void kNNsInserted(DBIDs insertions, DBIDs updates1, DBIDs updates2, LOFResult<O> lofResult) {
      StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;

      // recompute lrds
      if(stepprog != null) {
        stepprog.beginStep(1, "Recompute LRDs.", LOG);
      }
      DBIDs lrd_ids = DBIDUtil.union(insertions, updates2);
      ModifiableDBIDs affected_lrd_id_candidates = DBIDUtil.newHashSet(lrd_ids.size() * kreach);
      for(DBIDIter it = lrd_ids.iter(); it.valid(); it.advance()) {
        affected_lrd_id_candidates.addDBIDs(lofResult.getRkNNReach().getRKNN(it, kreach));
      }
      affected_lrd_id_candidates.addDBIDs(lrd_ids);
      WritableDoubleDataStore new_lrds = DataStoreUtil.makeDoubleStorage(affected_lrd_id_candidates, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      computeLRDs(lofResult.getKNNReach(), affected_lrd_id_candidates, new_lrds);

      ArrayModifiableDBIDs affected_lrd_ids = DBIDUtil.newArray(affected_lrd_id_candidates.size());
      for(DBIDIter iter = affected_lrd_id_candidates.iter(); iter.valid(); iter.advance()) {
        double new_lrd = new_lrds.doubleValue(iter);
        if(new_lrd != lofResult.getLrds().doubleValue(iter)) {
          lofResult.getLrds().putDouble(iter, new_lrd);
          affected_lrd_ids.add(iter);
        }
      }

      // recompute lofs
      if(stepprog != null) {
        stepprog.beginStep(2, "Recompute LOFS.", LOG);
      }
      ModifiableDBIDs affected_lof_ids = DBIDUtil.newHashSet(affected_lrd_ids.size() * kreach);
      for(DBIDIter it = affected_lrd_ids.iter(); it.valid(); it.advance()) {
        affected_lof_ids.addDBIDs(lofResult.getRkNNRefer().getRKNN(it, kreach));
      }
      affected_lof_ids.addDBIDs(affected_lrd_ids);
      affected_lof_ids.addDBIDs(insertions);
      affected_lof_ids.addDBIDs(updates1);
      recomputeLOFs(affected_lof_ids, lofResult);

      // fire result changed
      if(stepprog != null) {
        stepprog.beginStep(3, "Inform listeners.", LOG);
      }
      Metadata.of(lofResult.getResult()).notifyChanged();
      LOG.setCompleted(stepprog);
    }

    /**
     * Invoked after kNNs have been removed and updated, updates the result.
     *
     * @param deletions the ids of the removed neighborhoods
     * @param updates1 the ids of the updated neighborhood w.r.t. the
     *        neighborhood distance function
     * @param updates2 the ids of the updated neighborhood w.r.t. the
     *        reachability distance function
     * @param lofResult the result of the former LOF run
     */
    private void kNNsRemoved(DBIDs deletions, DBIDs updates1, DBIDs updates2, LOFResult<O> lofResult) {
      StepProgress stepprog = LOG.isVerbose() ? new StepProgress(4) : null;

      // delete lrds and lofs
      if(stepprog != null) {
        stepprog.beginStep(1, "Delete old LRDs and LOFs.", LOG);
      }
      for(DBIDIter iter = deletions.iter(); iter.valid(); iter.advance()) {
        lofResult.getLrds().delete(iter);
        lofResult.getLofs().delete(iter);
      }

      // recompute lrds
      if(stepprog != null) {
        stepprog.beginStep(2, "Recompute LRDs.", LOG);
      }
      ModifiableDBIDs affected_lrd_id_candidates = DBIDUtil.newHashSet(updates2.size() * kreach);
      for(DBIDIter it = updates2.iter(); it.valid(); it.advance()) {
        affected_lrd_id_candidates.addDBIDs(lofResult.getRkNNReach().getRKNN(it, kreach));
      }
      affected_lrd_id_candidates.addDBIDs(updates2);
      WritableDoubleDataStore new_lrds = DataStoreUtil.makeDoubleStorage(affected_lrd_id_candidates, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      computeLRDs(lofResult.getKNNReach(), affected_lrd_id_candidates, new_lrds);
      ArrayModifiableDBIDs affected_lrd_ids = DBIDUtil.newArray(affected_lrd_id_candidates.size());
      for(DBIDIter iter = affected_lrd_id_candidates.iter(); iter.valid(); iter.advance()) {
        double new_lrd = new_lrds.doubleValue(iter);
        if(new_lrd != lofResult.getLrds().doubleValue(iter)) {
          lofResult.getLrds().putDouble(iter, new_lrd);
          affected_lrd_ids.add(iter);
        }
      }

      // recompute lofs
      if(stepprog != null) {
        stepprog.beginStep(3, "Recompute LOFS.", LOG);
      }
      ModifiableDBIDs affected_lof_ids = DBIDUtil.newHashSet(affected_lrd_ids.size() * krefer);
      for(DBIDIter it = affected_lrd_ids.iter(); it.valid(); it.advance()) {
        affected_lof_ids.addDBIDs(lofResult.getRkNNRefer().getRKNN(it, krefer));
      }
      affected_lof_ids.addDBIDs(affected_lrd_ids);
      affected_lof_ids.addDBIDs(updates1);
      recomputeLOFs(affected_lof_ids, lofResult);

      // fire result changed
      if(stepprog != null) {
        stepprog.beginStep(4, "Inform listeners.", LOG);
      }
      Metadata.of(lofResult.getResult()).notifyChanged();
      LOG.setCompleted(stepprog);
    }

    /**
     * Recomputes the lofs of the specified ids.
     *
     * @param ids the ids of the lofs to be recomputed
     * @param lofResult the result of the former LOF run
     */
    private void recomputeLOFs(DBIDs ids, LOFResult<O> lofResult) {
      WritableDoubleDataStore new_lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      DoubleMinMax new_lofminmax = new DoubleMinMax();
      computeLOFs(lofResult.getKNNRefer(), ids, lofResult.getLrds(), new_lofs, new_lofminmax);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        lofResult.getLofs().putDouble(iter, new_lofs.doubleValue(iter));
      }
      // Actualize meta info
      if(new_lofminmax.isValid()) {
        if(lofResult.getResult().getOutlierMeta().getActualMaximum() < new_lofminmax.getMax()) {
          BasicOutlierScoreMeta scoreMeta = (BasicOutlierScoreMeta) lofResult.getResult().getOutlierMeta();
          scoreMeta.setActualMaximum(new_lofminmax.getMax());
        }
        if(lofResult.getResult().getOutlierMeta().getActualMinimum() > new_lofminmax.getMin()) {
          BasicOutlierScoreMeta scoreMeta = (BasicOutlierScoreMeta) lofResult.getResult().getOutlierMeta();
          scoreMeta.setActualMinimum(new_lofminmax.getMin());
        }
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends FlexibleLOF.Par<O> {
    @Override
    public OnlineLOF<O> make() {
      return new OnlineLOF<>(kreach, krefer, distance, reachabilityDistance);
    }
  }
}
