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
package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDatabaseDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Adapter from a similarity function to a distance function.
 * <p>
 * Note: The derived distance function will usually not satisfy the triangle
 * equation.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - Instance
 * 
 * @param <O> object class to process
 */
public abstract class AbstractSimilarityAdapter<O> extends AbstractDatabaseDistanceFunction<O> {
  /**
   * Holds the similarity function.
   */
  protected SimilarityFunction<? super O> similarityFunction;

  /**
   * Constructor.
   * 
   * @param similarityFunction Similarity function to use.
   */
  public AbstractSimilarityAdapter(SimilarityFunction<? super O> similarityFunction) {
    super();
    this.similarityFunction = similarityFunction;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return similarityFunction.getInputTypeRestriction();
  }

  @Override
  public boolean isSymmetric() {
    return similarityFunction.isSymmetric();
  }

  @Override
  abstract public <T extends O> DistanceQuery<T> instantiate(Relation<T> database);

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    // Same subclass
    if(!this.getClass().equals(obj.getClass())) {
      return false;
    }
    // Same similarity function
    AbstractSimilarityAdapter<?> other = (AbstractSimilarityAdapter<?>) obj;
    return other.similarityFunction.equals(other.similarityFunction);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ similarityFunction.hashCode();
  }

  /**
   * Inner proxy class for SNN distance function.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   */
  public abstract static class Instance<O> extends AbstractDatabaseDistanceFunction.Instance<O> {
    /**
     * The similarity query we use.
     */
    private SimilarityQuery<? super O> similarityQuery;

    /**
     * Constructor.
     * 
     * @param database Database to use
     * @param parent Parent distance function
     * @param similarityQuery Similarity query
     */
    public Instance(Relation<O> database, DistanceFunction<? super O> parent, SimilarityQuery<? super O> similarityQuery) {
      super(database, parent);
      this.similarityQuery = similarityQuery;
    }

    /**
     * Transformation function.
     * 
     * @param similarity Similarity value
     * @return Distance value
     */
    public abstract double transform(double similarity);

    @Override
    public double distance(DBIDRef id1, DBIDRef id2) {
      return transform(similarityQuery.similarity(id1, id2));
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<O, S extends SimilarityFunction<? super O>> extends AbstractParameterizer {
    /**
     * Parameter to specify the similarity function to derive the distance
     * between database objects from. Must extend
     * {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction}
     * .
     */
    public static final OptionID SIMILARITY_FUNCTION_ID = new OptionID("adapter.similarityfunction", //
        "Similarity function to derive the distance between database objects from.");

    /**
     * Holds the similarity function.
     */
    protected S similarityFunction = null;

    /**
     * Arbitrary Similarity functions
     */
    protected Class<SimilarityFunction<? super O>> ARBITRARY_SIMILARITY = ClassGenericsUtil.uglyCastIntoSubclass(SimilarityFunction.class);

    /**
     * Normalized similarity functions
     */
    protected Class<NormalizedSimilarityFunction<? super O>> NORMALIZED_SIMILARITY = ClassGenericsUtil.uglyCastIntoSubclass(NormalizedSimilarityFunction.class);

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<S> param = new ObjectParameter<>(SIMILARITY_FUNCTION_ID, getSimilarityRestriction());
      if(config.grab(param)) {
        similarityFunction = param.instantiateClass(config);
      }
    }

    /**
     * Get the similarity function restriction.
     * 
     * @return Distance function supported.
     */
    protected abstract Class<? extends S> getSimilarityRestriction();
  }
}
