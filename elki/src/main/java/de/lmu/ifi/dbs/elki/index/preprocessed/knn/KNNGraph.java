package de.lmu.ifi.dbs.elki.index.preprocessed.knn;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.database.datastore.memory.MapStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

public class KNNGraph<O extends NumberVector> extends AbstractMaterializeKNNPreprocessor<O> {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(KNNGraph.class);

  
  /**
   * Random generator
   */
  private final RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param distanceFunction distance function
   * @param k k
   * @param rnd Random generator
   */
  public KNNGraph(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k, RandomFactory rnd) {
    super(relation, distanceFunction, k);
    this.rnd = rnd;
  }
  
  @Override
  public void logStatistics() {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction);
    storage = new MapStore<KNNList>();
    MapStore<KNNHeap> store = new MapStore<KNNHeap>();
    //TODO avoid double storage
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), k, rnd);
      KNNHeap heap = DBIDUtil.newHeap(k);
      for (DBIDIter siter = sample.iter(); siter.valid(); siter.advance()){
        heap.insert(distanceQuery.distance(iditer, siter), siter);
      }
      store.put(iditer,heap);
    }
    int counter = 1;
    
    MapStore<HashSetModifiableDBIDs> trueNeighborHash = new MapStore<HashSetModifiableDBIDs>();    
    HashSetModifiableDBIDs allNeighbors = DBIDUtil.newHashSet(2*k);
    while (counter != 0){
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        //build reverse neighbors
        DBID id = DBIDUtil.deref(iditer);
        DBIDs neighbors = store.get(iditer).toKNNList();
        DBIDs rev = reverse(id,store);
        //join with neighbors
        allNeighbors.addDBIDs(neighbors);
        allNeighbors.addDBIDs(rev);
        trueNeighborHash.put(id, allNeighbors);
        //TODO use other types so that unorderedIterator for KNNHeap is used
      }
      counter = 0;
      //iterate through dataset
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        //for every neighbor of a neighbor do
        DBID id = DBIDUtil.deref(iditer);
        KNNHeap newNeighbors = store.get(id);
        HashSetModifiableDBIDs trueNeighbors = trueNeighborHash.get(id);
        for(DBIDMIter neighboriter = trueNeighbors.iter(); neighboriter.valid(); neighboriter.advance()) {
          DBID neighbor = DBIDUtil.deref(neighboriter);
          HashSetModifiableDBIDs nNeighbors = trueNeighborHash.get(neighbor);
          for (DBIDMIter nniter = nNeighbors.iter(); nniter.valid(); nniter.advance()){
            DBID nn = DBIDUtil.deref(nniter);
            if (nn.compareTo(id)!= 0){
                //calculate similarity of v and u2
                double distance = distanceQuery.distance(neighbor,nn);
                // TODO Heap should have always the same size --> contains-method?
                // TODO 1 if heap is changed, 0 if not --> also better with contains-method
                double newDistance = newNeighbors.insert(distance, id);
                if (distance <= newDistance){
                  counter=1;
                }
            } 
          }
        }
      }
      allNeighbors.clear();
    }
    //convert store to storage
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNHeap heap = store.get(iditer);
      KNNList list = heap.toKNNList();
      storage.put(iditer, list);
    }
  }

  private HashSetModifiableDBIDs reverse(DBID id, MapStore<KNNHeap> store) {
    HashSetModifiableDBIDs rev = DBIDUtil.newHashSet(k);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNList neighbors = store.get(iditer).toKNNList();
      if (neighbors.contains(id)){
        rev.add(id);
      }
    }
    return rev;
  }

  @Override
  protected Logging getLogger() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLongName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getShortName() {
    // TODO Auto-generated method stub
    return null;
  }

}

