package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Abstract base class for typical distance functions that allow
 * rectangle-to-rectangle lower bounds. These distances can then be used with
 * R*-trees for acceleration.
 * 
 * This class is largely a convenience class, to make implementing this type of
 * distance functions easier.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.landmark
 */
public abstract class AbstractSpatialNorm extends AbstractNumberVectorNorm implements SpatialPrimitiveDistanceFunction<NumberVector> {
  @Override
  public <T extends NumberVector> SpatialPrimitiveDistanceQuery<T> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<>(relation, this);
  }
}
