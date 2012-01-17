package de.lmu.ifi.dbs.elki.datasource.bundle;

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

/**
 * Abstract interface for object packages.
 * 
 * Shared API for both single-object and multi-object packages.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf BundleMeta
 */
public interface ObjectBundle {
  /**
   * Access the meta data.
   * 
   * @return metadata
   */
  public BundleMeta meta();

  /**
   * Access the meta data.
   * 
   * @param i component
   * @return metadata of component i
   */
  public SimpleTypeInformation<?> meta(int i);

  /**
   * Get the metadata length.
   * 
   * @return length of metadata
   */
  public int metaLength();

  /**
   * Get the number of objects contained.
   * 
   * @return Number of objects
   */
  public int dataLength();

  /**
   * Access a particular object and representation.
   * 
   * @param onum Object number
   * @param rnum Representation number
   * @return Contained data
   */
  public Object data(int onum, int rnum);
}