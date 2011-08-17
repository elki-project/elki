package de.lmu.ifi.dbs.elki.database.query;
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

/**
 * General interface for database queries.
 * Will only contain elemental stuff such as some hints.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public interface DatabaseQuery {
  /**
   * Optimizer hint: request bulk support.
   */
  public static final String HINT_BULK = "need_bulk";
  
  /**
   * Optimizer hint: only a single request will be done - avoid expensive optimizations
   */
  public static final String HINT_SINGLE = "single_query";
  
  /**
   * Optimizer hint: no linear scans allowed - return null then!
   */
  public static final String HINT_OPTIMIZED_ONLY = "optimized";
  
  /**
   * Optimizer hint: heavy use - caching/preprocessing/approximation recommended
   */
  public static final String HINT_HEAVY_USE = "heavy";
  
  /**
   * Optimizer hint: exact - no approximations allowed!
   */
  public static final String HINT_EXACT = "exact";
  
  /**
   * Optimizer hint: no cache instances
   */
  public static final String HINT_NO_CACHE = "no-cache";
}