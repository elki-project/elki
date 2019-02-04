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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * NN-desent (also known as KNNGraph) is an approximate nearest neighbor search
 * algorithm beginning with a random sample, then iteratively refining this
 * sample until.
 * <p>
 * Reference:
 * <p>
 * W. Dong and C. Moses and K. Li<br>
 * Efficient k-nearest neighbor graph construction for generic similarity
 * measures<br>
 * Proc. 20th Int. Conf. on World Wide Web (WWW'11)
 *
 * @author Evelyn Kirner
 * @since 0.7.5
 *
 * @param <O> Object type
 */
@Reference(authors = "W. Dong, C. Moses, K. Li", //
    title = "Efficient k-nearest neighbor graph construction for generic similarity measures", //
    booktitle = "Proc. 20th Int. Conf. on World Wide Web (WWW'11)", //
    url = "https://doi.org/10.1145/1963405.1963487", //
    bibkey = "DBLP:conf/www/DongCL11")
public class NNDescent<O> extends AbstractMaterializeKNNPreprocessor<O> {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(NNDescent.class);

  /**
   * Log prefix.
   */
  private String prefix = getClass().getCanonicalName();

  /**
   * Random generator
   */
  private final RandomFactory rnd;

  /**
   * early termination parameter
   */
  private double delta = 0.001;

  /**
   * sample rate
   */
  private double rho = 1.0;

  /**
   * maximum number of iterations
   */
  private int iterations = 100;

  /**
   * Do not use initial neighbors
   */
  private boolean noInitialNeighbors;

  /**
   * store for neighbors
   */
  private WritableDataStore<KNNHeap> store;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param distanceFunction distance function
   * @param k k
   * @param rnd Random generator
   * @param delta Delta threshold
   * @param rho Rho threshold
   * @param noInitialNeighbors Do not use initial neighbors
   * @param iterations Maximum number of iterations
   */
  public NNDescent(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k, RandomFactory rnd, double delta, double rho, boolean noInitialNeighbors, int iterations) {
    super(relation, distanceFunction, k);
    this.rnd = rnd;
    this.delta = delta;
    this.rho = rho;
    this.noInitialNeighbors = noInitialNeighbors;
    this.iterations = iterations;
  }

  @Override
  protected void preprocess() {
    final DBIDs ids = relation.getDBIDs();
    final long starttime = System.currentTimeMillis();
    IndefiniteProgress progress = LOG.isVerbose() ? new IndefiniteProgress("KNNGraph iteration", LOG) : null;

    // to add query point itself in the end, internally (k-1) is used
    final int internal_k = k - 1;

    // kNN store
    store = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, KNNHeap.class);
    // store for new reverse neighbors
    WritableDataStore<HashSetModifiableDBIDs> newReverseNeighbors = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, HashSetModifiableDBIDs.class);
    // store for new reverse neighbors
    WritableDataStore<HashSetModifiableDBIDs> oldReverseNeighbors = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, HashSetModifiableDBIDs.class);
    // Sample of new forward neighbors.
    WritableDataStore<HashSetModifiableDBIDs> sampleNewNeighbors = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, HashSetModifiableDBIDs.class);
    // data structures for new and sampled new neighbors
    WritableDataStore<HashSetModifiableDBIDs> flag = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, HashSetModifiableDBIDs.class);

    // Initialize data structures:
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      store.put(iditer, DBIDUtil.newHeap(internal_k));
      newReverseNeighbors.put(iditer, DBIDUtil.newHashSet());
      oldReverseNeighbors.put(iditer, DBIDUtil.newHashSet());
    }

    // this variable is the sampling size
    final int items = (int) Math.ceil(rho * internal_k);

    long counter_all = 0;

    // initialize neighbors (depends on -setInitialNeighbors option)
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      // initialize sampled NN
      ModifiableDBIDs sampleNew = DBIDUtil.randomSampleExcept(ids, iditer, items, rnd);
      sampleNewNeighbors.put(iditer, DBIDUtil.newHashSet(sampleNew));
      // initialize RNN
      ModifiableDBIDs sampleRev = DBIDUtil.randomSampleExcept(ids, iditer, items, rnd);
      newReverseNeighbors.put(iditer, DBIDUtil.newHashSet(sampleRev));
      // initialize new neighbors
      flag.put(iditer, DBIDUtil.newHashSet());
      // initialize store
      if(!noInitialNeighbors) {
        HashSetModifiableDBIDs flags = flag.get(iditer);
        for(DBIDIter siter = sampleNew.iter(); siter.valid(); siter.advance()) {
          if(add(iditer, siter, distanceQuery.distance(iditer, siter))) {
            flags.add(siter);
          }
        }
        counter_all += sampleNew.size();
      }
    }

    final int size = relation.size();
    double rate = 0.0;
    int iter = 0;

    for(; iter < iterations; iter++) {
      long counter = 0;

      // iterate through dataset
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        // determine new and old neighbors
        HashSetModifiableDBIDs newNeighbors = flag.get(iditer);
        HashSetModifiableDBIDs oldNeighbors = DBIDUtil.newHashSet();
        KNNHeap heap = store.get(iditer);
        for(DoubleDBIDListIter heapiter = heap.unorderedIterator(); heapiter.valid(); heapiter.advance()) {
          if(!newNeighbors.contains(heapiter)) {
            oldNeighbors.add(heapiter);
          }
        }

        // Sampling
        HashSetModifiableDBIDs sampleNew = sampleNewNeighbors.get(iditer);

        HashSetModifiableDBIDs newRev = newReverseNeighbors.get(iditer);
        newRev.removeDBIDs(sampleNew);
        boundSize(newRev, items);

        HashSetModifiableDBIDs oldRev = oldReverseNeighbors.get(iditer);
        oldRev.removeDBIDs(oldNeighbors);
        boundSize(oldRev, items);
        counter += processNewNeighbors(flag, sampleNew, oldNeighbors, newRev, oldRev);
      }
      counter_all += counter;
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(prefix + ".scan-rate", counter_all * .5 / (size * (size - 1L))));
      }

      // t is the number of new neighbors
      int t = sampleNew(ids, sampleNewNeighbors, flag, items);

      // calculate old and new reverse neighbors
      clearAll(ids, newReverseNeighbors);
      clearAll(ids, oldReverseNeighbors);
      reverse(sampleNewNeighbors, newReverseNeighbors, oldReverseNeighbors);

      rate = (double) t / (double) (internal_k * size);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(prefix + ".update-rate", rate));
      }
      if(counter < delta * internal_k * size) {
        LOG.verbose("KNNGraph terminated because we performaned delta*k*size distance computations.");
        break;
      }
      if(rate < delta) {
        LOG.verbose("KNNGraph terminated because update rate got smaller than delta.");
        break;
      }
      LOG.incrementProcessed(progress);
    }
    if(LOG.isVerbose() && iter == iterations) {
      LOG.verbose("KNNGraph terminated because the maximum number of iterations was reached.");
    }
    LOG.setCompleted(progress);
    // convert store to storage
    storage = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB, KNNList.class);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNHeap tempHeap = DBIDUtil.newHeap(k);
      // Add query point and convert heap to list:
      KNNHeap heap = store.get(iditer);
      tempHeap.insert(0, iditer);
      for(DoubleDBIDListIter heapiter = heap.unorderedIterator(); heapiter.valid(); heapiter.advance()) {
        tempHeap.insert(heapiter.doubleValue(), heapiter);
      }
      storage.put(iditer, tempHeap.toKNNList());
    }
    final long end = System.currentTimeMillis();
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(prefix + ".construction-time.ms", end - starttime));
    }
  }

  /**
   * Clear (but reuse) all sets in the given storage.
   * 
   * @param ids Ids to process
   * @param sets Sets to clear
   */
  private void clearAll(DBIDs ids, WritableDataStore<HashSetModifiableDBIDs> sets) {
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      sets.get(it).clear();
    }
  }

  /**
   * Bound the size of a set by random sampling.
   * 
   * @param set Set to process
   * @param items Maximum size
   */
  private void boundSize(HashSetModifiableDBIDs set, int items) {
    if(set.size() > items) {
      DBIDs sample = DBIDUtil.randomSample(set, items, rnd);
      set.clear();
      set.addDBIDs(sample);
    }
  }

  /**
   * Process new neighbors.
   * 
   * This is a complex join, because we do not need to join old neighbors with
   * old neighbors, and we have forward- and reverse neighbors each.
   *
   * @param flag Flags to mark new neighbors.
   * @param newFwd New forward neighbors
   * @param oldFwd Old forward neighbors
   * @param newRev New reverse neighbors
   * @param oldRev Old reverse neighbors
   * @return Number of new neighbors
   */
  private int processNewNeighbors(WritableDataStore<HashSetModifiableDBIDs> flag, HashSetModifiableDBIDs newFwd, HashSetModifiableDBIDs oldFwd, HashSetModifiableDBIDs newRev, HashSetModifiableDBIDs oldRev) {
    int counter = 0;
    // nn_new
    if(!newFwd.isEmpty()) {
      for(DBIDIter sniter = newFwd.iter(); sniter.valid(); sniter.advance()) {
        // nn_new X nn_new
        for(DBIDIter niter2 = newFwd.iter(); niter2.valid(); niter2.advance()) {
          if(DBIDUtil.compare(sniter, niter2) < 0) { // Only x < y.
            addpair(flag, sniter, niter2);
            counter++;
          }
        }
        // nn_new X nn_old
        for(DBIDMIter niter2 = oldFwd.iter(); niter2.valid(); niter2.advance()) {
          if(DBIDUtil.equal(sniter, niter2)) {
            continue;
          }
          addpair(flag, sniter, niter2);
          counter++;
        }
      }
    }
    // rnn_new
    if(!newRev.isEmpty()) {
      for(DBIDIter nriter = newRev.iter(); nriter.valid(); nriter.advance()) {
        // rnn_new X rnn_new
        for(DBIDIter niter2 = newRev.iter(); niter2.valid(); niter2.advance()) {
          if(DBIDUtil.compare(nriter, niter2) < 0) { // Only x < y
            addpair(flag, nriter, niter2);
            counter++;
          }
        }
        // rnn_new X rnn_old
        for(DBIDIter niter2 = oldRev.iter(); niter2.valid(); niter2.advance()) {
          if(DBIDUtil.equal(nriter, niter2)) {
            continue;
          }
          addpair(flag, nriter, niter2);
          counter++;
        }
      }
    }

    // nn_new
    if(!newFwd.isEmpty()) {
      for(DBIDIter sniter2 = newFwd.iter(); sniter2.valid(); sniter2.advance()) {
        // nn_new X rnn_old
        for(DBIDIter niter2 = oldRev.iter(); niter2.valid(); niter2.advance()) {
          if(!DBIDUtil.equal(sniter2, niter2)) {
            addpair(flag, sniter2, niter2);
            counter++;
          }
        }
        // nn_new X rnn_new
        for(DBIDIter niter2 = newRev.iter(); niter2.valid(); niter2.advance()) {
          if(DBIDUtil.compare(sniter2, niter2) < 0) {
            addpair(flag, sniter2, niter2);
            counter++;
          }
        }
      }
    }
    // nn_old
    if(!newRev.isEmpty() && !oldFwd.isEmpty()) {
      for(DBIDIter niter = oldFwd.iter(); niter.valid(); niter.advance()) {
        // nn_old X rnn_new
        for(DBIDIter niter2 = newRev.iter(); niter2.valid(); niter2.advance()) {
          if(DBIDUtil.equal(niter, niter2)) {
            continue;
          }
          addpair(flag, niter, niter2);
          counter++;
        }
      }
    }
    return counter;
  }

  /**
   * Add cand to cur's heap neighbors with distance
   *
   * @param cur Current object
   * @param cand Neighbor candidate
   * @param distance Distance
   * @return {@code true} if it was a new neighbor.
   */
  private boolean add(DBIDRef cur, DBIDRef cand, double distance) {
    KNNHeap neighbors = store.get(cur);
    if(neighbors.contains(cand)) {
      return false;
    }
    double newKDistance = neighbors.insert(distance, cand);
    return (distance <= newKDistance);
  }

  private void addpair(WritableDataStore<HashSetModifiableDBIDs> newNeighbors, DBIDRef o1, DBIDRef o2) {
    final double distance = distanceQuery.distance(o1, o2);
    if(add(o1, o2, distance)) {
      newNeighbors.get(o1).add(o2);
    }
    if(add(o2, o1, distance)) {
      newNeighbors.get(o2).add(o1);
    }
  }

  /**
   * samples newNeighbors for every object
   *
   * @param ids All ids
   * @param sampleNewNeighbors Output of sampled new neighbors
   * @param newNeighborHash - new neighbors for every object
   * @param items Number of items to collect
   * @return Number of new neighbors
   */
  private int sampleNew(DBIDs ids, WritableDataStore<HashSetModifiableDBIDs> sampleNewNeighbors, WritableDataStore<HashSetModifiableDBIDs> newNeighborHash, int items) {
    int t = 0;
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      KNNHeap realNeighbors = store.get(iditer);
      HashSetModifiableDBIDs newNeighbors = newNeighborHash.get(iditer);
      HashSetModifiableDBIDs realNewNeighbors = sampleNewNeighbors.get(iditer);
      realNewNeighbors.clear(); // Reuse
      for(DoubleDBIDListIter heapiter = realNeighbors.unorderedIterator(); heapiter.valid(); heapiter.advance()) {
        if(newNeighbors.contains(heapiter)) {
          realNewNeighbors.add(heapiter);
          t++;
        }
      }
      boundSize(realNewNeighbors, items);

      newNeighbors.removeDBIDs(realNewNeighbors);
      newNeighborHash.put(iditer, newNeighbors);
    }
    return t;
  }

  /**
   * calculates new and old neighbors for database
   * 
   * @param sampleNewHash new neighbors for every object
   * @param newReverseNeighbors new reverse neighbors
   * @param oldReverseNeighbors old reverse neighbors
   */
  private void reverse(WritableDataStore<HashSetModifiableDBIDs> sampleNewHash, WritableDataStore<HashSetModifiableDBIDs> newReverseNeighbors, WritableDataStore<HashSetModifiableDBIDs> oldReverseNeighbors) {
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNHeap heap = store.get(iditer);
      HashSetDBIDs newNeighbors = sampleNewHash.get(iditer);
      for(DoubleDBIDListIter heapiter = heap.unorderedIterator(); heapiter.valid(); heapiter.advance()) {
        (newNeighbors.contains(heapiter) ? newReverseNeighbors : oldReverseNeighbors).get(heapiter).add(iditer);
      }
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    // TODO
  }

  @Override
  public String getLongName() {
    return "NNDescent kNN";
  }

  @Override
  public String getShortName() {
    return "nn-descent-knn";
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    for(Object hint : hints) {
      if(DatabaseQuery.HINT_EXACT.equals(hint)) {
        return null;
      }
    }
    return super.getKNNQuery(distanceQuery, hints);
  }

  /**
   * Index factory.
   *
   * @author Evelyn Kirner
   *
   * @param <O> Object type
   */
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
    /**
     * Random generator
     */
    private final RandomFactory rnd;

    /**
     * early termination parameter
     */
    private final double delta;

    /**
     * sample rate
     */
    private final double rho;

    /**
     * set initial neighbors?
     */
    private final boolean noInitialNeighbors;

    /**
     * maximum number of iterations
     */
    private final int iterations;

    /**
     * Constructor.
     *
     * @param k K
     * @param distanceFunction distance function
     * @param rnd Random generator
     * @param delta Delta threshold
     * @param rho Rho threshold
     * @param noInitialNeighbors Do not use initial neighbors
     * @param iterations Maximum number of iterations
     */
    public Factory(int k, DistanceFunction<? super O> distanceFunction, RandomFactory rnd, double delta, double rho, boolean noInitialNeighbors, int iterations) {
      super(k, distanceFunction);
      this.rnd = rnd;
      this.delta = delta;
      this.rho = rho;
      this.noInitialNeighbors = noInitialNeighbors;
      this.iterations = iterations;
    }

    @Override
    public NNDescent<O> instantiate(Relation<O> relation) {
      return new NNDescent<>(relation, distanceFunction, k, rnd, delta, rho, noInitialNeighbors, iterations);
    }

    /**
     * Parameterization class
     *
     * @author Evelyn Kirner
     *
     * @hidden
     *
     * @param <O> Object type
     */
    public static class Parameterizer<O> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O> {
      /**
       * Random number generator seed.
       */
      public static final OptionID SEED_ID = new OptionID("knngraph.seed", "The random number seed.");

      /**
       * Early termination parameter.
       */
      public static final OptionID DELTA_ID = new OptionID("knngraph.delta", "The early termination parameter.");

      /**
       * Sample rate.
       */
      public static final OptionID RHO_ID = new OptionID("knngraph.rho", "The sample rate parameter");

      /**
       * Whether to initialize neighbors with sampled neighbors.
       */
      public static final OptionID INITIAL_ID = new OptionID("knngraph.no-initial", "Do not use initial neighbors.");

      /**
       * maximum number of iterations
       */
      public static final OptionID ITER_ID = new OptionID("knngraph.maxiter", "maximum number of iterations");

      /**
       * Random generator
       */
      private RandomFactory rnd;

      /**
       * early termination parameter
       */
      private double delta;

      /**
       * sample rate
       */
      private double rho;

      /**
       * No initial neighbors
       */
      private boolean noInitialNeighbors;

      /**
       * maximum number of iterations
       */
      private int iterations;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        RandomParameter rndP = new RandomParameter(SEED_ID);
        if(config.grab(rndP)) {
          rnd = rndP.getValue();
        }
        DoubleParameter deltaP = new DoubleParameter(DELTA_ID, 0.001) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
        if(config.grab(deltaP)) {
          delta = deltaP.getValue();
        }
        DoubleParameter rhoP = new DoubleParameter(RHO_ID, 1) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(config.grab(rhoP)) {
          rho = rhoP.getValue();
        }
        Flag initialP = new Flag(INITIAL_ID);
        if(config.grab(initialP)) {
          noInitialNeighbors = initialP.isTrue();
        }
        IntParameter iterP = new IntParameter(ITER_ID, 100) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(iterP)) {
          iterations = iterP.getValue();
        }
      }

      @Override
      protected NNDescent.Factory<O> makeInstance() {
        return new NNDescent.Factory<>(k, distanceFunction, rnd, delta, rho, noInitialNeighbors, iterations);
      }
    }
  }
}
