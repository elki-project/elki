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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;


/**
 * Array-oriented implementation of a modifiable DBID collection.
 * 
 * This should only be instantiated by a {@link de.lmu.ifi.dbs.elki.database.ids.DBIDFactory}!
 * 
 * Use {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newArray}!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DBID
 */
public class GenericArrayModifiableDBIDs extends ArrayList<DBID> implements ArrayModifiableDBIDs  {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with size hint.
   * 
   * @param initialCapacity Size hint
   */
  public GenericArrayModifiableDBIDs(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructor without extra hints
   */
  public GenericArrayModifiableDBIDs() {
    super();
  }

  /**
   * Constructor from existing DBIDs.
   * 
   * @param c Existing DBIDs.
   */
  public GenericArrayModifiableDBIDs(DBIDs c) {
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

  @Override
  public void sort() {
    Collections.sort(this);
  }
}