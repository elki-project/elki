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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.util.SplitHistory;

/**
 * Directory entry of a x-tree.
 *
 * @author Marisa Thoma
 * @since 0.7.5
 */
public class XTreeDirectoryEntry extends SpatialDirectoryEntry implements SplitHistorySpatialEntry {
  /**
   * The split history of this entry. Should be set via {@link #splitHistory}
   * and only afterwards queried by {@link #getSplitHistory()} or extended by
   * {@link #addSplitDimension(int)}. Only used if this {@link XTreeDirectoryEntry}
   * does <em>not</em> approximate a leaf node. If it does, all dimensions of
   * the parent node of <code>this</code> will be examined as potential split
   * axes, regardless of any former splits.
   */
  private SplitHistory splitHistory = null;

  public XTreeDirectoryEntry() {
    super();
  }

  public XTreeDirectoryEntry(int id, ModifiableHyperBoundingBox mbr) {
    super(id, mbr);
    if(mbr != null) {
      splitHistory = new SplitHistory(mbr.getDimensionality());
    }
  }

  @Override
  public void addSplitDimension(int dimension) {
    splitHistory.setDim(dimension);
  }

  @Override
  public SplitHistory getSplitHistory() {
    return splitHistory;
  }

  @Override
  public void setSplitHistory(SplitHistory splitHistory) {
    this.splitHistory = splitHistory;
  }

  /**
   * Calls the super method and writes the MBR object of this entry to the
   * specified output stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    splitHistory.writeExternal(out);
  }

  /**
   * Calls the super method and reads the MBR object of this entry from the
   * specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.splitHistory = SplitHistory.readExternal(in);
  }
}
