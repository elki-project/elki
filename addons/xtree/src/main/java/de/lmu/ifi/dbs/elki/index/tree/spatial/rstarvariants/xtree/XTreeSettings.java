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

import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.RTreeSettings;

/**
 * XTree settings.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class XTreeSettings extends RTreeSettings {
  /**
   * Relative min entries value.
   */
  public double relativeMinEntries;

  /**
   * Relative minimum fanout.
   */
  public double relativeMinFanout;

  /**
   * Minimum size to be allowed for page sizes after a split in case of a
   * minimum overlap split.
   */
  public int min_fanout;

  /**
   * Maximum overlap for a split partition.
   */
  public float max_overlap = .2f;

  /**
   * Overlap computation method.
   */
  public enum Overlap {
    /**
     * The maximum overlap is calculated as the ratio of total data objects in
     * the overlapping region.
     */
    DATA_OVERLAP,

    /**
     * The maximum overlap is calculated as the fraction of the overlapping
     * region of the two original mbrs:
     * <code>(overlap volume of mbr 1 and mbr 2) / (volume of mbr 1 + volume of mbr 2)</code>
     */
    VOLUME_OVERLAP
  }

  /**
   * Type of overlap to be used for testing on maximum overlap. Must be one of
   * {@link Overlap#DATA_OVERLAP} and {@link Overlap#VOLUME_OVERLAP}.
   */
  protected Overlap overlap_type = Overlap.DATA_OVERLAP;
}
