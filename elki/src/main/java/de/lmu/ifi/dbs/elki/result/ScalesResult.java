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
package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.math.scales.Scales;

/**
 * Class to keep shared scales across visualizers.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - LinearScale
 */
public class ScalesResult extends BasicResult {
  /**
   * The scales in use.
   */
  private LinearScale[] scales;

  /**
   * Constructor.
   *
   * @param relation Relation to use
   */
  public ScalesResult(Relation<? extends SpatialComparable> relation) {
    this(Scales.calcScales(relation));
  }

  /**
   * Constructor.
   *
   * @param scales Relation scales to use
   */
  public ScalesResult(LinearScale[] scales) {
    super("scales", "scales");
    this.scales = scales;
  }

  /**
   * Get the scale for dimension dim (starting at 1!).
   *
   * @param dim Dimension
   * @return Scale
   */
  public LinearScale getScale(int dim) {
    return scales[dim - 1];
  }

  /**
   * Set the scale for dimension dim (starting at 1!).
   *
   * Note: you still need to trigger an event. This is not done automatically,
   * as you might want to set more than one scale!
   *
   * @param dim Dimension
   * @param scale New scale
   */
  public void setScale(int dim, LinearScale scale) {
    scales[dim - 1] = scale;
  }

  /**
   * Get all scales. Note: you must not modify the array.
   *
   * @return Scales array.
   */
  public LinearScale[] getScales() {
    return scales;
  }

  /**
   * Get (or create) a scales result for a relation.
   *
   * @param rel Relation
   * @return associated scales result
   */
  public static ScalesResult getScalesResult(final Relation<? extends SpatialComparable> rel) {
    Collection<ScalesResult> scas = ResultUtil.filterResults(rel.getHierarchy(), rel, ScalesResult.class);
    if(scas.isEmpty()) {
      final ScalesResult newsca = new ScalesResult(rel);
      ResultUtil.addChildResult(rel, newsca);
      return newsca;
    }
    return scas.iterator().next();
  }
}