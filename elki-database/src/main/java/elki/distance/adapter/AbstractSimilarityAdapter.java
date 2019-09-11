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
package elki.distance.adapter;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDRef;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.AbstractDatabaseDistance;
import elki.distance.Distance;
import elki.similarity.NormalizedSimilarity;
import elki.similarity.Similarity;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
public abstract class AbstractSimilarityAdapter<O> extends AbstractDatabaseDistance<O> {
  /**
   * Holds the similarity function.
   */
  protected Similarity<? super O> similarityFunction;

  /**
   * Constructor.
   * 
   * @param similarityFunction Similarity function to use.
   */
  public AbstractSimilarityAdapter(Similarity<? super O> similarityFunction) {
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
  public abstract static class Instance<O> extends AbstractDatabaseDistance.Instance<O> {
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
    public Instance(Relation<O> database, Distance<? super O> parent, SimilarityQuery<? super O> similarityQuery) {
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
  public abstract static class Par<O, S extends Similarity<? super O>> implements Parameterizer {
    /**
     * Parameter to specify the similarity function to derive the distance
     * between database objects from. Must extend
     * {@link elki.similarity.Similarity}
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
    protected Class<Similarity<? super O>> ARBITRARY_SIMILARITY = ClassGenericsUtil.uglyCastIntoSubclass(Similarity.class);

    /**
     * Normalized similarity functions
     */
    protected Class<NormalizedSimilarity<? super O>> NORMALIZED_SIMILARITY = ClassGenericsUtil.uglyCastIntoSubclass(NormalizedSimilarity.class);

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<S>(SIMILARITY_FUNCTION_ID, getSimilarityRestriction()) //
          .grab(config, x -> similarityFunction = x);
    }

    /**
     * Get the similarity function restriction.
     * 
     * @return Distance function supported.
     */
    protected abstract Class<? extends S> getSimilarityRestriction();
  }
}
