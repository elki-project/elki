package experimentalcode.students.koosa;

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CholeskyDecomposition;
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

public abstract class AbstractGaussianDistributionFunction implements ProbabilityDensityFunction {

  protected List<DoubleVector> means;
  protected List<Matrix> variances;
  protected DoubleVector weights;
  protected int weightMax = 10000; // reset by constructor
  
  @Override
  public DoubleVector drawValue(SpatialComparable bounds, Random rand) {
    int index = 0;
    double[] values = new double[bounds.getDimensionality()];
    if(weights.getDimensionality() > 1 || weightMax != 10000) {
      final int ind = rand.nextInt(10000);
      int sum = 0;
      do {
        sum += weights.doubleValue(index++) * 10000;
      } while(sum < ind && sum < weightMax);
      index--;
    }
    
    for(int i = 0; i < values.length; i++) {
      values[i] = rand.nextGaussian();
    }
    
    return new DoubleVector(((Vector) means).plus((new CholeskyDecomposition(variances.get(index))).getL().times(new Vector(values))).getArrayCopy());
  }
  
  public Matrix getVarianceRef(final int position) {
    return this.variances.get(position);
  }
  
  public Matrix getVarianceCopy(final int position) {
    return this.variances.get(position).copy();
  }
  
  public void setMeans(final List<DoubleVector> means) {
    this.means = means;
  }
  
  public DoubleVector getMean(final int position) {
    return this.means.get(position);
  }
  
  public void setMean(final int position, final DoubleVector mean) {
    this.means.set(position, mean);
  }
  
  public void setWeights(final DoubleVector weights) {
    this.weights = weights;
  }
  
  public DoubleVector getWeights() {
    return this.weights;
  }
  
  public double getWeight(final int position) {
    return this.weights.doubleValue(position);
  }
}
