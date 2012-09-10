package de.lmu.ifi.dbs.elki.utilities.referencepoints;

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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;

/**
 * Strategy to use the complete database as reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type.
 */
public class FullDatabaseReferencePoints<O extends NumberVector<?>> implements ReferencePointsHeuristic<O> {
  /**
   * Constructor, Parameterizable style.
   */
  public FullDatabaseReferencePoints() {
    super();
  }

  @Override
  public <T extends O> Collection<O> getReferencePoints(Relation<T> db) {
    return new DatabaseUtil.CollectionFromRelation<O>(db);
  }
}