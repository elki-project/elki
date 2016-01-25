package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Cluster order entry for correlation-based OPTICS variants.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.7.0
 */
public class CorrelationClusterOrder extends ClusterOrder {
  /**
   * The component separator used by correlation distances.
   * 
   * Note: Do NOT use regular expression syntax characters!
   */
  public static final String SEPARATOR = "x";

  /**
   * The correlation dimension.
   */
  protected WritableIntegerDataStore correlationValue;

  /**
   * Constructor.
   *
   * @param ids Cluster order
   * @param name Result name
   * @param shortname Short result name
   * @param reachability Reachability
   * @param predecessor Predecessor (may be {@code null})
   * @param corrdim Correlation dimensionality
   */
  public CorrelationClusterOrder(String name, String shortname, ArrayModifiableDBIDs ids, WritableDoubleDataStore reachability, WritableDBIDDataStore predecessor, WritableIntegerDataStore corrdim) {
    super(name, shortname, ids, reachability, predecessor);
    this.correlationValue = corrdim;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * NOTE: for the use in an UpdatableHeap, only the ID is used! This is
   * important, otherwise OPTICS will not work.
   * 
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object has the same attribute values as
   *         the o argument; <code>false</code> otherwise.
   */
  /*
   * @Override public final boolean equals(Object o) { if(this == o) { return
   * true; } if(!(o instanceof ClusterOrderEntry)) { return false; }
   * 
   * final ClusterOrderEntry<?> that = (ClusterOrderEntry<?>) o; // Compare by
   * ID only, for UpdatableHeap! return DBIDUtil.equal(objectID, that.getID());
   * }
   */

  /*
   * @Override public int compareTo(SELF other) { if(this.correlationValue <
   * other.correlationValue) { return -1; } if(this.correlationValue >
   * other.correlationValue) { return +1; } return
   * Double.compare(this.euclideanValue, other.euclideanValue); }
   */

  /**
   * Get the correlation dimensionality.
   * 
   * @return Correlation dimensionality
   */
  public int getCorrelationValue(DBIDRef id) {
    return correlationValue.intValue(id);
  }

  /**
   * Get the Euclidean distance in the orthogonal space.
   * 
   * @return Euclidean distance
   */
  public double getEuclideanValue(DBIDRef id) {
    return reachability.doubleValue(id);
  }

  /*
   * @Override public void writeToText(TextWriterStream out, String label) {
   * out.inlinePrint("predecessor=" + DBIDUtil.toString((DBIDRef)
   * predecessorID)); out.inlinePrint("reach-dim=" + correlationValue);
   * out.inlinePrint("reachability=" + euclideanValue); }
   */
}
