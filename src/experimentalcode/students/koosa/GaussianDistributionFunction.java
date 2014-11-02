package experimentalcode.students.koosa;

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
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

public class GaussianDistributionFunction implements ProbabilityFunction {

  private List<DoubleVector> means;
  private List<Matrix> variances;
  private DoubleVector weights;
  private int weightMax = 10000; // reset by constructor
  
  // Constructor for the cases:
  //  -> Simple GaussianDistribution
  //  -> Multivariate GaussianDistribution
  //  -> Mixture of GaussianDistribution with uniformly distributed weights
  public GaussianDistributionFunction(final List<DoubleVector> means, final List<Matrix> variances) {
    this(means, variances, null);
  }
  
  // Constructor for the cases:
  //  -> GaussianDistribution with probability of drawing  no Point at all
  //  -> Mixture of GaussianDistribution with customized weights 
  public GaussianDistributionFunction(final List<DoubleVector> means, final List<Matrix> variances, final DoubleVector weights) {
    if(means.size() != variances.size() || (weights != null && variances.size() != weights.getDimensionality())) {
      throw new IllegalArgumentException("[W: ]\tSize of 'means' and 'variances' has to be the same, also Dimensionality of weights.");
    }
    if(weights == null) {
       if(means.size() == 1) {
        final double[] values = {1.0};
        this.weights = new DoubleVector(values);
      } else {
        final int ref = means.size();
        final double[] values = new double[means.size()];
        for(int i = 0; i < means.size(); i++) {
          values[i] = 1.0/ref;
        }
      }
    } else {
      this.weights = weights;
      weightMax = 0;
      for(int i = 0; i < weights.getDimensionality(); i++) {
        weightMax += (int) Math.ceil(weights.doubleValue(i) * 10000);
      }
    }
    this.means = means;
    this.variances = variances;
  }
  
  @Override
  public DoubleVector drawValue(HyperBoundingBox mbr, Random rand) {
    int index = 0;
    double[] values = new double[mbr.getDimensionality()];
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
  
  public void setVariances(final List<Matrix> variances) {
    this.variances = variances;
  }
  
  public Matrix getVarianceRef(final int position) {
    return this.variances.get(position);
  }
  
  // I find this to be the more sane of the two given options
  public Matrix getVarianceCopy(final int position) {
    return this.variances.get(position).copy();
  }
  
  public void setMeans(final List<DoubleVector> means) {
    this.means = means;
  }
  
  public DoubleVector getMean(final int position) {
    return this.means.get(position);
  }
  
  public void setWeights(final DoubleVector weights) {
    this.weights = weights;
  }
  
  public double getWeight(final int position) {
    return this.weights.doubleValue(position);
  }
}
