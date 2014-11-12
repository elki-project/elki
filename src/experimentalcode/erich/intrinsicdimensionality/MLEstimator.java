package experimentalcode.erich.intrinsicdimensionality;

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
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Maximum-Likelihood Estimator for intrinsic dimensionality.
 * 
 * With additional harmonic mean of all sub-samples.
 * 
 * @author Jonathan von Brünken
 * @author Erich Schubert
 */
public class MLEstimator extends AbstractIntrinsicDimensionalityEstimator {

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int n = adapter.size(data);
    if(n < 2) {
      return 0.0;
    }
    double id = 0.0;
    double sum = 0.0;
    double w = adapter.getDouble(data, 1);
    int p = 0;
    int sumk = 0;
    for(int i = 1; i < n; i++) {
      for(; p < i; p++) {
        sum += Math.log(adapter.getDouble(data, p) / w);
      }
      id -= sum;
      sumk += i;
      if(i < n - 1) {
        final double w2 = adapter.getDouble(data, i + 1);
        sum += i * Math.log(w / w2);
        w = w2;
      }
    }
    return (double) sumk / id;
  }
}
