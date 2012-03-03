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

//import experimentalcode.tobias._index.spaceIndex.QuadTree;
//import experimentalcode.tobias._index.spaceIndex.QuadTreeNode;

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
 * @apiviz.has RangeQuery
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
    
    List<ALOCIQuadTree<O>> qts = new ArrayList<ALOCIQuadTree<O>>(g);
    ALOCIQuadTree<O> qt = new ALOCIQuadTree<O>(relation, nmin, 0);
    for(DBID id : relation.iterDBIDs()) {
      qt.insert(relation.get(id));
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    qt.addLevel(alpha);
    qts.add(qt);
    
    Pair<O,O> hbbb = qt.getMinMax();
    int dim = DatabaseUtil.dimensionality(relation);
    double[] shiftVec = new double[dim];
    int shift = 1;
    while(g > shift){
      int mult = 0;
      if ((shift % dim) == (dim-1)){
        mult++;
      }
      for(int i= 0; i < dim; i++){ 
         shiftVec[i] = (((shift % dim) == i)? -1 : 1)*(hbbb.second.doubleValue(i+1) - hbbb.first.doubleValue(i+1)) / (1 << (alpha-mult));
      }
      O factory = DatabaseUtil.assumeVectorField(relation).getFactory();
      
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
      shift++;
      qts.add(qt);
    }
    if(progressPreproc != null) {
      progressPreproc.ensureCompleted(logger);
    }
    FiniteProgress progressLOCI = logger.isVerbose() ? new FiniteProgress("LOCI scores", relation.size(), logger) : null;
    WritableDoubleDataStore mdef_norm = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore mdef_level = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();

    for(DBID id : relation.iterDBIDs()) {
      AbstractALOCIQuadTreeNode<O> cg = qts.get(0).getCountingGrid(relation.get(id));
      for (int i = 1; i < g; i++){
        AbstractALOCIQuadTreeNode<O> cg2 = qts.get(i).getCountingGrid(relation.get(id));
        if (distFunc.distance(cg.getCenter(), relation.get(id)).doubleValue() > distFunc.distance(cg2.getCenter(), relation.get(id)).doubleValue()){
          cg = cg2;
        }
      }
      int level = cg.getLevel() - alpha;
      DoubleIntPair res =  calculate_MDEF_norm(qts, cg, level);
      double maxmdefnorm = res.first;
      double radius = res.second;
      while(level > 0){
        level--;
        cg = getBestCountingNode(qts, cg, relation.get(id), cg.getLevel()-1);
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
    Relation<Double> scoreResult = new MaterializedRelation<Double>("aLOCI normalized MDEF", "loci-mdef-outlier", TypeUtil.DOUBLE, mdef_norm, relation.getDBIDs());
    Relation<Double> levelResult = new MaterializedRelation<Double>("aLOCI Sampling Level radius", "aLOCI-sampling-radius", TypeUtil.DOUBLE, mdef_level, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(levelResult);
    return result;
  }
  
  private DoubleIntPair calculate_MDEF_norm(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode<O> cg, int level){
    AbstractALOCIQuadTreeNode<O> sn = getBestSamplingNode(qts, cg, level);
    if (sn == null){
      sn = cg.getParent();
      for(int i=1; i < alpha; i++){
        sn = sn.getParent();
      }
    }
    long sq = sn.getBoxCountSquareSum(alpha, qts.get(sn.getQTIndex()));
    double mdef_norm;
    if (sq == sn.getBucketCount()){
      mdef_norm = 0.0;
    }
    else{
      long cb = sn.getBoxCountCubicSum(alpha, qts.get(sn.getQTIndex()));
      double n_hat = new Double(sq) / new Double(sn.getBucketCount());
      double sig_n_hat = java.lang.Math.sqrt(cb*sn.getBucketCount() - (sq*sq))/sn.getBucketCount();
      double mdef = n_hat - cg.getBucketCount();
      mdef_norm = mdef / sig_n_hat;
    }
    return new DoubleIntPair (mdef_norm, sn.getLevel());
  }
  
  
  private AbstractALOCIQuadTreeNode<O> getBestSamplingNode(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode<O> cg, int level){
    int qti = 0;
    O center = cg.getCenter();
    AbstractALOCIQuadTreeNode<O> sn = cg.getParent();
    while(sn.getLevel() != level){
      sn = sn.getParent();
    }
    for (int i = 0; i < g; i++){
      if (i == qti)
        continue;
      AbstractALOCIQuadTreeNode<O> sn2 = qts.get(i).getSamplingNode(center, level);
      if (sn2 == null)
        continue;
      if (distFunc.distance(center, sn.getCenter()).doubleValue() > distFunc.distance(center, sn2.getCenter()).doubleValue()){
        sn = sn2;
      }
    }
    return sn;
  }
  
  private AbstractALOCIQuadTreeNode<O> getBestCountingNode(List<ALOCIQuadTree<O>> qts, AbstractALOCIQuadTreeNode<O> cg, O center, int level){
    int qti = 0;
    AbstractALOCIQuadTreeNode<O> cn = cg.getParent();
    while(cn.getLevel() != level){
      cn = cn.getParent();
    }
    qti = cn.getQTIndex();
    for (int i = 0; i < g; i++){
      if (i == qti)
        continue;
      AbstractALOCIQuadTreeNode<O> cn2 = qts.get(i).getCountingNode(center, level);
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