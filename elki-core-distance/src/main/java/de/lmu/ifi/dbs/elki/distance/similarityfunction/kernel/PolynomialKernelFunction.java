/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractVectorSimilarityFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import net.jafama.FastMath;

/**
 * Polynomial Kernel function that computes a similarity between the two feature
 * vectors x and y defined by \((x^T\cdot y+b)^{\text{degree}}\).
 * 
 * @author Simon Paradies
 * @since 0.1
 */
public class PolynomialKernelFunction extends AbstractVectorSimilarityFunction implements PrimitiveDistanceFunction<NumberVector> {
  /**
   * The default degree.
   */
  public static final int DEFAULT_DEGREE = 2;

  /**
   * Degree of the polynomial kernel function.
   */
  private final int degree;

  /**
   * Bias of the similarity function.
   */
  private final double bias;

  /**
   * Constructor.
   * 
   * @param degree Kernel degree
   * @param bias Bias offset
   */
  public PolynomialKernelFunction(int degree, double bias) {
    super();
    this.degree = degree;
    this.bias = bias;
  }

  /**
   * Constructor.
   * 
   * @param degree Kernel degree
   */
  public PolynomialKernelFunction(int degree) {
    this(degree, 0.);
  }

  @Override
  public double similarity(NumberVector o1, NumberVector o2) {
    return MathUtil.powi(VectorUtil.dot(o1, o2) + bias, degree);
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public double distance(NumberVector fv1, NumberVector fv2) {
    return FastMath.sqrt(similarity(fv1, fv1) + similarity(fv2, fv2) - 2 * similarity(fv1, fv2));
  }

  @Override
  public <T extends NumberVector> DistanceSimilarityQuery<T> instantiate(Relation<T> database) {
    return new PrimitiveDistanceSimilarityQuery<>(database, this, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Degree parameter.
     */
    public static final OptionID DEGREE_ID = new OptionID("kernel.polynomial.degree", "The degree of the polynomial kernel function. Default: " + DEFAULT_DEGREE);

    /**
     * Bias parameter.
     */
    public static final OptionID BIAS_ID = new OptionID("kernel.polynomial.bias", "The bias of the polynomial kernel, a constant that is added to the scalar product.");

    /**
     * Degree of the polynomial kernel function.
     */
    protected int degree = 0;

    /**
     * Bias parameter.
     */
    protected double bias = 0.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter degreeP = new IntParameter(DEGREE_ID, DEFAULT_DEGREE) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(degreeP)) {
        degree = degreeP.intValue();
      }
      final DoubleParameter biasP = new DoubleParameter(BIAS_ID) //
          .setOptional(true);
      if(config.grab(biasP)) {
        bias = biasP.doubleValue();
      }
    }

    @Override
    protected PolynomialKernelFunction makeInstance() {
      if(degree == 1 && (bias == 0.)) {
        return LinearKernelFunction.STATIC;
      }
      return new PolynomialKernelFunction(degree, bias);
    }
  }
}
