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
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Fast Outlier Detection Using the "approximate Local Correlation Integral".
 * <p>
 * Outlier detection using multiple epsilon neighborhoods.
 * <p>
 * Reference:
 * <p>
 * S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:<br>
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral.<br>
 * In: Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03)
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - ALOCIQuadTree
 *
 * @param <O> Object type
 */
@Title("Approximate LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", //
    title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", //
    booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03)", //
    url = "https://doi.org/10.1109/ICDE.2003.1260802", //
    bibkey = "DBLP:conf/icde/PapadimitriouKGF03")
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.ALOCI")
public class ALOCI<O extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ALOCI.class);

  /**
   * Minimum size for a leaf.
   */
  private int nmin;

  /**
   * Alpha (level difference of sampling and counting neighborhoods)
   */
  private int alpha;

  /**
   * Number of trees to generate (forest size)
   */
  private int g;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Distance function
   */
  private NumberVectorDistanceFunction<?> distFunc;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param nmin Minimum neighborhood size
   * @param alpha Alpha value
   * @param g Number of grids to use
   * @param rnd Random generator.
   */
  public ALOCI(NumberVectorDistanceFunction<?> distanceFunction, int nmin, int alpha, int g, RandomFactory rnd) {
    super();
    this.distFunc = distanceFunction;
    this.nmin = nmin;
    this.alpha = alpha;
    this.g = g;
    this.rnd = rnd;
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final Random random = rnd.getSingleThreadedRandom();
    FiniteProgress progressPreproc = LOG.isVerbose() ? new FiniteProgress("Build aLOCI quadtress", g, LOG) : null;

    // Compute extend of dataset.
    double[] min, max;
    {
      double[][] hbbs = RelationUtil.computeMinMax(relation);
      min = hbbs[0];
      max = hbbs[1];
      double maxd = 0;
      for(int i = 0; i < dim; i++) {
        maxd = MathUtil.max(maxd, max[i] - min[i]);
      }
      // Enlarge bounding box to have equal lengths.
      for(int i = 0; i < dim; i++) {
        double diff = (maxd - (max[i] - min[i])) * .5;
        min[i] -= diff;
        max[i] += diff;
      }
    }

    List<ALOCIQuadTree> qts = new ArrayList<>(g);

    double[] nshift = new double[dim];
    ALOCIQuadTree qt = new ALOCIQuadTree(min, max, nshift, nmin, relation);
    qts.add(qt);
    LOG.incrementProcessed(progressPreproc);
    /*
     * create the remaining g-1 shifted QuadTrees. This not clearly described in
     * the paper and therefore implemented in a way that achieves good results
     * with the test data.
     */
    for(int shift = 1; shift < g; shift++) {
      double[] svec = new double[dim];
      for(int i = 0; i < dim; i++) {
        svec[i] = random.nextDouble() * (max[i] - min[i]);
      }
      qt = new ALOCIQuadTree(min, max, svec, nmin, relation);
      qts.add(qt);
      LOG.incrementProcessed(progressPreproc);
    }
    LOG.ensureCompleted(progressPreproc);

    // aLOCI main loop: evaluate
    FiniteProgress progressLOCI = LOG.isVerbose() ? new FiniteProgress("Compute aLOCI scores", relation.size(), LOG) : null;
    WritableDoubleDataStore mdef_norm = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final O obj = relation.get(iditer);

      double maxmdefnorm = 0;
      // For each level
      for(int l = 0;; l++) {
        // Find the closest C_i
        Node ci = null;
        for(int i = 0; i < g; i++) {
          Node ci2 = qts.get(i).findClosestNode(obj, l);
          if(ci2.getLevel() != l) {
            continue;
          }
          // TODO: always use manhattan?
          if(ci == null || distFunc.distance(ci, obj) > distFunc.distance(ci2, obj)) {
            ci = ci2;
          }
        }
        // LOG.debug("level:" + (ci != null ? ci.getLevel() : -1) +" l:"+l);
        if(ci == null) {
          break; // no matching tree for this level.
        }

        // Find the closest C_j
        Node cj = null;
        for(int i = 0; i < g; i++) {
          Node cj2 = qts.get(i).findClosestNode(ci, l - alpha);
          // TODO: allow higher levels or not?
          if(cj != null && cj2.getLevel() < cj.getLevel()) {
            continue;
          }
          // TODO: always use manhattan?
          if(cj == null || distFunc.distance(cj, ci) > distFunc.distance(cj2, ci)) {
            cj = cj2;
          }
        }
        // LOG.debug("level:" + (cj != null ? cj.getLevel() : -1) +" l:"+l);
        if(cj == null) {
          continue; // no matching tree for this level.
        }
        double mdefnorm = calculate_MDEF_norm(cj, ci);
        // LOG.warning("level:" + ci.getLevel() + "/" + cj.getLevel() +
        // " mdef: " + mdefnorm);
        maxmdefnorm = MathUtil.max(maxmdefnorm, mdefnorm);
      }
      // Store results
      mdef_norm.putDouble(iditer, maxmdefnorm);
      minmax.put(maxmdefnorm);
      LOG.incrementProcessed(progressLOCI);
    }
    LOG.ensureCompleted(progressLOCI);
    DoubleRelation scoreResult = new MaterializedDoubleRelation("aLOCI normalized MDEF", "aloci-mdef-outlier", mdef_norm, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }

  /**
   * Method for the MDEF calculation
   *
   * @param sn Sampling Neighborhood
   * @param cg Counting Neighborhood
   *
   * @return MDEF norm
   */
  private static double calculate_MDEF_norm(Node sn, Node cg) {
    // get the square sum of the counting neighborhoods box counts
    long sq = sn.getSquareSum(cg.getLevel() - sn.getLevel());
    /*
     * if the square sum is equal to box count of the sampling Neighborhood then
     * n_hat is equal one, and as cg needs to have at least one Element mdef
     * would get zero or lower than zero. This is the case when all of the
     * counting Neighborhoods contain one or zero Objects. Additionally, the
     * cubic sum, square sum and sampling Neighborhood box count are all equal,
     * which leads to sig_n_hat being zero and thus mdef_norm is either negative
     * infinite or undefined. As the distribution of the Objects seem quite
     * uniform, a mdef_norm value of zero ( = no outlier) is appropriate and
     * circumvents the problem of undefined values.
     */
    if(sq == sn.getCount()) {
      return 0.0;
    }
    // calculation of mdef according to the paper and standardization as done in
    // LOCI
    long cb = sn.getCubicSum(cg.getLevel() - sn.getLevel());
    double n_hat = (double) sq / sn.getCount();
    double sig_n_hat = FastMath.sqrt(cb * sn.getCount() - (sq * sq)) / sn.getCount();
    // Avoid NaN - correct result 0.0?
    if(sig_n_hat < Double.MIN_NORMAL) {
      return 0.0;
    }
    double mdef = n_hat - cg.getCount();
    return mdef / sig_n_hat;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distFunc.getInputTypeRestriction());
  }

  /**
   * Simple quadtree for ALOCI. Not storing the actual objects, just the counts.
   *
   * Furthermore, the quadtree can be shifted by a specified vector, wrapping
   * around min/max
   *
   * @author Jonathan von Brünken
   * @author Erich Schubert
   *
   * @composed - - - Node
   */
  static class ALOCIQuadTree {
    /**
     * Tree parameters
     */
    private double[] shift, min, width;

    /**
     * Maximum fill for a page before splitting
     */
    private int nmin;

    /**
     * Tree root
     */
    Node root;

    /**
     * Relation indexed.
     */
    private Relation<? extends NumberVector> relation;

    /**
     * Constructor.
     *
     * @param min Minimum coordinates
     * @param max Maximum coordinates
     * @param shift Tree shift offset
     * @param nmin Maximum size for a page to split
     * @param relation Relation to index
     */
    public ALOCIQuadTree(double[] min, double[] max, double[] shift, int nmin, Relation<? extends NumberVector> relation) {
      super();
      assert (min.length <= 32) : "Quadtrees are only supported for up to 32 dimensions";
      this.shift = shift;
      this.nmin = nmin;
      this.min = min;
      this.width = new double[min.length];
      for(int d = 0; d < min.length; d++) {
        width[d] = max[d] - min[d];
        if(width[d] <= 0) {
          width[d] = 1;
        }
      }
      double[] center = new double[min.length];
      for(int d = 0; d < min.length; d++) {
        if(shift[d] < width[d] * .5) {
          center[d] = min[d] + shift[d] + width[d] * .5;
        }
        else {
          center[d] = min[d] + shift[d] - width[d] * .5;
        }
      }
      this.relation = relation;
      ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
      List<Node> children = new ArrayList<>();
      bulkLoad(min.clone(), max.clone(), children, ids, 0, ids.size(), 0, 0, 0);
      this.root = new Node(0, center, ids.size(), -1, children);
    }

    /**
     * Bulk load the tree
     *
     * @param lmin Subtree minimum (unshifted, will be modified)
     * @param lmax Subtree maximum (unshifted, will be modified)
     * @param children List of children for current parent
     * @param ids IDs to process
     * @param start Start of ids subinterval
     * @param end End of ids subinterval
     * @param dim Current dimension
     * @param level Current tree level
     * @param code Bit code of node position
     */
    private void bulkLoad(double[] lmin, double[] lmax, List<Node> children, ArrayModifiableDBIDs ids, int start, int end, int dim, int level, int code) {
      // logger.warning(FormatUtil.format(lmin)+" "+FormatUtil.format(lmax)+"
      // "+start+"->"+end+" "+(end-start));
      // Hack: Check degenerate cases that won't split
      if(dim == 0) {
        DBIDArrayIter iter = ids.iter();
        iter.seek(start);
        NumberVector first = relation.get(iter);
        iter.advance();
        boolean degenerate = true;
        loop: for(; iter.getOffset() < end; iter.advance()) {
          NumberVector other = relation.get(iter);
          for(int d = 0; d < lmin.length; d++) {
            if(Math.abs(first.doubleValue(d) - other.doubleValue(d)) > 1E-15) {
              degenerate = false;
              break loop;
            }
          }
        }
        if(degenerate) {
          double[] center = new double[lmin.length];
          for(int d = 0; d < lmin.length; d++) {
            center[d] = lmin[d] * .5 + lmax[d] * .5 + shift[d];
            if(center[d] > min[d] + width[d]) {
              center[d] -= width[d];
            }
          }
          children.add(new Node(code, center, end - start, level, null));
          return;
        }
      }
      // Complete level
      if(dim == lmin.length) {
        double[] center = new double[lmin.length];
        for(int d = 0; d < lmin.length; d++) {
          center[d] = lmin[d] * .5 + lmax[d] * .5 + shift[d];
          if(center[d] > min[d] + width[d]) {
            center[d] -= width[d];
          }
        }
        if(end - start < nmin) {
          children.add(new Node(code, center, end - start, level, null));
          return;
        }
        else {
          List<Node> newchildren = new ArrayList<>();
          bulkLoad(lmin, lmax, newchildren, ids, start, end, 0, level + 1, 0);
          children.add(new Node(code, center, end - start, level, newchildren));
          return;
        }
      }
      else {
        // Partially sort data, by dimension dim < mid
        DBIDArrayIter siter = ids.iter(), eiter = ids.iter();
        siter.seek(start);
        eiter.seek(end - 1);
        while(siter.getOffset() < eiter.getOffset()) {
          if(getShiftedDim(relation.get(siter), dim, level) <= .5) {
            siter.advance();
            continue;
          }
          if(getShiftedDim(relation.get(eiter), dim, level) > 0.5) {
            eiter.retract();
            continue;
          }
          ids.swap(siter.getOffset(), eiter.getOffset() - 1);
          siter.advance();
          eiter.retract();
        }
        final int spos = siter.getOffset();
        if(start < spos) {
          final double tmp = lmax[dim];
          lmax[dim] = lmax[dim] * .5 + lmin[dim] * .5;
          bulkLoad(lmin, lmax, children, ids, start, spos, dim + 1, level, code);
          lmax[dim] = tmp; // Restore
        }
        if(spos < end) {
          final double tmp = lmin[dim];
          lmin[dim] = lmax[dim] * .5 + lmin[dim] * .5;
          bulkLoad(lmin, lmax, children, ids, spos, end, dim + 1, level, code | (1 << dim));
          lmin[dim] = tmp; // Restore
        }
      }
    }

    /**
     * Shift and wrap a single dimension.
     *
     * @param obj Object
     * @param dim Dimension
     * @param level Level (controls scaling/wrapping!)
     * @return Shifted position
     */
    private double getShiftedDim(NumberVector obj, int dim, int level) {
      double pos = obj.doubleValue(dim) + shift[dim];
      pos = (pos - min[dim]) / width[dim] * (1 + level);
      return pos - FastMath.floor(pos);
    }

    /**
     * Find the closest node (of depth {@code tlevel} or above, if there is no
     * node at this depth) for the given vector.
     *
     * @param vec Query vector
     * @param tlevel Target level
     * @return Node
     */
    public Node findClosestNode(NumberVector vec, int tlevel) {
      Node cur = root;
      for(int level = 0; level <= tlevel; level++) {
        if(cur.children == null) {
          break;
        }
        int code = 0;
        for(int d = 0; d < min.length; d++) {
          if(getShiftedDim(vec, d, level) > .5) {
            code |= 1 << d;
          }
        }
        boolean found = false;
        for(Node child : cur.children) {
          if(child.code == code) {
            cur = child;
            found = true;
            break;
          }
        }
        if(!found) {
          break; // Do not descend
        }
      }
      return cur;
    }
  }

  /**
   * Node of the ALOCI Quadtree
   *
   * @author Erich Schubert
   */
  static class Node implements NumberVector {
    /**
     * Position code
     */
    final int code;

    /**
     * Number of elements
     */
    final int count;

    /**
     * Level of node
     */
    final int level;

    /**
     * Child nodes, may be null
     */
    List<Node> children;

    /**
     * Parent node
     */
    Node parent = null;

    /**
     * Center vector
     */
    double[] center;

    /**
     * Constructor.
     *
     * @param code Node code
     * @param center Center vector
     * @param count Element count
     * @param level Node level
     * @param children Children list
     */
    protected Node(int code, double[] center, int count, int level, List<Node> children) {
      this.code = code;
      this.center = center;
      this.count = count;
      this.level = level;
      this.children = children;
      if(children != null) {
        for(Node child : children) {
          child.parent = this;
        }
      }
    }

    /**
     * Get level of node.
     *
     * @return Level of node
     */
    public int getLevel() {
      return level;
    }

    /**
     * Get count of subtree
     *
     * @return subtree count
     */
    public int getCount() {
      return count;
    }

    /**
     * Get sum of squares, recursively
     *
     * @param levels Depth to collect
     * @return Sum of squares
     */
    public long getSquareSum(int levels) {
      if(levels <= 0 || children == null) {
        return ((long) count) * ((long) count);
      }
      long agg = 0;
      for(Node child : children) {
        agg += child.getSquareSum(levels - 1);
      }
      return agg;
    }

    /**
     * Get cubic sum.
     *
     * @param levels Level to collect
     * @return sum of cubes
     */
    public long getCubicSum(int levels) {
      if(levels <= 0 || children == null) {
        return ((long) count) * ((long) count) * ((long) count);
      }
      long agg = 0;
      for(Node child : children) {
        agg += child.getCubicSum(levels - 1);
      }
      return agg;
    }

    @Override
    public int getDimensionality() {
      return center.length;
    }

    @Override
    public double doubleValue(int dimension) {
      return center[dimension];
    }

    @Override
    public long longValue(int dimension) {
      return (long) center[dimension];
    }

    @Override
    public double[] toArray() {
      return center.clone();
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the minimum neighborhood size
     */
    public static final OptionID NMIN_ID = new OptionID("loci.nmin", "Minimum neighborhood size to be considered.");

    /**
     * Parameter to specify the averaging neighborhood scaling.
     */
    public static final OptionID ALPHA_ID = new OptionID("loci.alpha", "Scaling factor for averaging neighborhood");

    /**
     * Parameter to specify the number of Grids to use.
     */
    public static final OptionID GRIDS_ID = new OptionID("loci.g", "The number of Grids to use.");

    /**
     * Parameter to specify the seed to initialize Random.
     */
    public static final OptionID SEED_ID = new OptionID("loci.seed", "The seed to use for initializing Random.");

    /**
     * Neighborhood minimum size
     */
    protected int nmin = 0;

    /**
     * Alpha: number of levels difference to use in comparison
     */
    protected int alpha = 4;

    /**
     * G: number of shifted trees to create.
     */
    protected int g = 1;

    /**
     * Random generator
     */
    protected RandomFactory rnd;

    /**
     * The distance function
     */
    private NumberVectorDistanceFunction<?> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<NumberVectorDistanceFunction<?>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, NumberVectorDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      final IntParameter nminP = new IntParameter(NMIN_ID, 20);
      if(config.grab(nminP)) {
        nmin = nminP.getValue();
      }

      final IntParameter g = new IntParameter(GRIDS_ID, 1);
      if(config.grab(g)) {
        this.g = g.getValue();
      }

      final RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        this.rnd = rndP.getValue();
      }

      final IntParameter alphaP = new IntParameter(ALPHA_ID, 4);
      if(config.grab(alphaP)) {
        this.alpha = alphaP.getValue();
        if(this.alpha < 1) {
          this.alpha = 1;
        }
      }
    }

    @Override
    protected ALOCI<O> makeInstance() {
      return new ALOCI<>(distanceFunction, nmin, alpha, g, rnd);
    }
  }
}
