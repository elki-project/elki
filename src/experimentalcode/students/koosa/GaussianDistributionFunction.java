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
  
  // Constructor for most simple usage
  public GaussianDistributionFunction(final double mean, final double variance) {
     this(mean, variance, 1.0);
  }
  
  // Constructor for most simple use plus probability of not drawing a Point at all
  public GaussianDistributionFunction(final double mean, final double variance, final double weight) {
    double[] values = {mean};
    means.add(new DoubleVector(values));
    variances.add((new Matrix(1,1)).set(0, 0, variance));
    values[0] = 1.0;
    weights = new DoubleVector(values);
    weightMax = (int) Math.ceil(weight * 10000);
    
  }
  
  // Constructor for simple case with vector of means
  public GaussianDistributionFunction(final DoubleVector means, final DoubleVector variances) {
    this(means, variances, null);
  }
  
  // Constructor for case with vector of means plus customized weights
  public GaussianDistributionFunction(final DoubleVector means, final DoubleVector variances, final DoubleVector weights) {
    if(means.getDimensionality() != variances.getDimensionality() || (weights != null && variances.getDimensionality() != weights.getDimensionality())) {
      throw new IllegalArgumentException("[W: ]\tDimensionality of means, variances and weights has to be the same.");
    }
    if(weights == null) {
      if(means.getDimensionality() == 1) {
        final double[] values = {1.0};
        this.weights = new DoubleVector(values);
      } else {
        final int ref = means.getDimensionality();
        final double[] values = new double[means.getDimensionality()];
        for(int i = 0; i < means.getDimensionality(); i++) {
          values[i] = 1.0/ref;
        }
        this.weights = new DoubleVector(values);
      }
    } else {
      this.weights = weights;
      weightMax = 0;
      for(int i = 0; i < weights.getDimensionality(); i++) {
        weightMax += (int) Math.ceil(weights.doubleValue(i) * 10000);
      }
    }
    this.means.add(means);
    this.variances.add(new Matrix(1,variances.getDimensionality()));
    for(int i = 0; i < means.getDimensionality(); i++) {
      this.variances.get(0).set(0,i,variances.doubleValue(i));
    }
  }
  
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
    
    // I read, that usually the product of Cholesky-Decomposition of the covariance-matrix and
    // a vector of independent random-values is used for sampling on multivariate gaussian
    // distributions...
    return new DoubleVector(((Vector) means).plus((new CholeskyDecomposition(variances.get(index))).getL().times(new Vector(values))).getArrayCopy());
    // return new DoubleVector(((Vector) means).plus(variances.get(index).times(new Vector(values))).getArrayCopy());
  }
}
