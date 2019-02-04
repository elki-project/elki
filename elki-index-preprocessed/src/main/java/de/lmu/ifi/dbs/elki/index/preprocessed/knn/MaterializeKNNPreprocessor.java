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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.DynamicIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * <p>
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF}.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @has - - - DistanceFunction
 * @has - - - KNNQuery
 * @has - - - KNNListener
 *
 * @param <O> the type of database objects the preprocessor can be applied to
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
@Alias("de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor")
public class MaterializeKNNPreprocessor<O> extends AbstractMaterializeKNNPreprocessor<O> implements DynamicIndex {
  /**
   * Logger to use.
   */
  private static final Logging LOG = Logging.getLogger(MaterializeKNNPreprocessor.class);

  /**
   * Flag to use bulk operations.
   *
   * TODO: right now, bulk is not that good - so don't use
   */
  private static final boolean usebulk = false;

  /**
   * KNNQuery instance to use.
   */
  protected final KNNQuery<O> knnQuery;

  /**
   * Holds the listener.
   */
  protected final EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor with preprocessing step.
   *
   * @param relation Relation to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public MaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k) {
    super(relation, distanceFunction, k);
    this.knnQuery = relation.getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_BULK, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_NO_CACHE);
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
    List<? extends KNNList> kNNList = null;
    if(usebulk) {
      kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
      if(kNNList != null) {
        int i = 0;
        for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
          storage.put(id, kNNList.get(i));
          log.incrementProcessed(progress);
        }
      }
    }
    else {
      final boolean ismetric = getDistanceQuery().getDistanceFunction().isMetric();
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        if(ismetric && storage.get(iter) != null) {
          log.incrementProcessed(progress);
          continue; // Previously computed (duplicate point?)
        }
        KNNList knn = knnQuery.getKNNForDBID(iter, k);
        storage.put(iter, knn);
        if(ismetric) {
          for(DoubleDBIDListIter it = knn.iter(); it.valid() && it.doubleValue() == 0.; it.advance()) {
            storage.put(it, knn); // Reuse
          }
        }
        log.incrementProcessed(progress);
      }
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
    // Bulk-query kNNs
    List<? extends KNNList> kNNList = knnQuery.getKNNForBulkDBIDs(aids, k);
    // Store in storage
    DBIDIter iter = aids.iter();
    for(int i = 0; i < aids.size(); i++, iter.advance()) {
      storage.put(iter, kNNList.get(i));
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
    List<? extends KNNList> kNNList = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k);
    DBIDIter iter = rkNN_ids.iter();
    for(int i = 0; i < rkNN_ids.size(); i++, iter.advance()) {
      storage.put(iter, kNNList.get(i));
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

    log.ensureCompleted(stepprog);
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
  public String getLongName() {
    return "kNN Preprocessor";
  }

  @Override
  public String getShortName() {
    return "knn preprocessor";
  }

  @Override
  public void logStatistics() {
    // TODO: can we log some sensible statistics?
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
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O> distanceFunction) {
      super(k, distanceFunction);
    }

    @Override
    public MaterializeKNNPreprocessor<O> instantiate(Relation<O> relation) {
      MaterializeKNNPreprocessor<O> instance = new MaterializeKNNPreprocessor<O>(relation, distanceFunction, k);
      return instance;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer<O> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O> {
      @Override
      protected Factory<O> makeInstance() {
        return new Factory<>(k, distanceFunction);
      }
    }
  }
}
