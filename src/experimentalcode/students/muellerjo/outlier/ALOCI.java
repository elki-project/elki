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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.students.muellerjo.index.ALOCIQuadTree;
import experimentalcode.students.muellerjo.index.AbstractALOCIQuadTreeNode;


/**
 * Fast Outlier Detection Using the "approximate Local Correlation Integral".
 * 
 * Outlier detection using multiple epsilon neighborhoods.
 * 
 * Based on: S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral. In: Proc.
 * 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003.
 * 
 * @author Jonathan von Bruenken
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDE.2003.1260802")
public class ALOCI<O  extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ALOCI.class);

  /**
   * Parameter to specify the minimum neighborhood size
   */
  public static final OptionID NMIN_ID = OptionID.getOrCreateOptionID("loci.nmin", "Minimum neighborhood size to be considered.");

  /**
   * Parameter to specify the averaging neighborhood scaling.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("loci.alpha", "Scaling factor for averaging neighborhood");

  /**
   * Parameter to specify the number of Grids to use.
   */
  public static final OptionID GRIDS_ID = OptionID.getOrCreateOptionID("loci.g", "The number of Grids to use.");

  /**
   * Holds the value of {@link #NMIN_ID}.
   */
  private int nmin;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private int alpha;
  
  /**
   * 
   */
  private int g;
  
  private DistanceQuery<O, D> distFunc;
  
  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param rmax Maximum radius
   * @param nmin Minimum neighborhood size
   * @param alpha Alpha value
   * @param g Number of grids to use
   */
  public ALOCI(DistanceFunction<? super O, D> distanceFunction, int nmin, int alpha, int g) {
    super(distanceFunction);
    this.nmin = nmin;
    this.alpha = alpha;
    this.g = g;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  public OutlierResult run(Database database) throws IllegalStateException {    
    Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("aLOCI preprocessing", relation.size(), logger) : null;
    /* Initialization part
     * Generate a list for the g QuadTrees and insert the first unshifted Tree.
     */
    List<ALOCIQuadTree<O>> qts = new ArrayList<ALOCIQuadTree<O>>(g);
    ALOCIQuadTree<O> qt = new ALOCIQuadTree<O>(relation, nmin, 0);
    for(DBID id : relation.iterDBIDs()) {
      qt.insert(relation.get(id));
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    /* Add alpha levels to the QuadTree (Sampling Neighborhood holds atLeast nmin items. Therefore additional alpha-1 levels are needed to reach the lowest Counting Neighborhood)
     * The addLevel method adds these alpha-1 levels.
     */
    qt.addLevel(alpha);
    qts.add(qt);
    // Get the scale of Data from the generated QuadTree
    Pair<O,O> hbbs = qt.getMinMax();
    int dim = DatabaseUtil.dimensionality(relation);
    double[] shiftVec = new double[dim];
    int shift = 0;
    int mult = -1;
    /* create the remaining g-1 shifted QuadTrees.
     * This not clearly described in the paper and therefore implemented in a way that achieves good results with the test data.
     */
    while((g-1) > shift){
      // Every time d (# of Dimensions of the data) QuadTrees where added the length of the shift vector is halved.
      if ((shift % dim) == 0){
        mult++;
      }
      for(int i=0; i < dim; i++){ 
         /* shift vector is calculated
          * Starting with a shift on the main diagonal of the data, the next d-1 shifts replace the lowest dimension not yet replaced with zero.
          * i.e. : 3 Dimensions - Shifts along: (1,1,1)'; (0,1,1)'; (0,0,1)'
          * These shifts are multiplied with the length of the data main diagonal and divided by 2^(alpha+mult)
          */
         shiftVec[i] = (((shift % dim) <= i)? 1.0 : 0.0)*(hbbs.second.doubleValue(i+1) - hbbs.first.doubleValue(i+1)) / (1 << (alpha+mult));
      }
      O factory = DatabaseUtil.assumeVectorField(relation).getFactory();
      shift++;
      qt = new ALOCIQuadTree<O>(relation, nmin, factory.newNumberVector(shiftVec), shift);
      {
        for(DBID id : relation.iterDBIDs()) {
          qt.insert(relation.get(id));
          if(progressPreproc != null) {
            progressPreproc.incrementProcessed(logger);
          }
        }
        qt.addLevel(alpha);
      }
      qts.add(qt);
    }
    if(progressPreproc != null) {
      progressPreproc.ensureCompleted(logger);
    }
    /*
     * aLoci Main Part
     */
    
    FiniteProgress progressLOCI = logger.isVerbose() ? new FiniteProgress("LOCI scores", relation.size(), logger) : null;
    WritableDoubleDataStore mdef_norm = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore mdef_level = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();
    
    for(DBID id : relation.iterDBIDs()) {
      /* Get the lowest Counting Neighborhood whose center is closest to the Object
       * getBestCountingNode can not be used in this case, as the the lowest level is not necessarily the same across different QuadTrees. 
       */
      AbstractALOCIQuadTreeNode<O> cg = qts.get(0).getCountingGrid(relation.get(id));
      for (int i = 1; i < g; i++){
        AbstractALOCIQuadTreeNode<O> cg2 = qts.get(i).getCountingGrid(relation.get(id));
        if (distFunc.distance(cg.getCenter(), relation.get(id)).doubleValue() > distFunc.distance(cg2.getCenter(), relation.get(id)).doubleValue()){
          cg = cg2;
        }
      }
      /* Calculate the MDEF value for the Counting Neighborhood cg and the best matching Sampling Neighborhood alpha levels over cg. 
       * 
       */
      int level = cg.getLevel() - alpha;
      DoubleIntPair res =  calculate_MDEF_norm(qts, cg, level);
      double maxmdefnorm = res.first;
      double radius = res.second;
      /*
       * While the Sampling Neighborhood has not reached the root level, calculate MDEF for these levels 
       */
      while(level > 0){
        level--;
        cg = getBestCountingNode(qts, cg.getParent(), relation.get(id));
        res =  calculate_MDEF_norm(qts, cg, level);
        if(maxmdefnorm < res.first){
          maxmdefnorm = res.first;
          radius = res.second;
        }
      }
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
   * @param qts List of (shifted) QuadTrees
   * @param cg Node containing the counting Neighborhood found to fit best for the Object
   * @param level Target Level of the Sampling Neighborhood
   */
  
  private DoubleIntPair calculate_MDEF_norm(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode<O> cg, int level){
    // find the best Sampling Neighborhood for cg on the right level in qts 
    AbstractALOCIQuadTreeNode<O> sn = getBestSamplingNode(qts, cg, level);
    // get the square sum of the counting neighborhoods box counts
    long sq = sn.getBoxCountSquareSum(alpha, qts.get(sn.getQTIndex()));
    double mdef_norm;
    /* if the square sum is equal to box count of the sampling Neighborhood then n_hat is equal one, and as
     * cg needs to have at least one Element mdef would get zero or lower than zero.
     * This is the case when all of the counting Neighborhoods contain one or zero Objects.
     * Additionally, the cubic sum, square sum and sampling Neighborhood box count are all equal, which leads
     * to sig_n_hat being zero and thus mdef_norm is either negative infinite or undefined. As the distribution of the Objects
     * seem quite uniform, a mdef_norm value of zero ( = no outlier) is appropriate and circumvents the problem of undefined values.
     */ 
    if (sq == sn.getBucketCount()){
      mdef_norm = 0.0;
    }
    // calculation of mdef according to the paper and standardization as done in LOCI  
    else{
      long cb = sn.getBoxCountCubicSum(alpha, qts.get(sn.getQTIndex()));
      double n_hat = (double)sq / (double)sn.getBucketCount();
      double sig_n_hat = java.lang.Math.sqrt(cb*sn.getBucketCount() - (sq*sq))/sn.getBucketCount();
      double mdef = n_hat - cg.getBucketCount();
      mdef_norm = mdef / sig_n_hat;
    }
    return new DoubleIntPair (mdef_norm, sn.getLevel());
  }
  
  /**
   * Method for retrieving the best matching Sampling Node
   * 
   * @param qts List of (shifted) QuadTrees
   * @param cg Node containing the counting Neighborhood found to fit best for the Object
   * @param level Target Level of the Sampling Neighborhood
   */
  private AbstractALOCIQuadTreeNode<O> getBestSamplingNode(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode<O> cg, int level){
    int qti = cg.getQTIndex();
    O center = cg.getCenter();
    /* get the Sampling Node of cg in the same QuadTree as a base case
     * This choice is usually quite bad, but is always present.  
     */
    AbstractALOCIQuadTreeNode<O> sn = cg.getParent();
    while(sn.getLevel() != level){
      sn = sn.getParent();
    }
    // look through the other QuadTrees and choose the one with the lowest distance to the counting Neighborhoods center.
    for (int i = 0; i < g; i++){
      if (i == qti)
        continue;
      // getSamplingNode(O int) returns null if there is no node containing the coordinates given, or if the node does not have at least nmin Elements.
      AbstractALOCIQuadTreeNode<O> sn2 = qts.get(i).getSamplingNode(center, level);
      if (sn2 == null)
        continue;
      if (distFunc.distance(center, sn.getCenter()).doubleValue() > distFunc.distance(center, sn2.getCenter()).doubleValue()){
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
   * @param center location of the Object and therefore best possible center of the counting node
   */
  private AbstractALOCIQuadTreeNode<O> getBestCountingNode(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode<O> cg, O center){
    AbstractALOCIQuadTreeNode<O> cn = cg;
    int qti = cn.getQTIndex();
    // compare the other counting nodes at the right level in the QuadTrees and choose the one closest to center.  
    for (int i = 0; i < g; i++){
      if (i == qti)
        continue;
      // getCountingNode(O, int) returns null if the Object has not reached the right level in the Tree
      AbstractALOCIQuadTreeNode<O> cn2 = qts.get(i).getCountingNode(center, cn.getLevel());
      if (cn2 == null)
        continue;
      if (distFunc.distance(center, cn.getCenter()).doubleValue() > distFunc.distance(center, cn2.getCenter()).doubleValue()){
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
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {

    protected int nmin = 0;

    protected int alpha = 4;
    
    protected int g = 1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter nminP = new IntParameter(NMIN_ID, 20);
      if(config.grab(nminP)) {
        nmin = nminP.getValue();
      }
      
      final IntParameter g = new IntParameter(GRIDS_ID, 1);
      if(config.grab(g)) {
        this.g = g.getValue();
      }

      final IntParameter alphaP = new IntParameter(ALPHA_ID, 4);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
        if (alpha < 1){
          alpha = 1;
        }
      }
    }

    @Override
    protected ALOCI<O, D> makeInstance() {
      return new ALOCI<O, D>(distanceFunction, nmin, alpha, g);
    }
  }
}