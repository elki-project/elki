package experimentalcode.students.muellerjo.outlier;

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
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
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
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Fast Outlier Detection in High Dimensional Spaces
 * 
 * Outlier Detection using Hilbert space filling curves
 * 
 * Based on: F. Angiulli, C. Pizzuti: Fast Outlier Detection in High Dimensional
 * Spaces. In: Proc. European Conference on Principles of Knowledge Discovery
 * and Data Mining (PKDD'02), Helsinki, Finland, 2002.
 * 
 * @author Jonathan von Brünken
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
    this.k = k;
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
    }
    final double distfactor = Math.pow(.5 / diameter, d);
    logger.warning("Rescaling dataset by " + (.5 / diameter));
    logger.warning("Distances should be off by " + distfactor);

    // Initialization part
    capital_n_star = capital_n = relation.size();
    HilbertFeatures h = new HilbertFeatures(relation, capital_n, min, diameter);

    FiniteProgress progressHilOut = logger.isVerbose() ? new FiniteProgress("HilOut scores", relation.size(), logger) : null;
    // Main part: 1. Phase max. d+1 loops
    for(int j = 0; j <= d && n_star < n; j++) {
      // initialize (clear) out and wlb - not 100% clear in the paper
      h.out.clear();
      h.wlb.clear();
      final double v = .5 * j / (d + 1);
      // Initialize Hilbert values in pf according to current shift
      h.initialize(v);
      // scan the Data according to the current shift; build out and wlb
      scan(h, (int) (k * ((double) capital_n / (double) capital_n_star)));
      // determine the true outliers (n_star)
      trueOutliers(h);
      // Build the top Set as out + wlb
      h.top.clear();
      HashSetModifiableDBIDs top_keys = DBIDUtil.newHashSet(h.out.size());
      for(HilFeature entry : h.out) {
        top_keys.add(entry.id);
        h.top.add(entry);
      }
      for(HilFeature entry : h.wlb) {
        if(!top_keys.contains(entry.id)) {
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
    // Return weights in out
    if(tn == Selection.TopN) {
      for(DBID id : relation.iterDBIDs()) {
        hilout_weight.putDouble(id, 0.0);
      }
      for(HilFeature ent : h.out) {
        hilout_weight.putDouble(ent.id, distfactor * ent.ubound);
      }
    }
    // Return all weights in pf
    else {
      for(HilFeature ent : h.pf) {
        hilout_weight.putDouble(ent.id, distfactor * ent.ubound);
      }
    }
    Relation<Double> scoreResult = new MaterializedRelation<Double>("HilOut weight", "hilout-weight", TypeUtil.DOUBLE, hilout_weight, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }

  /**
   * Scan function performs a squential scan over the data.
   * 
   * @param h the hilbert features
   * @param k0
   */
  private void scan(HilbertFeatures h, int k0) {
    for(int i = 0; i < h.pf.length; i++) {
      if(h.pf[i].ubound < omega_star) {
        continue;
      }
      if(h.pf[i].lbound < h.pf[i].ubound) {
        double omega = h.fastUpperBound(i);
        if(omega < omega_star) {
          h.pf[i].ubound = omega;
        }
        else {
          int maxcount;
          // capital_n-1 instead of capital_n: all, except self
          if(h.top.contains(h.pf[i])) {
            maxcount = capital_n - 1;
          }
          else {
            maxcount = java.lang.Math.min(2 * k0, capital_n - 1);
          }
          DoubleDoublePair bounds = innerScan(h, i, maxcount);
          double newlb = bounds.first;
          double newub = bounds.second;
          if(newlb > h.pf[i].lbound) {
            h.pf[i].lbound = newlb;
          }
          if(newub < h.pf[i].ubound) {
            h.pf[i].ubound = newub;
          }
        }
      }
      h.updateOUT(i);
      h.updateWLB(i);
      if(h.wlb.size() >= n) {
        omega_star = java.lang.Math.max(omega_star, h.wlb.peek().lbound);
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
   * 
   * @return DoubleDoublePair containing the new lower and upper bound
   */
  private DoubleDoublePair innerScan(HilbertFeatures hf, int i, int maxcount) {
    int a = i, b = i;
    int level = h, levela = h, levelb = h;
    int count = 0;
    boolean stop = false;
    // Small changes to prevent ArrayOutOfBound Exceptions
    while(count < maxcount && !stop) {
      int c;
      count++;
      if(a > 0 && hf.pf[a - 1].level >= hf.pf[b].level) {
        a--;
        levela = java.lang.Math.min(levela, hf.pf[a].level);
        c = a;
      }
      else if(b < capital_n - 1) {
        levelb = java.lang.Math.min(levelb, hf.pf[b].level);
        b++;
        c = b;
      }
      else {
        a--;
        levela = java.lang.Math.min(levela, hf.pf[a].level);
        c = a;
      }
      if(!hf.pf[i].nn_keys.contains(hf.pf[c].id)) {
        insert(hf, i, hf.pf[c].id, distq.distance(hf.pf[i].id, hf.pf[c].id).doubleValue());
        if(hf.pf[i].nn.size() == k) {
          if(hf.pf[i].sum_nn < omega_star) {
            stop = true;
          }
          else if(java.lang.Math.max(levela, levelb) < level) {
            level = java.lang.Math.max(levela, levelb);
            double delta = hf.minDist(hf.pf[i].id, level); // FIXME: 0 oder v?
            stop = (delta >= hf.pf[i].nn.peek().distance);
          }
        }
      }
    }
    double br = hf.boxRadius(i, (a - 1 < 0) ? 0 : a - 1, (b + 1 > capital_n - 1) ? capital_n - 1 : b + 1);
    double newlb = 0.0;
    double newub = 0.0;
    for(NN entry : hf.pf[i].nn) {
      newub += entry.distance;
      if(entry.distance <= br) {
        newlb += entry.distance;
      }
    }
    return new DoubleDoublePair(newlb, newub);
  }

  /**
   * insert function inserts a nearest neighbor into a features nn list and its
   * distance
   * 
   * @param i index of the feature in pf
   * @param id DBID of the nearest neighbor
   * @param dt distance or the neighbor to the features position
   */
  private void insert(HilbertFeatures h, int i, DBID id, double dt) {
    if(!h.pf[i].nn_keys.contains(id)) {
      if(h.pf[i].nn.size() < k) {
        NN entry = new NN(id, dt);
        h.pf[i].nn.offer(entry);
        h.pf[i].nn_keys.add(id);
        h.pf[i].sum_nn += entry.distance;
      }
      else {
        NN head = h.pf[i].nn.peek();
        if(dt < head.distance) {
          NN entry = new NN(id, dt);
          h.pf[i].nn.offer(entry);
          h.pf[i].nn_keys.remove(head.id);
          h.pf[i].nn_keys.add(id);
          head = h.pf[i].nn.poll();
          h.pf[i].sum_nn -= (head.distance - entry.distance);
        }
      }
    }
  }

  private void trueOutliers(HilbertFeatures h) {
    n_star = 0;
    for(HilFeature entry : h.out) {
      if(entry.lbound >= entry.ubound && entry.ubound >= omega_star) {
        n_star++;
      }
    }
  }

  /**
   * numberOfLeadingZeros wrapper function for the corresponding BitsUtil
   * function
   * 
   * @param in long Array input
   * 
   * @return number of leading zeros and 0 if none
   */
  private static int numberOfLeadingZeros(long[] in) {
    int out = BitsUtil.numberOfLeadingZeros(in);
    return (out == -1) ? 0 : out;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new LPNormDistanceFunction(t).getInputTypeRestriction());
  }

  private class HilLowerComparator implements Comparator<HilFeature> {
    @Override
    public int compare(HilFeature o1, HilFeature o2) {
      return (int) java.lang.Math.signum(o1.lbound - o2.lbound);
    }
  }

  private class HilUpperComparator implements Comparator<HilFeature> {
    @Override
    public int compare(HilFeature o1, HilFeature o2) {
      return (int) java.lang.Math.signum(o1.ubound - o2.ubound);
    }
  }

  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractParameterizer {
    protected int k = 5;

    protected int n = 10;

    protected int h = 32;

    protected double t = 2.0;

    protected Enum<Selection> tn;

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
        t = java.lang.Math.abs(tP.getValue());
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

  final static class NNComparator implements Comparator<NN> {
    @Override
    public int compare(NN o1, NN o2) {
      return (int) java.lang.Math.signum(o1.distance - o2.distance);
    }

  }

  final static class NN {
    public DBID id;

    public double distance;

    public NN(DBID id, double distance) {
      super();
      this.id = id;
      this.distance = distance;
    }
  }

  class HilbertFeatures {
    // todo add "n"
    public HilbertFeatures(Relation<O> relation, int capital_n, double[] min, double diameter) {
      super();
      this.relation = relation;
      this.min = min;
      this.diameter = diameter;
      this.pf = new HilFeature[capital_n];

      final NNComparator distcheck = new NNComparator();
      int pos = 0;
      for(DBID id : relation.iterDBIDs()) {
        pf[pos++] = new HilFeature(id, new Heap<NN>(k + 1, distcheck));
      }
      this.out = new Heap<HilFeature>(n + 1, new HilUpperComparator());
      this.wlb = new Heap<HilFeature>(n + 1, new HilLowerComparator());
      this.top = new HashSet<HilFeature>(2 * n);
    }

    Relation<O> relation;

    HilFeature[] pf;

    double[] min;

    double diameter;

    double shift;

    private Set<HilFeature> top;

    private Heap<HilFeature> out;

    private Heap<HilFeature> wlb;

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
            coord[dim] = (long) (getDimForObject(obj, dim) * scale);
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
            coord[dim] = (int) (getDimForObject(obj, dim) * scale);
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
            coord[dim] = (short) (getDimForObject(obj, dim) * scale);
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
            coord[dim] = (byte) (getDimForObject(obj, dim) * scale);
          }
          pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h, 24);
        }
      }
      java.util.Arrays.sort(pf);
      capital_n_star = 0;
      for(int i = 0; i < pf.length - 1; i++) {
        pf[i].level = minReg(i, i + 1);
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
          out.offer(pf[i]);
          out.poll();
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
          wlb.offer(pf[i]);
          wlb.poll();
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
      return k * maxDist(pf[i].id, minReg(pre, post));
    }

    /**
     * minDist function calculate the minimal Distance from Vector p to the
     * border of the corresponding r-region at the given level
     * 
     * @param p Point as Vector
     * @param level Level of the corresponding r-region
     */
    private double minDist(DBID id, int level) {
      double dist = Double.POSITIVE_INFINITY;
      double r = 1.0 / (double) (1 << level + 1);
      O obj = relation.get(id);
      for(int dim = 0; dim < d; dim++) {
        final double pd = getDimForObject(obj, dim);
        double p_m_r = pd - java.lang.Math.floor(pd / r) * r;
        dist = java.lang.Math.min(dist, java.lang.Math.min(p_m_r, r - p_m_r));
      }
      return dist;
    }

    /**
     * maxDist function calculate the maximal Distance from Vector p to the
     * border of the corresponding r-region at the given level
     * 
     * @param p Point as Vector
     * @param level Level of the corresponding r-region
     */
    private double maxDist(DBID id, int level) {
      O obj = relation.get(id);
      double dist;
      double r = 1.0 / (double) (1 << level + 1);
      if(t == 1.0) {
        dist = 0.0;
        for(int dim = 0; dim < d; dim++) {
          final double pd = getDimForObject(obj, dim);
          double p_m_r = pd - java.lang.Math.floor(pd / r) * r;
          dist += java.lang.Math.max(p_m_r, r - p_m_r);
        }
      }
      else if(Double.isInfinite(t)) {
        dist = Double.NEGATIVE_INFINITY;
        for(int dim = 0; dim < d; dim++) {
          final double pd = getDimForObject(obj, dim);
          double p_m_r = pd - java.lang.Math.floor(pd / r) * r;
          dist = java.lang.Math.max(dist, java.lang.Math.max(p_m_r, r - p_m_r));
        }
      }
      else {
        dist = 0.0;
        for(int dim = 0; dim < d; dim++) {
          final double pd = getDimForObject(obj, dim);
          double p_m_r = pd - java.lang.Math.floor(pd / r) * r;
          dist += java.lang.Math.pow(java.lang.Math.max(p_m_r, r - p_m_r), t);
        }
        dist = java.lang.Math.pow(dist, 1.0 / t);
      }
      return dist;
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
    private int minReg(int a, int b) {
      long[] pf_a = BitsUtil.copy(pf[a].hilbert);
      BitsUtil.xorI(pf_a, pf[b].hilbert);
      return (-1 + (numberOfLeadingZeros(pf_a) / d));
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
      long[] hil1 = BitsUtil.copy(pf[a].hilbert);
      long[] hil2 = BitsUtil.copy(pf[b].hilbert);
      BitsUtil.xorI(hil1, pf[i].hilbert);
      BitsUtil.xorI(hil2, pf[i].hilbert);
      int level = java.lang.Math.max(numberOfLeadingZeros(hil1), numberOfLeadingZeros(hil2)) / d;
      return minDist(pf[i].id, level);
    }

    /**
     * Get the (projected) position of the object in dimension dim.
     * 
     * @param obj Object
     * @param dim Dimension
     * @return Projected and shifted position
     */
    private double getDimForObject(O obj, int dim) {
      return (obj.doubleValue(dim + 1) - min[dim]) / (2 * diameter) + shift;
    }
  }

  final static class HilFeature implements Comparable<HilFeature> {
    public DBID id;

    public long[] hilbert = null;

    public int level = 0;

    public double ubound = Double.POSITIVE_INFINITY;

    public double lbound = 0.0;

    public Heap<NN> nn;

    public HashSetModifiableDBIDs nn_keys = DBIDUtil.newHashSet();

    public double sum_nn = 0.0;

    public HilFeature(DBID id, Heap<NN> nn) {
      super();
      this.id = id;
      this.nn = nn;
    }

    @Override
    public int compareTo(HilFeature o) {
      return BitsUtil.compare(this.hilbert, o.hilbert);
    }
  }
}