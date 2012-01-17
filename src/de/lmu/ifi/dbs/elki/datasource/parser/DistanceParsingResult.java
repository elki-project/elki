package de.lmu.ifi.dbs.elki.datasource.parser;

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

import java.util.Map;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Provides a list of database objects and labels associated with these objects
 * and a cache of precomputed distances between the database objects.
 * 
 * @author Elke Achtert
 * 
 * @param <D> distance type
 */
public class DistanceParsingResult<D extends Distance<D>> {
  /**
   * The cache of precomputed distances between the database objects.
   */
  private final Map<DBIDPair, D> distanceCache;
  
  /**
   * Objects representation (DBIDs and/or external IDs)
   */
  private MultipleObjectsBundle objects;

  /**
   * Provides a list of database objects, a list of label objects associated
   * with these objects and cached distances between these objects.
   * 
   * @param objectAndLabelList the list of database objects and labels
   *        associated with these objects
   * @param distanceCache the cache of precomputed distances between the
   *        database objects
   */
  public DistanceParsingResult(MultipleObjectsBundle objectAndLabelList, Map<DBIDPair, D> distanceCache) {
    this.objects = objectAndLabelList;
    this.distanceCache = distanceCache;
  }

  /**
   * Returns the cache of precomputed distances between the database objects.
   * 
   * @return the cache of precomputed distances between the database objects
   */
  public Map<DBIDPair, D> getDistanceCache() {
    return distanceCache;
  }

  /**
   * Get the objects
   * 
   * @return the objects bundle
   */
  public MultipleObjectsBundle getObjects() {
    return objects;
  }
}