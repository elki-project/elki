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
package elki.database;

import java.util.Arrays;

import elki.database.ids.DBIDs;
import elki.database.relation.DBIDView;
import elki.database.relation.ProxyView;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.result.Metadata;

/**
 * A proxy database to use e.g. for projections and partitions.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class ProxyDatabase extends AbstractDatabase {
  /**
   * Logger class.
   */
  private static final Logging LOG = Logging.getLogger(ProxyDatabase.class);

  /**
   * Our DBID representation
   */
  protected DBIDView idrep;

  /**
   * Constructor.
   *
   * @param ids DBIDs to use
   */
  public ProxyDatabase(DBIDs ids) {
    super();
    this.idrep = new DBIDView(ids);
    this.relations.add(idrep);
    Metadata.hierarchyOf(this).addChild(idrep);
  }

  /**
   * Constructor.
   *
   * @param ids DBIDs to use
   * @param relations Relations to contain
   */
  public ProxyDatabase(DBIDs ids, Iterable<Relation<?>> relations) {
    super();
    this.idrep = new DBIDView(ids);
    this.relations.add(idrep);
    Metadata.hierarchyOf(this).addChild(idrep);
    for (Relation<?> orel : relations) {
      Relation<?> relation = new ProxyView<>(ids, orel);
      this.relations.add(relation);
      Metadata.hierarchyOf(this).addChild(relation);
    }
  }

  /**
   * Constructor.
   *
   * @param ids DBIDs to use
   * @param relations Relations to contain
   */
  public ProxyDatabase(DBIDs ids, Relation<?>... relations) {
    this(ids, Arrays.asList(relations));
  }

  /**
   * Constructor, proxying all relations of an existing database.
   *
   * @param ids ids to proxy
   * @param database Database to wrap
   */
  public ProxyDatabase(DBIDs ids, Database database) {
    this(ids, database.getRelations());
  }

  @Override
  public void initialize() {
    // Nothing to do - we were initialized on construction time.
  }

  /**
   * Add a new representation.
   *
   * @param relation Representation to add.
   */
  public void addRelation(Relation<?> relation) {
    this.relations.add(relation);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Set the DBIDs to use.
   *
   * @param ids DBIDs to use
   */
  public void setDBIDs(DBIDs ids) {
    this.idrep.setDBIDs(ids);
    // Update relations.
    for (Relation<?> orel : this.relations) {
      if (orel instanceof ProxyView) {
        ((ProxyView<?>) orel).setDBIDs(this.idrep.getDBIDs());
      }
    }
  }
}
