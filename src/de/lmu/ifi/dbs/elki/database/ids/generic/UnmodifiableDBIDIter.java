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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Make an existing DBIDMIter unmodifiable.
 * 
 * @author Erich Schubert
 */
class UnmodifiableDBIDIter implements DBIDIter {
  /**
   * Wrapped iterator
   */
  private DBIDIter it;

  /**
   * Constructor.
   * 
   * @param it inner iterator
   */
  public UnmodifiableDBIDIter(DBIDIter it) {
    super();
    this.it = it;
  }

  @Override
  public boolean valid() {
    return it.valid();
  }

  @Override
  public void advance() {
    it.advance();
  }

  @Override
  public DBIDRef deref() {
    return it;
  }
}