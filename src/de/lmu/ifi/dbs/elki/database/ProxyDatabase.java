package de.lmu.ifi.dbs.elki.database;

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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * A proxy database to use e.g. for projections and partitions.
 * 
 * @author Erich Schubert
 */
public class ProxyDatabase extends AbstractDatabase {
  /**
   * Logger class.
   */
  private static final Logging logger = Logging.getLogger(ProxyDatabase.class);

  /**
   * Our DBIDs
   */
  final protected DBIDs ids;

  /**
   * Our DBID representation
   */
  final protected DBIDView idrep;

  /**
   * Constructor.
   * 
   * @param ids DBIDs to use
   */
  public ProxyDatabase(DBIDs ids) {
    super();
    this.ids = ids;
    this.idrep = new DBIDView(this, this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);
  }

  /**
   * Constructor.
   * 
   * @param ids DBIDs to use
   * @param relations Relations to contain
   */
  public ProxyDatabase(DBIDs ids, Iterable<Relation<?>> relations) {
    super();
    this.ids = ids;
    this.idrep = new DBIDView(this, this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);
    for(Relation<?> orel : relations) {
      Relation<?> relation = ProxyView.wrap(this, ids, orel);
      this.relations.add(relation);
      this.addChildResult(relation);
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

  @Deprecated
  @Override
  public int size() {
    return ids.size();
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}