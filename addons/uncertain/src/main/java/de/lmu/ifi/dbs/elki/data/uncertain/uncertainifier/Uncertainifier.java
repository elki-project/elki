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
package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Class to derive uncertain object from exact vectors.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <UO> Object type
 *
 * @navhas - produces - UncertainObject
 * @navassoc - reads - NumberArrayAdapter
 */
public interface Uncertainifier<UO extends UncertainObject> {
  /**
   * Shared parameter: to force centering the uncertain region on the exact
   * vector.
   */
  OptionID SYMMETRIC_ID = new OptionID("uo.symmetric", "Generate a symetric uncertain region, centered around the exact data.");

  /**
   * Generate a new uncertain object. This interface is specialized to numerical
   * arrays.
   *
   * The generics allow the use with primitive {@code double[]} arrays:
   *
   * <pre>
   * UO obj = newFeatureVector(array, ArrayLikeUtil.DOUBLEARRAYADAPTER);
   * </pre>
   *
   * @param rand Random generator
   * @param array Array
   * @param adapter Array type adapter
   * @param <A> Array type
   * @return Uncertain object
   */
  <A> UO newFeatureVector(Random rand, A array, NumberArrayAdapter<?, A> adapter);

  /**
   * Get the vector factory used for type information and serialization (if
   * supported).
   *
   * @return Vector factory.
   */
  FeatureVector.Factory<UO, ?> getFactory();
}