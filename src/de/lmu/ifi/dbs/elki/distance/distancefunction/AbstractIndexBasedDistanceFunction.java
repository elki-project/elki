package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDatabaseDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a database index.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has AbstractIndexBasedDistanceFunction.Instance
 * @apiviz.composedOf IndexFactory
 * 
 * @param <O> the type of object to compute the distances in between
 * @param <I> the type of Index used
 * @param <D> the type of Distance used
 */
public abstract class AbstractIndexBasedDistanceFunction<O, I extends Index, D extends Distance<D>> extends AbstractDatabaseDistanceFunction<O, D> implements IndexBasedDistanceFunction<O, D> {
  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -distancefunction.preprocessor}
   * </p>
   */
  protected IndexFactory<O, I> indexFactory;

  /**
   * Constructor.
   * 
   * @param indexFactory Index factory
   */
  public AbstractIndexBasedDistanceFunction(IndexFactory<O, I> indexFactory) {
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
   * @param <D> Distance type
   * @param <F> Distance function type
   */
  abstract public static class Instance<O, I extends Index, D extends Distance<D>, F extends DistanceFunction<? super O, D>> extends AbstractDatabaseDistanceQuery<O, D> implements IndexBasedDistanceFunction.Instance<O, I, D> {
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
     * @param database Database
     * @param index Index to use
     * @param parent Parent distance function
     */
    public Instance(Relation<O> database, I index, F parent) {
      super(database);
      this.index = index;
      this.parent = parent;
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
   * @apiviz.exclude
   * 
   * @param <F> Factory type
   */
  public abstract static class Parameterizer<F extends IndexFactory<?, ?>> extends AbstractParameterizer {
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
      ObjectParameter<F> param = new ObjectParameter<F>(INDEX_ID, restriction, defaultClass);
      if(config.grab(param)) {
        factory = param.instantiateClass(config);
      }
    }
  }
}