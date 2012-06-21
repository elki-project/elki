package experimentalcode.students.muellerjo.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.students.muellerjo.index.ALOCIQuadTree;
import experimentalcode.students.muellerjo.index.AbstractALOCIQuadTreeNode;

/**
 * Fast Outlier Detection Using the "approximate Local Correlation Integral".
 * 
 * Outlier detection using multiple epsilon neighborhoods.
 * 
 * Reference:
 * <p>
 * S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:<br />
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral.<br />
 * In: Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore,
 * India, 2003.
 * </p>
 * 
 * @author Jonathan von Bruenken
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDE.2003.1260802")
public class ALOCI<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ALOCI.class);

  /**
   * Holds the value of {@link #NMIN_ID}.
   */
  private int nmin;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private int alpha;

  /**
   * Holds the value of {@link #LUN_ID}.
   */
  private int levelUnderNmin;

  /**
   * Holds the value of {@link #GRIDS_ID}.
   */
  private int g;

  /**
   * Holds the value of {@link #GRIDS_ID}.
   */
  private Random random;

  private NumberVectorDistanceFunction<D> distFunc;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param rmax Maximum radius
   * @param nmin Minimum neighborhood size
   * @param alpha Alpha value
   * @param g Number of grids to use
   * @param lun extra levels
   * @param seed Random generator seed.
   */
  public ALOCI(NumberVectorDistanceFunction<D> distanceFunction, int nmin, int alpha, int g, int lun, Long seed) {
    super();
    this.distFunc = distanceFunction;
    this.nmin = nmin;
    this.alpha = alpha;
    this.g = g;
    this.levelUnderNmin = lun;
    this.random = (seed != null) ? new Random(seed) : new Random(0);
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    final int dim = DatabaseUtil.dimensionality(relation);
    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("Build aLOCI quadtress", g, logger) : null;

    // Compute extend of dataset.
    double[] min, max;
    {
      Pair<O, O> hbbs = DatabaseUtil.computeMinMax(relation);
      double maxd = 0;
      min = new double[dim];
      max = new double[dim];
      for(int i = 0; i < dim; i++) {
        min[i] = hbbs.first.doubleValue(i + 1);
        max[i] = hbbs.second.doubleValue(i + 1);
        maxd = Math.max(maxd, max[i] - min[i]);
      }
      // Enlarge bounding box to have equal lengths.
      for(int i = 0; i < dim; i++) {
        double diff = (maxd - (max[i] - min[i])) / 2;
        min[i] -= diff;
        max[i] += diff;
      }
    }

    List<ALOCIQuadTree<O>> qts = new ArrayList<ALOCIQuadTree<O>>(g);

    double[] nshift = new double[dim];
    ALOCIQuadTree<O> qt = new ALOCIQuadTree<O>(0, min, max, nshift, nmin, levelUnderNmin, relation);
    qts.add(qt);
    if(progressPreproc != null) {
      progressPreproc.incrementProcessed(logger);
    }
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
      qt = new ALOCIQuadTree<O>(shift, min, max, svec, nmin, levelUnderNmin, relation);
      qts.add(qt);
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    if(progressPreproc != null) {
      progressPreproc.ensureCompleted(logger);
    }

    // aLOCI main loop: evaluate
    FiniteProgress progressLOCI = logger.isVerbose() ? new FiniteProgress("Compute aLOCI scores", relation.size(), logger) : null;
    WritableDoubleDataStore mdef_norm = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore mdef_level = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBID id : relation.iterDBIDs()) {
      /*
       * Get the lowest Counting Neighborhood whose center is closest to the
       * Object getBestCountingNode can not be used in this case, as the the
       * lowest level is not necessarily the same across different QuadTrees.
       */
      AbstractALOCIQuadTreeNode cg = null;
      final O obj = relation.get(id);
      for(int i = 0; i < g; i++) {
        AbstractALOCIQuadTreeNode cg2 = qts.get(i).getCountingNode(obj);
        // TODO: statt distFunc evtl. Manhattan direkt - bei Würfeln sollte es
        // keinen unterschied machen, welche LP norm!
        if(cg == null || distFunc.distance(cg.getCenter(), obj).compareTo(distFunc.distance(cg2.getCenter(), obj)) > 0) {
          cg = cg2;
        }
      }
      /*
       * Calculate the MDEF value for the Counting Neighborhood cg and the best
       * matching Sampling Neighborhood alpha levels over cg.
       */
      int level = cg.getLevel() - alpha;

      // find the best Sampling Neighborhood for cg on the right level in qts
      AbstractALOCIQuadTreeNode sn = getBestSamplingNode(qts, cg, (level > 0) ? level : 0);
      double maxmdefnorm = calculate_MDEF_norm(sn, cg);
      double radius = sn.getLevel();
      /*
       * While the Sampling Neighborhood has not reached the root level,
       * calculate MDEF for these levels
       */
      while(level > 0) {
        level--;
        cg = getBestCountingNode(qts, cg.getParent(), obj);
        sn = getBestSamplingNode(qts, cg.getParent(), level);
        double newnorm = calculate_MDEF_norm(sn, cg);
        if(maxmdefnorm < newnorm) {
          maxmdefnorm = newnorm;
          radius = sn.getLevel();
        }
      }
      // Store results
      mdef_norm.putDouble(id, maxmdefnorm);
      mdef_level.putDouble(id, radius);
      minmax.put(maxmdefnorm);
      if(progressLOCI != null) {
        progressLOCI.incrementProcessed(logger);
      }
    }
    if(progressLOCI != null) {
      progressLOCI.ensureCompleted(logger);
    }
    Relation<Double> scoreResult = new MaterializedRelation<Double>("aLOCI normalized MDEF", "aloci-mdef-outlier", TypeUtil.DOUBLE, mdef_norm, relation.getDBIDs());
    Relation<Double> levelResult = new MaterializedRelation<Double>("aLOCI Sampling Level", "aloci-sampling-level", TypeUtil.DOUBLE, mdef_level, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(levelResult);
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
  private double calculate_MDEF_norm(AbstractALOCIQuadTreeNode sn, AbstractALOCIQuadTreeNode cg) {
    // get the square sum of the counting neighborhoods box counts
    long sq = sn.getBoxCountSquareSum(alpha);
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
    if(sq == sn.getBucketCount()) {
      return 0.0;
    }
    // calculation of mdef according to the paper and standardization as done in
    // LOCI
    long cb = sn.getBoxCountCubicSum(alpha);
    double n_hat = (double) sq / (double) sn.getBucketCount();
    double sig_n_hat = java.lang.Math.sqrt(cb * sn.getBucketCount() - (sq * sq)) / sn.getBucketCount();
    // Avoid NaN - correct result 0.0?
    if(sig_n_hat < Double.MIN_NORMAL) {
      return 0.0;
    }
    double mdef = n_hat - cg.getBucketCount();
    return mdef / sig_n_hat;
  }

  /**
   * Method for retrieving the best matching Sampling Node
   * 
   * @param qts List of (shifted) QuadTrees
   * @param cg Node containing the counting Neighborhood found to fit best for
   *        the Object
   * @param level Target Level of the Sampling Neighborhood
   */
  private AbstractALOCIQuadTreeNode getBestSamplingNode(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode cg, int level) {
    Vector center = cg.getCenter();
    int qti = cg.getIndex();
    /*
     * get the Sampling Node of cg in the same QuadTree as a base case This
     * choice is usually quite bad, but is always present.
     */
    AbstractALOCIQuadTreeNode sn = cg.getParent();
    while(sn.getLevel() != level) {
      sn = sn.getParent();
    }
    // look through the other QuadTrees and choose the one with the lowest
    // distance to the counting Neighborhoods center.
    for(int i = 0; i < g; i++) {
      if(i == qti) {
        continue;
      }
      // getSamplingNode(O int) returns null if there is no node containing the
      // coordinates given, or if the node does not have at least nmin Elements.
      AbstractALOCIQuadTreeNode sn2 = qts.get(i).getSamplingNode(center, level);
      if(sn2 == null) {
        continue;
      }
      if(distFunc.distance(center, sn.getCenter()).doubleValue() > distFunc.distance(center, sn2.getCenter()).doubleValue()) {
        sn = sn2;
      }
    }
    return sn;
  }

  /**
   * Method for retrieving the best matching counting Node
   * 
   * @param qts List of (shifted) QuadTrees
   * @param cg Node containing a possible counting Neighborhood
   * @param center location of the Object and therefore best possible center of
   *        the counting node
   */
  private AbstractALOCIQuadTreeNode getBestCountingNode(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode cg, O center) {
    AbstractALOCIQuadTreeNode cn = cg;
    int qti = cg.getIndex();
    // compare the other counting nodes at the right level in the QuadTrees and
    // choose the one closest to center.
    for(int i = 0; i < g; i++) {
      if(i == qti) {
        continue;
      }
      // getCountingNode(O, int) returns null if the Object has not reached the
      // right level in the Tree
      AbstractALOCIQuadTreeNode cn2 = qts.get(i).getCountingNode(center, cn.getLevel());
      if(cn2 == null) {
        continue;
      }
      if(distFunc.distance(center, cn.getCenter()).doubleValue() > distFunc.distance(center, cn2.getCenter()).doubleValue()) {
        cn = cn2;
      }
    }
    return cn;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distFunc.getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractParameterizer {
    /**
     * Parameter to specify the minimum neighborhood size
     */
    public static final OptionID NMIN_ID = OptionID.getOrCreateOptionID("loci.nmin", "Minimum neighborhood size to be considered.");

    /**
     * Parameter to specify the averaging neighborhood scaling.
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("loci.alpha", "Scaling factor for averaging neighborhood");

    /**
     * Parameter to specify the the number of Levels to add under the sampling
     * neighborhood.
     */
    public static final OptionID LUN_ID = OptionID.getOrCreateOptionID("loci.levelUnderNmin", "Number of Levels to add under the sampling neighborhood");

    /**
     * Parameter to specify the number of Grids to use.
     */
    public static final OptionID GRIDS_ID = OptionID.getOrCreateOptionID("loci.g", "The number of Grids to use.");

    /**
     * Parameter to specify the seed to initialize Random.
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("loci.seed", "The seed to use for initializing Random.");

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
     * Random generator seed
     */
    protected Long seed = null;

    /**
     * Pseudo level to create for neighborhoods
     */
    protected int lun = alpha;

    /**
     * The distance function
     */
    private NumberVectorDistanceFunction<D> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<NumberVectorDistanceFunction<D>> distanceFunctionP = makeParameterDistanceFunction(EuclideanDistanceFunction.class, NumberVectorDistanceFunction.class);
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

      final LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        this.seed = seedP.getValue();
      }

      final IntParameter alphaP = new IntParameter(ALPHA_ID, 4);
      if(config.grab(alphaP)) {
        this.alpha = alphaP.getValue();
        if(this.alpha < 1) {
          this.alpha = 1;
        }
      }

      final IntParameter lunP = new IntParameter(LUN_ID, alpha);
      if(config.grab(lunP)) {
        this.lun = lunP.getValue();
      }
    }

    @Override
    protected ALOCI<O, D> makeInstance() {
      return new ALOCI<O, D>(distanceFunction, nmin, alpha, g, lun, seed);
    }
  }
}