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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * A virtual partitioning of the database. For the accepted DBIDs, access is
 * passed on to the wrapped representation.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Object type
 */
public class ProxyView<O> extends AbstractRelation<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ProxyView.class);

  /**
   * The DBIDs we contain
   */
  private DBIDs idview;

  /**
   * The wrapped representation where we get the IDs from.
   */
  private final Relation<O> inner;

  /**
   * Constructor.
   *
   * @param idview Ids to expose
   * @param inner Inner representation
   */
  public ProxyView(DBIDs idview, Relation<O> inner) {
    super();
    this.idview = DBIDUtil.makeUnmodifiable(idview);
    this.inner = inner;
  }

  @Override
  public O get(DBIDRef id) {
    assert(idview.contains(id)) : "Accessing object not included in view.";
    return inner.get(id);
  }

  @Override
  public DBIDs getDBIDs() {
    return idview;
  }

  @Override
  public DBIDIter iterDBIDs() {
    return idview.iter();
  }

  @Override
  public int size() {
    return idview.size();
  }

  @Override
  public SimpleTypeInformation<O> getDataTypeInformation() {
    return inner.getDataTypeInformation();
  }

  @Override
  public String getLongName() {
    return "Partition of " + inner.getLongName();
  }

  @Override
  public String getShortName() {
    return "partition";
  }

  /**
   * Set the DBIDs to use.
   *
   * @param ids DBIDs
   */
  public void setDBIDs(DBIDs ids) {
    this.idview = ids;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
