package de.lmu.ifi.dbs.elki.database.ids.generic;
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

import java.util.Collection;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * This should only be instantiated by a {@link de.lmu.ifi.dbs.elki.database.ids.DBIDFactory}!
 * 
 * Use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newTreeSet}!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.ids.DBID
 */
// TODO: implement this optimized for integers?
public class GenericTreeSetModifiableDBIDs extends TreeSet<DBID> implements TreeSetModifiableDBIDs {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public GenericTreeSetModifiableDBIDs(int initialCapacity) {
    super();
  }

  /**
   * Constructor without extra hints
   */
  public GenericTreeSetModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public GenericTreeSetModifiableDBIDs(DBIDs c) {
    super(c.asCollection());
  }

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    return super.addAll(ids.asCollection());
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    return super.removeAll(ids.asCollection());
  }
}
