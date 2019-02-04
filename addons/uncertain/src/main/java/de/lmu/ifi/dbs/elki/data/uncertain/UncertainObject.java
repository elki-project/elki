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
package de.lmu.ifi.dbs.elki.data.uncertain;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Interface for uncertain objects.
 *
 * Uncertain objects <em>must</em> provide a bounding box in this model, and
 * have an option to randomly sample from the data.
 *
 * TODO: Eventually allow float precision, too.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navhas - contains/produces - DoubleVector
 */
public interface UncertainObject extends SpatialComparable, FeatureVector<Double> {
  /**
   * Uncertain objects.
   */
  VectorFieldTypeInformation<UncertainObject> UNCERTAIN_OBJECT_FIELD = VectorFieldTypeInformation.typeRequest(UncertainObject.class);

  /**
   * Uncertain objects.
   */
  VectorFieldTypeInformation<DiscreteUncertainObject> DISCRETE_UNCERTAIN_OBJECT = VectorFieldTypeInformation.typeRequest(DiscreteUncertainObject.class);

  /**
   * Draw a random sampled instance.
   *
   * @param rand Random generator
   * @return Sampled object.
   */
  DoubleVector drawSample(Random rand);

  /**
   * Get the center of mass of the uncertain object.
   *
   * @return Center of mass.
   */
  DoubleVector getCenterOfMass();
}
