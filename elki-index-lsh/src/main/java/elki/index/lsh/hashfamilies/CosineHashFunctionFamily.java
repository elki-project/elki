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
package elki.index.lsh.hashfamilies;

import java.util.ArrayList;

import elki.data.NumberVector;
import elki.data.projection.random.RandomProjectionFamily;
import elki.data.projection.random.SimplifiedRandomHyperplaneProjectionFamily;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.CosineDistance;
import elki.distance.Distance;
import elki.index.lsh.hashfunctions.CosineLocalitySensitiveHashFunction;
import elki.index.lsh.hashfunctions.LocalitySensitiveHashFunction;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.constraints.LessEqualConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Hash function family to use with Cosine distance, using simplified hash
 * functions where the projection is only drawn from +-1, instead of Gaussian
 * distributions.
 * <p>
 * References:
 * <p>
 * M. S. Charikar<br>
 * Similarity estimation techniques from rounding algorithms<br>
 * Proc. 34th ACM Symposium on Theory of Computing, STOC'02
 * <p>
 * M. Henzinger<br>
 * Finding near-duplicate web pages: a large-scale evaluation of algorithms<br>
 * Proc. 29th ACM Conf. Research and Development in Information Retrieval
 * (SIGIR 2006)
 *
 * @author Evgeniy Faerman
 * @since 0.7.0
 */
@Reference(authors = "M. S. Charikar", //
    title = "Similarity estimation techniques from rounding algorithms", //
    booktitle = "Proc. 34th ACM Symposium on Theory of Computing, STOC'02", //
    url = "https://doi.org/10.1145/509907.509965", //
    bibkey = "DBLP:conf/stoc/Charikar02")
@Reference(authors = "M. Henzinger", //
    title = "Finding near-duplicate web pages: a large-scale evaluation of algorithms", //
    booktitle = "Proc. 29th ACM Conf. Research and Development in Information Retrieval (SIGIR 2006)", //
    url = "https://doi.org/10.1145/1148170.1148222", //
    bibkey = "DBLP:conf/sigir/Henzinger06")
public class CosineHashFunctionFamily implements LocalitySensitiveHashFunctionFamily<NumberVector> {
  /**
   * Projection family to use.
   */
  private RandomProjectionFamily proj;

  /**
   * The number of projections to use for each hash function.
   */
  private int k;

  /**
   * Constructor.
   *
   * @param k Number of projections to use.
   * @param random Random factory.
   */
  public CosineHashFunctionFamily(int k, RandomFactory random) {
    super();
    this.proj = new SimplifiedRandomHyperplaneProjectionFamily(random);
    this.k = k;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public ArrayList<? extends LocalitySensitiveHashFunction<? super NumberVector>> generateHashFunctions(Relation<? extends NumberVector> relation, int l) {
    int dim = RelationUtil.dimensionality(relation);
    ArrayList<LocalitySensitiveHashFunction<? super NumberVector>> ps = new ArrayList<>(l);
    for(int i = 0; i < l; i++) {
      RandomProjectionFamily.Projection projection = proj.generateProjection(dim, k);
      ps.add(new CosineLocalitySensitiveHashFunction(projection));
    }
    return ps;
  }

  @Override
  public boolean isCompatible(Distance<?> df) {
    return df instanceof CosineDistance;
  }

  /**
   * Parameterization class.
   *
   * @author Evgeniy Faerman
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for fixing the random seed.
     */
    public static final OptionID RANDOM_ID = new OptionID("lsh.projection.random", "Random seed for generating the projections.");

    /**
     * Number of projections to use in each hash function.
     */
    public static final OptionID NUMPROJ_ID = new OptionID("lsh.projection.projections", "Number of projections to use for each hash function.");

    /**
     * Random generator to use.
     */
    RandomFactory random;

    /**
     * The number of projections to use for each hash function.
     */
    int k;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(RANDOM_ID).grab(config, x -> random = x);
      new IntParameter(NUMPROJ_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .addConstraint(new LessEqualConstraint(32)) // Integer precision
          .grab(config, x -> k = x);
    }

    @Override
    public CosineHashFunctionFamily make() {
      return new CosineHashFunctionFamily(k, random);
    }
  }
}
