package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DistanceFunction
 * @apiviz.has KNNQuery
 * @apiviz.has KNNListener
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D, KNNResult<D>> {
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
  protected final KNNQuery<O, D> knnQuery;

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
  public MaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(relation, distanceFunction, k);
    this.knnQuery = relation.getDatabase().getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_BULK, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_NO_CACHE);
  }

  /**
   * The actual preprocessing step.
   */
  @Override
  protected void preprocess() {
    createStorage();

    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), getLogger()) : null;

    // Try bulk
    List<? extends KNNResult<D>> kNNList = null;
    if(usebulk) {
      kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
      if(kNNList != null) {
        int i = 0;
        for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
          storage.put(id, kNNList.get(i));
          if(progress != null) {
            progress.incrementProcessed(getLogger());
          }
        }
      }
    }
    else {
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        KNNResult<D> knn = knnQuery.getKNNForDBID(iter, k);
        storage.put(iter, knn);
        if(progress != null) {
          progress.incrementProcessed(getLogger());
        }
      }
    }

    if(progress != null) {
      progress.ensureCompleted(getLogger());
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
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // materialize the new kNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New insertions ocurred, materialize their new kNNs.", getLogger());
    }
    // Bulk-query kNNs
    List<? extends KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(aids, k);
    // Store in storage
    DBIDIter iter = aids.iter();
    for(int i = 0; i < aids.size(); i++, iter.advance()) {
      storage.put(iter, kNNList.get(i));
    }

    // update the affected kNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New insertions ocurred, update the affected kNNs.", getLogger());
    }
    ArrayDBIDs rkNN_ids = updateKNNsAfterInsertion(ids);

    // inform listener
    if(stepprog != null) {
      stepprog.beginStep(3, "New insertions ocurred, inform listeners.", getLogger());
    }
    fireKNNsInserted(ids, rkNN_ids);

    if(stepprog != null) {
      stepprog.setCompleted(getLogger());
    }
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
      KNNResult<D> kNNs = storage.get(iter);
      D knnDist = kNNs.get(kNNs.size() - 1).getDistance();
      // look for new kNNs
      KNNHeap<D> heap = null;
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        D dist = distanceQuery.distance(iter, iter2);
        if(dist.compareTo(knnDist) <= 0) {
          if(heap == null) {
            heap = KNNUtil.newHeap(kNNs);
          }
          heap.add(dist, iter2);
        }
      }
      if(heap != null) {
        kNNs = heap.toKNNList();
        storage.put(iter, kNNs);
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
      KNNResult<D> kNNs = storage.get(iditer);
      for(DBIDIter it = kNNs.iter(); it.valid(); it.advance()) {
        if(idsSet.contains(it)) {
          rkNN_ids.add(iditer);
          break;
        }
      }
    }

    // update the kNNs of the RkNNs
    List<? extends KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k);
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
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    // delete the materialized (old) kNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New deletions ocurred, remove their materialized kNNs.", getLogger());
    }
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      storage.delete(iter);
    }

    // update the affected kNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New deletions ocurred, update the affected kNNs.", getLogger());
    }
    ArrayDBIDs rkNN_ids = updateKNNsAfterDeletion(ids);

    // inform listener
    if(stepprog != null) {
      stepprog.beginStep(3, "New deletions ocurred, inform listeners.", getLogger());
    }
    fireKNNsRemoved(ids, rkNN_ids);

    if(stepprog != null) {
      stepprog.ensureCompleted(getLogger());
    }
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
   * Removes a {@link KNNListener} previously added with {@link #addKNNListener}
   * .
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
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses MaterializeKNNPreprocessor oneway - - «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<O, D, KNNResult<D>> {
    /**
     * Index factory.
     * 
     * @param k k parameter
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction) {
      super(k, distanceFunction);
    }

    @Override
    public MaterializeKNNPreprocessor<O, D> instantiate(Relation<O> relation) {
      MaterializeKNNPreprocessor<O, D> instance = new MaterializeKNNPreprocessor<O, D>(relation, distanceFunction, k);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      @Override
      protected Factory<O, D> makeInstance() {
        return new Factory<O, D>(k, distanceFunction);
      }
    }
  }
}