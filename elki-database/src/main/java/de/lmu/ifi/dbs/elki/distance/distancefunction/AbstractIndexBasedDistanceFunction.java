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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a database index.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @has - - - AbstractIndexBasedDistanceFunction.Instance
 * @composed - - - IndexFactory
 * 
 * @param <O> the type of object to compute the distances in between
 * @param <F> the index factory type
 */
public abstract class AbstractIndexBasedDistanceFunction<O, F extends IndexFactory<O>> extends AbstractDatabaseDistanceFunction<O> implements IndexBasedDistanceFunction<O> {
  /**
   * Parameter to specify the preprocessor to be used.
   */
  protected F indexFactory;

  /**
   * Constructor.
   * 
   * @param indexFactory Index factory
   */
  public AbstractIndexBasedDistanceFunction(F indexFactory) {
    super();
    this.indexFactory = indexFactory;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  final public TypeInformation getInputTypeRestriction() {
    return indexFactory.getInputTypeRestriction();
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <I> Index type
   * @param <F> Distance function type
   */
  abstract public static class Instance<O, I extends Index, F extends DistanceFunction<? super O>> implements IndexBasedDistanceFunction.Instance<O, I> {
    /**
     * Relation to query.
     */
    protected final Relation<O> relation;

    /**
     * Index we use
     */
    protected final I index;

    /**
     * Our parent distance function
     */
    protected F parent;

    /**
     * Constructor.
     * 
     * @param relation Database
     * @param index Index to use
     * @param parent Parent distance function
     */
    public Instance(Relation<O> relation, I index, F parent) {
      super();
      this.relation = relation;
      this.index = index;
      this.parent = parent;
    }

    @Override
    public Relation<? extends O> getRelation() {
      return relation;
    }

    @Override
    public I getIndex() {
      return index;
    }

    @Override
    public F getDistanceFunction() {
      return parent;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <F> Factory type
   */
  public abstract static class Parameterizer<F extends IndexFactory<?>> extends AbstractParameterizer {
    /**
     * The index factory we use.
     */
    protected F factory;

    /**
     * Index factory parameter
     * 
     * @param config Parameterization
     * @param restriction Restriction class
     * @param defaultClass Default value
     */
    public void configIndexFactory(Parameterization config, Class<?> restriction, Class<?> defaultClass) {
      ObjectParameter<F> param = new ObjectParameter<>(INDEX_ID, restriction, defaultClass);
      if(config.grab(param)) {
        factory = param.instantiateClass(config);
      }
    }
  }
}
