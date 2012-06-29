package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractDistanceFunction provides some methods valid for any extending class.
 * 
 * @author Arthur Zimek
 * 
 * @param <D> the type of Distance used
 */
public abstract class AbstractDBIDDistanceFunction<D extends Distance<D>> implements DBIDDistanceFunction<D> {
  /**
   * Provides an abstract DistanceFunction.
   */
  protected AbstractDBIDDistanceFunction() {
    // Empty
  }

  @Override
  abstract public D distance(DBIDRef o1, DBIDRef o2);

  @Override
  abstract public D getDistanceFactory();

  @Override
  public boolean isSymmetric() {
    // Assume symmetric by default!
    return true;
  }

  @Override
  public boolean isMetric() {
    // Do NOT assume triangle equation by default!
    return false;
  }

  @Override
  public SimpleTypeInformation<DBID> getInputTypeRestriction() {
    return TypeUtil.DBID;
  }

  @SuppressWarnings("unchecked")
  @Override
  final public <O extends DBID> DistanceQuery<O, D> instantiate(Relation<O> database) {
    return (DistanceQuery<O, D>) new DBIDDistanceQuery<D>((Relation<DBID>) database, this);
  }
}