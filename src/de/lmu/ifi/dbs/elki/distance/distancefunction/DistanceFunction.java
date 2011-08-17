package de.lmu.ifi.dbs.elki.distance.distancefunction;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Base interface for any kind of distances.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance result type
 * 
 * @apiviz.landmark
 * @apiviz.has Distance
 */
public interface DistanceFunction<O, D extends Distance<?>> extends Parameterizable {
  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  D getDistanceFactory();

  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();

  /**
   * Is this distance function metric (in particular, does it satisfy the
   * triangle equation?)
   * 
   * @return {@code true} when metric.
   */
  boolean isMetric();

  /**
   * Get the input data type of the function.
   * 
   * @return Type restriction
   */
  TypeInformation getInputTypeRestriction();

  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param relation The representation to use
   * @return Actual distance query.
   */
  public <T extends O> DistanceQuery<T, D> instantiate(Relation<T> relation);
}