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
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractPrimitiveSimilarityFunction;

/**
 * Provides a linear Kernel function that computes a similarity between the two
 * feature vectors V1 and V2 defined by V1^T*V2.
 * 
 * @author Simon Paradies
 * @param <O> vector type
 */
public class LinearKernelFunction<O extends NumberVector<?>> extends AbstractPrimitiveSimilarityFunction<O, DoubleDistance> implements PrimitiveDistanceFunction<O, DoubleDistance> {
  /**
   * Provides a linear Kernel function that computes a similarity between the
   * two vectors V1 and V2 defined by V1^T*V2.
   */
  public LinearKernelFunction() {
    super();
  }

  /**
   * Provides a linear Kernel function that computes a similarity between the
   * two feature vectors V1 and V2 definded by V1^T*V2
   * 
   * @param o1 first feature vector
   * @param o2 second feature vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public DoubleDistance similarity(final O o1, final O o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }
    double sim = 0;
    for(int i = 1; i <= o1.getDimensionality(); i++) {
      sim += o1.doubleValue(i) * o2.doubleValue(i);
    }
    return new DoubleDistance(sim);
  }

  @Override
  public DoubleDistance distance(final O fv1, final O fv2) {
    return new DoubleDistance(Math.sqrt(similarity(fv1, fv1).doubleValue() + similarity(fv2, fv2).doubleValue() - 2 * similarity(fv1, fv2).doubleValue()));
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
    return new PrimitiveDistanceSimilarityQuery<T, DoubleDistance>(database, this, this);
  }
}