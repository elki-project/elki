package experimentalcode.students.koosa;

import java.util.List;
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



import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;

public class MultivariateGaussianDistributionFunction extends AbstractGaussianDistributionFunction {

  public MultivariateGaussianDistributionFunction(final List<DoubleVector> means, final List<Matrix> variances) {
    this(means, variances, null);
  }
  
  public MultivariateGaussianDistributionFunction(final List<DoubleVector> means, final List<Matrix> variances, final DoubleVector weights) {
    if(means.size() != variances.size() || (weights != null && variances.size() != weights.getDimensionality())) {
      throw new IllegalArgumentException("[W: ]\tSize of 'means' and 'variances' has to be the same, also Dimensionality of weights.");
    }
    for(int i = 0; i < means.size(); i++) {
      if(variances.get(i).getColumnDimensionality() != means.get(i).getDimensionality()) {
        throw new IllegalArgumentException("[W: ]\tDimensionality of contained DoubleVectors for 'means' and 'variances' hast to be the same.");
      }
    }
    if(weights == null) {
      if(means.size() == 1) {
        this.weights = new DoubleVector(new double[] {1.0});
      } else {
        final double ref = 1.0/means.size();
        final double[] values = new double[means.size()];
        for(int i = 0; i < means.size(); i++) {
          values[i] = ref;
        }
        this.weights = new DoubleVector(values);
      }
    } else {
      this.weights = weights;
      weightMax = 0;
      for(int i = 0; i < weights.getDimensionality(); i++) {
        weightMax += (int) Math.ceil(weights.doubleValue(i)) * 10000;
      }
    }
    this.means = means;
    this.variances = variances;
  }
  
  public void setVariances(final List<Matrix> variances) {
    this.variances = variances;
  }
}
