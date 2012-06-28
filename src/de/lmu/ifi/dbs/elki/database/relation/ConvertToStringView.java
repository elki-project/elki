package de.lmu.ifi.dbs.elki.database.relation;

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
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;

/**
 * Representation adapter that uses toString() to produce a string
 * representation.
 * 
 * @author Erich Schubert
 */
public class ConvertToStringView extends AbstractHierarchicalResult implements Relation<String> {
  /**
   * The database we use
   */
  final Relation<?> existing;

  /**
   * Constructor.
   * 
   * @param existing Existing representation
   */
  public ConvertToStringView(Relation<?> existing) {
    super();
    this.existing = existing;
  }

  @Override
  public Database getDatabase() {
    return existing.getDatabase();
  }

  @Override
  public String get(DBIDRef id) {
    return existing.get(id).toString();
  }

  @Override
  public void set(DBIDRef id, String val) {
    throw new UnsupportedOperationException("Covnersion representations are not writable!");
  }

  @Override
  public void delete(DBIDRef id) {
    throw new UnsupportedOperationException("Covnersion representations are not writable!");
  }

  @Override
  public DBIDs getDBIDs() {
    return existing.getDBIDs();
  }

  @Override
  public DBIDIter iterDBIDs() {
    return existing.iterDBIDs();
  }

  @Override
  public int size() {
    return existing.size();
  }

  @Override
  public SimpleTypeInformation<String> getDataTypeInformation() {
    return TypeUtil.STRING;
  }

  @Override
  public String getLongName() {
    return "toString(" + existing.getLongName() + ")";
  }

  @Override
  public String getShortName() {
    return "tostring-" + existing.getShortName();
  }
}