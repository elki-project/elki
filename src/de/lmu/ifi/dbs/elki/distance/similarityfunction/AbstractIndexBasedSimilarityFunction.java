package de.lmu.ifi.dbs.elki.distance.similarityfunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.query.similarity.AbstractDBIDSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
 * 
 * @apiviz.uses IndexFactory
 * @apiviz.has Instance oneway - - «create»
 * 
 * @param <O> object type
 * @param <I> index type
 * @param <D> distance type
 */
public abstract class AbstractIndexBasedSimilarityFunction<O, I extends Index, R, D extends Distance<D>> implements IndexBasedSimilarityFunction<O, D> {
  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -similarityfunction.preprocessor}
   * </p>
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("similarityfunction.preprocessor", "Preprocessor to use.");

  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -similarityfunction.preprocessor}
   * </p>
   */
  protected IndexFactory<O, I> indexFactory;

  /**
   * Constructor.
   * 
   * @param indexFactory
   */
  public AbstractIndexBasedSimilarityFunction(IndexFactory<O, I> indexFactory) {
    super();
    this.indexFactory = indexFactory;
  }

  @Override
  abstract public <T extends O> Instance<T, ?, R, D> instantiate(Relation<T> database);

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
   * @apiviz.uses Index
   * 
   * @param <O> Object type
   * @param <I> Index type
   * @param <D> Distance result type
   */
  abstract public static class Instance<O, I extends Index, R, D extends Distance<D>> extends AbstractDBIDSimilarityQuery<O, D> implements IndexBasedSimilarityFunction.Instance<O, I, D> {
    /**
     * Parent index
     */
    protected final I index;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Index to use
     */
    public Instance(Relation<O> database, I index) {
      super(database);
      this.index = index;
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
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<F extends IndexFactory<?, ?>> extends AbstractParameterizer {
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
      final ObjectParameter<F> param = new ObjectParameter<F>(INDEX_ID, restrictionClass, defaultClass);
      if(config.grab(param)) {
        factory = param.instantiateClass(config);
      }
    }
  }
}