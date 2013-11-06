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
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractPrimitiveSimilarityFunction;

/**
 * Provides a linear Kernel function that computes a similarity between the two
 * feature vectors V1 and V2 defined by V1^T*V2.
 * 
 * @author Simon Paradies
 * @param <O> vector type
 */
public class LinearKernelFunction<O extends NumberVector<?>> extends AbstractPrimitiveSimilarityFunction<O, DoubleDistance> implements PrimitiveDoubleDistanceFunction<O> {
  /**
   * Provides a linear Kernel function that computes a similarity between the
   * two vectors V1 and V2 defined by V1^T*V2.
   */
  public LinearKernelFunction() {
    super();
  }

  @Override
  public DoubleDistance similarity(final O o1, final O o2) {
    return new DoubleDistance(doubleSimilarity(o1, o2));
  }

  protected double doubleSimilarity(final O o1, final O o2) {
    if (o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }
    double sim = 0;
    for (int i = 0; i < o1.getDimensionality(); i++) {
      sim += o1.doubleValue(i) * o2.doubleValue(i);
    }
    return sim;
  }

  @Override
  public double doubleDistance(final O fv1, final O fv2) {
    return Math.sqrt(doubleSimilarity(fv1, fv1) + doubleSimilarity(fv2, fv2) - 2 * doubleSimilarity(fv1, fv2));
  }

  @Override
  public DoubleDistance distance(final O fv1, final O fv2) {
    return new DoubleDistance(doubleDistance(fv1, fv2));
  }

  @Override
  public VectorFieldTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public <T extends O> DistanceSimilarityQuery<T, DoubleDistance> instantiate(Relation<T> database) {
    return new PrimitiveDistanceSimilarityQuery<>(database, this, this);
  }
}
