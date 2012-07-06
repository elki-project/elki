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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.PreprocessorRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultUtil;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors and the reverse k
 * nearest neighbors (and their distances) to each database object.
 * 
 * @author Elke Achtert
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
// TODO: rewrite the double optimization. Maybe use a specialized subclass?
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
  private WritableDataStore<TreeSet<DistanceDBIDPair<D>>> materialized_RkNN;

  /**
   * Use optimizations for double values
   */
  protected boolean doubleOptimize;

  /**
   * Constructor.
   * 
   * @param relation Relation to process
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public MaterializeKNNAndRKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(relation, distanceFunction, k);
    this.doubleOptimize = DistanceUtil.isDoubleDistanceFunction(distanceFunction);
  }

  @Override
  protected void preprocess() {
    createStorage();
    materialized_RkNN = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, TreeSet.class);
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
    Comparator<DistanceDBIDPair<D>> comp = DistanceDBIDResultUtil.distanceComparator();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(materialized_RkNN.get(iter) == null) {
        materialized_RkNN.put(iter, new TreeSet<DistanceDBIDPair<D>>(comp));
      }
    }

    // knn query
    List<? extends KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
    int i = 0;
    for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
      KNNResult<D> kNNs = kNNList.get(i);
      storage.put(id, kNNs);
      for(DistanceDBIDResultIter<D> iter = kNNs.iter(); iter.valid(); iter.advance()) {
        TreeSet<DistanceDBIDPair<D>> rknns = materialized_RkNN.get(iter);
        rknns.add(makePair(iter, id));
      }
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }

    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
  }

  @SuppressWarnings("unchecked")
  private DistanceDBIDPair<D> makePair(DistanceDBIDResultIter<D> iter, DBIDIter id) {
    if(doubleOptimize) {
      return (DistanceDBIDPair<D>) DBIDFactory.FACTORY.newDistancePair(((DoubleDistanceDBIDPair) iter.getDistancePair()).doubleDistance(), id);
    }
    return DBIDFactory.FACTORY.newDistancePair(iter.getDistance(), id);
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
    for(DBIDIter id = oldids.iter(); id.valid(); id.advance()) {
      KNNResult<D> oldkNNs = storage.get(id);
      D knnDist = oldkNNs.getKNNDistance();
      // look for new kNNs
      KNNHeap<D> heap = null;
      for(DBIDIter newid = ids.iter(); newid.valid(); newid.advance()) {
        D dist = distanceQuery.distance(id, newid);
        if(dist.compareTo(knnDist) <= 0) {
          // New id changes the kNNs of oldid.
          if(heap == null) {
            heap = KNNUtil.newHeap(oldkNNs);
          }
          heap.add(dist, newid);
        }
      }
      // kNNs for oldid have changed:
      if(heap != null) {
        KNNResult<D> newkNNs = heap.toKNNList();
        storage.put(id, newkNNs);

        // get the difference
        int i = 0;
        int j = 0;
        GenericDistanceDBIDList<D> added = new GenericDistanceDBIDList<D>();
        GenericDistanceDBIDList<D> removed = new GenericDistanceDBIDList<D>();
        // TODO: use iterators.
        while(i < oldkNNs.size() && j < newkNNs.size()) {
          DistanceDBIDPair<D> drp1 = oldkNNs.get(i);
          DistanceDBIDPair<D> drp2 = newkNNs.get(j);
          // NOTE: we assume that on ties they are ordered the same way!
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
          for(; i < oldkNNs.size(); i++) {
            removed.add(oldkNNs.get(i));
          }
          for(; j < newkNNs.size(); i++) {
            added.add(newkNNs.get(i));
          }
        }
        // add new RkNN
        for(DistanceDBIDResultIter<D> newnn = added.iter(); newnn.valid(); newnn.advance()) {
          TreeSet<DistanceDBIDPair<D>> rknns = materialized_RkNN.get(newnn);
          rknns.add(makePair(newnn, id));
        }
        // remove old RkNN
        for(DistanceDBIDResultIter<D> oldnn = removed.iter(); oldnn.valid(); oldnn.advance()) {
          TreeSet<DistanceDBIDPair<D>> rknns = materialized_RkNN.get(oldnn);
          rknns.remove(makePair(oldnn, id));
        }

        rkNN_ids.add(id);
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
    // Temporary storage of removed lists
    List<KNNResult<D>> kNNs = new ArrayList<KNNResult<D>>(ids.size());
    List<TreeSet<DistanceDBIDPair<D>>> rkNNs = new ArrayList<TreeSet<DistanceDBIDPair<D>>>(ids.size());
    for(DBIDIter iter = aids.iter(); iter.valid(); iter.advance()) {
      kNNs.add(storage.get(iter));
      storage.delete(iter);
      rkNNs.add(materialized_RkNN.get(iter));
      materialized_RkNN.delete(iter);
    }
    // Keep only those IDs not also removed
    ArrayDBIDs kNN_ids = affectedkNN(kNNs, aids);
    ArrayDBIDs rkNN_ids = affectedRkNN(rkNNs, aids);

    // update the affected kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New deletions ocurred, update the affected kNNs and RkNNs.", getLogger());
    }
    // Recompute the kNN for affected objects (in rkNN lists)
    {
      List<? extends KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k);
      int i = 0;
      for(DBIDIter reknn = rkNN_ids.iter(); reknn.valid(); reknn.advance(), i++) {
        storage.put(reknn, kNNList.get(i));
        for(DistanceDBIDResultIter<D> it = kNNList.get(i).iter(); it.valid(); it.advance()) {
          materialized_RkNN.get(it).add(makePair(it, reknn));
        }
      }
    }
    // remove objects from RkNNs of obejcts (in kNN lists)
    {
      SetDBIDs idsSet = DBIDUtil.ensureSet(ids);
      for(DBIDIter nn = kNN_ids.iter(); nn.valid(); nn.advance()) {
        TreeSet<DistanceDBIDPair<D>> rkNN = materialized_RkNN.get(nn);
        for(Iterator<DistanceDBIDPair<D>> it = rkNN.iterator(); it.hasNext();) {
          if(idsSet.contains(it.next())) {
            it.remove();
          }
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
   * Extracts and removes the DBIDs in the given collections.
   * 
   * @param extraxt a list of lists of DistanceResultPair to extract
   * @param remove the ids to remove
   * @return the DBIDs in the given collection
   */
  protected ArrayDBIDs affectedkNN(List<? extends KNNResult<D>> extraxt, DBIDs remove) {
    HashSetModifiableDBIDs ids = DBIDUtil.newHashSet();
    for(KNNResult<D> drps : extraxt) {
      for(DBIDIter iter = drps.iter(); iter.valid(); iter.advance()) {
        ids.add(iter);
      }
    }
    ids.removeDBIDs(remove);
    // Convert back to array
    return DBIDUtil.newArray(ids);
  }

  /**
   * Extracts and removes the DBIDs in the given collections.
   * 
   * @param extraxt a list of lists of DistanceResultPair to extract
   * @param remove the ids to remove
   * @return the DBIDs in the given collection
   */
  protected ArrayDBIDs affectedRkNN(List<? extends Collection<DistanceDBIDPair<D>>> extraxt, DBIDs remove) {
    HashSetModifiableDBIDs ids = DBIDUtil.newHashSet();
    for(Collection<DistanceDBIDPair<D>> drps : extraxt) {
      for(DistanceDBIDPair<D> drp : drps) {
        ids.add(drp);
      }
    }
    ids.removeDBIDs(remove);
    // Convert back to array
    return DBIDUtil.newArray(ids);
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
  public GenericDistanceDBIDList<D> getRKNN(DBIDRef id) {
    TreeSet<DistanceDBIDPair<D>> rKNN = materialized_RkNN.get(id);
    if(rKNN == null) {
      return null;
    }
    GenericDistanceDBIDList<D> ret = new GenericDistanceDBIDList<D>(rKNN.size());
    for(DistanceDBIDPair<D> pair : rKNN) {
      ret.add(pair);
    }
    ret.sort();
    return ret;
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