package experimentalcode.students.muellerjo.outlier;

import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;


import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.erich.BitsUtil;
import experimentalcode.erich.HilbertSpatialSorter;

/**
 * @author Jonathan von Brünken
 *
 * @param <O> Object type
 */
public class HilOut<O  extends NumberVector<O, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HilOut.class);
  
  /**
   * Parameter to specify how many next neighbors should be used in the computation
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
   * Distance function for HilOut
   */  
  private LPNormDistanceFunction distfunc;
  
  private int capital_n, n_star,capital_n_star,d;
  private double omega_star = 0.0;
  private Set<HilFeature> top;
  private HilFeature[] pf;
  private Heap<HilFeature> out;
  private Heap<HilFeature> wlb;
  private O factory;
  
  protected HilOut(int k, int n, int h, double t) {
    super();
    this.n = n;
    this.k = k;
    this.h = h;
    this.t = t;
    this.distfunc = new LPNormDistanceFunction(t);
    HilUpperComparator uc = new HilUpperComparator();
    HilLowerComparator lc = new HilLowerComparator();
    this.out = new Heap<HilFeature>(n+1, uc);
    this.wlb = new Heap<HilFeature>(n+1, lc);
    this.top = new HashSet<HilFeature>(2*n+1);
    this.n_star = 0;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    
    Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("HilOut preprocessing", relation.size(), logger) : null;
    WritableDoubleDataStore hilout_weight = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    factory = DatabaseUtil.assumeVectorField(relation).getFactory();
    
    capital_n_star = capital_n = relation.size();
    int j = 0;
    
    pf = new HilFeature[capital_n];
    d = DatabaseUtil.dimensionality(relation);
    
    NNComparator distcheck = new NNComparator();
    Pair<O,O> minMax = DatabaseUtil.computeMinMax(relation);
    double shift = 1.0 / (double)d;
    int pos = 0;
    for(DBID id : relation.iterDBIDs()) {
      hilout_weight.putDouble(id, 0.0);
      HilFeature entry = new HilFeature();
      entry.id  = id;
      entry.lbound = 0.0;
      entry.level = 0;
      entry.point = new double[d];
      for (int dim=0; dim < d; dim++)
        entry.point[dim] = (relation.get(entry.id).doubleValue(dim+1) - minMax.first.doubleValue(dim+1)) / (minMax.second.doubleValue(dim+1) - minMax.first.doubleValue(dim+1));
      entry.ubound = Double.POSITIVE_INFINITY;
      entry.hilbert = null;
      entry.nn = new Heap<NN>(k+1, distcheck);
      entry.nn_keys = new HashSet<DBID>(k);
      entry.sum_nn = 0.0;
      pf[pos++] = entry;
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    FiniteProgress progressHilOut = logger.isVerbose() ? new FiniteProgress("LOCI scores", relation.size(), logger) : null;
    while(j <= d && n_star < n){
      //out.clear();
      //wlb.clear();
      double v = j*shift;
      hilbert(v, j);
      scan(v, (int)(k * ((double)capital_n / (double)capital_n_star)));
      trueOutliers();
      top.clear();
      Set<DBID> top_keys = new HashSet<DBID>(out.size());
      for(HilFeature entry : out){
        top_keys.add(entry.id);
        top.add(entry);
      }
      for(HilFeature entry : wlb){
        if(!top_keys.contains(entry.id)){
          top.add(entry);
        }
      }
      j++;
      if(progressHilOut != null) {
        progressHilOut.incrementProcessed(logger);
      }
    }
    if(n_star < n){
      scan(shift*d, capital_n);
    }
    if(progressHilOut != null) {
      progressHilOut.ensureCompleted(logger);
    }
    for(HilFeature entry: out){
      hilout_weight.putDouble(entry.id, entry.sum_nn);
    }
    Relation<Double> scoreResult = new MaterializedRelation<Double>("HilOut weight", "hilout-weight", TypeUtil.DOUBLE, hilout_weight, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }
  
  private void hilbert(double v, int j){
    int half_max_int = Integer.MAX_VALUE >> 1;
    int v_half_max_int = (int)(v * half_max_int);
    for (int i=0; i < pf.length; i++){
      int[] coord = new int[d];
      for(int dim=0; dim < d; dim++){
        coord[dim] = (int)(v_half_max_int + half_max_int * pf[i].point[dim]);
      }
      pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h);
    }
    java.util.Arrays.sort(pf);
    capital_n_star = 0;
    for (int i=0; i < pf.length-1; i++){
      pf[i].level = minReg(i, i+1);
      if (pf[i].ubound >= omega_star){
        capital_n_star++;
      }
    }
  }
  
  private void scan(double v, int k0){
    for (int i=0; i < pf.length; i++){
      if (pf[i].ubound < omega_star){
        continue;
      }          
      if (pf[i].lbound < pf[i].ubound){
        double omega = fastUpperBound(i);
        if (omega < omega_star){
          pf[i].ubound = omega;
        }
        else{
          int maxcount;
          if (top.contains(pf[i])){
            maxcount = capital_n-1;
          }
          else{
            maxcount = java.lang.Math.min(2*k0, capital_n-1);
          }
          DoubleDoublePair bounds = innerScan(i, maxcount, v);
          double newlb = bounds.first;
          double newub = bounds.second;
          if(newlb > pf[i].lbound){
            pf[i].lbound = newlb;
          }
          if(newub < pf[i].ubound){
            pf[i].ubound = newub;
          }
        }
      }
      updateOUT(i);
      updateWLB(i);
      omega_star = java.lang.Math.max(omega_star, wlb.peek().lbound);
    }
  }
  
  private void updateOUT(int i){
    if (out.size() < n){
      out.offer(pf[i]);
    }
    else{
      HilFeature head = out.peek();
      if(pf[i].ubound > head.ubound){
        out.offer(pf[i]);
        out.poll();
      }
    }
  }
  
  private void updateWLB(int i){
    if (wlb.size() < n){
      wlb.offer(pf[i]);
    }
    else{
      HilFeature head = wlb.peek();
      if(pf[i].lbound > head.lbound){
        wlb.offer(pf[i]);
        wlb.poll();
      }
    } 
  }
  
  
  private double fastUpperBound(int i){
    int pre = i;
    int post = i;
    int z = 0;
    while(z < k){
      int pre_level = (pre-1 >= 0) ?  pf[pre-1].level : -1;
      int post_level = (post < capital_n-1)? pf[post].level : -1;
      if (post_level >= pre_level){
        post++;
      }
      else{
        pre--;
      }
      z++;
    }
    return k*maxDist(pf[i].point,minReg(pre, post));
  }
  
  private DoubleDoublePair innerScan(int i, int maxcount, double v){
    int a;
    int b = a = i;
    int levela,levelb;
    int level = levela = levelb = h;
    int count = 0;
    boolean stop = false;
    while(count < maxcount && !stop){
      int c;
      count++;
      if(a > 0 && pf[a-1].level >= pf[b].level){
        a--;
        levela = java.lang.Math.min(levela, pf[a].level);
        c = a;                  
      }
      else if(b < maxcount) {
        levelb = java.lang.Math.min(levelb, pf[b].level);
        b++;
        c = b;
      }
      else{
        a--;
        levela = java.lang.Math.min(levela, pf[a].level);
        c = a;
      }
      insert(i, pf[c].id, distfunc.doubleDistance(factory.newNumberVector(pf[i].point), factory.newNumberVector(pf[c].point)));
      if(pf[i].nn.size() == k){
        if(pf[i].sum_nn < omega_star){
          stop = true;
        }
        else if(java.lang.Math.max(levela, levelb) < level){
            level = java.lang.Math.max(levela, levelb);
            double delta = minDist(pf[i].point, level);
            stop = (delta >= pf[i].nn.peek().distance);
          }
      }
    }
    double br = boxRadius(i, (a-1 < 0) ? 0 : a-1 , (b+1 > capital_n-1) ? capital_n-1 : b+1, v);
    double newlb = 0.0;
    double newub = 0.0;
    for(NN entry : pf[i].nn){
      newub += entry.distance;
      if (entry.distance <= br){
        newlb += entry.distance;
      }
    }
    return new DoubleDoublePair(newlb, newub);
  }
  
  private double minDist(double[] p, int level){
    double dist = Double.POSITIVE_INFINITY;
    double r = 2.0 / (double)(1 << level+1);
    for (int dim=0; dim < d; dim++){
      double p_m_r = p[dim] - java.lang.Math.floor(p[dim] / r)*r;
      dist = java.lang.Math.min(dist, java.lang.Math.min(p_m_r, r-p_m_r));
    }
    return dist;
  }
  
  private double maxDist(double[] p, int level){
    double dist = Double.NEGATIVE_INFINITY;
    double r = 2.0 / (double)(1 << level+1);
    if (t == Double.POSITIVE_INFINITY){
      for (int dim=0; dim < d; dim++){
        double p_m_r = p[dim] - java.lang.Math.floor(p[dim] / r)*r;
        dist = java.lang.Math.max(dist, java.lang.Math.max(p_m_r, r-p_m_r));
      }
    }
    else{
      for (int dim=0; dim < d; dim++){
        double p_m_r = p[dim] - java.lang.Math.floor(p[dim] / r)*r;
        dist += java.lang.Math.pow(java.lang.Math.max(p_m_r, r-p_m_r), t);
      }
      dist = java.lang.Math.pow(dist, 1.0/t);
    }
    return dist;
  }
  
  private int minReg(int a, int b){
      long[] pf_a = BitsUtil.copy(pf[a].hilbert);
      BitsUtil.xorI(pf_a, pf[b].hilbert);
      return (1 << (numberOfLeadingZeros(pf_a) / d)) >> 1;
  }
  
  private void insert(int i, DBID id, double dt){
    if (!pf[i].nn_keys.contains(id)){
      if (pf[i].nn.size() < k){
        NN entry = new NN();
        entry.id = id;
        entry.distance = dt;
        pf[i].nn.offer(entry);
        pf[i].nn_keys.add(id);
        pf[i].sum_nn += entry.distance;
      }
      else{
        NN entry = new NN();
        entry.id = id;
        entry.distance = dt;
        NN head = pf[i].nn.peek();
        if(entry.distance < head.distance){
          pf[i].nn.offer(entry);
          pf[i].nn_keys.remove(head.id);
          pf[i].nn_keys.add(id);
          pf[i].nn.poll();
          pf[i].sum_nn -= (head.distance - entry.distance);
        }
      }
    }
  }
  
  private void trueOutliers(){
    n_star = 0;
    for (HilFeature  entry : out){
      if (entry.lbound == entry.ubound && entry.ubound >= omega_star){
        n_star++;
      }
    }
  }
  
  private double boxRadius(int i, int a, int b, double v){
    long[] hil1 = BitsUtil.copy(pf[a].hilbert);
    long[] hil2 = BitsUtil.copy(pf[b].hilbert);
    BitsUtil.xorI(hil1, pf[i].hilbert);
    BitsUtil.xorI(hil2, pf[i].hilbert);
    int level = (1 << (java.lang.Math.max(numberOfLeadingZeros(hil1), numberOfLeadingZeros(hil2)) / d));
    return minDist(pf[i].point, level);
  }
  
  private int numberOfLeadingZeros(long[] in){
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
  
  private class HilLowerComparator implements Comparator<HilFeature>{
    @Override
    public int compare(HilFeature o1, HilFeature o2) {
      return (int)java.lang.Math.signum(o2.lbound - o1.lbound);
    }
    
  }
  
  private class HilUpperComparator implements Comparator<HilFeature>{
    @Override
    public int compare(HilFeature o1, HilFeature o2) {
      return (int)java.lang.Math.signum(o2.ubound - o1.ubound);
    }
    
  }
  
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractParameterizer {

    protected int k = 5;
    
    protected int n = 10;
    
    protected int h = 32;
    
    protected double t = 2.0;
    
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
        t = (tP.getValue() >= 1.0)? tP.getValue() : 2.0;
      }
    }

    @Override
    protected HilOut<O> makeInstance() {
      return new HilOut<O>(k, n, h, t);
    }
    
  }

}

final class NNComparator implements Comparator<NN>{

  @Override
  public int compare(NN o1, NN o2) {
    return (int)java.lang.Math.signum(o1.distance - o2.distance);
  }
  
}

final class NN{
  public DBID id;
  public double distance;  
}

final class HilFeature implements Comparable<HilFeature>{
  public DBID id;
  public double[] point;
  public long[] hilbert;
  public int level;
  public double ubound;
  public double lbound;
  public Heap<NN> nn;
  public Set<DBID> nn_keys;
  public double sum_nn;
  
  @Override
  public int compareTo(HilFeature o) {
    return BitsUtil.compare(this.hilbert, o.hilbert);
  }
 
  
}
