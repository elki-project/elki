package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;
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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Vector factory for uncertain objects.
 *
 * @author Erich Schubert
 *
 * @param <UO> Object type
 */
public interface Uncertainifier<UO extends UncertainObject> {
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
   * @param array Array
   * @param adapter Array type adapter
   * @param <A> Array type
   * @return Uncertain object
   */
  <A> UO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter);

  /**
   * Get the vector factory used for type information and serialization (if
   * supported).
   *
   * @return Vector factory.
   */
  FeatureVector.Factory<UO, ?> getFactory();
}