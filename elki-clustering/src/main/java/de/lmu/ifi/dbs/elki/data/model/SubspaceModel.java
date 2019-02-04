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
package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Model for Subspace Clusters.
 * 
 * @author Erich Schubert
 * @author Elke Achtert
 * @since 0.3
 * 
 * @composed - - - Subspace
 */
public class SubspaceModel extends MeanModel implements TextWriteable {
  /**
   * The subspace of the cluster.
   */
  private final Subspace subspace;

  /**
   * Creates a new SubspaceModel for the specified subspace with the given
   * cluster mean.
   * 
   * @param subspace the subspace of the cluster
   * @param mean the cluster mean
   */
  public SubspaceModel(Subspace subspace, double[] mean) {
    super(mean);
    this.subspace = subspace;
  }

  /**
   * Returns the subspace of this SubspaceModel.
   * 
   * @return the subspace
   */
  public Subspace getSubspace() {
    return subspace;
  }

  /**
   * Returns the BitSet that represents the dimensions of the subspace of this
   * SubspaceModel.
   * 
   * @return the dimensions of the subspace
   */
  public long[] getDimensions() {
    return subspace.getDimensions();
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Subspace: " + subspace.toString());
  }
}
