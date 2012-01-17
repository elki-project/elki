package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

/**
 * Represents an entry in a leaf node of a MkCoP-Tree. Additionally to an
 * MTreeLeafEntry an MkCoPLeafEntry holds the conservative and progressive
 * approximation of its knn-distances.
 * 
 * @author Elke Achtert
 */
class MkCoPLeafEntry<D extends NumberDistance<D, ?>> extends MTreeLeafEntry<D> implements MkCoPEntry<D> {
  private static final long serialVersionUID = 1;

  /**
   * The conservative approximation.
   */
  private ApproximationLine conservativeApproximation;

  /**
   * The progressive approximation.
   */
  private ApproximationLine progressiveApproximation;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkCoPLeafEntry() {
    // empty constructor
  }

  /**
   * Provides a new MkCoPLeafEntry with the given parameters.
   * 
   * @param objectID the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its
   *        parent's routing object
   * @param conservativeApproximation the conservative approximation of the knn
   *        distances
   * @param progressiveApproximation the progressive approximation of the knn
   *        distances
   */
  public MkCoPLeafEntry(DBID objectID, D parentDistance, ApproximationLine conservativeApproximation, ApproximationLine progressiveApproximation) {
    super(objectID, parentDistance);
    this.conservativeApproximation = conservativeApproximation;
    this.progressiveApproximation = progressiveApproximation;
  }

  /**
   * Returns the conservative approximated knn distance of the entry.
   * 
   * @param k the parameter k of the knn distance
   * @param distanceFunction the distance function
   * @return the conservative approximated knn distance of the entry
   */
  @Override
  public <O> D approximateConservativeKnnDistance(int k, DistanceQuery<O, D> distanceFunction) {
    return conservativeApproximation.getApproximatedKnnDistance(k, distanceFunction);
  }

  /**
   * Returns the progressive approximated knn distance of the entry.
   * 
   * @param <O> Object type
   * @param k the parameter k of the knn distance
   * @param distanceFunction the distance function
   * @return the progressive approximated knn distance of the entry
   */
  public <O> D approximateProgressiveKnnDistance(int k, DistanceQuery<O, D> distanceFunction) {
    return progressiveApproximation.getApproximatedKnnDistance(k, distanceFunction);
  }

  /**
   * Returns the conservative approximation line.
   * 
   * @return the conservative approximation line
   */
  @Override
  public ApproximationLine getConservativeKnnDistanceApproximation() {
    return conservativeApproximation;
  }

  /**
   * Returns the progressive approximation line.
   * 
   * @return the progressive approximation line
   */
  public ApproximationLine getProgressiveKnnDistanceApproximation() {
    return progressiveApproximation;
  }

  /**
   * Sets the conservative approximation line
   * 
   * @param conservativeApproximation the conservative approximation line to be
   *        set
   */
  @Override
  public void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation) {
    this.conservativeApproximation = conservativeApproximation;
  }

  /**
   * Sets the progressive approximation line
   * 
   * @param progressiveApproximation the progressive approximation line to be
   *        set
   */
  public void setProgressiveKnnDistanceApproximation(ApproximationLine progressiveApproximation) {
    this.progressiveApproximation = progressiveApproximation;
  }

  /**
   * Calls the super method and writes the conservative and progressive
   * approximation of the knn distances of this entry to the specified stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(conservativeApproximation);
    out.writeObject(progressiveApproximation);
  }

  /**
   * Calls the super method and reads the the conservative and progressive
   * approximation of the knn distances of this entry from the specified input
   * stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    conservativeApproximation = (ApproximationLine) in.readObject();
    progressiveApproximation = (ApproximationLine) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if the super method returns true and o is an MkCoPLeafEntry
   *         and has the same conservative and progressive approximation as this
   *         entry.
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }
    if(!super.equals(o)) {
      return false;
    }

    final MkCoPLeafEntry<D> that = (MkCoPLeafEntry<D>) o;

    if(conservativeApproximation != null ? !conservativeApproximation.equals(that.conservativeApproximation) : that.conservativeApproximation != null) {
      return false;
    }

    return !(progressiveApproximation != null ? !progressiveApproximation.equals(that.progressiveApproximation) : that.progressiveApproximation != null);
  }

  /**
   * Returns a string representation of this entry.
   * 
   * @return a string representation of this entry
   */
  @Override
  public String toString() {
    return super.toString() + "\ncons " + conservativeApproximation + "\n";
    // "prog " + progressiveApproximation;
  }
}