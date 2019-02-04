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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

/**
 * Represents an entry in a leaf node of a MkCoP-Tree. Additionally to an
 * MTreeLeafEntry an MkCoPLeafEntry holds the conservative and progressive
 * approximation of its knn-distances.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
class MkCoPLeafEntry extends MTreeLeafEntry implements MkCoPEntry {
  /**
   * Serialization version ID.
   */
  private static final long serialVersionUID = 2;

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
   * Constructor.
   * 
   * @param objectID the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its
   *        parent's routing object
   * @param conservativeApproximation the conservative approximation of the knn
   *        distances
   * @param progressiveApproximation the progressive approximation of the knn
   *        distances
   */
  public MkCoPLeafEntry(DBID objectID, double parentDistance, ApproximationLine conservativeApproximation, ApproximationLine progressiveApproximation) {
    super(objectID, parentDistance);
    this.conservativeApproximation = conservativeApproximation;
    this.progressiveApproximation = progressiveApproximation;
  }

  /**
   * Returns the conservative approximated knn distance of the entry.
   * 
   * @param k the parameter k of the knn distance
   * @return the conservative approximated knn distance of the entry
   */
  @Override
  public double approximateConservativeKnnDistance(int k) {
    return conservativeApproximation.getApproximatedKnnDistance(k);
  }

  /**
   * Returns the progressive approximated knn distance of the entry.
   * 
   * @param k the parameter k of the knn distance
   * @return the progressive approximated knn distance of the entry
   */
  public double approximateProgressiveKnnDistance(int k) {
    return progressiveApproximation.getApproximatedKnnDistance(k);
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

  @Override
  public String toString() {
    return super.toString() + "\ncons " + conservativeApproximation + "\n";
    // "prog " + progressiveApproximation;
  }
}
