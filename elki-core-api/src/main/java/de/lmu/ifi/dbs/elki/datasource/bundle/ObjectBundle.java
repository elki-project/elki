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
package de.lmu.ifi.dbs.elki.datasource.bundle;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;

/**
 * Abstract interface for object packages.
 * 
 * Shared API for both single-object and multi-object packages.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - BundleMeta
 */
public interface ObjectBundle {
  /**
   * Access the meta data.
   * 
   * @return metadata
   */
  BundleMeta meta();

  /**
   * Access the meta data.
   * 
   * @param i component
   * @return metadata of component i
   */
  SimpleTypeInformation<?> meta(int i);

  /**
   * Get the metadata length.
   * 
   * @return length of metadata
   */
  int metaLength();

  /**
   * Get the number of objects contained.
   * 
   * @return Number of objects
   */
  int dataLength();

  /**
   * Access a particular object and representation.
   * 
   * @param onum Object number
   * @param rnum Representation number
   * @return Contained data
   */
  Object data(int onum, int rnum);

  /**
   * Assign the object DBID to a variable
   * 
   * @param onum Object number
   * @param var Variable
   * @return {@code false} if there was no predefined DBID.
   */
  boolean assignDBID(int onum, DBIDVar var);
}