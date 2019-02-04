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
 * Result with an internal hierarchy.
 * 
 * Note: while this often seems a bit clumsy to use, the benefit of having this
 * delegate is to keep the APIs simpler, and thus make ELKI development easier.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - ResultHierarchy
 */
public interface HierarchicalResult extends Result {
  /**
   * Get the objects current hierarchy - may be {@code null}!
   * 
   * @return current hierarchy. May be {@code null}!
   */
  ResultHierarchy getHierarchy();

  /**
   * Set (exchange) the hierarchy implementation (e.g. after merging!)
   * 
   * @param hierarchy New hierarchy
   */
  void setHierarchy(ResultHierarchy hierarchy);
}
