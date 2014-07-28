package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Cluster order entry for correlation-based OPTICS variants.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @param <SELF> Type self-reference
 */
public abstract class CorrelationClusterOrderEntry<SELF extends CorrelationClusterOrderEntry<SELF>> implements ClusterOrderEntry<SELF>, Comparable<SELF>, TextWriteable {
  /**
   * The component separator used by correlation distances.
   * 
   * Note: Do NOT use regular expression syntax characters!
   */
  public static final String SEPARATOR = "x";

  /**
   * The id of the entry.
   */
  protected DBID objectID;

  /**
   * The id of the entry's predecessor.
   */
  protected DBID predecessorID;

  /**
   * The correlation dimension.
   */
  protected int correlationValue;

  /**
   * The Euclidean distance in the subspace.
   */
  protected double euclideanValue;

  /**
   * Constructs a new CorrelationDistance object.
   * 
   * @param correlationValue the correlation dimension to be represented by the
   *        CorrelationDistance
   * @param euclideanValue the Euclidean distance to be represented by the
   *        CorrelationDistance
   */
  public CorrelationClusterOrderEntry(DBID objectID, DBID predecessorID, int correlationValue, double euclideanValue) {
    this.objectID = objectID;
    this.predecessorID = predecessorID;
    this.correlationValue = correlationValue;
    this.euclideanValue = euclideanValue;
  }

  @Override
  public String toString() {
    return Integer.toString(correlationValue) + SEPARATOR + Double.toString(euclideanValue);
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
  @Override
  public final boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(!(o instanceof ClusterOrderEntry)) {
      return false;
    }

    final ClusterOrderEntry<?> that = (ClusterOrderEntry<?>) o;
    // Compare by ID only, for UpdatableHeap!
    return DBIDUtil.equal(objectID, that.getID());
  }

  /**
   * Returns a hash code value for the object.
   * 
   * NOTE: for the use in an UpdatableHeap, only the ID is used! This is
   * important, otherwise OPTICS will not work.
   * 
   * @return the object id if this entry
   */
  @Override
  public final int hashCode() {
    return objectID.hashCode();
  }

  @Override
  public int compareTo(SELF other) {
    if(this.correlationValue < other.correlationValue) {
      return -1;
    }
    if(this.correlationValue > other.correlationValue) {
      return +1;
    }
    return Double.compare(this.euclideanValue, other.euclideanValue);
  }

  @Override
  public DBID getID() {
    return objectID;
  }

  @Override
  public DBID getPredecessorID() {
    return predecessorID;
  }

  /**
   * Get the correlation dimensionality.
   * 
   * @return Correlation dimensionality
   */
  public int getCorrelationValue() {
    return correlationValue;
  }

  /**
   * Get the Euclidean distance in the orthogonal space.
   * 
   * @return Euclidean distance
   */
  public double getEuclideanValue() {
    return euclideanValue;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    out.inlinePrint("predecessor=" + DBIDUtil.toString((DBIDRef) predecessorID));
    out.inlinePrint("reach-dim=" + correlationValue);
    out.inlinePrint("reachability=" + euclideanValue);
  }
}
