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
package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @assoc - - - IndexFactory
 * @navhas - create - Instance
 * 
 * @param <O> object type
 * @param <F> index type
 */
public abstract class AbstractIndexBasedSimilarityFunction<O, F extends IndexFactory<O>> implements IndexBasedSimilarityFunction<O> {
  /**
   * Parameter to specify the preprocessor to be used.
   */
  protected F indexFactory;

  /**
   * Constructor.
   * 
   * @param indexFactory
   */
  public AbstractIndexBasedSimilarityFunction(F indexFactory) {
    super();
    this.indexFactory = indexFactory;
  }

  @Override
  abstract public <T extends O> Instance<T, ?> instantiate(Relation<T> database);

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
   * @assoc - - - Index
   * 
   * @param <O> Object type
   * @param <I> Index type
   */
  abstract public static class Instance<O, I extends Index> implements IndexBasedSimilarityFunction.Instance<O, I> {
    /**
     * Relation to query.
     */
    protected final Relation<O> relation;

    /**
     * Parent index
     */
    protected final I index;

    /**
     * Constructor.
     * 
     * @param relation Data relation
     * @param index Index to use
     */
    public Instance(Relation<O> relation, I index) {
      super();
      this.relation = relation;
      this.index = index;
    }

    @Override
    public Relation<? extends O> getRelation() {
      return relation;
    }

    @Override
    public I getIndex() {
      return index;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<F extends IndexFactory<?>> extends AbstractParameterizer {
    /**
     * Parameter to specify the preprocessor to be used.
     */
    public static final OptionID INDEX_ID = new OptionID("similarityfunction.preprocessor", "Preprocessor to use.");

    /**
     * The index factory we use.
     */
    protected F factory = null;

    /**
     * Get the index factory parameter.
     * 
     * @param config Parameterization
     * @param restrictionClass Restriction class
     * @param defaultClass Default class
     */
    protected void configIndexFactory(Parameterization config, final Class<?> restrictionClass, final Class<?> defaultClass) {
      final ObjectParameter<F> param = new ObjectParameter<>(INDEX_ID, restrictionClass, defaultClass);
      if(config.grab(param)) {
        factory = param.instantiateClass(config);
      }
    }
  }
}
