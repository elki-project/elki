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
package elki.index.laesa;

import java.util.Random;

import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.memory.ArrayDoubleStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.IndexFactory;
import elki.index.KNNIndex;
import elki.index.RangeIndex;
import elki.logging.Logging;
import elki.logging.LoggingUtil;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Linear Approximating and Eliminating Search Algorithm
 * <p>
 * Reference:
 * <p>
 * L. Micó, J. Oncina, E. Vidal<br>
 * A new version of the nearest-neighbour approximating and eliminating search
 * algorithm (AESA) with linear preprocessing time and memory requirements<br>
 * Pattern Recognit. Lett. 15(1)
 * 
 * @author Erich Schubert
 * @author Robert Gehde
 * @since 0.8.0
 * 
 * @param <O> Object type
 */
@Reference(authors = "L. Micó, J. Oncina, E. Vidal", //
    title = "A new version of the nearest-neighbour approximating and eliminating search  algorithm (AESA) with linear preprocessing time and memory requirements", //
    booktitle = "Pattern Recognit. Lett. 15(1)", //
    url = "https://doi.org/10.1016/0167-8655(94)90095-7", //
    bibkey = "DBLP:journals/prl/MicoOV94")
public class LAESA<O> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LAESA.class);

  /**
   * Distance function
   */
  Distance<? super O> distance;

  /**
   * Distance query, bound to the relation
   */
  DistanceQuery<? super O> distq;

  /**
   * Number of reference points
   */
  int m;

  /**
   * Condition parameter
   */
  int k;

  /**
   * Random generator
   */
  RandomFactory rng;

  /**
   * Relation indexed.
   */
  Relation<O> relation;

  /**
   * Reference points
   */
  ArrayModifiableDBIDs refp;

  /**
   * Data storage for precomputed distances to reference points.
   */
  DoubleDataStore[] dists;

  /**
   * fast lookup
   */
  HashSetDBIDs chosenRefP;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param m Number of reference points
   * @param rng Random generator
   */
  public LAESA(Relation<O> relation, Distance<? super O> distance, int m, int k, RandomFactory rng) {
    this.relation = relation;
    this.distance = distance;
    this.distq = distance.instantiate(relation);
    this.m = m;
    this.k = k;
    this.rng = rng;
  }

  @Override
  public void initialize() {
    DBIDs ids = relation.getDBIDs();
    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    Random rand = rng.getSingleThreadedRandom();
    int best = rand.nextInt(aids.size()); // First is chosen randomly
    DBIDVar cur = DBIDUtil.newVar();
    dists = new ArrayDoubleStore[m];
    // Store chosen reference points
    refp = DBIDUtil.newArray(m);
    // Fast lookup for existing reference points
    HashSetModifiableDBIDs chosen = DBIDUtil.newHashSet(m);
    WritableDoubleDataStore a = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0);
    WritableDoubleDataStore ds;
    FiniteProgress rprog = LOG.isVerbose() ? new FiniteProgress("Computing distances to reference points", m, LOG) : null;
    for(int i = 0; i < m; i++) {
      refp.add(aids.assignVar(best, cur));
      chosen.add(cur);
      best = 0;
      double besta = 0;
      ds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT);
      for(DBIDArrayIter iter = aids.iter(); iter.valid(); iter.advance()) {
        if(chosen.contains(iter)) {
          if(DBIDUtil.equal(iter, cur)) {
            ds.putDouble(iter, 0);
            continue;
          }
          for(DBIDArrayIter l = refp.iter(); l.valid(); l.advance()) {
            if(DBIDUtil.equal(iter, l)) {
              ds.putDouble(iter, dists[l.getOffset()].doubleValue(cur));
              break;
            }
          }
          continue;
        }
        final double d = distq.distance(cur, iter);
        ds.putDouble(iter, d);
        // For choosing the next reference point:
        double ap = a.doubleValue(iter) + d;
        a.putDouble(iter, ap);
        if(ap > besta) {
          besta = ap;
          best = iter.getOffset();
        }
      }
      dists[i] = ds;
      LOG.incrementProcessed(rprog);
    }
    chosenRefP = chosen;
    LOG.ensureCompleted(rprog);
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distq.getDistance()) ? //
            new LAESAKNNByObjectSearcher(k) : null;
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new LAESAKNNByDBIDSearcher(k) : null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new LAESARangeByObjectSearcher() : null;
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return (flags & QueryBuilder.FLAG_PRECOMPUTE) == 0 && //
        distanceQuery.getRelation() == relation && this.distance.equals(distanceQuery.getDistance()) ? //
            new LAESARangeByDBIDSearcher() : null;
  }

  /**
   * KNN searcher class
   * 
   * @author Robert Gehde
   *
   * @param <Q> query object
   */
  public abstract class LAESAKNNSearcher<Q> implements KNNSearcher<Q> {
    /**
     * Maximum number of distance computations before also pruning reference
     * points.
     */
    int maxnc;

    /**
     * Constructor.
     *
     * @param k condition parameter
     */
    public LAESAKNNSearcher(int k) {
      super();
      this.maxnc = m / k;
      assert maxnc < Integer.MAX_VALUE;
    }

    /**
     * Search the k nearest neighbors
     * 
     * @param knns result heap
     */
    protected void laesaKNNSearch(KNNHeap knns) {
      double dBest = Double.POSITIVE_INFINITY;
      ModifiableDoubleDBIDList P = DBIDUtil.newDistanceDBIDList(relation.size() - m);
      ModifiableDoubleDBIDList RP = DBIDUtil.newDistanceDBIDList(m);
      DBIDArrayIter refIter = refp.iter();
      for(DBIDIter p = relation.iterDBIDs(); p.valid(); p.advance()) {
        (chosenRefP.contains(p) ? RP : P).add(0, p);
      }
      // Always choose first reference point as "arbitrary" starting point.
      DoubleDBIDListMIter pIter = P.iter(), rpIter = RP.iter();
      final DBIDVar s = DBIDUtil.newVar(rpIter);
      RP.removeSwap(0);

      boolean isrp = true;
      int nc = 0;
      while(true) {
        // distance computing
        final double dxs = queryDistance(s);
        nc++; // count number of distances
        dBest = knns.insert(dxs, s);
        DoubleDataStore rdists = isrp ? dists[findInRef(s, refIter)] : null;
        int rpsi = processPoints(RP, rpIter, dBest, dxs, rdists, nc);
        int psi = processPoints(P, pIter, dBest, dxs, rdists, Integer.MAX_VALUE);
        // Choose next element s:
        if(rpsi > -1) {
          RP.assignVar(rpsi, s);
          RP.removeSwap(rpsi);
          isrp = true;
        }
        else if(psi > -1) {
          P.assignVar(psi, s);
          P.removeSwap(psi);
          isrp = false;
        }
        else {
          break; // No more candidates
        }
      }
    }

    /**
     * Process a set of points
     *
     * @param cands Points to process
     * @param iter iterator
     * @param threshold Pruning distance
     * @param dxs Distance to current point
     * @param rdists Precomputed distances
     * @param nc Number of distance computations
     * @return Index of best candidate
     */
    private int processPoints(ModifiableDoubleDBIDList cands, DoubleDBIDListMIter iter, double threshold, final double dxs, DoubleDataStore rdists, int nc) {
      double best = Double.POSITIVE_INFINITY; // best candidate
      int bestindex = -1;
      for(iter.seek(0); iter.valid(); iter.advance()) {
        double gp = iter.doubleValue();
        // Integrate new information, if available:
        if(rdists != null) {
          final double t = Math.abs(rdists.doubleValue(iter) - dxs);
          if(t > gp) {
            iter.setDouble(gp = t);
          }
        }
        // Point elimination (nc is only used for reference points)
        if(gp > threshold && nc > maxnc) {
          cands.removeSwap(iter.getOffset());
          iter.retract();
        }
        else if(gp < best) {
          best = gp;
          bestindex = iter.getOffset();
        }
      }
      return bestindex;
    }

    /**
     * Find the given DBIDVar p in the reference iter
     * 
     * @param p Object to find
     * @param ref Reference iterator
     * @return index into the reference points list
     */
    private int findInRef(DBIDRef p, DBIDArrayIter ref) {
      for(ref.seek(0); ref.valid(); ref.advance()) {
        if(DBIDUtil.equal(p, ref)) {
          return ref.getOffset();
        }
      }
      return -1;
    }

    /**
     * Compute the distance to a candidate object.
     * 
     * @param p Object
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef p);
  }

  /**
   * KNN searcher by object class
   * 
   * @author Robert Gehde
   */
  public class LAESAKNNByObjectSearcher extends LAESAKNNSearcher<O> {
    /**
     * Query object
     */
    private O query;

    /**
     * Constructor.
     *
     * @param k condition parameter
     */
    public LAESAKNNByObjectSearcher(int k) {
      super(k);
    }

    @Override
    public KNNList getKNN(O query, int k) {
      this.query = query;
      final KNNHeap knns = DBIDUtil.newHeap(k);
      laesaKNNSearch(knns);
      return knns.toKNNList();
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return LAESA.this.distq.distance(query, p);
    }
  }

  /**
   * KNN searcher by DBID class
   * 
   * @author Robert Gehde
   */
  public class LAESAKNNByDBIDSearcher extends LAESAKNNSearcher<DBIDRef> {
    /**
     * Query object
     */
    private DBIDRef query;

    /**
     * Constructor.
     *
     * @param k condition parameter
     */
    public LAESAKNNByDBIDSearcher(int k) {
      super(k);
    }

    @Override
    public KNNList getKNN(DBIDRef query, int k) {
      this.query = query;
      final KNNHeap knns = DBIDUtil.newHeap(k);
      laesaKNNSearch(knns);
      return knns.toKNNList();
    }

    @Override
    protected double queryDistance(DBIDRef p) {
      return LAESA.this.distq.distance(query, p);
    }
  }

  /**
   * Range searcher class
   * 
   * @author Robert Gehde
   *
   * @param <Q> query object
   */
  public abstract class LAESARangeSearcher<Q> implements RangeSearcher<Q> {
    /**
     * Perform a range search
     * 
     * @param range radius to search
     * @param result result list
     */
    protected void laesaRangeSearch(double range, ModifiableDoubleDBIDList result) {
      ModifiableDoubleDBIDList refpdists = DBIDUtil.newDistanceDBIDList(m);
      for(DBIDIter it = refp.iter(); it.valid(); it.advance()) {
        refpdists.add(queryDistance(it), it);
      }

      for(DBIDIter p = relation.iterDBIDs(); p.valid(); p.advance()) {
        double highlowbound = 0;
        for(DoubleDBIDListMIter r = refpdists.iter(); r.valid(); r.advance()) {
          double t = Math.abs(dists[r.getOffset()].doubleValue(p) - r.doubleValue());
          if(t > highlowbound) {
            highlowbound = t;
          }
        }
        if(highlowbound <= range) {
          // Refinement
          final double dist = queryDistance(p);
          if(dist <= range) {
            result.add(dist, p);
          }
        }
      }
    }

    /**
     * Compute the distance to a candidate object.
     * 
     * @param it Object reference
     * @return Distance
     */
    protected abstract double queryDistance(DBIDRef it);
  }

  /**
   * Range searcher by object class
   * 
   * @author Robert Gehde
   */
  public class LAESARangeByObjectSearcher extends LAESARangeSearcher<O> {
    /**
     * Query object
     */
    private O query;

    @Override
    public ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      laesaRangeSearch(range, result);
      return result;
    }

    @Override
    protected double queryDistance(DBIDRef ref) {
      return LAESA.this.distq.distance(query, ref);
    }
  }

  /**
   * Range searcher by DBID class
   * 
   * @author Robert Gehde
   */
  public class LAESARangeByDBIDSearcher extends LAESARangeSearcher<DBIDRef> {
    /**
     * Query object
     */
    private DBIDRef query;

    @Override
    public ModifiableDoubleDBIDList getRange(DBIDRef query, double range, ModifiableDoubleDBIDList result) {
      this.query = query;
      laesaRangeSearch(range, result);
      return result;
    }

    @Override
    protected double queryDistance(DBIDRef ref) {
      return LAESA.this.distq.distance(query, ref);
    }
  }

  /**
   * Index factory.
   * 
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Factory<O> implements IndexFactory<O> {
    /**
     * Distance function
     */
    Distance<? super O> distance;

    /**
     * Number of reference points
     */
    int m = 10;

    /**
     * Condition parameter
     */
    int k = Integer.MAX_VALUE;

    /**
     * Random generator
     */
    RandomFactory rng;

    /**
     * Constructor.
     *
     * @param distance Distance function
     * @param m Number of reference points
     * @param rng Random generator
     */
    public Factory(Distance<? super O> distance, int m, int k, RandomFactory rng) {
      this.distance = distance;
      this.m = m;
      this.k = k;
      this.rng = rng;
    }

    @Override
    public LAESA<O> instantiate(Relation<O> relation) {
      return new LAESA<O>(relation, distance, m, k, rng);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     * 
     * @param <O> object type
     */
    public static class Par<O> implements Parameterizer {
      /**
       * Distance function to use in the index
       */
      public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("laesa.distance", "Distance function to determine the distance between objects.");

      /**
       * Number of reference points to choose.
       */
      public static final OptionID M_ID = new OptionID("laesa.m", "Number of reference points to use.");

      /**
       * Condition parameter. Controls the deletion of reference points during
       * knn search.
       */
      public static final OptionID K_ID = new OptionID("laesa.k", "Condition parameter. Controls the deletion of reference points during knn search.");

      /**
       * Random generator to use
       */
      public static final OptionID SEED_ID = new OptionID("laesa.seed", "Random generator seed to use.");

      /**
       * Distance function
       */
      Distance<? super O> distance;

      /**
       * Number of reference points
       */
      int m;

      /**
       * condition parameter
       */
      int k = Integer.MAX_VALUE;

      /**
       * Random generator
       */
      RandomFactory rng;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<? super O>>(DISTANCE_FUNCTION_ID, Distance.class) //
            .grab(config, x -> {
              distance = x;
              if(!distance.isMetric()) {
                LoggingUtil.warning("LAESA requires a metric to be exact.");
              }
            });
        new IntParameter(M_ID, 10) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> m = x);
        new IntParameter(K_ID).setOptional(true) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> k = x);
        new RandomParameter(SEED_ID) //
            .grab(config, x -> rng = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<O>(distance, m, k, rng);
      }
    }
  }
}
