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
package de.lmu.ifi.dbs.elki.result;

/**
 * Abstract class for a result object with hierarchy
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractHierarchicalResult implements HierarchicalResult {
  /**
   * The hierarchy storage.
   */
  private ResultHierarchy hierarchy;

  /**
   * Constructor.
   */
  public AbstractHierarchicalResult() {
    super();
    this.hierarchy = new ResultHierarchy();
  }

  @Override
  public final ResultHierarchy getHierarchy() {
    return hierarchy;
  }

  @Override
  public final void setHierarchy(ResultHierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  /**
   * Add a child result.
   * 
   * @param child Child result
   */
  public void addChildResult(Result child) {
    hierarchy.add(this, child);
  }
}