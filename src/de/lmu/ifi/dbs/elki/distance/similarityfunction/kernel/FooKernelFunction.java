package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides an experimental KernelDistanceFunction for NumberVectors. Currently
 * only supports 2D data and x1^2 ~ x2 correlations.
 * 
 * @author Simon Paradies
 */
public class FooKernelFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements PrimitiveSimilarityFunction<NumberVector<?, ?>, DoubleDistance> {
  /**
   * The default max_degree.
   */
  public static final int DEFAULT_MAX_DEGREE = 2;

  /**
   * Parameter for the maximum degree
   */
  public static final OptionID MAX_DEGREE_ID = OptionID.getOrCreateOptionID("fookernel.max_degree", "The max degree of the" + FooKernelFunction.class.getSimpleName() + ". Default: " + DEFAULT_MAX_DEGREE);

  /**
   * Degree of the polynomial kernel function
   */
  private int max_degree;

  /**
   * Constructor.
   * 
   * @param max_degree Maximum degree-
   */
  public FooKernelFunction(int max_degree) {
    super();
    this.max_degree = max_degree;
  }

  /**
   * Provides an experimental kernel similarity between the given two vectors.
   * 
   * @param o1 first vector
   * @param o2 second vector
   * @return the experimental kernel similarity between the given two vectors as
   *         an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public DoubleDistance similarity(final NumberVector<?, ?> o1, final NumberVector<?, ?> o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }
    double sim = 0.0;
    // iterate over differently powered dimensions
    for(int degree = 1; degree <= max_degree; degree++) {
      sim += Math.pow(o1.doubleValue(degree) * o2.doubleValue(degree), degree);
    }
    return new DoubleDistance(sim);
  }

  @Override
  public DoubleDistance distance(final NumberVector<?, ?> fv1, final NumberVector<?, ?> fv2) {
    return new DoubleDistance(Math.sqrt(similarity(fv1, fv1).doubleValue() + similarity(fv2, fv2).doubleValue() - 2 * similarity(fv1, fv2).doubleValue()));
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public <T extends NumberVector<?, ?>> DistanceSimilarityQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new PrimitiveDistanceSimilarityQuery<T, DoubleDistance>(database, this, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected int max_degree = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter max_degreeP = new IntParameter(MAX_DEGREE_ID, DEFAULT_MAX_DEGREE);
      if(config.grab(max_degreeP)) {
        max_degree = max_degreeP.getValue();
      }
    }

    @Override
    protected FooKernelFunction makeInstance() {
      return new FooKernelFunction(max_degree);
    }
  }
}