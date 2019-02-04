/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Pseudo-representation that is the object ID itself.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class DBIDView extends AbstractRelation<DBID> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(DBIDView.class);

  /**
   * The ids object
   */
  private DBIDs ids;

  /**
   * Constructor.
   *
   * @param ids DBIDs
   */
  public DBIDView(DBIDs ids) {
    super();
    this.ids = DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  public DBID get(DBIDRef id) {
    assert(ids.contains(id));
    return DBIDUtil.deref(id);
  }

  @Override
  public SimpleTypeInformation<DBID> getDataTypeInformation() {
    return TypeUtil.DBID;
  }

  @Override
  public DBIDs getDBIDs() {
    return ids;
  }

  @Override
  public DBIDIter iterDBIDs() {
    return ids.iter();
  }

  /**
   * Set the DBIDs of the view.
   *
   * @param ids IDs to use
   */
  public void setDBIDs(DBIDs ids) {
    this.ids = DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public String getLongName() {
    return "Database IDs";
  }

  @Override
  public String getShortName() {
    return "DBID";
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
