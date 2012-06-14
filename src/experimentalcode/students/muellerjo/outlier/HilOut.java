package experimentalcode.students.muellerjo.outlier;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Fast Outlier Detection in High Dimensional Spaces
 * 
 * Outlier Detection using Hilbert space filling curves
 * 
 * Reference:
 * <p>
 * F. Angiulli, C. Pizzuti:<br />
 * Fast Outlier Detection in High Dimensional Spaces.<br />
 * In: Proc. European Conference on Principles of Knowledge Discovery and Data
 * Mining (PKDD'02), Helsinki, Finland, 2002.
 * </p>
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Title("Fast Outlier Detection in High Dimensional Spaces")
@Description("Algorithm to compute outliers using Hilbert space filling curves")
@Reference(authors = "F. Angiulli, C. Pizzuti", title = "Fast Outlier Detection in High Dimensional Spaces", booktitle = "Proc. European Conference on Principles of Knowledge Discovery and Data Mining (PKDD'02)", url = "http://dx.doi.org/10.1145/375663.375668")
public class HilOut<O extends NumberVector<O, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HilOut.class);

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Holds the value of {@link #N_ID}.
   */
  private int n;

  /**
   * Holds the value of {@link #H_ID}.
   */
  private int h;

  /**
   * Holds the value of {@link #T_ID}.
   */
  private double t;

  /**
   * Holds the value of {@link #TN_ID}.
   */
  private Enum<Selection> tn;

  /**
   * Distance function for HilOut
   */
  private LPNormDistanceFunction distfunc;

  private DistanceQuery<O, DoubleDistance> distq;

  private int capital_n, n_star, capital_n_star, d;

  private double omega_star;

  /**
   * Constructor.
   * 
   * @param k Number of Next Neighbors
   * @param n Number of Outlier
   * @param h Number of Bits for precision to use - max 32
   * @param t p of LP-NormDistance - 1.0-Infinity
   * @param tn TopN or All Outlier Rank to return
   */
  protected HilOut(int k, int n, int h, double t, Enum<Selection> tn) {
    super();
    this.n = n;
    this.k = k - 1; // HilOut does not count the object itself. We do in
                    // KNNWeightOutlier.
    this.h = h;
    this.t = t;
    this.tn = tn;
    // TODO: Make parameterizable with any LP norm, get t from the LP norm
    if(t == 1.0) {
      this.distfunc = ManhattanDistanceFunction.STATIC;
    }
    else if(t == 2.0) {
      this.distfunc = EuclideanDistanceFunction.STATIC;
    }
    else {
      this.distfunc = new LPNormDistanceFunction(t);
    }
    this.n_star = 0;
    this.omega_star = 0.0;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    distq = database.getDistanceQuery(relation, distfunc);
    d = DatabaseUtil.dimensionality(relation);
    WritableDoubleDataStore hilout_weight = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    // Compute extend of dataset.
    double[] min;
    double diameter = 0; // Actually "length of edge"
    {
      Pair<O, O> hbbs = DatabaseUtil.computeMinMax(relation);
      min = new double[d];
      double[] max = new double[d];
      for(int i = 0; i < d; i++) {
        min[i] = hbbs.first.doubleValue(i + 1);
        max[i] = hbbs.second.doubleValue(i + 1);
        diameter = Math.max(diameter, max[i] - min[i]);
      }
      // Enlarge bounding box to have equal lengths.
      for(int i = 0; i < d; i++) {
        double diff = (diameter - (max[i] - min[i])) / 2;
        min[i] -= diff;
        max[i] += diff;
      }
      if (logger.isVerbose()) {
        logger.verbose("Rescaling dataset by " + (1 / diameter)+" to fit the unit cube.");
      }
    }

    // Initialization part
    capital_n_star = capital_n = relation.size();
    HilbertFeatures h = new HilbertFeatures(relation, capital_n, min, diameter);

    FiniteProgress progressHilOut = logger.isVerbose() ? new FiniteProgress("HilOut scores", relation.size(), logger) : null;
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
      logger.verbose("True outliers found: " + n_star);
      // Build the top Set as out + wlb
      h.top.clear();
      HashSetModifiableDBIDs top_keys = DBIDUtil.newHashSet(h.out.size());
      for(HilFeature entry : h.out) {
        top_keys.add(entry.id);
        h.top.add(entry);
      }
      for(HilFeature entry : h.wlb) {
        if(!top_keys.contains(entry.id)) {
          // No need to update top_keys - discarded
          h.top.add(entry);
        }
      }
      if(progressHilOut != null) {
        progressHilOut.incrementProcessed(logger);
      }
    }
    // 2. Phase: Additional Scan if less than n true outliers determined
    if(n_star < n) {
      h.out.clear();
      h.wlb.clear();
      // TODO: reinitialize shift to 0?
      scan(h, capital_n);
    }
    if(progressHilOut != null) {
      progressHilOut.ensureCompleted(logger);
    }
    DoubleMinMax minmax = new DoubleMinMax();
    // Return weights in out
    if(tn == Selection.TopN) {
      minmax.put(0.0);
      for(DBID id : relation.iterDBIDs()) {
        hilout_weight.putDouble(id, 0.0);
      }
      for(HilFeature ent : h.out) {
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
    Relation<Double> scoreResult = new MaterializedRelation<Double>("HilOut weight", "hilout-weight", TypeUtil.DOUBLE, hilout_weight, relation.getDBIDs());
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
    if (logger.isDebuggingFine()) {
      logger.debugFine("Scanning with k0=" + k0 + " (" + mink0 + ")" + " N*=" + capital_n_star);
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
        hf.pf[i].insert(hf.pf[c].id, distq.distance(p, hf.pf[c].id).doubleValue(), k);
        if(hf.pf[i].nn.size() == k) {
          if(hf.pf[i].sum_nn < omega_star) {
            break; // stop = true
          }
          final int mlevel = Math.max(levela, levelb);
          if(mlevel < level) {
            level = mlevel;
            final double delta = hf.minDistLevel(hf.pf[i].id, level);
            if(delta >= hf.pf[i].nn.peek().getDoubleDistance()) {
              break; // stop = true
            }
          }
        }
      }
    }
    double br = hf.boxRadius(i, a - 1, b + 1);
    double newlb = 0.0;
    double newub = 0.0;
    for(DoubleDistanceResultPair entry : hf.pf[i].nn) {
      newub += entry.getDoubleDistance();
      if(entry.getDoubleDistance() <= br) {
        newlb += entry.getDoubleDistance();
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
    for(HilFeature entry : h.out) {
      if(entry.ubound >= omega_star && (entry.ubound - entry.lbound < 1E-10)) {
        n_star++;
      }
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new LPNormDistanceFunction(t).getInputTypeRestriction());
  }

  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractParameterizer {
    protected int k = 5;

    protected int n = 10;

    protected int h = 32;

    protected double t = 2.0;

    protected Enum<Selection> tn;

    /**
     * Parameter to specify how many next neighbors should be used in the
     * computation
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("HilOut.k", "Compute up to k next neighbors");

    /**
     * Parameter to specify how many outliers should be computed
     */
    public static final OptionID N_ID = OptionID.getOrCreateOptionID("HilOut.n", "Compute n outliers");

    /**
     * Parameter to specify the maximum Hilbert-Level
     */
    public static final OptionID H_ID = OptionID.getOrCreateOptionID("HilOut.h", "Max. Hilbert-Level");

    /**
     * Parameter to specify p of LP-NormDistance
     */
    public static final OptionID T_ID = OptionID.getOrCreateOptionID("HilOut.t", "t of Lt Metric");

    /**
     * Parameter to specify if only the Top n, or also approximations for the
     * other elements, should be returned
     */
    public static final OptionID TN_ID = OptionID.getOrCreateOptionID("HilOut.tn", "output of Top n or all elements");

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

      final DoubleParameter tP = new DoubleParameter(T_ID, 2.0);
      if(config.grab(tP)) {
        t = Math.abs(tP.getValue());
        t = (t >= 1.0) ? t : 1.0;
      }

      final EnumParameter<Selection> tnP = new EnumParameter<Selection>(TN_ID, Selection.class, Selection.TopN);
      if(config.grab(tnP)) {
        tn = tnP.getValue();
      }
    }

    @Override
    protected HilOut<O> makeInstance() {
      return new HilOut<O>(k, n, h, t, tn);
    }
  }

  public static enum Selection {
    All, TopN
  }

  class HilbertFeatures {
    Relation<O> relation;

    HilFeature[] pf;

    double[] min;

    double diameter;

    double shift;

    private Set<HilFeature> top;

    private Heap<HilFeature> out;

    private Heap<HilFeature> wlb;

    // todo add "n"
    public HilbertFeatures(Relation<O> relation, int capital_n, double[] min, double diameter) {
      super();
      this.relation = relation;
      this.min = min;
      this.diameter = diameter;
      this.pf = new HilFeature[capital_n];

      int pos = 0;
      for(DBID id : relation.iterDBIDs()) {
        pf[pos++] = new HilFeature(id, new Heap<DoubleDistanceResultPair>(k, Collections.reverseOrder()));
      }
      this.out = new Heap<HilFeature>(n, new Comparator<HilFeature>() {
        @Override
        public int compare(HilFeature o1, HilFeature o2) {
          return Double.compare(o1.ubound, o2.ubound);
        }
      });
      this.wlb = new Heap<HilFeature>(n, new Comparator<HilFeature>() {
        @Override
        public int compare(HilFeature o1, HilFeature o2) {
          return Double.compare(o1.lbound, o2.lbound);
        }
      });
      this.top = new HashSet<HilFeature>(2 * n);
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
      // with
      // the rescaling. 63 bit should be fine. The sign bit probably needs to be
      // handled differently, or at least needs careful unit testing of the API
      if(h >= 32) { // 32 to 63 bit
        final long scale = Long.MAX_VALUE; // = 63 bits
        for(int i = 0; i < pf.length; i++) {
          O obj = relation.get(pf[i].id);
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
          O obj = relation.get(pf[i].id);
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
          O obj = relation.get(pf[i].id);
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
          O obj = relation.get(pf[i].id);
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
        out.offer(pf[i]);
      }
      else {
        HilFeature head = out.peek();
        if(pf[i].ubound > head.ubound) {
          // replace smallest
          out.poll();
          // assert (out.peek().ubound >= head.ubound);
          out.offer(pf[i]);
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
        wlb.offer(pf[i]);
      }
      else {
        HilFeature head = wlb.peek();
        if(pf[i].lbound > head.lbound) {
          // replace smallest
          wlb.poll();
          // assert (wlb.peek().lbound >= head.lbound);
          wlb.offer(pf[i]);
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
     * @param p Point as Vector
     * @param level Level of the corresponding r-region
     */
    private double minDistLevel(DBID id, int level) {
      final O obj = relation.get(id);
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
     * @param p Point as Vector
     * @param level Level of the corresponding r-region
     */
    private double maxDistLevel(DBID id, int level) {
      final O obj = relation.get(id);
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
        dist = Math.sqrt(dist);
      }
      else if(!Double.isInfinite(t)) {
        dist = 0.0;
        for(int dim = 0; dim < d; dim++) {
          final double p_m_r = getDimForObject(obj, dim) % r;
          dist += Math.pow(Math.max(p_m_r, r - p_m_r), t);
        }
        dist = Math.pow(dist, 1.0 / t);
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
     * @return
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
    private double getDimForObject(O obj, int dim) {
      return (obj.doubleValue(dim + 1) - min[dim]) / diameter + shift;
    }
  }

  final static class HilFeature implements Comparable<HilFeature> {
    public DBID id;

    public long[] hilbert = null;

    public int level = 0;

    public double ubound = Double.POSITIVE_INFINITY;

    public double lbound = 0.0;

    public Heap<DoubleDistanceResultPair> nn;

    public HashSetModifiableDBIDs nn_keys = DBIDUtil.newHashSet();

    public double sum_nn = 0.0;

    public HilFeature(DBID id, Heap<DoubleDistanceResultPair> nn) {
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
     * @param i index of the feature in pf
     * @param id DBID of the nearest neighbor
     * @param dt distance or the neighbor to the features position
     */
    protected void insert(DBID id, double dt, int k) {
      // assert (!nn_keys.contains(id));
      if(nn.size() < k) {
        DoubleDistanceResultPair entry = new DoubleDistanceResultPair(dt, id);
        nn.offer(entry);
        nn_keys.add(id);
        sum_nn += dt;
      }
      else {
        DoubleDistanceResultPair head = nn.peek();
        if(dt < head.getDoubleDistance()) {
          head = nn.poll(); // Remove worst
          sum_nn -= head.getDoubleDistance();
          nn_keys.remove(head.getDBID());

          // assert (nn.peek().getDoubleDistance() <= head.getDoubleDistance());

          DoubleDistanceResultPair entry = new DoubleDistanceResultPair(dt, id);
          nn.offer(entry);
          nn_keys.add(id);
          sum_nn += dt;
        }
      }

    }
  }
}