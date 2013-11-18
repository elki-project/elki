package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.similarity.PrimitiveSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the laplace / exponential radial basis function kernel.
 * 
 * @author Erich Schubert
 */
public class LaplaceKernelFunction implements PrimitiveSimilarityFunction<NumberVector<?>, DoubleDistance> {
  /**
   * Scaling factor mgamma. (= - 1/sigma)
   */
  private final double mgamma;

  /**
   * Constructor.
   * 
   * @param sigma Scaling parameter sigma (as in laplace kernel)
   */
  public LaplaceKernelFunction(double sigma) {
    super();
    this.mgamma = -.5 / (sigma * sigma);
  }

  /**
   * Provides the linear kernel similarity between the given two vectors.
   * 
   * @param o1 first vector
   * @param o2 second vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   */
  public double doubleSimilarity(NumberVector<?> o1, NumberVector<?> o2) {
    if (o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }

    double sim = 0;
    for (int i = 0; i < o1.getDimensionality(); i++) {
      final double v = o1.doubleValue(i) - o2.doubleValue(i);
      sim += v * v;
    }
    return Math.exp(mgamma * Math.sqrt(sim));
  }

  @Override
  public DoubleDistance similarity(NumberVector<?> o1, NumberVector<?> o2) {
    return new DoubleDistance(doubleSimilarity(o1, o2));
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public <T extends NumberVector<?>> SimilarityQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new PrimitiveSimilarityQuery<>(database, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Sigma parameter: standard deviation.
     */
    public static final OptionID SIGMA_ID = new OptionID("kernel.laplace.sigma", "Standard deviation of the laplace RBF kernel.");

    /**
     * Sigma parameter
     */
    protected double sigma = 1.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter sigmaP = new DoubleParameter(SIGMA_ID, 1.);
      sigmaP.addConstraint(new GreaterConstraint(0.));
      if (config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }
    }

    @Override
    protected LaplaceKernelFunction makeInstance() {
      return new LaplaceKernelFunction(sigma);
    }
  }
}
