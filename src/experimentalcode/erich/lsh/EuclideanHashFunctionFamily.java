package experimentalcode.erich.lsh;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.GaussianRandomProjectionFamily;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * 2-stable hash function family for Euclidean distances.
 * 
 * Reference:
 * <p>
 * Locality-sensitive hashing scheme based on p-stable distributions<br />
 * M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni<br />
 * Proc. 20th annual symposium on Computational geometry<br />
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni", title = "Locality-sensitive hashing scheme based on p-stable distributions", booktitle = "Proc. 20th annual symposium on Computational geometry", url = "http://dx.doi.org/10.1145/997817.997857")
public class EuclideanHashFunctionFamily implements LocalitySensitiveHashFunctionFamily<NumberVector<?>> {
  /**
   * Random generator to use.
   */
  private RandomFactory random;

  /**
   * Projection family to use.
   */
  GaussianRandomProjectionFamily proj;

  /**
   * Width of each bin.
   */
  double width;

  /**
   * Constructor.
   * 
   * @param random Random generator
   * @param width Bin width
   */
  public EuclideanHashFunctionFamily(RandomFactory random, double width) {
    super();
    this.random = random;
    this.proj = new GaussianRandomProjectionFamily(random);
    this.width = width;
  }

  @Override
  public ArrayList<? extends LocalitySensitiveHashFunction<? super NumberVector<?>>> generateHashFunctions(Relation<? extends NumberVector<?>> relation, int k) {
    int dim = RelationUtil.dimensionality(relation);
    ArrayList<LocalitySensitiveHashFunction<? super NumberVector<?>>> ps = new ArrayList<>(k);
    for (int i = 0; i < k; i++) {
      Matrix mat = proj.generateProjectionMatrix(dim, k);
      ps.add(new MultipleProjectionsLocalitySensitiveHashFunction(mat, width, random));
    }
    return ps;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }
}
