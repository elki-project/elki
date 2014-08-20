package experimentalcode.students.yangy;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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


import java.util.Arrays;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.dependence.AbstractDependenceMeasure;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;

/**
 * Calculate the Hoeffding's D as a measure of dependence and derive the p value; 
 * 
 * References:
 * <p>
 * Hoeffding W. (1948): A non-parametric test of independence. Ann Math Stat 19:546–57.
 * Frank E Harrell Jr and with contributions from Charles Dupont and many others. (2014): Hmisc: Harrell Miscellaneous
 * http://support.sas.com/documentation/cdl/en/procstat/63104/HTML/default/viewer.htm#procstat_corr_sect016.htm
 * </p>
 * 
 * @author Yinchong Yang
 */

public class HoeffdingsDependenceMeasure extends AbstractDependenceMeasure{
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(HoeffdingsDependenceMeasure.class);

  /**
   * Static instance.
   */
  public static final HoeffdingsDependenceMeasure STATIC = new HoeffdingsDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected HoeffdingsDependenceMeasure() {
    super();
  }
  
  private double dependenceMeasure = 0.;
  private double pValue = 0.;

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int n = size(adapter1, data1, adapter2, data2);
    int[] r = computeRanks(adapter1, data1, n);
    int[] s = computeRanks(adapter2, data2, n);
    int[] q = computeRanks(adapter1, data1, adapter2, data2, n);
    
    int d1 = 0, d2 = 0, d3 = 0;
    
    for (int i = 0; i < n; i ++) {
      d1 = d1 + (q[i] - 1) * (q[i] - 2); 
      d2 = d2 + (r[i] - 1) * (r[i] - 2) * (s[i] - 1) * (s[i] - 2); 
      d3 = d3 + (r[i] - 2) * (s[i] - 2) * (q[i] - 1); 
    }
    
    int d = 30 * ( (n-2)*(n-3)*d1 + d2 - 2*(n-2)*d3 ) / (n*(n-1)*(n-2)*(n-3)*(n-4)); 
    
    // bad idea: 
    // Normalization: the Hoeffding's D lies between -0.5 and 1 in case of no ties in data
//    double normalizedD = ( d - (-0.5) ) / (1 - (-0.5)); 
    
    // rather
    setDependenceMeasure(d); 
    setpValue(phoeffd(d/30, n)); 
    
    return getDependenceMeasure(); 
    
  }
  
  // function calculating univariate ranks 
  public static <A> int[] computeRanks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    // Sort the objects:
    int[] s1 = MathUtil.sequence(0, len);
    IntegerArrayQuickSort.sort(s1, new IntegerComparator() {
      @Override
      public int compare(int x, int y) {
        return Double.compare(adapter.getDouble(data, x), adapter.getDouble(data, y));
      }
    });
    int[] ret = new int[len];
    for(int i = 0; i < len;) {
      final int start = i++;
      double val = adapter.getDouble(data, s1[start]);
      while(i < len && adapter.getDouble(data, s1[i]) <= val) {
        i++;
      }
      final int score = (start + i - 1) / 2 + 1;
      for(int j = start; j < i; j++) {
        ret[s1[j]] = score;
      }
    }
    return ret;
  }
  
  // function calculatin bivariate ranks
  public static <A, B> int[] computeRanks(NumberArrayAdapter<?, A> adapter1, A data1, 
                                          NumberArrayAdapter<?, B> adapter2, B data2, int len) {
    
    int n = len;
    int[] ret = new int[n]; 
    Arrays.fill(ret, 1);
    
    for (int i = 0; i < n; i ++) {
      for (int j = 0; j < n; j ++) {
        if (adapter1.getDouble(data1, j) < adapter1.getDouble(data1, i) && 
            adapter2.getDouble(data2, j) < adapter2.getDouble(data2, i)) {
          ret[i] ++;
        }
      }
    }
    return ret;
    
  }
  
  // function deriving the p value
  public static double phoeffd(double d, int n) {
    
    double b = d + (double) 1/36/n; 
    System.out.println("b = " + b);
    double z = 0.5 * Math.pow(Math.PI, 4) * n * b; 
    double[] tabvals = {0.5297,0.4918,0.4565,0.4236,0.393,0.3648,0.3387,0.3146,
        0.2924,0.2719,0.253,0.2355,0.2194,0.2045,0.1908,0.1781,0.1663,0.1554,
        0.1453,0.1359,0.1273,0.1192,0.1117,0.1047,0.0982,0.0921,0.0864,0.0812,
        0.0762,0.0716,0.0673,0.0633,0.0595,0.056,0.0527,0.0496,0.0467,0.044,
        0.0414,0.039,0.0368,0.0347,0.0327,0.0308,0.0291,0.0274,0.0259,0.0244,
        0.023,0.0217,0.0205,0.0194,0.0183,0.0173,0.0163,0.0154,0.0145,0.0137,
        0.013,0.0123,0.0116,0.011,0.0104,0.0098,0.0093,0.0087,0.0083,0.0078,
        0.0074,0.007,0.0066,0.0063,0.0059,0.0056,0.0053,0.005,0.0047,0.0045,
        0.0042,0.0025,0.0014,0.0008,0.0005,0.0003,0.0002,0.0001}; 
    
    double p = -1.0; 
    
    if (z < 1.1 | z > 8.5) {
//      System.out.println("1st condition true because z = " + z);
      double o1 = 1e-8;
      double o2 = Math.exp(0.3885037 - 1.164879 * z);
      double o3 = o2;
      p = 01; 
      if (1 < o2) {
        o3 = 1; 
      }
      if (o3 > o1) {
        p = o3;
      }
        
    } else {
//      System.out.println("2nd condition true because z = " + z);
      double[] seq = new double[86]; 
      for (int i = 0; i < 79; i++) {
        seq[i] = 1.1 + i * 0.05;   
      }
      for (int j = 79; j< 86; j++) {
        seq[j] = 5.5 + (j-79) * 0.5; 
      }
      
      for (int i = 0; i < 86; i ++) {
        if (seq[i] >= z) {
          if (seq[i] == z) {
            p = tabvals[i]; 
            break;
          } else {
            double x1 = seq[i]; 
            double x0 = seq[i-1];
            double y1 = tabvals[i];
            double y0 = tabvals[i-1];
//            System.out.println("x0 = " + x0);
//            System.out.println("x1 = " + x1);
//            System.out.println("y0 = " + y0);
//            System.out.println("y1 = " + y1);
            p = y0 + (y1 - y0) * (z - x0) / (x1 - x0); 
            break;
          }
        }
      }
    }
   
    return p;
    
  }

  public double getDependenceMeasure() {
    return dependenceMeasure;
  }

  public void setDependenceMeasure(double dependenceMeasure) {
    this.dependenceMeasure = dependenceMeasure;
  }

  public double getpValue() {
    return pValue;
  }

  public void setpValue(double pValue) {
    this.pValue = pValue;
  }


}
