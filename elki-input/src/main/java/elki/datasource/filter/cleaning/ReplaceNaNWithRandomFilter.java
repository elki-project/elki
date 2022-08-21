/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.datasource.filter.cleaning;

import java.util.ArrayList;
import java.util.Random;

import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.bundle.BundleMeta;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.AbstractStreamFilter;
import elki.logging.Logging;
import elki.math.statistics.distribution.Distribution;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

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
   * Random generator.
   */
  private Random rnd;

  /**
   * Constructor.
   * 
   * @param dist Distribution to draw from
   * @param rnd Random generator
   */
  public ReplaceNaNWithRandomFilter(Distribution dist, RandomFactory rnd) {
    super();
    this.dist = dist;
    this.rnd = rnd.getSingleThreadedRandom();
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
                ro[i] = dist.nextRandom(rnd);
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
                ro[d] = dist.nextRandom(rnd);
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
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the distribution to sample replacement values from.
     */
    public static final OptionID REPLACEMENT_DISTRIBUTION = new OptionID("nanfilter.replacement", "Distribution to sample replacement values from.");

    /**
     * Random source
     */
    public static final OptionID RANDOM_ID = new OptionID("nanfilter.seed", "Random generator seed.");

    /**
     * Distribution to generate replacement values with.
     */
    private Distribution dist;

    /**
     * Random generator.
     */
    private RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distribution>(REPLACEMENT_DISTRIBUTION, Distribution.class) //
          .grab(config, x -> dist = x);
      new RandomParameter(RANDOM_ID).grab(config, x -> rnd = x);
    }

    @Override
    public ReplaceNaNWithRandomFilter make() {
      return new ReplaceNaNWithRandomFilter(dist, rnd);
    }
  }
}
