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

import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDatabaseDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a database context.
 * 
 * @author Elke Achtert
 * 
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 */
public abstract class AbstractDatabaseDistanceFunction<O, D extends Distance<D>> implements DistanceFunction<O, D> {
  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   */
  public AbstractDatabaseDistanceFunction() {
    super();
  }

  @Override
  abstract public D getDistanceFactory();

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  abstract public static class Instance<O, D extends Distance<D>> extends AbstractDatabaseDistanceQuery<O, D> {
    /**
     * Parent distance
     */
    DistanceFunction<? super O, D> parent;
    
    /**
     * Constructor.
     * 
     * @param database Database
     * @param parent Parent distance
     */
    public Instance(Relation<O> database, DistanceFunction<? super O, D> parent) {
      super(database);
      this.parent = parent;
    }

    @Override
    public DistanceFunction<? super O, D> getDistanceFunction() {
      return parent;
    }
  }
}