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
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.typeconversions.ClassLabelFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Base class for supervised projection methods.
 *
 * TODO: re-add sampling.
 *
 * @author Angela Peng
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <V> Vector type
 */
public abstract class AbstractSupervisedProjectionVectorFilter<V extends NumberVector> implements ObjectFilter {
  /**
   * The dimensionality to which the data should be reduced.
   */
  protected int tdim;

  /**
   * Constructor.
   *
   * @param projdimension Projection dimensionality
   */
  public AbstractSupervisedProjectionVectorFilter(int projdimension) {
    super();
    this.tdim = projdimension;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    final int dataLength = objects.dataLength();
    if(dataLength == 0) {
      return objects;
    }

    List<? extends ClassLabel> classcolumn = null;
    // First of all, identify a class label column.
    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = objects.meta(r);
      List<?> column = objects.getColumn(r);
      if(TypeUtil.CLASSLABEL.isAssignableFromType(type)) {
        @SuppressWarnings("unchecked")
        final List<? extends ClassLabel> castcolumn = (List<? extends ClassLabel>) column;
        classcolumn = castcolumn;
        break;
      }
    }
    if(classcolumn == null) {
      getLogger().warning("No class label column found (try " + ClassLabelFilter.class.getSimpleName() + ") -- cannot run " + this.getClass().getSimpleName());
      return objects;
    }

    boolean somesuccess = false;
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    // Secondly, look for columns to train the projection on.
    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = objects.meta(r);
      List<?> column = objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      @SuppressWarnings("unchecked")
      List<V> vectorcolumn = (List<V>) column;
      final VectorFieldTypeInformation<?> vtype = (VectorFieldTypeInformation<?>) type;
      @SuppressWarnings("unchecked")
      NumberVector.Factory<V> factory = (NumberVector.Factory<V>) vtype.getFactory();
      int dim = vtype.getDimensionality();

      if(tdim > dim) {
        if(getLogger().isVerbose()) {
          getLogger().verbose("Setting projection dimension to original dimension: projection dimension: " + tdim + " larger than original dimension: " + dim);
        }
        tdim = dim;
      }

      try {
        double[][] proj = computeProjectionMatrix(vectorcolumn, classcolumn, dim);
        for(int i = 0; i < dataLength; i++) {
          double[] pv = times(proj, vectorcolumn.get(i).toArray());
          vectorcolumn.set(i, factory.newNumberVector(pv));
        }
        bundle.appendColumn(convertedType(type, factory), column);
        somesuccess = true;
      }
      catch(Exception e) {
        getLogger().error("Projection failed -- continuing with unprojected data!", e);
        bundle.appendColumn(type, column);
        continue;
      }
    }

    if(!somesuccess) {
      getLogger().warning("No vector field of fixed dimensionality found.");
      return objects;
    }
    return bundle;
  }

  /**
   * Get the output type from the input type after conversion.
   *
   * @param in input type restriction
   * @param factory Vector factory
   * @return output type restriction
   */
  protected SimpleTypeInformation<?> convertedType(SimpleTypeInformation<?> in, NumberVector.Factory<V> factory) {
    return new VectorFieldTypeInformation<>(factory, tdim);
  }

  /**
   * Class logger.
   *
   * @return Logger
   */
  protected abstract Logging getLogger();

  /**
   * computes the projection matrix
   *
   * @param vectorcolumn Vectors
   * @param classcolumn Class information
   * @param dim Dimensionality Dimensionality
   * @return Projection matrix
   */
  protected abstract double[][] computeProjectionMatrix(List<V> vectorcolumn, List<? extends ClassLabel> classcolumn, int dim);

  /**
   * Partition the bundle based on the class label.
   *
   * @param classcolumn
   * @return Partitioned data set.
   */
  protected <O> Map<O, IntList> partition(List<? extends O> classcolumn) {
    Map<O, IntList> classes = new HashMap<>();
    Iterator<? extends O> iter = classcolumn.iterator();
    for(int i = 0; iter.hasNext(); i++) {
      O lbl = iter.next();
      IntList ids = classes.get(lbl);
      if(ids == null) {
        ids = new IntArrayList();
        classes.put(lbl, ids);
      }
      ids.add(i);
    }
    return classes;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public abstract static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * The number of dimensions to keep.
     */
    public static final OptionID P_ID = new OptionID("projection.dim", "Projection dimensionality");

    /**
     * Target dimensionality.
     */
    protected int tdim;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter dimP = new IntParameter(P_ID, 2) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);

      if(config.grab(dimP)) {
        tdim = dimP.getValue();
      }
    }
  }
}
