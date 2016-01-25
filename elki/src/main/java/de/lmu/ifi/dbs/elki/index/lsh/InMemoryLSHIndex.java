package de.lmu.ifi.dbs.elki.index.lsh;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractRefiningIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.lsh.hashfamilies.LocalitySensitiveHashFunctionFamily;
import de.lmu.ifi.dbs.elki.index.lsh.hashfunctions.LocalitySensitiveHashFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Locality Sensitive Hashing.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.has LocalitySensitiveHashFunctionFamily
 * @apiviz.has Instance
 *
 * @param <V> Object type to index
 */
public class InMemoryLSHIndex<V> implements IndexFactory<V, InMemoryLSHIndex<V>.Instance> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(InMemoryLSHIndex.class);

  /**
   * LSH hash function family to use.
   */
  LocalitySensitiveHashFunctionFamily<? super V> family;

  /**
   * Number of hash tables to use.
   */
  int l;

  /**
   * Number of buckets to use.
   */
  int numberOfBuckets;

  /**
   * Constructor.
   *
   * @param family Projection family
   * @param l Number of hash tables to use
   * @param numberOfBuckets Number of buckets to use.
   */
  public InMemoryLSHIndex(LocalitySensitiveHashFunctionFamily<? super V> family, int l, int numberOfBuckets) {
    super();
    this.family = family;
    this.l = l;
    this.numberOfBuckets = numberOfBuckets;
  }

  @Override
  public Instance instantiate(Relation<V> relation) {
    return new Instance(relation, family.generateHashFunctions(relation, l), numberOfBuckets);
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return family.getInputTypeRestriction();
  }

  /**
   * Instance of a LSH index for a single relation.
   *
   * @author Erich Schubert
   *
   * @apiviz.has LocalitySensitiveHashFunction
   */
  public class Instance extends AbstractRefiningIndex<V> implements KNNIndex<V>, RangeIndex<V> {
    /**
     * Hash functions to use.
     */
    ArrayList<? extends LocalitySensitiveHashFunction<? super V>> hashfunctions;

    /**
     * The actual table
     */
    ArrayList<TIntObjectMap<DBIDs>> hashtables;

    /**
     * Number of buckets to use.
     */
    private int numberOfBuckets;

    /**
     * Constructor.
     *
     * @param relation Relation to index.
     * @param hashfunctions Hash functions.
     */
    public Instance(Relation<V> relation, ArrayList<? extends LocalitySensitiveHashFunction<? super V>> hashfunctions, int numberOfBuckets) {
      super(relation);
      this.hashfunctions = hashfunctions;
      this.numberOfBuckets = numberOfBuckets;
    }

    @Override
    public String getLongName() {
      return "LSH index";
    }

    @Override
    public String getShortName() {
      return "lsh-index";
    }

    @Override
    public void initialize() {
      final int numhash = hashfunctions.size();
      hashtables = new ArrayList<>(numhash);
      for(int i = 0; i < numhash; i++) {
        hashtables.add(new TIntObjectHashMap<DBIDs>(numberOfBuckets));
      }

      // TODO: We assume all hash functions have the same dimensionality.
      double[] buf = new double[hashfunctions.get(0).getNumberOfProjections()];
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Building LSH index", relation.size(), LOG) : null;
      int expect = Math.max(2, (int) Math.ceil(relation.size() / (double) numberOfBuckets));
      for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
        V obj = relation.get(iter);
        for(int i = 0; i < numhash; i++) {
          final TIntObjectMap<DBIDs> table = hashtables.get(i);
          final LocalitySensitiveHashFunction<? super V> hashfunc = hashfunctions.get(i);
          // Get the initial (unbounded) hash code:
          int hash = hashfunc.hashObject(obj, buf);
          // Reduce to hash table size
          int bucket = hash % numberOfBuckets;
          DBIDs cur = table.get(bucket);
          if(cur == null) {
            table.put(bucket, DBIDUtil.deref(iter));
          }
          else if(cur.size() > 1) {
            ((ModifiableDBIDs) cur).add(iter);
          }
          else {
            ModifiableDBIDs newbuck = DBIDUtil.newArray(expect);
            newbuck.addDBIDs(cur);
            newbuck.add(iter);
            table.put(bucket, newbuck);
          }
        }
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      if(LOG.isStatistics()) {
        int min = Integer.MAX_VALUE, max = 0;
        for(int i = 0; i < numhash; i++) {
          final TIntObjectMap<DBIDs> table = hashtables.get(i);
          for(TIntObjectIterator<DBIDs> iter = table.iterator(); iter.hasNext();) {
            iter.advance();
            int size = iter.value().size();
            if(size < min) {
              min = size;
            }
            if(size > max) {
              max = size;
            }
          }
        }
        LOG.statistics(new LongStatistic(this.getClass().getName() + ".fill.min", min));
        LOG.statistics(new LongStatistic(this.getClass().getName() + ".fill.max", max));
        LOG.statistics(new LongStatistic(this.getClass().getName() + ".hashtables", hashtables.size()));
      }
    }

    @Override
    public Logging getLogger() {
      return LOG;
    }

    @Override
    public KNNQuery<V> getKNNQuery(DistanceQuery<V> distanceQuery, Object... hints) {
      for(Object hint : hints) {
        if(DatabaseQuery.HINT_EXACT.equals(hint)) {
          return null;
        }
      }
      DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
      if(!family.isCompatible(df)) {
        return null;
      }
      return new LSHKNNQuery(distanceQuery);
    }

    @Override
    public RangeQuery<V> getRangeQuery(DistanceQuery<V> distanceQuery, Object... hints) {
      for(Object hint : hints) {
        if(DatabaseQuery.HINT_EXACT.equals(hint)) {
          return null;
        }
      }
      DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
      if(!family.isCompatible(df)) {
        return null;
      }
      return new LSHRangeQuery(distanceQuery);
    }

    /**
     * Class for handling kNN queries against the LSH index.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    protected class LSHKNNQuery extends AbstractKNNQuery {
      /**
       * Constructor.
       *
       * @param distanceQuery
       */
      public LSHKNNQuery(DistanceQuery<V> distanceQuery) {
        super(distanceQuery);
      }

      @Override
      public KNNList getKNNForObject(V obj, int k) {
        ModifiableDBIDs candidates = null;
        final int numhash = hashtables.size();
        double[] buf = new double[hashfunctions.get(0).getNumberOfProjections()];
        for(int i = 0; i < numhash; i++) {
          final TIntObjectMap<DBIDs> table = hashtables.get(i);
          final LocalitySensitiveHashFunction<? super V> hashfunc = hashfunctions.get(i);
          // Get the initial (unbounded) hash code:
          int hash = hashfunc.hashObject(obj, buf);
          // Reduce to hash table size
          int bucket = hash % numberOfBuckets;
          DBIDs cur = table.get(bucket);
          if(cur != null) {
            if(candidates == null) {
              candidates = DBIDUtil.newHashSet(cur.size() * numhash + k);
            }
            candidates.addDBIDs(cur);
          }
        }
        if(candidates == null) {
          candidates = DBIDUtil.newArray();
        }

        // Refine.
        KNNHeap heap = DBIDUtil.newHeap(k);
        for(DBIDIter iter = candidates.iter(); iter.valid(); iter.advance()) {
          final double dist = distanceQuery.distance(obj, iter);
          super.incRefinements(1);
          heap.insert(dist, iter);
        }
        return heap.toKNNList();
      }
    }

    /**
     * Class for handling kNN queries against the LSH index.
     *
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    protected class LSHRangeQuery extends AbstractRangeQuery {
      /**
       * Constructor.
       *
       * @param distanceQuery
       */
      public LSHRangeQuery(DistanceQuery<V> distanceQuery) {
        super(distanceQuery);
      }

      @Override
      public void getRangeForObject(V obj, double range, ModifiableDoubleDBIDList result) {
        ModifiableDBIDs candidates = DBIDUtil.newHashSet();
        final int numhash = hashtables.size();
        double[] buf = new double[hashfunctions.get(0).getNumberOfProjections()];
        for(int i = 0; i < numhash; i++) {
          final TIntObjectMap<DBIDs> table = hashtables.get(i);
          final LocalitySensitiveHashFunction<? super V> hashfunc = hashfunctions.get(i);
          // Get the initial (unbounded) hash code:
          int hash = hashfunc.hashObject(obj, buf);
          // Reduce to hash table size
          int bucket = hash % numberOfBuckets;
          DBIDs cur = table.get(bucket);
          if(cur != null) {
            candidates.addDBIDs(cur);
          }
        }

        // Refine.
        for(DBIDIter iter = candidates.iter(); iter.valid(); iter.advance()) {
          final double dist = distanceQuery.distance(obj, iter);
          super.incRefinements(1);
          if(dist <= range) {
            result.add(dist, iter);
          }
        }
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends AbstractParameterizer {
    /**
     * Hash function family parameter.
     */
    public static final OptionID FAMILY_ID = new OptionID("lsh.family", "Hash function family to use for LSH.");

    /**
     * Number of hash tables to use for LSH.
     */
    public static final OptionID L_ID = new OptionID("lsh.tables", "Number of hash tables to use.");

    /**
     * Number of hash tables to use for LSH.
     */
    public static final OptionID BUCKETS_ID = new OptionID("lsh.buckets", "Number of hash buckets to use.");

    /**
     * LSH hash function family to use.
     */
    LocalitySensitiveHashFunctionFamily<? super V> family;

    /**
     * Number of hash functions for each table.
     */
    int l;

    /**
     * Number of buckets to use.
     */
    int numberOfBuckets;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<LocalitySensitiveHashFunctionFamily<? super V>> familyP = new ObjectParameter<>(FAMILY_ID, LocalitySensitiveHashFunctionFamily.class);
      if(config.grab(familyP)) {
        family = familyP.instantiateClass(config);
      }

      IntParameter lP = new IntParameter(L_ID);
      lP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(lP)) {
        l = lP.intValue();
      }

      IntParameter bucketsP = new IntParameter(BUCKETS_ID);
      bucketsP.setDefaultValue(7919); // Primes work best, apparently.
      bucketsP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(bucketsP)) {
        numberOfBuckets = bucketsP.intValue();
      }
    }

    @Override
    protected InMemoryLSHIndex<V> makeInstance() {
      return new InMemoryLSHIndex<>(family, l, numberOfBuckets);
    }
  }
}
