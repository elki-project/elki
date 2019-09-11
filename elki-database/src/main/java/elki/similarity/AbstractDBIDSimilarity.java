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
package elki.similarity;

import elki.database.ids.DBID;
import elki.database.relation.Relation;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 */
public abstract class AbstractDBIDSimilarity implements DBIDSimilarity {
  /**
   * The database we work on
   */
  protected Relation<? extends DBID> database;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDBIDSimilarity(Relation<? extends DBID> database) {
    super();
    this.database = database;
  }
}