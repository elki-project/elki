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
package de.lmu.ifi.dbs.elki.index.lsh.hashfamilies;

import de.lmu.ifi.dbs.elki.data.projection.random.CauchyRandomProjectionFamily;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * 2-stable hash function family for Euclidean distances.
 * <p>
 * Reference:
 * <p>
 * M. Datar, N. Immorlica, P. Indyk, V. S. Mirrokni<br>
 * Locality-sensitive hashing scheme based on p-stable distributions<br>
 * Proc. 20th Annual Symposium on Computational Geometry<br>
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @assoc - - - CauchyRandomProjectionFamily
 */
@Reference(authors = "M. Datar, N. Immorlica, P. Indyk, V. S. Mirrokni", //
    title = "Locality-sensitive hashing scheme based on p-stable distributions", //
    booktitle = "Proc. 20th Annual Symposium on Computational Geometry", //
    url = "https://doi.org/10.1145/997817.997857", //
    bibkey = "DBLP:conf/compgeom/DatarIIM04")
public class ManhattanHashFunctionFamily extends AbstractProjectedHashFunctionFamily {
  /**
   * Constructor.
   * 
   * @param random Random generator
   * @param width Bin width
   * @param k Number of projections to combine.
   */
  public ManhattanHashFunctionFamily(RandomFactory random, double width, int k) {
    super(random, new CauchyRandomProjectionFamily(random), width, k);
  }

  @Override
  public boolean isCompatible(DistanceFunction<?> df) {
    // TODO: also allow HistogramIntersectionDistance?
    return ManhattanDistanceFunction.class.isInstance(df);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractProjectedHashFunctionFamily.Parameterizer {
    @Override
    protected ManhattanHashFunctionFamily makeInstance() {
      return new ManhattanHashFunctionFamily(random, width, k);
    }
  }
}
