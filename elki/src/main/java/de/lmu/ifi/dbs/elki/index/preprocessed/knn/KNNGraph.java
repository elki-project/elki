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

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.memory.MapStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleIntegerMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

public class KNNGraph<O> extends AbstractMaterializeKNNPreprocessor<O> {
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
    // TODO which statistics to log?
    
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction);
    
    storage = new MapStore<KNNList>();
    MapStore<KNNHeap> store = new MapStore<KNNHeap>();
    
    IndefiniteProgress progress = LOG.isVerbose() ? new IndefiniteProgress("KNNGraph iteration", LOG) : null;
    
    Random random = rnd.getSingleThreadedRandom();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id = DBIDUtil.deref(iditer);
      
      final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), k, random);
      KNNHeap heap = DBIDUtil.newHeap(k);
      for (DBIDIter siter = sample.iter(); siter.valid(); siter.advance()){
        heap.insert(distanceQuery.distance(iditer, siter), siter);
      }
      store.put(id,heap);
    }
    
    int counter = 1;
    
    MapStore<HashSetModifiableDBIDs> trueNeighborHash = new MapStore<HashSetModifiableDBIDs>();    
    while (counter != 0){
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        //build reverse neighbors
        DBID id = DBIDUtil.deref(iditer);
        KNNHeap heap = store.get(iditer);
        DBIDs rev = reverse(id,store);
//        String s = "";
//        for (DBIDIter riter = rev.iter(); riter.valid(); riter.advance()){
//          s+=DBIDUtil.deref(riter)+"\t";
//        }
//        System.out.println(s);
        //join neighbors with reverse neighbors
        HashSetModifiableDBIDs allNeighbors = DBIDUtil.newHashSet(2*k);
        for (DoubleDBIDListIter heapiter = heap.unorderedIterator(); heapiter.valid(); heapiter.advance()){
          allNeighbors.add(heapiter);
        }
        allNeighbors.addDBIDs(rev);
        //join neighbors with reverse
        
        trueNeighborHash.put(id, allNeighbors);
      }
      counter = 0;
      //iterate through dataset
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        //for every neighbor of a neighbor do
        DBID id = DBIDUtil.deref(iditer);
        KNNHeap newNeighbors = store.get(iditer);

        HashSetModifiableDBIDs trueNeighbors = trueNeighborHash.get(iditer);
        
        for(DBIDMIter neighboriter = trueNeighbors.iter(); neighboriter.valid(); neighboriter.advance()) {
          HashSetModifiableDBIDs nNeighbors = trueNeighborHash.get(neighboriter);
          
          for (DBIDMIter nniter = nNeighbors.iter(); nniter.valid(); nniter.advance()){
            if (DBIDUtil.compare(id, nniter)!= 0){
                //see if actual object is already contained in hash
                boolean contained=false;
                for (DoubleDBIDListIter heapiter = newNeighbors.unorderedIterator(); heapiter.valid(); heapiter.advance()){                 
                  if (DBIDUtil.compare(heapiter, nniter)==0){
                    contained=true;
                  }
                }
                if (!contained){
                  //calculate similarity of v and u2
                  double distance = distanceQuery.distance(iditer,nniter);
                  double newDistance = newNeighbors.insert(distance,nniter);
                  if (distance <= newDistance){
                    counter++;
                  }
                }
            } 
          }
        }
        store.put(iditer, newNeighbors);
      }
      LOG.incrementProcessed(progress);
    }
    //convert store to storage
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNHeap heap = store.get(iditer);
      KNNList list = heap.toKNNList();
      storage.put(iditer, list);
    }
    LOG.setCompleted(progress);
  }

  private String print(KNNHeap heap) {
    String s ="";
    for (DoubleDBIDListIter heapiter = heap.unorderedIterator(); heapiter.valid(); heapiter.advance()){
      s+=DBIDUtil.deref(heapiter)+"\t";
    }
    return s;
  }

  private HashSetModifiableDBIDs reverse(DBID id, MapStore<KNNHeap> store) {
    HashSetModifiableDBIDs rev = DBIDUtil.newHashSet(k);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNHeap heap = store.get(iditer);
      for (DoubleDBIDListIter heapiter = heap.unorderedIterator(); heapiter.valid(); heapiter.advance()){
        if (id.compareTo(heapiter)==0){
          rev.add(iditer);
        }
      }
    }
    return rev;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "NNDescent kNN";
  }

  @Override
  public String getShortName() {
    return "nn-descent-knn";
  }
  
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
    /**
     * Random generator
     */
    private final RandomFactory rnd;

    /**
     * Constructor.
     *
     * @param k K
     * @param distanceFunction distance function
     * @param rnd Random generator
     */
    public Factory(int k, DistanceFunction<? super O> distanceFunction, RandomFactory rnd) {
      super(k, distanceFunction);
      this.rnd = rnd;
    }

    @Override
    public KNNGraph<O> instantiate(Relation<O> relation) {
      return new KNNGraph<>(relation, distanceFunction, k, rnd);
    }

    /**
     * Parameterization class
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     *
     * @param <O> Object type
     */
    public static class Parameterizer<O> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O> {
      /**
       * Random number generator seed.
       *
       * <p>
       * Key: {@code -knngraph.seed}
       * </p>
       */
      public static final OptionID SEED_ID = new OptionID("randomknn.seed", "The random number seed.");
      
      /**
       * Random generator
       */
      private RandomFactory rnd;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        RandomParameter rndP = new RandomParameter(SEED_ID);
        if(config.grab(rndP)) {
          rnd = rndP.getValue();
        }
      }

      @Override
      protected KNNGraph.Factory<O> makeInstance() {
        return new KNNGraph.Factory<>(k, distanceFunction, rnd);
      }
    }
  }
}



