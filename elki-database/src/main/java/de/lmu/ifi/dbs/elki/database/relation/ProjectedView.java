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

import de.lmu.ifi.dbs.elki.data.projection.Projection;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Projected relation view (non-materialized)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <IN> Vector type
 * @param <OUT> Vector type
 */
public class ProjectedView<IN, OUT> extends AbstractRelation<OUT> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ProjectedView.class);

  /**
   * The wrapped representation where we get the IDs from.
   */
  private final Relation<? extends IN> inner;

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
  public ProjectedView(Relation<? extends IN> inner, Projection<IN, OUT> projection) {
    super();
    this.inner = inner;
    this.projection = projection;
    projection.initialize(inner.getDataTypeInformation());
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
  public OUT get(DBIDRef id) {
    return projection.project(inner.get(id));
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

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}