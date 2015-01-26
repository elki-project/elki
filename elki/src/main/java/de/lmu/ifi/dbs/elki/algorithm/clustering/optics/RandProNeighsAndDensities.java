package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/* 
 Copyright (C) 2014
 Johannes Schneider, ABB Research, Switzerland, johannes.schneider@alumni.ethz.ch

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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Random Projections used for computing neighbors and density estimates
 * 
 * @author Johannes Schneider
 * @author Erich Schubert
 */
public class RandProNeighsAndDensities<V extends NumberVector> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(RandProNeighsAndDensities.class);

  /**
   * Default Seed for random numbers
   */
  private static final int DefaultRandSeed = 4711;

  /**
   * Default constant used to compute number of projections as well as number of
   * splits of point set, ie. constant *log N*d
   */
  // constant in O(log N*d) used to compute number of projections as well as
  // number of splis of point set
  private static final int logOProjectionConst = 20;

  /**
   * Sets used for neighborhood computation should be about minSplitSize Sets
   * are still used if they deviate by less (1+/- sizeTolerance)
   */
  private static final float sizeTolerance = 2f / 3;

  // variables
  /**
   * minimum size for which a point set is further partitioned (roughly
   * corresponds to minPts in OPTICS)
   */
  int minSplitSize;

  /**
   * entire point set
   */
  Relation<V> points;

  /**
   * random number generator
   */
  Random rand;

  /**
   * number of times the point set is projected onto a 1d random line, typically
   * something like O(log N)
   */
  protected int nProject1d;

  /**
   * number of times the entire point set is split, typically something like
   * O(log N)
   */
  protected int nPointSetSplits;

  /**
   * sets that resulted from recursive split of entire point set
   */
  protected ArrayList<ArrayDBIDs> splitsets;

  /**
   * all projected points
   */
  protected DoubleDataStore[] projectedPoints;

  /**
   * number of points
   */
  int N;

  /**
   * number of dimensions
   */
  int nDimensions;

  /**
   * Constructor
   * 
   * @param points to process
   * @param minimum size for which a point set is further partitioned (roughly
   *        corresponds to minPts in OPTICS)
   */
  public RandProNeighsAndDensities(Relation<V> points, int minSplitSize) {
    this.minSplitSize = minSplitSize;
    N = points.size();
    nDimensions = RelationUtil.dimensionality(points);
    this.points = points;// new float[N][nDimensions];

    // perform O(log N+log dim) splits of the entire point sets projections
    nPointSetSplits = (int) (logOProjectionConst * MathUtil.log2(N * nDimensions + 1));
    // perform O(log N+log dim) projections of the point set onto a random
    // line
    nProject1d = (int) (logOProjectionConst * MathUtil.log2(N * nDimensions + 1));
    rand = new Random(DefaultRandSeed);
  }

  /**
   * Random Seed Setting (optional to use)
   * 
   * @param random seed
   */
  public void setRandomSeed(long seed) {
    rand = new Random(seed);
  }

  /**
   * Set number of times entire point set is split(optional to use)
   * 
   * @param number of Splits
   */
  public void setNumberPointSetSplits(int nSplits) {
    this.nPointSetSplits = nSplits;
  }

  /**
   * Create random projections, project points and put points into sets of size
   * about minSplitSize/2
   * 
   * @param indexes of points in point set that are projected, typically 0..N-1
   */
  public void computeSetsBounds(DBIDs ptList) {
    LOG.statistics(new LongStatistic(RandProNeighsAndDensities.class.getName() + ".partition-size", nPointSetSplits));
    LOG.statistics(new LongStatistic(RandProNeighsAndDensities.class.getName() + ".num-projections", nProject1d));
    splitsets = new ArrayList<>();

    // perform projections of points
    projectedPoints = new DoubleDataStore[nProject1d];
    DoubleDataStore[] tmpPro = new DoubleDataStore[nProject1d];

    FiniteProgress projp = LOG.isVerbose() ? new FiniteProgress("Random projections", nProject1d, LOG) : null;
    for(int j = 0; j < nProject1d; j++) {
      double[] currRp = new double[nDimensions];
      double sum = 0;
      for(int i = 0; i < nDimensions; i++) {
        double fl = rand.nextDouble() - 0.5;
        currRp[i] = fl;
        sum += fl * fl;
      }
      sum = Math.sqrt(sum);
      for(int i = 0; i < nDimensions; i++) {
        currRp[i] /= sum;
      }
      WritableDoubleDataStore currPro = DataStoreUtil.makeDoubleStorage(ptList, DataStoreFactory.HINT_HOT);

      for(DBIDIter it = ptList.iter(); it.valid(); it.advance()) {
        NumberVector vecPt = points.get(it);
        // Dot product:
        double sum2 = 0;
        for(int i = 0; i < nDimensions; i++) {
          sum2 += currRp[i] * vecPt.doubleValue(i);
        }
        currPro.put(it, sum2);
      }
      projectedPoints[j] = currPro;
      LOG.incrementProcessed(projp);
    }
    LOG.ensureCompleted(projp);

    // split entire point set, reuse projections by shuffling them
    TIntArrayList proind = new TIntArrayList(nProject1d);
    for(int j = 0; j < nProject1d; j++) {
      proind.add(j);
    }
    FiniteProgress splitp = LOG.isVerbose() ? new FiniteProgress("Splitting data", nPointSetSplits, LOG) : null;
    for(int avgP = 0; avgP < nPointSetSplits; avgP++) {
      // shuffle projections
      for(int i = 0; i < nProject1d; i++) {
        tmpPro[i] = projectedPoints[i];
      }
      proind.shuffle(rand);
      TIntIterator it = proind.iterator();
      int i = 0;
      while(it.hasNext()) {
        int cind = it.next();
        projectedPoints[cind] = tmpPro[i];
        i++;
      }

      // split point set
      splitupNoSort(DBIDUtil.newArray(ptList), 0);
      LOG.incrementProcessed(splitp);
    }
    LOG.ensureCompleted(splitp);
  }

  /**
   * Recursively splits entire point set until the set is below a threshold
   * 
   * @param indexes of points in point set that are in the current set
   * @param depth of projection (how many times point set has been split
   *        already)
   */
  public void splitupNoSort(ArrayModifiableDBIDs ind, int dim) {
    int nele = ind.size();
    dim = dim % nProject1d;// choose a projection of points
    DoubleDataStore tpro = projectedPoints[dim];

    // save set such that used for density or neighborhood computation
    // sets should be roughly minSplitSize
    if(nele > minSplitSize * (1 - sizeTolerance) && nele < minSplitSize * (1 + sizeTolerance)) {
      // sort set, since need median element later
      ind.sort(0, nele - 1, new DataStoreUtil.AscendingByDoubleDataStore(tpro));
      splitsets.add(ind);
    }

    // compute splitting element
    // do not store set or even sort set, since it is too large
    if(nele > minSplitSize) {
      // splits can be performed either by distance (between min,maxCoord) or by
      // picking a point randomly(picking index of point)
      // outcome is similar

      // int minInd splitByDistance(ind, nele, tpro);
      int minInd = splitRandomly(ind, nele, tpro);

      // split set recursively
      // position used for splitting the projected points into two
      // sets used for recursive splitting
      int splitpos = minInd + 1;
      splitupNoSort(DBIDUtil.newArray(ind.slice(0, splitpos)), dim + 1);
      splitupNoSort(DBIDUtil.newArray(ind.slice(splitpos, nele)), dim + 1);
    }
  }

  public int splitRandomly(ArrayModifiableDBIDs ind, int nele, DoubleDataStore tpro) {
    // pick random splitting element based on position
    int randInd = rand.nextInt(nele); // element less than nele
    DBIDArrayIter it = ind.iter().seek(randInd);
    double rs = tpro.doubleValue(it);
    int minInd = 0, maxInd = nele - 1;
    // permute elements such that all points smaller than the splitting
    // element are on the right and the others on the left in the array
    while(minInd < maxInd) {
      double currEle = tpro.doubleValue(it.seek(minInd));
      if(currEle > rs) {
        while(minInd < maxInd && tpro.doubleValue(it.seek(maxInd)) > rs) {
          maxInd--;
        }
        if(minInd == maxInd) {
          break;
        }
        ind.swap(minInd, maxInd);
        maxInd--;
      }
      minInd++;
    }
    if(minInd == nele - 1) {
      minInd = nele / 2;
    }
    return minInd;
  }

  public int splitByDistance(ArrayModifiableDBIDs ind, int nele, DoubleDataStore tpro) {
    DBIDArrayIter it = ind.iter();
    // pick random splitting point based on distance
    double rmin = Double.MAX_VALUE / 2, rmax = -Double.MAX_VALUE / 2;
    int minInd = 0, maxInd = nele - 1;
    for(it.seek(0); it.valid(); it.advance()) {
      double currEle = tpro.doubleValue(it);
      rmin = Math.min(currEle, rmin);
      rmax = Math.max(currEle, rmax);
    }

    if(rmin != rmax) { // if not all elements are the same
      double rs = rmin + rand.nextDouble() * (rmax - rmin);

      // permute elements such that all points smaller than the splitting
      // element are on the right and the others on the left in the array
      while(minInd < maxInd) {
        double currEle = tpro.doubleValue(it.seek(minInd));
        if(currEle > rs) {
          while(minInd < maxInd && tpro.doubleValue(it.seek(maxInd)) > rs) {
            maxInd--;
          }
          if(minInd == maxInd) {
            break;
          }
          ind.swap(minInd, maxInd);
          maxInd--;
        }
        minInd++;
      }
    }
    else {
      // if all elements are the same split in the middle
      minInd = nele / 2;
    }
    return minInd;
  }

  /**
   * Compute list of neighbors for each point from sets resulting from
   * projection
   * 
   * @return list of neighbors for each point
   */
  public DataStore<? extends DBIDs> getNeighs() {
    final DBIDs ids = points.getDBIDs();
    // init lists
    WritableDataStore<ArrayModifiableDBIDs> neighs = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT, ArrayModifiableDBIDs.class);
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      neighs.put(it, DBIDUtil.newArray());
    }

    FiniteProgress splitp = LOG.isVerbose() ? new FiniteProgress("Processing splits for neighborhoods", splitsets.size(), LOG) : null;
    // go through all sets
    Iterator<ArrayDBIDs> it1 = splitsets.iterator();
    while(it1.hasNext()) {
      ArrayDBIDs pinSet = it1.next();

      // for each set, ie. each projection line
      final int len = pinSet.size();
      final int indoff = len >> 1; // middle point of projection
      DBIDRef oldind = pinSet.iter().seek(indoff);
      ArrayModifiableDBIDs cneighs2 = neighs.get(oldind);

      // add all points as neighbors to middle point and the the middle point to
      // all other points in set
      for(DBIDIter it = pinSet.iter(); it.valid(); it.advance()) {
        // TODO: original code used binary search and insertions.
        // But maybe our hashes are faster?
        ArrayModifiableDBIDs cneighs = neighs.get(it);
        // only add point if not a neighbor already
        int pos = cneighs.binarySearch(oldind);
        if(pos < 0) { // element not found
          if(-pos > cneighs.size()) {
            cneighs.add(oldind); // append at end
          }
          else {
            cneighs.insert(-pos - 1, oldind);
          }
        }
        pos = cneighs2.binarySearch(it);
        if(pos < 0) { // element not found
          if(-pos > cneighs2.size()) {
            cneighs2.add(it); // append at end
          }
          else {
            cneighs2.insert(-pos - 1, it);
          }
        }
      }
      LOG.incrementProcessed(splitp);
    }
    LOG.ensureCompleted(splitp);
    return neighs;
  }

  /**
   * Compute for each point a density estimate as inverse of average distance to
   * a point in a projected set
   * 
   * @return for each point average distance to point in a set
   */
  public DoubleDataStore computeAverageDistInSet() {
    WritableDoubleDataStore davg = DataStoreUtil.makeDoubleStorage(points.getDBIDs(), DataStoreFactory.HINT_HOT);
    WritableIntegerDataStore nDists = DataStoreUtil.makeIntegerStorage(points.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress splitp = LOG.isVerbose() ? new FiniteProgress("Processing splits for density estimation", splitsets.size(), LOG) : null;
    for(Iterator<ArrayDBIDs> it1 = splitsets.iterator(); it1.hasNext();) {
      ArrayDBIDs pinSet = it1.next();
      final int len = pinSet.size();
      final int indoff = len >> 1;
      DBIDRef oldind = pinSet.iter().seek(indoff);
      for(DBIDArrayIter it = pinSet.iter(); it.getOffset() < len; it.advance()) {
        if(DBIDUtil.equal(it, oldind)) {
          continue;
        }
        double dist = EuclideanDistanceFunction.STATIC.distance(points.get(it), points.get(oldind));
        davg.increment(oldind, dist);
        nDists.increment(oldind, 1);
        davg.increment(it, dist);
        nDists.increment(it, 1);
      }
      LOG.incrementProcessed(splitp);
    }
    LOG.ensureCompleted(splitp);
    for(DBIDIter it = points.getDBIDs().iter(); it.valid(); it.advance()) {
      // it might be that a point does not occur for a certain size of a
      // projection (likely if do few projections, in this case there is no avg
      // distance)
      int count = nDists.intValue(it);
      double val = (count == 0) ? FastOPTICS.undefinedDist : (davg.doubleValue(it) / count);
      davg.put(it, val);
    }
    nDists.destroy();
    return davg;
  }
}
