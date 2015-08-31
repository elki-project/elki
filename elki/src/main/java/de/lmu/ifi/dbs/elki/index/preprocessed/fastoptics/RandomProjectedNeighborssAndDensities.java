package de.lmu.ifi.dbs.elki.index.preprocessed.fastoptics;

/* 
 Copyright (C) 2015
 Johannes Schneider, ABB Research, Switzerland, johannes.schneider@alumni.ethz.ch

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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.FastOPTICS;
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
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Random Projections used for computing neighbors and density estimates.
 * 
 * This index is specialized for the algorithm
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.optics.FastOPTICS}
 * 
 * Reference:
 * <p>
 * Schneider, J., & Vlachos, M<br />
 * Fast parameterless density-based clustering via random projections<br />
 * Proc. 22nd ACM international conference on Conference on Information &
 * Knowledge Management (CIKM)
 * </p>
 * 
 * This is based on the original code provided by Johannes Schneider, with
 * ELKIfications and optimizations by Erich Schubert.
 * 
 * TODO: implement one of the Index APIs?
 *
 * @author Johannes Schneider
 * @author Erich Schubert
 */
@Reference(authors = "Schneider, J., & Vlachos, M", //
title = "Fast parameterless density-based clustering via random projections", //
booktitle = "Proc. 22nd ACM international conference on Conference on Information & Knowledge Management (CIKM)", //
url = "http://dx.doi.org/10.1145/2505515.2505590")
public class RandomProjectedNeighborssAndDensities<V extends NumberVector> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(RandomProjectedNeighborssAndDensities.class);

  /**
   * Default constant used to compute number of projections as well as number of
   * splits of point set, ie. constant *log N*d
   */
  // constant in O(log N*d) used to compute number of projections as well as
  // number of splits of point set
  private static final int logOProjectionConst = 20;

  /**
   * Sets used for neighborhood computation should be about minSplitSize Sets
   * are still used if they deviate by less (1+/- sizeTolerance)
   */
  private static final float sizeTolerance = 2f / 3;

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
   * sets that resulted from recursive split of entire point set
   */
  ArrayList<ArrayDBIDs> splitsets;

  /**
   * all projected points
   */
  DoubleDataStore[] projectedPoints;

  /**
   * Random factory.
   */
  RandomFactory rnd;

  /**
   * random number generator
   */
  Random rand;

  /**
   * Constructor.
   *
   * @param rnd Random factory.
   */
  public RandomProjectedNeighborssAndDensities(RandomFactory rnd) {
    this.rnd = rnd;
  }

  /**
   * Create random projections, project points and put points into sets of size
   * about minSplitSize/2
   * 
   * @param points to process
   * @param minimum size for which a point set is further partitioned (roughly
   *        corresponds to minPts in OPTICS)
   * @param indexes of points in point set that are projected, typically 0..N-1
   */
  public void computeSetsBounds(Relation<V> points, int minSplitSize, DBIDs ptList) {
    this.minSplitSize = minSplitSize;
    final int size = points.size();
    final int dim = RelationUtil.dimensionality(points);
    this.points = points;

    // perform O(log N+log dim) splits of the entire point sets projections
    int nPointSetSplits = (int) (logOProjectionConst * MathUtil.log2(size * dim + 1));
    // perform O(log N+log dim) projections of the point set onto a random line
    int nProject1d = (int) (logOProjectionConst * MathUtil.log2(size * dim + 1));

    LOG.statistics(new LongStatistic(RandomProjectedNeighborssAndDensities.class.getName() + ".partition-size", nPointSetSplits));
    LOG.statistics(new LongStatistic(RandomProjectedNeighborssAndDensities.class.getName() + ".num-projections", nProject1d));
    splitsets = new ArrayList<>();

    // perform projections of points
    projectedPoints = new DoubleDataStore[nProject1d];
    DoubleDataStore[] tmpPro = new DoubleDataStore[nProject1d];

    rand = rnd.getSingleThreadedRandom();
    FiniteProgress projp = LOG.isVerbose() ? new FiniteProgress("Random projections", nProject1d, LOG) : null;
    for(int j = 0; j < nProject1d; j++) {
      double[] currRp = new double[dim];
      double sum = 0;
      for(int i = 0; i < dim; i++) {
        double fl = rand.nextDouble() - 0.5;
        currRp[i] = fl;
        sum += fl * fl;
      }
      sum = Math.sqrt(sum);
      for(int i = 0; i < dim; i++) {
        currRp[i] /= sum;
      }
      WritableDoubleDataStore currPro = DataStoreUtil.makeDoubleStorage(ptList, DataStoreFactory.HINT_HOT);

      for(DBIDIter it = ptList.iter(); it.valid(); it.advance()) {
        NumberVector vecPt = points.get(it);
        // Dot product:
        double sum2 = 0;
        for(int i = 0; i < dim; i++) {
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
      splitupNoSort(DBIDUtil.newArray(ptList), 0, size, 0);
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
  public void splitupNoSort(ArrayModifiableDBIDs ind, int begin, int end, int dim) {
    final int nele = end - begin;
    dim = dim % projectedPoints.length;// choose a projection of points
    DoubleDataStore tpro = projectedPoints[dim];

    // save set such that used for density or neighborhood computation
    // sets should be roughly minSplitSize
    if(nele > minSplitSize * (1 - sizeTolerance) && nele < minSplitSize * (1 + sizeTolerance)) {
      // sort set, since need median element later
      ind.sort(begin, end, new DataStoreUtil.AscendingByDoubleDataStore(tpro));
      splitsets.add(DBIDUtil.newArray(ind.slice(begin, end)));
    }

    // compute splitting element
    // do not store set or even sort set, since it is too large
    if(nele > minSplitSize) {
      // splits can be performed either by distance (between min,maxCoord) or by
      // picking a point randomly(picking index of point)
      // outcome is similar

      // int minInd splitByDistance(ind, nele, tpro);
      int minInd = splitRandomly(ind, begin, end, tpro);

      // split set recursively
      // position used for splitting the projected points into two
      // sets used for recursive splitting
      int splitpos = minInd + 1;
      splitupNoSort(ind, begin, splitpos, dim + 1);
      splitupNoSort(ind, splitpos, end, dim + 1);
    }
  }

  /**
   * Split the data set randomly.
   * 
   * @param ind Object index
   * @param begin Interval begin
   * @param end Interval end
   * @param tpro Projection
   * @return Splitting point
   */
  public int splitRandomly(ArrayModifiableDBIDs ind, int begin, int end, DoubleDataStore tpro) {
    final int nele = end - begin;
    DBIDArrayIter it = ind.iter();
    // pick random splitting element based on position
    double rs = tpro.doubleValue(it.seek(begin + rand.nextInt(nele)));
    int minInd = begin, maxInd = end - 1;
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
    // if all elements are the same split in the middle
    if(minInd == end - 1) {
      minInd = (begin + end) >>> 1;
    }
    return minInd;
  }

  /**
   * Split the data set by distances.
   * 
   * @param ind Object index
   * @param begin Interval begin
   * @param end Interval end
   * @param tpro Projection
   * @return Splitting point
   */
  public int splitByDistance(ArrayModifiableDBIDs ind, int begin, int end, DoubleDataStore tpro) {
    DBIDArrayIter it = ind.iter();
    // pick random splitting point based on distance
    double rmin = Double.MAX_VALUE * .5, rmax = -Double.MAX_VALUE * .5;
    int minInd = begin, maxInd = end - 1;
    for(it.seek(begin); it.getOffset() < end; it.advance()) {
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
      minInd = (begin + end) >>> 1;
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
    WritableDataStore<ModifiableDBIDs> neighs = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT, ModifiableDBIDs.class);
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      neighs.put(it, DBIDUtil.newHashSet());
    }

    FiniteProgress splitp = LOG.isVerbose() ? new FiniteProgress("Processing splits for neighborhoods", splitsets.size(), LOG) : null;
    // go through all sets
    Iterator<ArrayDBIDs> it1 = splitsets.iterator();
    while(it1.hasNext()) {
      ArrayDBIDs pinSet = it1.next();

      final int indoff = pinSet.size() >> 1; // middle point of projection
      DBIDRef oldind = pinSet.iter().seek(indoff);
      // add all points as neighbors to middle point
      neighs.get(oldind).addDBIDs(pinSet);

      // and the the middle point to all other points in set
      for(DBIDIter it = pinSet.iter(); it.valid(); it.advance()) {
        neighs.get(it).add(oldind);
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
      V midpoint = points.get(oldind);
      for(DBIDArrayIter it = pinSet.iter(); it.getOffset() < len; it.advance()) {
        if(DBIDUtil.equal(it, oldind)) {
          continue;
        }
        double dist = EuclideanDistanceFunction.STATIC.distance(points.get(it), midpoint);
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
      double val = (count == 0) ? FastOPTICS.UNDEFINED_DISTANCE : (davg.doubleValue(it) / count);
      davg.put(it, val);
    }
    nDists.destroy(); // No longer needed after normalization
    return davg;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Random seed parameter.
     */
    public static final OptionID RANDOM_ID = new OptionID("fastoptics.randomproj.seed", "Random seed for generating projections.");

    /**
     * Random factory.
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected RandomProjectedNeighborssAndDensities<NumberVector> makeInstance() {
      return new RandomProjectedNeighborssAndDensities<>(rnd);
    }
  }
}
