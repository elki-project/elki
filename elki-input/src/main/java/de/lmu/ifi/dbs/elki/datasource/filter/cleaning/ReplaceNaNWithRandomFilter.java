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
package de.lmu.ifi.dbs.elki.datasource.filter.cleaning;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A filter to replace all NaN values with random values.
 * <p>
 * Note: currently, only dense vector columns are supported.
 * <p>
 * TODO: add support for sparse vectors.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias("de.lmu.ifi.dbs.elki.datasource.filter.ReplaceNaNWithRandomFilter")
public class ReplaceNaNWithRandomFilter extends AbstractStreamFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ReplaceNaNWithRandomFilter.class);

  /**
   * Columns to check.
   */
  private NumberVector.Factory<?>[] densecols = null;

  /**
   * Distribution to generate replacement values with.
   */
  private Distribution dist;

  /**
   * Row cache.
   */
  private ArrayList<Object> rows = new ArrayList<>();

  /**
   * Constructor.
   */
  public ReplaceNaNWithRandomFilter(Distribution dist) {
    super();
    this.dist = dist;
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return rows.get(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        updateMeta(source.getMeta());
        return ev;
      case NEXT_OBJECT:
        if(densecols == null) {
          updateMeta(source.getMeta());
        }
        rows.clear();
        for(int j = 0; j < densecols.length; j++) {
          Object o = source.data(j);
          if(densecols[j] != null) {
            NumberVector v = (NumberVector) o;
            if(v == null) {
              continue;
            }
            double[] ro = null; // replacement
            for(int i = 0; i < v.getDimensionality(); i++) {
              if(Double.isNaN(v.doubleValue(i))) {
                ro = ro != null ? ro : v.toArray();
                ro[i] = dist.nextRandom();
              }
            }
            // If there was no NaN, ro will still be null.
            if(ro != null) {
              o = densecols[j].newNumberVector(ro);
            }
          }
          rows.add(o);
        }
        return ev;
      }
    }
  }

  /**
   * Process an updated meta record.
   * 
   * @param meta Meta record
   */
  private void updateMeta(BundleMeta meta) {
    final int cols = meta.size();
    densecols = new NumberVector.Factory<?>[cols];
    for(int i = 0; i < cols; i++) {
      if(TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH.isAssignableFromType(meta.get(i))) {
        throw new AbortException("Filtering sparse vectors is not yet supported by this filter. Please contribute.");
      }
      if(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(meta.get(i))) {
        VectorFieldTypeInformation<?> vmeta = (VectorFieldTypeInformation<?>) meta.get(i);
        densecols[i] = (NumberVector.Factory<?>) vmeta.getFactory();
        continue;
      }
    }
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("Removing records with NaN values.");
    }

    updateMeta(objects.meta());
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), new ArrayList<>());
    }
    for(int i = 0; i < objects.dataLength(); i++) {
      final Object[] row = objects.getRow(i);
      for(int j = 0; j < densecols.length; j++) {
        if(densecols[j] != null) {
          NumberVector v = (NumberVector) row[j];
          double[] ro = null; // replacement
          if(v != null) {
            for(int d = 0; d < v.getDimensionality(); d++) {
              if(Double.isNaN(v.doubleValue(d))) {
                ro = ro != null ? ro : v.toArray();
                ro[d] = dist.nextRandom();
              }
            }
          }
          row[j] = densecols[j].newNumberVector(ro);
        }
      }
      bundle.appendSimple(row);
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the distribution to sample replacement values from.
     */
    public static final OptionID REPLACEMENT_DISTRIBUTION = new OptionID("nanfilter.replacement", "Distribution to sample replacement values from.");

    /**
     * Distribution to generate replacement values with.
     */
    private Distribution dist;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Distribution> distP = new ObjectParameter<>(REPLACEMENT_DISTRIBUTION, Distribution.class);
      if(config.grab(distP)) {
        dist = distP.instantiateClass(config);
      }
    }

    @Override
    protected ReplaceNaNWithRandomFilter makeInstance() {
      return new ReplaceNaNWithRandomFilter(dist);
    }
  }
}
