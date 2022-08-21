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

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.rknn.PreprocessorRKNNQuery;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.RKNNIndex;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors and the reverse k
 * nearest neighbors (and their distances) to each database object.
 * <p>
 * BUG: This class currently does <em>not</em> seem able to correctly keep track
 * of the nearest neighbors?
 * <p>
 * TODO: for better performance, we would need some
 * ModifiableDoubleDBIDHashSet or TreeSet to store the rkNN.
 *
 * @author Elke Achtert
 * @since 0.4.0
 *
 * @param <O> the type of database objects the preprocessor can be applied to
 *        the type of distance the used distance function will return
 */
@Title("Materialize kNN and RkNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors and the reverse k nearest neighbors of objects of a database.")
public class MaterializeKNNAndRKNNPreprocessor<O> extends MaterializeKNNPreprocessor<O> implements RKNNIndex<O> {
  /**
   * Logger to use.
   */
  private static final Logging LOG = Logging.getLogger(MaterializeKNNAndRKNNPreprocessor.class);

  /**
   * Additional data storage for RkNN.
   */
  private WritableDataStore<ModifiableDoubleDBIDList> storageRkNN;

  /**
   * Constructor.
   *
   * @param relation Relation to process
   * @param distance the distance function to use
   * @param k query k
   */
  public MaterializeKNNAndRKNNPreprocessor(Relation<O> relation, Distance<? super O> distance, int k) {
    super(relation, distance, k);
  }

  @Override
  protected void preprocess() {
    createStorage();
    storageRkNN = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, ModifiableDoubleDBIDList.class);
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Materializing k nearest neighbors and reverse k nearest neighbors (k=" + k + ")", relation.size(), getLogger()) : null;
    materializeKNNAndRKNNs(DBIDUtil.ensureArray(relation.getDBIDs()), progress);
  }

  /**
   * Materializes the kNNs and RkNNs of the specified object IDs.
   *
   * @param ids the IDs of the objects
   */
  private void materializeKNNAndRKNNs(ArrayDBIDs ids, FiniteProgress progress) {
    // add an empty list to each rknn
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(storageRkNN.get(iter) == null) {
        storageRkNN.put(iter, DBIDUtil.newDistanceDBIDList());
      }
    }

    // knn query
    for(DBIDArrayIter id = ids.iter(); id.valid(); id.advance()) {
      KNNList kNNs = knnQuery.getKNN(id, k);
      storage.put(id, kNNs);
      // inverse rkNN index:
      for(DoubleDBIDListIter iter = kNNs.iter(); iter.valid(); iter.advance()) {
        storageRkNN.get(iter).add(iter.doubleValue(), id);
      }
      LOG.incrementProcessed(progress);
    }

    LOG.ensureCompleted(progress);
  }

  @Override
  protected void objectsInserted(DBIDs ids) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // materialize the new kNNs and RkNNs
    LOG.beginStep(stepprog, 1, "New insertions ocurred, materialize their new kNNs and RkNNs.");
    materializeKNNAndRKNNs(aids, null);

    // update the old kNNs and RkNNs
    LOG.beginStep(stepprog, 2, "New insertions ocurred, update the affected kNNs and RkNNs.");
    DBIDs rkNN_ids = updateKNNsAndRkNNs(ids);

    // inform listener
    LOG.beginStep(stepprog, 3, "New insertions ocurred, inform listeners.");
    fireKNNsInserted(ids, rkNN_ids);
    LOG.setCompleted(stepprog);
  }

  /**
   * Updates the kNNs and RkNNs after insertion of the specified ids.
   *
   * @param ids the ids of newly inserted objects causing a change of
   *        materialized kNNs and RkNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private DBIDs updateKNNsAndRkNNs(DBIDs ids) {
    ArrayModifiableDBIDs rkNN_ids = DBIDUtil.newArray();
    DBIDs oldids = DBIDUtil.difference(relation.getDBIDs(), ids);
    for(DBIDIter id = oldids.iter(); id.valid(); id.advance()) {
      KNNList oldkNNs = storage.get(id);
      double knnDist = oldkNNs.getKNNDistance();
      // look for new kNNs
      KNNHeap heap = null;
      for(DBIDIter newid = ids.iter(); newid.valid(); newid.advance()) {
        double dist = distanceQuery.distance(id, newid);
        if(dist <= knnDist) {
          // New id changes the kNNs of oldid.
          heap = heap != null ? heap : DBIDUtil.newHeap(oldkNNs);
          heap.insert(dist, newid);
        }
      }
      // kNNs for oldid have changed:
      if(heap != null) {
        KNNList newkNNs = heap.toKNNList();
        storage.put(id, newkNNs);

        // get the difference
        ModifiableDoubleDBIDList added = DBIDUtil.newDistanceDBIDList(),
            removed = DBIDUtil.newDistanceDBIDList();
        DoubleDBIDListIter olditer = oldkNNs.iter(), newiter = newkNNs.iter();
        while(olditer.valid() && newiter.valid()) {
          if(DBIDUtil.equal(olditer, newiter)) {
            olditer.advance();
            newiter.advance();
            continue;
          }
          double newd = newiter.doubleValue(), oldd = olditer.doubleValue();
          if(newd < oldd || (newd == oldd && !oldkNNs.contains(newiter))) {
            added.add(newiter.doubleValue(), newiter);
            newiter.advance();
          }
          else if(oldd < newd || (oldd == newd && !newkNNs.contains(olditer))) {
            removed.add(olditer.doubleValue(), olditer);
            olditer.advance();
          }
          else {
            throw new IllegalStateException("Unexpected third case, needs debug!");
          }
        }
        for(; olditer.valid(); olditer.advance()) {
          removed.add(olditer.doubleValue(), olditer);
        }
        for(; newiter.valid(); newiter.advance()) {
          added.add(newiter.doubleValue(), newiter);
        }
        // add new RkNN
        for(DoubleDBIDListIter newnn = added.iter(); newnn.valid(); newnn.advance()) {
          storageRkNN.get(newnn).add(newnn.doubleValue(), id);
        }
        // remove old RkNN
        for(DoubleDBIDListIter oldnn = removed.iter(); oldnn.valid(); oldnn.advance()) {
          for(DoubleDBIDListMIter iter = storageRkNN.get(oldnn).iter(); iter.valid(); iter.advance()) {
            if(DBIDUtil.equal(iter, id)) {
              iter.remove();
              break;
            }
          }
        }
        rkNN_ids.add(id);
      }
    }
    return rkNN_ids;
  }

  @Override
  protected void objectsRemoved(DBIDs ids) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;
    // delete the materialized (old) kNNs and RkNNs
    LOG.beginStep(stepprog, 1, "New deletions ocurred, remove their materialized kNNs and RkNNs.");
    ModifiableDBIDs kNNs = DBIDUtil.newHashSet(), rkNNs = DBIDUtil.newHashSet();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      kNNs.addDBIDs(storage.get(iter));
      storage.delete(iter);
      rkNNs.addDBIDs(storageRkNN.get(iter));
      storageRkNN.delete(iter);
    }
    // Keep only those IDs not also removed
    kNNs.removeDBIDs(ids);
    rkNNs.removeDBIDs(ids);

    // update the affected kNNs and RkNNs
    LOG.beginStep(stepprog, 2, "New deletions ocurred, update the affected kNNs and RkNNs.");
    // remove objects from RkNNs of objects (in kNN lists)
    {
      SetDBIDs idsSet = DBIDUtil.ensureSet(ids);
      for(DBIDIter nn = kNNs.iter(); nn.valid(); nn.advance()) {
        ModifiableDoubleDBIDList rkNN = storageRkNN.get(nn);
        for(DoubleDBIDListMIter it = rkNN.iter(); it.valid(); it.advance()) {
          if(idsSet.contains(it)) {
            it.remove();
            break;
          }
        }
      }
    }
    // Recompute the kNN for affected objects (in rkNN lists)
    {
      for(DBIDIter reknn = rkNNs.iter(); reknn.valid(); reknn.advance()) {
        KNNList rknnlist = knnQuery.getKNN(reknn, k);
        if(rknnlist == null) {
          LOG.warning("BUG in online kNN/RkNN maintainance: " + DBIDUtil.toString(reknn) + " no longer in database.");
          continue;
        }
        storage.put(reknn, rknnlist);
        for(DoubleDBIDListIter it = rknnlist.iter(); it.valid(); it.advance()) {
          ModifiableDoubleDBIDList rstor = storageRkNN.get(it);
          if(!rstor.contains(reknn)) {
            rstor.add(it.doubleValue(), reknn);
          }
        }
      }
    }

    // inform listener
    LOG.beginStep(stepprog, 3, "New deletions ocurred, inform listeners.");
    fireKNNsRemoved(ids, rkNNs);
    LOG.setCompleted(stepprog);
  }

  /**
   * Returns the materialized kNNs of the specified id.
   *
   * @param id the query id
   * @return the kNNs
   */
  public KNNList getKNN(DBID id) {
    return storage.get(id);
  }

  /**
   * Returns the materialized RkNNs of the specified id.
   *
   * @param id the query id
   * @return the RkNNs
   */
  public DoubleDBIDList getRKNN(DBIDRef id) {
    // TODO: do not sort?
    return storageRkNN.get(id).sort();
  }

  @Override
  public RKNNSearcher<O> rkNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return null; // not possible
  }

  @Override
  public RKNNSearcher<DBIDRef> rkNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return relation == distanceQuery.getRelation() && distance.equals(distanceQuery.getDistance()) //
        && maxk == k ? new PreprocessorRKNNQuery<>(relation, this) : null;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * The parameterizable factory.
   *
   * @author Elke Achtert
   *
   * @param <O> The object type
   */
  public static class Factory<O> extends MaterializeKNNPreprocessor.Factory<O> {
    /**
     * Constructor.
     *
     * @param k k
     * @param distance distance function
     */
    public Factory(int k, Distance<? super O> distance) {
      super(k, distance);
    }

    @Override
    public MaterializeKNNAndRKNNPreprocessor<O> instantiate(Relation<O> relation) {
      return new MaterializeKNNAndRKNNPreprocessor<>(relation, distance, k);
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends MaterializeKNNPreprocessor.Factory.Par<O> {
      @Override
      public Factory<O> make() {
        return new Factory<>(k, distance);
      }
    }
  }
}
