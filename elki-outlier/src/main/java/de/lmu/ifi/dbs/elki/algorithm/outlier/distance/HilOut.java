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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparatorMinHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ObjectHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Fast Outlier Detection in High Dimensional Spaces
 * <p>
 * Outlier Detection using Hilbert space filling curves
 * <p>
 * Reference:
 * <p>
 * F. Angiulli, C. Pizzuti<br>
 * Fast Outlier Detection in High Dimensional Spaces<br>
 * Proc. European Conf. Principles of Knowledge Discovery and Data Mining
 * (PKDD'02)
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - HilbertFeatures
 * @composed - - - ScoreType
 * @assoc - - - HilFeature
 *
 * @param <O> Object type
 */
@Title("Fast Outlier Detection in High Dimensional Spaces")
@Description("Algorithm to compute outliers using Hilbert space filling curves")
@Reference(authors = "F. Angiulli, C. Pizzuti", //
    title = "Fast Outlier Detection in High Dimensional Spaces", //
    booktitle = "Proc. European Conf. Principles of Knowledge Discovery and Data Mining (PKDD'02)", //
    url = "https://doi.org/10.1007/3-540-45681-3_2", //
    bibkey = "DBLP:conf/pkdd/AngiulliP02")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.HilOut" })
public class HilOut<O extends NumberVector> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HilOut.class);

  /**
   * Number of nearest neighbors
   */
  private int k;

  /**
   * Number of outliers to compute exactly
   */
  private int n;

  /**
   * Hilbert precision
   */
  private int h;

  /**
   * LPNorm p parameter
   */
  private double t;

  /**
   * Reporting mode: exact (top n) only, or all
   */
  private Enum<ScoreType> tn;

  /**
   * Distance query
   */
  private DistanceQuery<O> distq;

  /**
   * Set sizes, total and current iteration
   */
  private int capital_n, n_star, capital_n_star, d;

  /**
   * Outlier threshold
   */
  private double omega_star;

  /**
   * Type of output: all scores (upper bounds) or top n only
   * 
   * @author Jonathan von Brünken
   */
  public enum ScoreType {
    All, TopN
  }

  /**
   * Constructor.
   * 
   * @param k Number of Next Neighbors
   * @param n Number of Outlier
   * @param h Number of Bits for precision to use - max 32
   * @param tn TopN or All Outlier Rank to return
   */
  protected HilOut(LPNormDistanceFunction distfunc, int k, int n, int h, Enum<ScoreType> tn) {
    super(distfunc);
    this.n = n;
    // HilOut does not count the object itself. We do in KNNWeightOutlier.
    this.k = k - 1;
    this.h = h;
    this.tn = tn;
    this.t = distfunc.getP();
    this.n_star = 0;
    this.omega_star = 0.0;
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    distq = database.getDistanceQuery(relation, getDistanceFunction());
    d = RelationUtil.dimensionality(relation);
    WritableDoubleDataStore hilout_weight = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    // Compute extend of dataset.
    double[] min;
    double diameter = 0; // Actually "length of edge"
    {
      double[][] hbbs = RelationUtil.computeMinMax(relation);
      min = hbbs[0];
      double[] max = hbbs[1];
      for(int i = 0; i < d; i++) {
        diameter = Math.max(diameter, max[i] - min[i]);
      }
      // Enlarge bounding box to have equal lengths.
      for(int i = 0; i < d; i++) {
        double diff = (diameter - (max[i] - min[i])) * .5;
        min[i] -= diff;
        max[i] += diff;
      }
      if(LOG.isVerbose()) {
        LOG.verbose("Rescaling dataset by " + (1 / diameter) + " to fit the unit cube.");
      }
    }

    // Initialization part
    capital_n_star = capital_n = relation.size();
    HilbertFeatures h = new HilbertFeatures(relation, min, diameter);

    FiniteProgress progressHilOut = LOG.isVerbose() ? new FiniteProgress("HilOut iterations", d + 1, LOG) : null;
    FiniteProgress progressTrueOut = LOG.isVerbose() ? new FiniteProgress("True outliers found", n, LOG) : null;
    // Main part: 1. Phase max. d+1 loops
    for(int j = 0; j <= d && n_star < n; j++) {
      // initialize (clear) out and wlb - not 100% clear in the paper
      h.out.clear();
      h.wlb.clear();
      // Initialize Hilbert values in pf according to current shift
      h.initialize(.5 * j / (d + 1));
      // scan the Data according to the current shift; build out and wlb
      scan(h, (int) (k * capital_n / (double) capital_n_star));
      // determine the true outliers (n_star)
      trueOutliers(h);
      if(progressTrueOut != null) {
        progressTrueOut.setProcessed(n_star, LOG);
      }
      // Build the top Set as out + wlb
      h.top.clear();
      HashSetModifiableDBIDs top_keys = DBIDUtil.newHashSet(h.out.size());
      for(ObjectHeap.UnsortedIter<HilFeature> iter = h.out.unsortedIter(); iter.valid(); iter.advance()) {
        HilFeature entry = iter.get();
        top_keys.add(entry.id);
        h.top.add(entry);
      }
      for(ObjectHeap.UnsortedIter<HilFeature> iter = h.wlb.unsortedIter(); iter.valid(); iter.advance()) {
        HilFeature entry = iter.get();
        if(!top_keys.contains(entry.id)) {
          // No need to update top_keys - discarded
          h.top.add(entry);
        }
      }
      LOG.incrementProcessed(progressHilOut);
    }
    // 2. Phase: Additional Scan if less than n true outliers determined
    if(n_star < n) {
      h.out.clear();
      h.wlb.clear();
      // TODO: reinitialize shift to 0?
      scan(h, capital_n);
    }
    if(progressHilOut != null) {
      progressHilOut.setProcessed(d, LOG);
      progressHilOut.ensureCompleted(LOG);
    }
    if(progressTrueOut != null) {
      progressTrueOut.setProcessed(n, LOG);
      progressTrueOut.ensureCompleted(LOG);
    }
    DoubleMinMax minmax = new DoubleMinMax();
    // Return weights in out
    if(tn == ScoreType.TopN) {
      minmax.put(0.0);
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        hilout_weight.putDouble(iditer, 0.0);
      }
      for(ObjectHeap.UnsortedIter<HilFeature> iter = h.out.unsortedIter(); iter.valid(); iter.advance()) {
        HilFeature ent = iter.get();
        minmax.put(ent.ubound);
        hilout_weight.putDouble(ent.id, ent.ubound);
      }
    }
    // Return all weights in pf
    else {
      for(HilFeature ent : h.pf) {
        minmax.put(ent.ubound);
        hilout_weight.putDouble(ent.id, ent.ubound);
      }
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("HilOut weight", "hilout-weight", hilout_weight, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }

  /**
   * Scan function performs a squential scan over the data.
   * 
   * @param hf the hilbert features
   * @param k0
   */
  private void scan(HilbertFeatures hf, int k0) {
    final int mink0 = Math.min(2 * k0, capital_n - 1);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Scanning with k0=" + k0 + " (" + mink0 + ")" + " N*=" + capital_n_star);
    }
    for(int i = 0; i < hf.pf.length; i++) {
      if(hf.pf[i].ubound < omega_star) {
        continue;
      }
      if(hf.pf[i].lbound < hf.pf[i].ubound) {
        double omega = hf.fastUpperBound(i);
        if(omega < omega_star) {
          hf.pf[i].ubound = omega;
        }
        else {
          int maxcount;
          // capital_n-1 instead of capital_n: all, except self
          if(hf.top.contains(hf.pf[i])) {
            maxcount = capital_n - 1;
          }
          else {
            maxcount = mink0;
          }
          innerScan(hf, i, maxcount);
        }
      }
      if(hf.pf[i].ubound > 0) {
        hf.updateOUT(i);
      }
      if(hf.pf[i].lbound > 0) {
        hf.updateWLB(i);
      }
      if(hf.wlb.size() >= n) {
        omega_star = Math.max(omega_star, hf.wlb.peek().lbound);
      }
    }
  }

  /**
   * innerScan function calculates new upper and lower bounds and inserts the
   * points of the neighborhood the bounds are based on in the NN Set
   * 
   * @param i position in pf of the feature for which the bounds should be
   *        calculated
   * @param maxcount maximal size of the neighborhood
   */
  private void innerScan(HilbertFeatures hf, final int i, final int maxcount) {
    final O p = hf.relation.get(hf.pf[i].id); // Get only once for performance
    int a = i, b = i;
    int level = h, levela = h, levelb = h;
    // Explore up to "maxcount" neighbors in this pass
    for(int count = 0; count < maxcount; count++) {
      final int c; // Neighbor to explore
      if(a == 0) { // At left end, explore right
        // assert (b < capital_n - 1);
        levelb = Math.min(levelb, hf.pf[b].level);
        b++;
        c = b;
      }
      else if(b >= capital_n - 1) { // At right end, explore left
        // assert (a > 0);
        a--;
        levela = Math.min(levela, hf.pf[a].level);
        c = a;
      }
      else if(hf.pf[a - 1].level >= hf.pf[b].level) { // Prefer higher level
        a--;
        levela = Math.min(levela, hf.pf[a].level);
        c = a;
      }
      else {
        // assert (b < capital_n - 1);
        levelb = Math.min(levelb, hf.pf[b].level);
        b++;
        c = b;
      }
      if(!hf.pf[i].nn_keys.contains(hf.pf[c].id)) {
        // hf.distcomp ++;
        hf.pf[i].insert(hf.pf[c].id, distq.distance(p, hf.pf[c].id), k);
        if(hf.pf[i].nn.size() == k) {
          if(hf.pf[i].sum_nn < omega_star) {
            break; // stop = true
          }
          final int mlevel = Math.max(levela, levelb);
          if(mlevel < level) {
            level = mlevel;
            final double delta = hf.minDistLevel(hf.pf[i].id, level);
            if(delta >= hf.pf[i].nn.peek().doubleValue()) {
              break; // stop = true
            }
          }
        }
      }
    }
    double br = hf.boxRadius(i, a - 1, b + 1);
    double newlb = 0.0;
    double newub = 0.0;
    for(ObjectHeap.UnsortedIter<DoubleDBIDPair> iter = hf.pf[i].nn.unsortedIter(); iter.valid(); iter.advance()) {
      DoubleDBIDPair entry = iter.get();
      newub += entry.doubleValue();
      if(entry.doubleValue() <= br) {
        newlb += entry.doubleValue();
      }
    }
    if(newlb > hf.pf[i].lbound) {
      hf.pf[i].lbound = newlb;
    }
    if(newub < hf.pf[i].ubound) {
      hf.pf[i].ubound = newub;
    }
  }

  /**
   * trueOutliers function updates n_star
   * 
   * @param h the HilberFeatures
   * 
   */

  private void trueOutliers(HilbertFeatures h) {
    n_star = 0;
    for(ObjectHeap.UnsortedIter<HilFeature> iter = h.out.unsortedIter(); iter.valid(); iter.advance()) {
      HilFeature entry = iter.get();
      if(entry.ubound >= omega_star && (entry.ubound - entry.lbound < 1E-10)) {
        n_star++;
      }
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new LPNormDistanceFunction(t).getInputTypeRestriction());
  }

  /**
   * Class organizing the data points along a hilbert curve.
   * 
   * @author Jonathan von Brünken
   * 
   * @composed - - - HilFeature
   */
  class HilbertFeatures {
    // public int distcomp = 1;

    /**
     * Relation indexed
     */
    Relation<O> relation;

    /**
     * Hilbert representation ("point features")
     */
    HilFeature[] pf;

    /**
     * Data space minimums
     */
    double[] min;

    /**
     * Data space diameter
     */
    double diameter;

    /**
     * Current curve shift
     */
    double shift;

    /**
     * Top candidates
     */
    private Set<HilFeature> top;

    /**
     * "OUT"
     */
    private ObjectHeap<HilFeature> out;

    /**
     * "WLB"
     */
    private ObjectHeap<HilFeature> wlb;

    /**
     * Constructor.
     * 
     * @param relation Relation to index
     * @param min Minimums for data space
     * @param diameter Diameter of data space
     */
    public HilbertFeatures(Relation<O> relation, double[] min, double diameter) {
      super();
      this.relation = relation;
      this.min = min;
      this.diameter = diameter;
      this.pf = new HilFeature[relation.size()];

      int pos = 0;
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        pf[pos++] = new HilFeature(DBIDUtil.deref(iditer), new ComparableMaxHeap<DoubleDBIDPair>(k));
      }
      this.out = new ComparatorMinHeap<>(n, new Comparator<HilFeature>() {
        @Override
        public int compare(HilFeature o1, HilFeature o2) {
          return Double.compare(o1.ubound, o2.ubound);
        }
      });
      this.wlb = new ComparatorMinHeap<>(n, new Comparator<HilFeature>() {
        @Override
        public int compare(HilFeature o1, HilFeature o2) {
          return Double.compare(o1.lbound, o2.lbound);
        }
      });
      this.top = new HashSet<>(2 * n);
    }

    /**
     * Hilbert function to fill pf with shifted Hilbert values. Also calculates
     * the number current Outlier candidates capital_n_star
     * 
     * @param shift the new shift factor
     */
    private void initialize(double shift) {
      this.shift = shift;
      // FIXME: 64 bit mode untested - sign bit is tricky to handle correctly
      // with the rescaling. 63 bit should be fine. The sign bit probably needs
      // to be handled differently, or at least needs careful testing of the API
      if(h >= 32) { // 32 to 63 bit
        final long scale = Long.MAX_VALUE; // = 63 bits
        for(int i = 0; i < pf.length; i++) {
          NumberVector obj = relation.get(pf[i].id);
          long[] coord = new long[d];
          for(int dim = 0; dim < d; dim++) {
            coord[dim] = (long) (getDimForObject(obj, dim) * .5 * scale);
          }
          pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h, 1);
        }
      }
      else if(h >= 16) { // 16-31 bit
        final int scale = ~1 >>> 1;
        for(int i = 0; i < pf.length; i++) {
          NumberVector obj = relation.get(pf[i].id);
          int[] coord = new int[d];
          for(int dim = 0; dim < d; dim++) {
            coord[dim] = (int) (getDimForObject(obj, dim) * .5 * scale);
          }
          pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h, 1);
        }
      }
      else if(h >= 8) { // 8-15 bit
        final int scale = ~1 >>> 16;
        for(int i = 0; i < pf.length; i++) {
          NumberVector obj = relation.get(pf[i].id);
          short[] coord = new short[d];
          for(int dim = 0; dim < d; dim++) {
            coord[dim] = (short) (getDimForObject(obj, dim) * .5 * scale);
          }
          pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h, 16);
        }
      }
      else { // 1-7 bit
        final int scale = ~1 >>> 8;
        for(int i = 0; i < pf.length; i++) {
          NumberVector obj = relation.get(pf[i].id);
          byte[] coord = new byte[d];
          for(int dim = 0; dim < d; dim++) {
            coord[dim] = (byte) (getDimForObject(obj, dim) * .5 * scale);
          }
          pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h, 24);
        }
      }
      java.util.Arrays.sort(pf);
      // Update levels
      for(int i = 0; i < pf.length - 1; i++) {
        pf[i].level = minRegLevel(i, i + 1);
      }
      // Count candidates
      capital_n_star = 0;
      for(int i = 0; i < pf.length; i++) {
        if(pf[i].ubound >= omega_star) {
          capital_n_star++;
        }
      }
    }

    /**
     * updateOUT function inserts pf[i] in out.
     * 
     * @param i position in pf of the feature to be inserted
     */
    private void updateOUT(int i) {
      if(out.size() < n) {
        out.add(pf[i]);
      }
      else {
        HilFeature head = out.peek();
        if(pf[i].ubound > head.ubound) {
          // replace smallest
          out.replaceTopElement(pf[i]);
        }
      }
    }

    /**
     * updateWLB function inserts pf[i] in wlb.
     * 
     * @param i position in pf of the feature to be inserted
     */
    private void updateWLB(int i) {
      if(wlb.size() < n) {
        wlb.add(pf[i]);
      }
      else {
        HilFeature head = wlb.peek();
        if(pf[i].lbound > head.lbound) {
          // replace smallest
          wlb.replaceTopElement(pf[i]);
        }
      }
    }

    /**
     * fastUpperBound function calculates an upper Bound as k*maxDist(pf[i],
     * smallest neighborhood)
     * 
     * @param i position in pf of the feature for which the bound should be
     *        calculated
     */
    private double fastUpperBound(int i) {
      int pre = i;
      int post = i;
      while(post - pre < k) {
        int pre_level = (pre - 1 >= 0) ? pf[pre - 1].level : -2;
        int post_level = (post < capital_n - 1) ? pf[post].level : -2;
        if(post_level >= pre_level) {
          post++;
        }
        else {
          pre--;
        }
      }
      return k * maxDistLevel(pf[i].id, minRegLevel(pre, post));
    }

    /**
     * minDist function calculate the minimal Distance from Vector p to the
     * border of the corresponding r-region at the given level
     * 
     * @param id Object ID
     * @param level Level of the corresponding r-region
     */
    private double minDistLevel(DBID id, int level) {
      final NumberVector obj = relation.get(id);
      // level 1 is supposed to have r=1 as in the original publication
      // 2 ^ - (level - 1)
      final double r = 1.0 / (1 << (level - 1));
      double dist = Double.POSITIVE_INFINITY;
      for(int dim = 0; dim < d; dim++) {
        final double p_m_r = getDimForObject(obj, dim) % r;
        dist = Math.min(dist, Math.min(p_m_r, r - p_m_r));
      }
      return dist * diameter;
    }

    /**
     * maxDist function calculate the maximal Distance from Vector p to the
     * border of the corresponding r-region at the given level
     * 
     * @param id Object ID
     * @param level Level of the corresponding r-region
     */
    private double maxDistLevel(DBID id, int level) {
      final NumberVector obj = relation.get(id);
      // level 1 is supposed to have r=1 as in the original publication
      final double r = 1.0 / (1 << (level - 1));
      double dist;
      if(t == 1.0) {
        dist = 0.0;
        for(int dim = 0; dim < d; dim++) {
          final double p_m_r = getDimForObject(obj, dim) % r;
          // assert (p_m_r >= 0);
          dist += Math.max(p_m_r, r - p_m_r);
        }
      }
      else if(t == 2.0) {
        dist = 0.0;
        for(int dim = 0; dim < d; dim++) {
          final double p_m_r = getDimForObject(obj, dim) % r;
          // assert (p_m_r >= 0);
          double a = Math.max(p_m_r, r - p_m_r);
          dist += a * a;
        }
        dist = FastMath.sqrt(dist);
      }
      else if(!Double.isInfinite(t)) {
        dist = 0.0;
        for(int dim = 0; dim < d; dim++) {
          final double p_m_r = getDimForObject(obj, dim) % r;
          dist += FastMath.pow(Math.max(p_m_r, r - p_m_r), t);
        }
        dist = FastMath.pow(dist, 1.0 / t);
      }
      else {
        dist = Double.NEGATIVE_INFINITY;
        for(int dim = 0; dim < d; dim++) {
          final double p_m_r = getDimForObject(obj, dim) % r;
          dist = Math.max(dist, Math.max(p_m_r, r - p_m_r));
        }
      }
      return dist * diameter;
    }

    /**
     * Number of levels shared
     * 
     * @param a First bitset
     * @param b Second bitset
     * @return Number of level shared
     */
    private int numberSharedLevels(long[] a, long[] b) {
      for(int i = 0, j = a.length - 1; i < a.length; i++, j--) {
        final long diff = a[j] ^ b[j];
        if(diff != 0) {
          // expected unused = available - used
          final int expected = (a.length * Long.SIZE) - (d * h);
          return ((BitsUtil.numberOfLeadingZeros(diff) + i * Long.SIZE) - expected) / d;
        }
      }
      return h - 1;
    }

    /**
     * minReg function calculate the minimal r-region level containing two
     * points
     * 
     * @param a index of first point in pf
     * @param b index of second point in pf
     * 
     * @return Level of the r-region
     */
    private int minRegLevel(int a, int b) {
      // Sanity test: first level different -> region of level 0, r=2
      // all same: level h - 1
      return numberSharedLevels(pf[a].hilbert, pf[b].hilbert);
    }

    /**
     * Level of the maximum region containing ref but not q
     * 
     * @param ref Reference point
     * @param q Query point
     * @return Number of bits shared across all dimensions
     */
    private int maxRegLevel(int ref, int q) {
      // Sanity test: first level different -> region of level 1, r=1
      // all same: level h
      return numberSharedLevels(pf[ref].hilbert, pf[q].hilbert) + 1;
    }

    /**
     * boxRadius function calculate the Boxradius
     * 
     * @param i index of first point
     * @param a index of second point
     * @param b index of third point
     * 
     * @return box radius
     */
    private double boxRadius(int i, int a, int b) {
      // level are inversely ordered to box sizes. min -> max
      final int level;
      if(a < 0) {
        if(b >= pf.length) {
          return Double.POSITIVE_INFINITY;
        }
        level = maxRegLevel(i, b);
      }
      else if(b >= pf.length) {
        level = maxRegLevel(i, a);
      }
      else {
        level = Math.max(maxRegLevel(i, a), maxRegLevel(i, b));
      }
      return minDistLevel(pf[i].id, level);
    }

    /**
     * Get the (projected) position of the object in dimension dim.
     * 
     * @param obj Object
     * @param dim Dimension
     * @return Projected and shifted position
     */
    private double getDimForObject(NumberVector obj, int dim) {
      return (obj.doubleValue(dim) - min[dim]) / diameter + shift;
    }
  }

  /**
   * Hilbert representation of a single object.
   * 
   * Details of this representation are discussed in the main HilOut
   * publication, see "point features".
   * 
   * @author Jonathan von Brünken
   */
  final static class HilFeature implements Comparable<HilFeature> {
    /**
     * Object ID
     */
    public DBID id;

    /**
     * Hilbert representation
     * 
     * TODO: use byte[] to save some memory, but slower?
     */
    public long[] hilbert = null;

    /**
     * Object level
     */
    public int level = 0;

    /**
     * Upper bound for object
     */
    public double ubound = Double.POSITIVE_INFINITY;

    /**
     * Lower bound of object
     */
    public double lbound = 0.0;

    /**
     * Heap with the nearest known neighbors
     */
    public ObjectHeap<DoubleDBIDPair> nn;

    /**
     * Set representation of the nearest neighbors for faster lookups
     */
    public HashSetModifiableDBIDs nn_keys = DBIDUtil.newHashSet();

    /**
     * Current weight (sum of nn distances)
     */
    public double sum_nn = 0.0;

    /**
     * Constructor.
     * 
     * @param id Object ID
     * @param nn Heap for neighbors
     */
    public HilFeature(DBID id, ObjectHeap<DoubleDBIDPair> nn) {
      super();
      this.id = id;
      this.nn = nn;
    }

    @Override
    public int compareTo(HilFeature o) {
      return BitsUtil.compare(this.hilbert, o.hilbert);
    }

    /**
     * insert function inserts a nearest neighbor into a features nn list and
     * its distance
     * 
     * @param id DBID of the nearest neighbor
     * @param dt distance or the neighbor to the features position
     * @param k K
     */
    protected void insert(DBID id, double dt, int k) {
      // assert (!nn_keys.contains(id));
      if(nn.size() < k) {
        DoubleDBIDPair entry = DBIDUtil.newPair(dt, id);
        nn.add(entry);
        nn_keys.add(id);
        sum_nn += dt;
      }
      else {
        DoubleDBIDPair head = nn.peek();
        if(dt < head.doubleValue()) {
          head = nn.poll(); // Remove worst
          sum_nn -= head.doubleValue();
          nn_keys.remove(head);

          // assert (nn.peek().doubleDistance() <= head.doubleDistance());

          DoubleDBIDPair entry = DBIDUtil.newPair(dt, id);
          nn.add(entry);
          nn_keys.add(id);
          sum_nn += dt;
        }
      }
    }
  }

  /**
   * Parameterization class
   * 
   * @author Jonathan von Brünken
   * 
   * @hidden
   * 
   * @param <O> Vector type
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify how many next neighbors should be used in the
     * computation
     */
    public static final OptionID K_ID = new OptionID("HilOut.k", "Compute up to k next neighbors");

    /**
     * Parameter to specify how many outliers should be computed
     */
    public static final OptionID N_ID = new OptionID("HilOut.n", "Compute n outliers");

    /**
     * Parameter to specify the maximum Hilbert-Level
     */
    public static final OptionID H_ID = new OptionID("HilOut.h", "Max. Hilbert-Level");

    /**
     * Parameter to specify p of LP-NormDistance
     */
    public static final OptionID T_ID = new OptionID("HilOut.t", "t of Lt Metric");

    /**
     * Parameter to specify if only the Top n, or also approximations for the
     * other elements, should be returned
     */
    public static final OptionID TN_ID = new OptionID("HilOut.tn", "output of Top n or all elements");

    /**
     * Neighborhood size
     */
    protected int k = 5;

    /**
     * Top-n candidates to compute exactly
     */
    protected int n = 10;

    /**
     * Hilbert curve precision
     */
    protected int h = 32;

    /**
     * LPNorm distance function
     */
    protected LPNormDistanceFunction distfunc;

    /**
     * Scores to report: all or top-n only
     */
    protected Enum<ScoreType> tn;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter kP = new IntParameter(K_ID, 5);
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      final IntParameter nP = new IntParameter(N_ID, 10);
      if(config.grab(nP)) {
        n = nP.getValue();
      }

      final IntParameter hP = new IntParameter(H_ID, 32);
      if(config.grab(hP)) {
        h = hP.getValue();
      }

      ObjectParameter<LPNormDistanceFunction> distP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, LPNormDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        distfunc = distP.instantiateClass(config);
      }

      final EnumParameter<ScoreType> tnP = new EnumParameter<>(TN_ID, ScoreType.class, ScoreType.TopN);
      if(config.grab(tnP)) {
        tn = tnP.getValue();
      }
    }

    @Override
    protected HilOut<O> makeInstance() {
      return new HilOut<>(distfunc, k, n, h, tn);
    }
  }
}
