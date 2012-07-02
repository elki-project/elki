package de.lmu.ifi.dbs.elki.database.ids.generic;

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

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Iterator for classic collections.
 * 
 * @author Erich Schubert
 */
public class DBIDIterAdapter implements DBIDMIter {
  /**
   * Current DBID
   */
  DBID cur = null;

  /**
   * The real iterator
   */
  Iterator<DBID> iter;

  /**
   * Constructor.
   * 
   * @param iter Iterator
   */
  public DBIDIterAdapter(Iterator<DBID> iter) {
    super();
    this.iter = iter;
    advance();
  }

  @Override
  public boolean valid() {
    return cur != null;
  }

  @Override
  public void advance() {
    if(iter.hasNext()) {
      cur = iter.next();
    }
    else {
      cur = null;
    }
  }

  @Override
  public int getIntegerID() {
    return DBIDFactory.FACTORY.asInteger(cur);
  }

  @Override
  public DBID getDBID() {
    return cur;
  }

  @Override
  public void remove() {
    iter.remove();
  }

  @Override
  public boolean sameDBID(DBIDRef other) {
    return DBIDFactory.FACTORY.equal(cur, other);
  }

  @Override
  public int compareDBID(DBIDRef o) {
    return DBIDFactory.FACTORY.compare(cur, o);
  }
}