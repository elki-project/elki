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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.dependence.AbstractDependenceMeasure;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;

public class HoeffdingsDependenceMeasure extends AbstractDependenceMeasure{
  
  protected HoeffdingsDependenceMeasure() {
    super();
  }

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
    
//    System.out.println("d1 = " + d1);
//    System.out.println("d2 = " + d2);
//    System.out.println("d3 = " + d3);
    
    int d = 30 * ( (n-2)*(n-3)*d1 + d2 - 2*(n-2)*d3 ) / (n*(n-1)*(n-2)*(n-3)*(n-4)); 
    
    // Normalization: the Hoeffding's D lies between -0.5 and 1 in case of no ties in data
    double normalizedD = ( d - (-0.5) ) / (1 - (-0.5)); 
    
//    return d;
    return normalizedD; 
    
  }
  
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
//    System.out.println(Arrays.toString(ret));
    return ret;
  }
  
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
//    System.out.println(Arrays.toString(ret));
    return ret;
    
  }


}
