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
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
   * Parameter to specify the maximum Hilbert-Level
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
  
  protected HilOut(int k, int n, int h, double t) {
    super();
    this.n = n;
    this.k = k;
    this.h = h;
    this.t = t;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("HilOut preprocessing", relation.size(), logger) : null;
    int capital_n, n_star;
    double omega_star = 0.0;
    int capital_n_star = capital_n = relation.size();
    int j = n_star = 0;
    Set<HilFeature> top = new HashSet<HilFeature>();
    HilFeature[] pf = new HilFeature[capital_n];
    int d = DatabaseUtil.dimensionality(relation);
    HilUpperComparator uc = new HilUpperComparator();
    HilLowerComparator lc = new HilLowerComparator();
    Heap<HilFeature> out;
    Heap<HilFeature> wlb;
    Pair<O,O> minMax = DatabaseUtil.computeMinMax(relation);
    double shift = 0.5 / (double)d;
    int pos = 0;
    for(DBID id : relation.iterDBIDs()) {
      HilFeature entry = new HilFeature();
      entry.id  = id;
      entry.lbound = 0.0;
      entry.level = 0;
      entry.point = new double[d];
      entry.ubound = Double.POSITIVE_INFINITY;
      entry.hilbert = null;
      entry.nn = new HashSet<NN>();
      pf[pos++] = entry;
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    
    while(j <= d && n_star < n){
      out = new Heap<HilFeature>(n, uc);
      wlb = new Heap<HilFeature>(n, lc);
      // Hilbert
      for (int i=0; i < pf.length; i++){
        int[] coord = new int[d];
        for(int dim=0; dim < d; dim++){
          pf[i].point[dim] = j*shift+((relation.get(pf[i].id).doubleValue(dim+1) - minMax.first.doubleValue(dim+1)) / (minMax.second.doubleValue(dim+1) - minMax.first.doubleValue(dim+1)));
          coord[dim] = (int)(((Integer.MAX_VALUE >> 1) * pf[i].point[dim]) + ((Integer.MAX_VALUE >> 1) * ((double)j / (double)d)));
        }
        pf[i].hilbert = HilbertSpatialSorter.coordinatesToHilbert(coord, h);
      }
      java.util.Arrays.sort(pf);
      // Hilbert
      
      // Scan
      {
        int level_max_pos = (k < capital_n) ? capital_n -1 : k;
        int window_min = 0;
        int window_max = level_max_pos;
        long[] hilbert_min = BitsUtil.copy(pf[window_min].hilbert);
        long[] hilbert_max = pf[window_max].hilbert;
        int lowest_level = (BitsUtil.numberOfLeadingZeros(BitsUtil.xorI(hilbert_min, hilbert_max)) >> 1);
        for (int i=0; i < pf.length; i++){
          if (pf[i].ubound < omega_star){
            continue;
          }          
          if (pf[i].lbound < pf[i].ubound){
            window_min = (i - k < 0) ? 0 : i - k;
            window_max = (i + k < capital_n) ? capital_n -1 : i + k;
            if(level_max_pos - k < window_min){
              hilbert_max = pf[window_max].hilbert;
              hilbert_min = BitsUtil.copy(pf[window_max - k].hilbert);
              int lowest_level_candidate = (BitsUtil.numberOfLeadingZeros(BitsUtil.xorI(hilbert_min, hilbert_max)) >> 1);
              int level_max_pos_candidate = window_max;
              while (level_max_pos_candidate > window_min + k && lowest_level_candidate >= lowest_level -1){
                level_max_pos_candidate--;
                hilbert_max = pf[level_max_pos_candidate].hilbert;
                hilbert_min = BitsUtil.copy(pf[level_max_pos_candidate - k].hilbert);
                int new_candidate = (BitsUtil.numberOfLeadingZeros(BitsUtil.xorI(hilbert_min, hilbert_max)) >> 1);
                if (lowest_level_candidate < new_candidate){
                  lowest_level_candidate = new_candidate;
                }
              }
              lowest_level = lowest_level_candidate;
              level_max_pos = level_max_pos_candidate;
            }
            else{
              hilbert_max = pf[window_max].hilbert;
              hilbert_min = BitsUtil.copy(pf[window_max - k].hilbert);
              int lowest_level_candidate = (BitsUtil.numberOfLeadingZeros(BitsUtil.xorI(hilbert_min, hilbert_max)) >> 1);
              if (lowest_level_candidate >= lowest_level){
                lowest_level = lowest_level_candidate;
                level_max_pos = window_max;
              }
            }
            double maxdist = 0.0;
            double r = 2.0 / (double)(1 << lowest_level);
            for (int dim=1; dim <= d; dim++){
              double p_m_r = pf[i].point[dim] - java.lang.Math.floor(pf[i].point[dim] / r)*r;
              double max = java.lang.Math.max(p_m_r, r-p_m_r);
              if (t == Double.POSITIVE_INFINITY){
                maxdist = java.lang.Math.max(max, maxdist);
              }
              else{
                maxdist += java.lang.Math.pow(max, t);
              }
            }
            double omega;
            if (t == Double.POSITIVE_INFINITY){
              omega = k * java.lang.Math.pow(maxdist, 1.0/(double)t);
            }
            else{
              omega = k * maxdist;
            }
            if (omega < omega_star){
              pf[i].ubound = omega;
            }
            else{
              int maxcount;
              if (top.contains(pf[i])){
                maxcount = capital_n;
              }
              else{
                maxcount = java.lang.Math.min(2 * k * capital_n / capital_n_star, capital_n);
              }
              // innerScan
              
              
              // innerScan
              
              
            }
            
          }
        }
      }
      // Scan
      
      j++;
    } 
    return null;
  }
  

  @Override
  protected Logging getLogger() {
    return logger;
  }
  
  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array();
  }
  
  private class HilLowerComparator implements Comparator<HilFeature>{
    @Override
    public int compare(HilFeature o1, HilFeature o2) {
      return (int)java.lang.Math.signum(o1.lbound - o2.lbound);
    }
    
  }
  
  private class HilUpperComparator implements Comparator<HilFeature>{
    @Override
    public int compare(HilFeature o1, HilFeature o2) {
      return (int)java.lang.Math.signum(o1.ubound - o2.ubound);
    }
    
  }
  
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractParameterizer {

    protected int k = 5;
    
    protected int n = 10;
    
    protected int h = 31;
    
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
      
      final IntParameter hP = new IntParameter(H_ID, 31);
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
final class NN{
  public DBID id;
  public double dist;
}

final class HilFeature implements Comparable<HilFeature>{
  public DBID id;
  public double[] point;
  public long[] hilbert;
  public int level;
  public double ubound;
  public double lbound;
  public Set<NN> nn;
  
  @Override
  public int compareTo(HilFeature o) {
    return BitsUtil.compare(this.hilbert, o.hilbert);
  }
 
  
}
