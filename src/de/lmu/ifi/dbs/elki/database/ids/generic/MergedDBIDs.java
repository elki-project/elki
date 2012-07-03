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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Merge the IDs of multiple layers into one.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.ids.DBIDs
 */
// TODO: include ID mapping?
public class MergedDBIDs implements DBIDs {
  /**
   * Childs to merge
   */
  DBIDs childs[];

  /**
   * Constructor.
   * 
   * @param childs
   */
  public MergedDBIDs(DBIDs... childs) {
    super();
    this.childs = childs;
  }

  @Override
  public DBIDIter iter() {
    throw new AbortException("Merged iterators not completely implemented yet!");
  }

  @Override
  public int size() {
    int si = 0;
    for(DBIDs child : childs) {
      si += child.size();
    }
    return si;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean contains(DBIDRef o) {
    for(DBIDs child : childs) {
      if(child.contains(o)) {
        return true;
      }
    }
    return false;
  }
}