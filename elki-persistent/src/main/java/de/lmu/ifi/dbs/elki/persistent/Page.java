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
package de.lmu.ifi.dbs.elki.persistent;

/**
 * Defines the requirements for objects that can be stored in a cache and can be
 * persistently saved.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 */
public interface Page {
  /**
   * Returns the unique id of this Page.
   * 
   * @return the unique id of this Page. Return -1 for unassigned page numbers.
   */
  int getPageID();

  /**
   * Sets the unique id of this Page.
   * 
   * @param id the id to be set
   */
  void setPageID(int id);

  /**
   * Returns true if this page is dirty, false otherwise.
   * 
   * @return true if this page is dirty, false otherwise
   */
  boolean isDirty();

  /**
   * Sets the dirty flag of this page.
   * 
   * @param dirty the dirty flag to be set
   */
  void setDirty(boolean dirty);
}