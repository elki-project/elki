package de.lmu.ifi.dbs.elki.data.uncertain.probabilitydensityfunction;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2015
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
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain.PWCClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.uncertain.ContinuousUncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.transform.UncertainifyFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CholeskyDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * ProbabilityDensityFunction class to model n-variate gaussian distributions.
 *
 * Used for construction of {@link UncertainObject}, filtering with
 * {@link UncertainifyFilter} and sampling with {@link PWCClusteringAlgorithm}.
 *
 * @author Alexander Koos
 */
public class MultivariateGaussianDistributionFunction extends AbstractGaussianDistributionFunction<Matrix> {
  /**
   * Flag specifying if covariance matrices are randomly generated during
   * uncertainification.
   */
  private boolean rotate;

  /**
   * Constructor.
   *
   * @param minDev
   * @param maxDev
   * @param minMin
   * @param maxMin
   * @param minMax
   * @param maxMax
   * @param multMin
   * @param multMax
   * @param rotate
   * @param urand
   */
  public MultivariateGaussianDistributionFunction(final double minDev, final double maxDev, final double minMin, final double maxMin, final double minMax, final double maxMax, final long multMin, final long multMax, final boolean rotate, final Random urand) {
    this.minDev = minDev;
    this.maxDev = maxDev;
    this.minMin = minMin;
    this.maxMin = maxMin;
    this.minMax = minMax;
    this.maxMax = maxMax;
    this.multMin = multMin;
    this.multMax = multMax;
    this.rotate = rotate;
    this.urand = urand;
  }

  /**
   * Constructor.
   *
   * @param means
   * @param variances
   */
  public MultivariateGaussianDistributionFunction(final List<DoubleVector> means, final List<Matrix> variances) {
    this(means, variances, null);
  }

  /**
   * Constructor.
   *
   * @param means
   * @param variances
   * @param weights
   */
  public MultivariateGaussianDistributionFunction(final List<DoubleVector> means, final List<Matrix> variances, final int[] weights) {
    if(means.size() != variances.size() || (weights != null && variances.size() != weights.length)) {
      throw new IllegalArgumentException("[W: ]\tSize of 'means' and 'variances' has to be the same, also Dimensionality of weights.");
    }
    for(int i = 0; i < means.size(); i++) {
      if(variances.get(i).getColumnDimensionality() > 1 && variances.get(i).getColumnDimensionality() != means.get(i).getDimensionality()) {
        throw new IllegalArgumentException("[W: ]\tDimensionality of contained DoubleVectors for 'means' and 'variances' hast to be the same.");
      }
    }
    if(weights == null) {
      this.weights = new int[means.size()];
      final int c = UOModel.PROBABILITY_SCALE / means.size();
      for(int i = 0; i < means.size(); i++) {
        this.weights[i] = c;
      }
    }
    else {
      this.weights = weights;
      this.weightMax = 0;
      for(int i = 0; i < weights.length; i++) {
        this.weightMax += weights[i];
      }
    }
    this.means = means;
    this.variances = variances;
  }

  @Override
  public DoubleVector drawValue(final SpatialComparable bounds, final Random rand) {
    int index = 0;
    final double[] values = new double[bounds.getDimensionality()];

    for(int j = 0; j < UOModel.DEFAULT_TRY_LIMIT; j++) {
      if(this.weights.length > 1) {
        index = UncertainUtil.drawIndexFromIntegerWeights(rand, this.weights, this.weightMax);
      }
      boolean inBounds = index < this.weights.length;

      if(!inBounds) {
        continue;
      }
      for(int i = 0; i < values.length; i++) {
        values[i] = rand.nextGaussian();
      }
      final double[] result = this.variances.get(index).getColumnDimensionality() > 1 ? (this.means.get(index).getColumnVector()).plus((new CholeskyDecomposition(this.variances.get(index))).getL().times(new Vector(values))).getArrayCopy() : this.getGaussianDrawnVector(this.means.get(index).getColumnVector(), Math.sqrt(this.variances.get(index).get(0, 0)), new Vector(values));
      for(int i = 0; i < result.length; i++) {
        inBounds &= result[i] <= bounds.getMax(i) && result[i] >= bounds.getMin(i);
      }
      if(inBounds) {
        return new DoubleVector(result);
      }
    }

    return AbstractGaussianDistributionFunction.noSample;
  }

  @Override
  protected List<DoubleVector> getDeviationVector() {
    return this.getDeviationVector(this.variances);
  }

  /**
   * For a list of covariance matrices given look up the values from their
   * CholeskyDecompositions diagonal and give them back as a list of vectors.
   *
   * The values of those vectors describe variances for the particular
   * dimensions.
   *
   * Iff the matrices are scalar (1x1) the vector will also be scalar (just 1
   * row).
   *
   * @param variances
   * @return
   */
  private List<DoubleVector> getDeviationVector(final List<Matrix> variances) {
    final List<DoubleVector> deviationVectors = new ArrayList<DoubleVector>();
    for(int i = 0; i < variances.size(); i++) {
      final double[] vector = new double[variances.get(i).getColumnDimensionality()];
      for(int j = 0; j < variances.get(0).getColumnDimensionality(); j++) {
        vector[j] = (new CholeskyDecomposition(variances.get(i))).getL().get(j, j);
      }
      deviationVectors.add(new DoubleVector(vector));
    }
    return deviationVectors;
  }

  @Override
  public UncertainObject<UOModel> uncertainify(final NumberVector vec, final boolean blur, final boolean uncertainify, final int dims) {
    final int multiplicity = this.urand.nextInt((int) (this.multMax - this.multMin) + 1) + (int) this.multMin;
    final List<DoubleVector> means = new ArrayList<DoubleVector>();
    final List<Matrix> variances = new ArrayList<Matrix>();
    int[] weights;
    if(uncertainify) {
      weights = UncertainUtil.calculateRandomIntegerWeights(multiplicity, this.weightMax, this.urand);
      for(int h = 0; h < multiplicity; h++) {
        final double[] imeans = new double[vec.getDimensionality()];
        final double minBound = this.urand.nextDouble() * (this.maxMin - this.minMin) + this.minMin;
        final double maxBound = this.urand.nextDouble() * (this.maxMax - this.minMax) + this.minMax;
        for(int i = 0; i < UOModel.DEFAULT_TRY_LIMIT; i++) {
          final Matrix ivariances = this.randomDrawCovMatrix(this.rotate, vec.getDimensionality());
          for(int j = 0; j < vec.getDimensionality(); j++) {
            imeans[j] = vec.getMax(j) + (blur ? (this.urand.nextInt(2) == 1 ? this.urand.nextGaussian() * minBound : this.urand.nextGaussian() * maxBound) : 0);
          }
          final double[] result = ivariances.getColumnDimensionality() > 1 ? (vec.getColumnVector()).plus((new CholeskyDecomposition(ivariances).getL()).times(new Vector(imeans))).getArrayCopy() : this.getGaussianDrawnVector(vec.getColumnVector(), Math.sqrt(ivariances.get(0, 0)), new Vector(imeans));
          boolean valid = true;
          for(int j = 0; j < vec.getDimensionality(); j++) {
            valid &= (result[j] >= vec.doubleValue(j) - minBound) && (result[j] <= vec.doubleValue(j) + maxBound);
          }
          if(valid && i < (UOModel.DEFAULT_TRY_LIMIT - 1)) {
            means.add(new DoubleVector(result));
            variances.add(ivariances);
            break;
          }
          else if(!valid && i == (UOModel.DEFAULT_TRY_LIMIT - 1)) {
            final double[] noResult = new double[vec.getDimensionality()];
            for(int k = 0; k < noResult.length; k++) {
              noResult[k] = Double.MAX_VALUE;
            }
            means.add(new DoubleVector(noResult));
            variances.add(new Matrix(new double[1][1]));
          }
        }
      }
    }
    else {
      final int buildFac = this.rotate ? (dims * ((dims - 1) + 2) + 1) : dims + 2;
      final int s = vec.getDimensionality() / (this.rotate ? 1 + dims * ((dims - 1) + 2) : 2 + dims);
      weights = new int[s];
      for(int i = 0, mark = 1; i < s; i++, mark++) {
        weights[i] = (int) vec.doubleValue(i * buildFac);
        means.add(this.getMeanVector(vec, dims, i * buildFac, mark));
        if(this.rotate) {
          variances.add(this.getMatrixFromData(vec, dims, i, mark + dims));
        }
        else {
          variances.add(new Matrix(new double[][] { { vec.doubleValue(i + buildFac + 1 + mark) } }));
        }
      }
    }

    return new UncertainObject<UOModel>(new ContinuousUncertainObject<>(new MultivariateGaussianDistributionFunction(means, variances, weights), vec.getDimensionality()), vec.getColumnVector());
  }

  /**
   * Extracts the vector of means from a given vector of input data.
   *
   * @param vec
   * @param dims
   * @param i
   * @param mark
   * @return
   */
  private DoubleVector getMeanVector(final NumberVector vec, final int dims, final int pos, final int mark) {
    final double[] means = new double[dims];
    for(int i = 0; i < dims; i++) {
      means[i] = vec.doubleValue(pos + mark + i);
    }
    return new DoubleVector(means);
  }

  /**
   * Extracts a covariance matrix from a given vector of input data containing
   * the matrix' serialized form.
   *
   * @param vec
   * @param dims
   * @param i
   * @return
   */
  private Matrix getMatrixFromData(final NumberVector vec, final int dims, final int pos, final int mark) {
    final double[][] matrix = new double[dims][dims];
    final double[][] tmatrix = new double[dims][dims];
    int ind = 0;
    for(int i = 0; i < dims; i++) {
      for(int j = 0; j < i || j == 0; j++) {
        matrix[i][j] = tmatrix[j][i] = vec.doubleValue(pos * (dims * ((dims - 1) + 1)) + mark + ind++);
      }
    }
    return (new Matrix(matrix)).times((new Matrix(tmatrix)));
  }

  /**
   * Iff rotate: randomly generate covariance matrix by multiplying one lower
   * triangular matrix with its transposed. That way drawn samples will be
   * rotated in their space.
   *
   * Else: randomly generate a scalar variance.
   *
   * @param rotate
   * @param dimensions
   * @return
   */
  private Matrix randomDrawCovMatrix(final boolean rotate, final int dimensions) {
    final double[][] lmat = rotate ? new double[dimensions][dimensions] : new double[1][1];
    final double[][] rmat = rotate ? new double[dimensions][dimensions] : new double[1][1];
    for(int i = 0; i < lmat.length; i++) {
      for(int j = 0; j < lmat.length; j++) {
        if(i >= j) {
          final double val = this.urand.nextGaussian() * (this.maxDev - this.minDev) + this.minDev;
          lmat[i][j] = val;
          rmat[j][i] = val;
        }
      }
    }
    return (new Matrix(lmat)).times(new Matrix(rmat));
  }

  private double[] getGaussianDrawnVector(final Vector means, final double variance, final Vector gaussianDrawn) {
    return ((gaussianDrawn.times(variance)).plus(means)).getArrayCopy();
  }

  public static class Parameterizer extends IndependentGaussianDistributionFunction.Parameterizer {
    /**
     * Field to hold parameter value.
     */
    protected boolean rotate;

    /**
     * Parameter to specify if randomly generated covariance matrices shall be
     * used.
     */
    public static final OptionID ROTATE_ID = new OptionID("uo.rotate", "Create randomized covariance matrix. Else use scalar variance (1x1 CovMatrix).");

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final Flag protate = new Flag(Parameterizer.ROTATE_ID);
      if(config.grab(protate)) {
        this.rotate = protate.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new MultivariateGaussianDistributionFunction(this.stddevMin, this.stddevMax, this.minMin, this.maxMin, this.minMax, this.maxMax, this.multMin, this.multMax, this.rotate, this.randFac.getRandom());
    }
  }
}