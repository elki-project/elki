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
package de.lmu.ifi.dbs.elki.index.lsh.hashfunctions;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.random.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.data.projection.random.RandomProjectionFamily.Projection;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random projection family to use with sparse vectors.
 * <p>
 * Reference:
 * <p>
 * M. S. Charikar<br>
 * Similarity estimation techniques from rounding algorithms<br>
 * Proc. 34th ACM Symposium on Theory of Computing, STOC'02
 *
 * @author Evgeniy Faerman
 * @since 0.7.0
 */
@Reference(authors = "M. S. Charikar", //
    title = "Similarity estimation techniques from rounding algorithms", //
    booktitle = "Proc. 34th ACM Symposium on Theory of Computing, STOC'02", //
    url = "https://doi.org/10.1145/509907.509965", //
    bibkey = "DBLP:conf/stoc/Charikar02")
public class CosineLocalitySensitiveHashFunction implements LocalitySensitiveHashFunction<NumberVector> {
  /**
   * Projection function.
   */
  private RandomProjectionFamily.Projection projection;

  /**
   * Constructor.
   *
   * @param projection Projection
   */
  public CosineLocalitySensitiveHashFunction(Projection projection) {
    this.projection = projection;
  }

  @Override
  public int hashObject(NumberVector obj) {
    double[] buf = projection.project(obj);
    int hashValue = 0;
    for(int i = 0, j = 1; i < buf.length; i++, j <<= 1) {
      if(buf[i] > 0) {
        hashValue = hashValue | j;
      }
    }
    return hashValue;
  }

  @Override
  public int hashObject(NumberVector obj, double[] buf) {
    projection.project(obj, buf);
    int hashValue = 0;
    for(int i = 0, j = 1; i < buf.length; i++, j <<= 1) {
      if(buf[i] > 0) {
        hashValue = hashValue | j;
      }
    }
    return hashValue;
  }

  @Override
  public int getNumberOfProjections() {
    return projection.getOutputDimensionality();
  }
}
