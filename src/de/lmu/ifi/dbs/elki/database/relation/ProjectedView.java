package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.projection.Projection;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;

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

/**
 * Projected relation view (non-materialized)
 * 
 * @author Erich Schubert
 * 
 * @param <IN> Vector type
 * @param <OUT> Vector type
 */
public class ProjectedView<IN, OUT> extends AbstractHierarchicalResult implements Relation<OUT> {
  /**
   * The wrapped representation where we get the IDs from.
   */
  private final Relation<IN> inner;

  /**
   * The projection we use.
   */
  private Projection<IN, OUT> projection;

  /**
   * Constructor.
   * 
   * @param inner Inner relation
   * @param projection Projection function
   */
  public ProjectedView(Relation<IN> inner, Projection<IN, OUT> projection) {
    super();
    this.inner = inner;
    this.projection = projection;
  }

  @Override
  public String getLongName() {
    return "projection";
  }

  @Override
  public String getShortName() {
    return "projection";
  }

  @Override
  public Database getDatabase() {
    return inner.getDatabase();
  }

  @Override
  public OUT get(DBID id) {
    return projection.project(inner.get(id));
  }

  @Override
  public OUT get(DBIDIter id) {
    return projection.project(inner.get(id));
  }

  @Override
  public void set(DBID id, OUT val) {
    throw new UnsupportedOperationException("Projections are read-only.");
  }

  @Override
  public void delete(DBID id) {
    inner.delete(id);
  }

  @Override
  public SimpleTypeInformation<OUT> getDataTypeInformation() {
    return projection.getOutputDataTypeInformation();
  }

  @Override
  public DBIDs getDBIDs() {
    return inner.getDBIDs();
  }

  @Override
  public DBIDIter iterDBIDs() {
    return inner.iterDBIDs();
  }

  @Override
  public int size() {
    return inner.size();
  }
}