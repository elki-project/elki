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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.query.rknn.PreprocessorRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNList;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors and the reverse k
 * nearest neighbors (and their distances) to each database object.
 * 
 * @author Elke Achtert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN and RkNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors and the reverse k nearest neighbors of objects of a database.")
public class MaterializeKNNAndRKNNPreprocessor<O, D extends Distance<D>> extends MaterializeKNNPreprocessor<O, D> implements RKNNIndex<O> {
  /**
   * Logger to use.
   */
  private static final Logging logger = Logging.getLogger(MaterializeKNNAndRKNNPreprocessor.class);

  /**
   * Additional data storage for RkNN.
   */
  private WritableDataStore<SortedSet<DistanceResultPair<D>>> materialized_RkNN;

  /**
   * Constructor.
   * 
   * @param relation Relation to process
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public MaterializeKNNAndRKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(relation, distanceFunction, k);
  }

  @Override
  protected void preprocess() {
    createStorage();
    materialized_RkNN = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, Set.class);
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing k nearest neighbors and reverse k nearest neighbors (k=" + k + ")", relation.size(), getLogger()) : null;
    materializeKNNAndRKNNs(DBIDUtil.ensureArray(relation.getDBIDs()), progress);
  }

  /**
   * Materializes the kNNs and RkNNs of the specified object IDs.
   * 
   * @param ids the IDs of the objects
   */
  private void materializeKNNAndRKNNs(ArrayDBIDs ids, FiniteProgress progress) {
    // add an empty list to each rknn
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(materialized_RkNN.get(iter) == null) {
        materialized_RkNN.put(iter, new TreeSet<DistanceResultPair<D>>());
      }
    }

    // knn query
    List<KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      KNNResult<D> kNNs = kNNList.get(i);
      storage.put(id, kNNs);
      for(DistanceResultPair<D> kNN : kNNs) {
        Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(kNN);
        rknns.add(new GenericDistanceResultPair<D>(kNN.getDistance(), id));
      }
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }

    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
  }

  @Override
  protected void objectsInserted(DBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // materialize the new kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New insertions ocurred, materialize their new kNNs and RkNNs.", getLogger());
    }
    materializeKNNAndRKNNs(aids, null);

    // update the old kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New insertions ocurred, update the affected kNNs and RkNNs.", getLogger());
    }
    ArrayDBIDs rkNN_ids = updateKNNsAndRkNNs(ids);

    // inform listener
    if(stepprog != null) {
      stepprog.beginStep(3, "New insertions ocurred, inform listeners.", getLogger());
    }
    fireKNNsInserted(ids, rkNN_ids);

    if(stepprog != null) {
      stepprog.ensureCompleted(getLogger());
    }
  }

  /**
   * Updates the kNNs and RkNNs after insertion of the specified ids.
   * 
   * @param ids the ids of newly inserted objects causing a change of
   *        materialized kNNs and RkNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private ArrayDBIDs updateKNNsAndRkNNs(DBIDs ids) {
    ArrayModifiableDBIDs rkNN_ids = DBIDUtil.newArray();
    DBIDs oldids = DBIDUtil.difference(relation.getDBIDs(), ids);
    for (DBIDIter iter = oldids.iter(); iter.valid(); iter.advance()) {
      KNNResult<D> kNNs = storage.get(iter);
      D knnDist = kNNs.getKNNDistance();
      // look for new kNNs
      KNNHeap<D> heap = null;
      for (DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        D dist = distanceQuery.distance(iter, iter2);
        if(dist.compareTo(knnDist) <= 0) {
          if(heap == null) {
            heap = new KNNHeap<D>(k);
            heap.addAll(kNNs);
          }
          heap.add(dist, iter2);
        }
      }
      if(heap != null) {
        KNNList<D> newKNNs = heap.toKNNList();
        storage.put(iter, newKNNs);

        // get the difference
        int i = 0;
        int j = 0;
        List<DistanceResultPair<D>> added = new ArrayList<DistanceResultPair<D>>();
        List<DistanceResultPair<D>> removed = new ArrayList<DistanceResultPair<D>>();
        while(i < kNNs.size() && j < newKNNs.size()) {
          DistanceResultPair<D> drp1 = kNNs.get(i);
          DistanceResultPair<D> drp2 = newKNNs.get(j);
          if(!DBIDUtil.equal(drp1, drp2)) {
            added.add(drp2);
            j++;
          }
          else {
            i++;
            j++;
          }
        }
        if(i != j) {
          for(; i < kNNs.size(); i++)
            removed.add(kNNs.get(i));
        }
        // add new RkNN
        for(DistanceResultPair<D> drp : added) {
          Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(drp);
          rknns.add(new GenericDistanceResultPair<D>(drp.getDistance(), iter.getDBID()));
        }
        // remove old RkNN
        for(DistanceResultPair<D> drp : removed) {
          Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(drp);
          rknns.remove(new GenericDistanceResultPair<D>(drp.getDistance(), iter.getDBID()));
        }

        rkNN_ids.add(iter);
      }
    }
    return rkNN_ids;
  }

  @Override
  protected void objectsRemoved(DBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // delete the materialized (old) kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New deletions ocurred, remove their materialized kNNs and RkNNs.", getLogger());
    }
    List<KNNResult<D>> kNNs = new ArrayList<KNNResult<D>>(ids.size());
    List<List<DistanceResultPair<D>>> rkNNs = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for (DBIDIter iter = aids.iter(); iter.valid(); iter.advance()) {
      kNNs.add(storage.get(iter));
      storage.delete(iter);
      rkNNs.add(new ArrayList<DistanceResultPair<D>>(materialized_RkNN.get(iter)));
      materialized_RkNN.delete(iter);
    }
    ArrayDBIDs kNN_ids = extractAndRemoveIDs(kNNs, aids);
    ArrayDBIDs rkNN_ids = extractAndRemoveIDs(rkNNs, aids);

    // update the affected kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New deletions ocurred, update the affected kNNs and RkNNs.", getLogger());
    }
    // update the kNNs of the RkNNs
    List<KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k);
    for(int i = 0; i < rkNN_ids.size(); i++) {
      DBID id = rkNN_ids.get(i);
      storage.put(id, kNNList.get(i));
      for(DistanceResultPair<D> kNN : kNNList.get(i)) {
        materialized_RkNN.get(kNN).add(new GenericDistanceResultPair<D>(kNN.getDistance(), id));
      }
    }
    // update the RkNNs of the kNNs
    SetDBIDs idsSet = DBIDUtil.ensureSet(ids);
    for(int i = 0; i < kNN_ids.size(); i++) {
      DBID id = kNN_ids.get(i);
      SortedSet<DistanceResultPair<D>> rkNN = materialized_RkNN.get(id);
      for(Iterator<DistanceResultPair<D>> it = rkNN.iterator(); it.hasNext();) {
        DistanceResultPair<D> drp = it.next();
        if(idsSet.contains(drp)) {
          it.remove();
        }
      }
    }

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
   * Returns the materialized kNNs of the specified id.
   * 
   * @param id the query id
   * @return the kNNs
   */
  public KNNResult<D> getKNN(DBID id) {
    return storage.get(id);
  }

  /**
   * Returns the materialized RkNNs of the specified id.
   * 
   * @param id the query id
   * @return the RkNNs
   */
  public List<DistanceResultPair<D>> getRKNN(DBIDRef id) {
    SortedSet<DistanceResultPair<D>> rKNN = materialized_RkNN.get(id);
    if(rKNN == null)
      return null;
    return new ArrayList<DistanceResultPair<D>>(rKNN);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    if(!this.distanceFunction.equals(distanceQuery.getDistanceFunction())) {
      return null;
    }
    // k max supported?
    for(Object hint : hints) {
      if(hint instanceof Integer) {
        if(((Integer) hint) > k) {
          return null;
        }
        break;
      }
    }
    return new PreprocessorRKNNQuery<O, S>(relation, (MaterializeKNNAndRKNNPreprocessor<O, S>) this);
  }

  @Override
  public String getLongName() {
    return "kNN and RkNN Preprocessor";
  }

  @Override
  public String getShortName() {
    return "knn and rknn preprocessor";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The parameterizable factory.
   * 
   * @author Elke Achtert
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O, D extends Distance<D>> extends MaterializeKNNPreprocessor.Factory<O, D> {
    /**
     * Constructor.
     * 
     * @param k k
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction) {
      super(k, distanceFunction);
    }

    @Override
    public MaterializeKNNAndRKNNPreprocessor<O, D> instantiate(Relation<O> relation) {
      MaterializeKNNAndRKNNPreprocessor<O, D> instance = new MaterializeKNNAndRKNNPreprocessor<O, D>(relation, distanceFunction, k);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O, D extends Distance<D>> extends MaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      @Override
      protected Factory<O, D> makeInstance() {
        return new Factory<O, D>(k, distanceFunction);
      }
    }
  }
}