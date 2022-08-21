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
package elki.index.preprocessed.knn;

import javax.swing.event.EventListenerList;

import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.DynamicIndex;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * <p>
 * Automatically added by the query optimizer if memory permits.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @has - - - Distance
 * @has - - - KNNSearcher
 * @has - - - KNNListener
 *
 * @param <O> the type of database objects the preprocessor can be applied to
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O> extends AbstractMaterializeKNNPreprocessor<O> implements DynamicIndex {
  /**
   * Logger to use.
   */
  private static final Logging LOG = Logging.getLogger(MaterializeKNNPreprocessor.class);

  /**
   * KNNSearcher instance to use.
   */
  protected final KNNSearcher<DBIDRef> knnQuery;

  /**
   * Holds the listener.
   */
  protected final EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor with preprocessing step.
   *
   * @param relation Relation to preprocess
   * @param distance the distance function to use
   * @param k query k
   */
  public MaterializeKNNPreprocessor(Relation<O> relation, Distance<? super O> distance, int k) {
    super(relation, distance, k);
    this.knnQuery = new QueryBuilder<>(distanceQuery).noCache().kNNByDBID(k);
    assert !(knnQuery instanceof PreprocessorKNNQuery) : knnQuery.toString();
  }

  /**
   * Constructor with preprocessing step.
   *
   * @param relation Relation to preprocess
   * @param distanceQuery the distance function to use
   * @param k query k
   * @param noopt Flag to disable optimization
   */
  public MaterializeKNNPreprocessor(Relation<O> relation, DistanceQuery<O> distanceQuery, int k, boolean noopt) {
    super(relation, distanceQuery, k);
    QueryBuilder<O> qb = new QueryBuilder<>(distanceQuery).noCache();
    this.knnQuery = (noopt ? qb.cheapOnly() : qb).kNNByDBID(k);
    assert !(knnQuery instanceof PreprocessorKNNQuery) : knnQuery.toString();
  }

  /**
   * The actual preprocessing step.
   */
  @Override
  protected void preprocess() {
    final Logging log = getLogger(); // Could be subclass
    createStorage();

    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    if(log.isStatistics()) {
      log.statistics(new LongStatistic(this.getClass().getName() + ".k", k));
    }
    Duration duration = log.isStatistics() ? log.newDuration(this.getClass().getName() + ".precomputation-time").begin() : null;
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), getLogger()) : null;
    // Try bulk
    final boolean ismetric = getDistanceQuery().getDistance().isMetric();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(ismetric && storage.get(iter) != null) {
        log.incrementProcessed(progress);
        continue; // Previously computed (duplicate point?)
      }
      KNNList knn = knnQuery.getKNN(iter, k);
      storage.put(iter, knn);
      if(ismetric) {
        for(DoubleDBIDListIter it = knn.iter(); it.valid() && it.doubleValue() == 0.; it.advance()) {
          storage.put(it, knn); // Reuse
        }
      }
      log.incrementProcessed(progress);
    }
    log.ensureCompleted(progress);
    if(duration != null) {
      log.statistics(duration.end());
    }
  }

  @Override
  public final void insert(DBIDRef id) {
    objectsInserted(DBIDUtil.deref(id));
  }

  @Override
  public void insertAll(DBIDs ids) {
    if(storage == null && ids.size() > 0) {
      preprocess();
    }
    else {
      objectsInserted(ids);
    }
  }

  @Override
  public boolean delete(DBIDRef id) {
    objectsRemoved(DBIDUtil.deref(id));
    return true;
  }

  @Override
  public void deleteAll(DBIDs ids) {
    objectsRemoved(ids);
  }

  /**
   * Called after new objects have been inserted, updates the materialized
   * neighborhood.
   *
   * @param ids the ids of the newly inserted objects
   */
  protected void objectsInserted(DBIDs ids) {
    final Logging log = getLogger(); // Could be subclass
    StepProgress stepprog = log.isVerbose() ? new StepProgress(3) : null;
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // materialize the new kNNs
    log.beginStep(stepprog, 1, "New insertions ocurred, materialize their new kNNs.");
    // Store in storage
    for(DBIDIter iter = aids.iter(); iter.valid(); iter.advance()) {
      storage.put(iter, knnQuery.getKNN(iter, k));
    }

    // update the affected kNNs
    log.beginStep(stepprog, 2, "New insertions ocurred, update the affected kNNs.");
    ArrayDBIDs rkNN_ids = updateKNNsAfterInsertion(ids);

    // inform listener
    log.beginStep(stepprog, 3, "New insertions ocurred, inform listeners.");
    fireKNNsInserted(ids, rkNN_ids);
    log.setCompleted(stepprog);
  }

  /**
   * Updates the kNNs of the RkNNs of the specified ids.
   *
   * @param ids the ids of newly inserted objects causing a change of
   *        materialized kNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private ArrayDBIDs updateKNNsAfterInsertion(DBIDs ids) {
    ArrayModifiableDBIDs rkNN_ids = DBIDUtil.newArray();
    DBIDs oldids = DBIDUtil.difference(relation.getDBIDs(), ids);
    for(DBIDIter iter = oldids.iter(); iter.valid(); iter.advance()) {
      KNNList kNNs = storage.get(iter);
      double knnDist = kNNs.getKNNDistance();
      // look for new kNNs
      KNNHeap heap = null;
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        final double dist = distanceQuery.distance(iter, iter2);
        if(dist <= knnDist) {
          heap = heap != null ? heap : DBIDUtil.newHeap(kNNs);
          heap.insert(dist, iter2);
        }
      }
      if(heap != null) {
        storage.put(iter, kNNs = heap.toKNNList());
        rkNN_ids.add(iter);
      }
    }
    return rkNN_ids;
  }

  /**
   * Updates the kNNs of the RkNNs of the specified ids.
   *
   * @param ids the ids of deleted objects causing a change of materialized kNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private ArrayDBIDs updateKNNsAfterDeletion(DBIDs ids) {
    SetDBIDs idsSet = DBIDUtil.ensureSet(ids);
    ArrayModifiableDBIDs rkNN_ids = DBIDUtil.newArray();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNList kNNs = storage.get(iditer);
      for(DBIDIter it = kNNs.iter(); it.valid(); it.advance()) {
        if(idsSet.contains(it)) {
          rkNN_ids.add(iditer);
          break;
        }
      }
    }

    // update the kNNs of the RkNNs
    for(DBIDIter iter = rkNN_ids.iter(); iter.valid(); iter.advance()) {
      storage.put(iter, knnQuery.getKNN(iter, k));
    }

    return rkNN_ids;
  }

  /**
   * Called after objects have been removed, updates the materialized
   * neighborhood.
   *
   * @param ids the ids of the removed objects
   */
  protected void objectsRemoved(DBIDs ids) {
    final Logging log = getLogger();
    StepProgress stepprog = log.isVerbose() ? new StepProgress(3) : null;
    // delete the materialized (old) kNNs
    log.beginStep(stepprog, 1, "New deletions ocurred, remove their materialized kNNs.");
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      storage.delete(iter);
    }

    // update the affected kNNs
    log.beginStep(stepprog, 2, "New deletions ocurred, update the affected kNNs.");
    ArrayDBIDs rkNN_ids = updateKNNsAfterDeletion(ids);

    // inform listener
    log.beginStep(stepprog, 3, "New deletions ocurred, inform listeners.");
    fireKNNsRemoved(ids, rkNN_ids);
    log.setCompleted(stepprog);
  }

  /**
   * Informs all registered KNNListener that new kNNs have been inserted and as
   * a result some kNNs have been changed.
   *
   * @param insertions the ids of the newly inserted kNNs
   * @param updates the ids of kNNs which have been changed due to the
   *        insertions
   * @see KNNListener
   */
  protected void fireKNNsInserted(DBIDs insertions, DBIDs updates) {
    KNNChangeEvent e = new KNNChangeEvent(this, KNNChangeEvent.Type.INSERT, insertions, updates);
    Object[] listeners = listenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == KNNListener.class) {
        ((KNNListener) listeners[i + 1]).kNNsChanged(e);
      }
    }
  }

  /**
   * Informs all registered KNNListener that existing kNNs have been removed and
   * as a result some kNNs have been changed.
   *
   * @param removals the ids of the removed kNNs
   * @param updates the ids of kNNs which have been changed due to the removals
   * @see KNNListener
   */
  protected void fireKNNsRemoved(DBIDs removals, DBIDs updates) {
    KNNChangeEvent e = new KNNChangeEvent(this, KNNChangeEvent.Type.DELETE, removals, updates);
    Object[] listeners = listenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == KNNListener.class) {
        ((KNNListener) listeners[i + 1]).kNNsChanged(e);
      }
    }
  }

  /**
   * Adds a {@link KNNListener} which will be invoked when the kNNs of objects
   * are changing.
   *
   * @param l the listener to add
   * @see #removeKNNListener
   * @see KNNListener
   */
  public void addKNNListener(KNNListener l) {
    listenerList.add(KNNListener.class, l);
  }

  /**
   * Removes a {@link KNNListener} previously added with
   * {@link #addKNNListener}.
   *
   * @param l the listener to remove
   * @see #addKNNListener
   * @see KNNListener
   */
  public void removeKNNListener(KNNListener l) {
    listenerList.remove(KNNListener.class, l);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * The parameterizable factory.
   *
   * @author Erich Schubert
   *
   * @opt nodefillcolor LemonChiffon
   * @stereotype factory
   * @navassoc - create - MaterializeKNNPreprocessor
   *
   * @param <O> The object type
   */
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
    /**
     * Index factory.
     *
     * @param k k parameter
     * @param distance distance function
     */
    public Factory(int k, Distance<? super O> distance) {
      super(k, distance);
    }

    @Override
    public MaterializeKNNPreprocessor<O> instantiate(Relation<O> relation) {
      MaterializeKNNPreprocessor<O> instance = new MaterializeKNNPreprocessor<O>(relation, distance, k);
      return instance;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends AbstractMaterializeKNNPreprocessor.Factory.Par<O> {
      @Override
      public Factory<O> make() {
        return new Factory<>(k, distance);
      }
    }
  }
}
