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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkapp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

/**
 * Represents an entry in a leaf node of a MkApp-Tree. Additionally to an
 * MTreeLeafEntry an MkAppLeafEntry holds the polynomial approximation of its
 * knn-distances.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
class MkAppLeafEntry extends MTreeLeafEntry implements MkAppEntry {
  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 2;

  /**
   * The polynomial approximation.
   */
  private PolynomialApproximation approximation;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkAppLeafEntry() {
    // empty constructor
  }

  /**
   * Constructor.
   * 
   * @param objectID the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its
   *        parent's routing object
   * @param approximation the polynomial approximation of the knn distances
   */
  public MkAppLeafEntry(DBID objectID, double parentDistance, PolynomialApproximation approximation) {
    super(objectID, parentDistance);
    this.approximation = approximation;
  }

  /**
   * Returns the approximated value at the specified k.
   * 
   * @param k the parameter k of the knn distance
   * @return the approximated value at the specified k
   */
  @Override
  public double approximatedValueAt(int k) {
    return approximation.getValueAt(k);
  }

  /**
   * Returns the polynomial approximation.
   * 
   * @return the polynomial approximation
   */
  @Override
  public PolynomialApproximation getKnnDistanceApproximation() {
    return approximation;
  }

  /**
   * Sets the polynomial approximation.
   * 
   * @param approximation the polynomial approximation to be set
   */
  @Override
  public void setKnnDistanceApproximation(PolynomialApproximation approximation) {
    this.approximation = approximation;
  }

  /**
   * Calls the super method and writes the polynomiale approximation of the knn
   * distances of this entry to the specified stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(approximation);
  }

  /**
   * Calls the super method and reads the the polynomial approximation of the
   * knn distances of this entry from the specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    approximation = (PolynomialApproximation) in.readObject();
  }

  /**
   * Returns a string representation of this entry.
   * 
   * @return a string representation of this entry
   */
  @Override
  public String toString() {
    return super.toString() + "\napprox " + approximation;
  }
}
