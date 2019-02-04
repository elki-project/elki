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
package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for simple conversion filters such as normalizations and projections.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <I> Input object type
 * @param <O> Input object type
 */
public abstract class AbstractConversionFilter<I, O> implements ObjectFilter {
  /**
   * A standard implementation of the filter process. First of all, all suitable
   * representations are found. Then (if {@link #prepareStart} returns true),
   * the data is processed read-only in a first pass.
   * 
   * In the main pass, each object is then filtered using
   * {@link #filterSingleObject}.
   * 
   * @param objects Objects to filter
   * @return Filtered bundle
   */
  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    final Logging logger = getLogger();
    for(int r = 0; r < objects.metaLength(); r++) {
      @SuppressWarnings("unchecked")
      SimpleTypeInformation<Object> type = (SimpleTypeInformation<Object>) objects.meta(r);
      @SuppressWarnings("unchecked")
      final List<Object> column = (List<Object>) objects.getColumn(r);
      if(!getInputTypeRestriction().isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final SimpleTypeInformation<I> castType = (SimpleTypeInformation<I>) type;

      // When necessary, perform an initialization scan
      if(prepareStart(castType)) {
        FiniteProgress pprog = logger.isVerbose() ? new FiniteProgress("Preparing normalization", objects.dataLength(), logger) : null;
        for(Object o : column) {
          @SuppressWarnings("unchecked")
          final I obj = (I) o;
          prepareProcessInstance(obj);
          logger.incrementProcessed(pprog);
        }
        logger.ensureCompleted(pprog);
        prepareComplete();
      }

      @SuppressWarnings("unchecked")
      final List<O> castColumn = (List<O>) column;
      bundle.appendColumn(convertedType(castType), castColumn);

      // Normalization scan
      FiniteProgress nprog = logger.isVerbose() ? new FiniteProgress("Data normalization", objects.dataLength(), logger) : null;
      for(int i = 0; i < objects.dataLength(); i++) {
        @SuppressWarnings("unchecked")
        final I obj = (I) column.get(i);
        final O normalizedObj = filterSingleObject(obj);
        castColumn.set(i, normalizedObj);
        logger.incrementProcessed(nprog);
      }
      logger.ensureCompleted(nprog);
    }
    return bundle;
  }

  /**
   * Class logger.
   * 
   * @return Logger
   */
  abstract protected Logging getLogger();

  /**
   * Normalize a single instance.
   * 
   * You can implement this as UnsupportedOperationException if you override
   * both public "normalize" functions!
   * 
   * @param obj Database object to normalize
   * @return Normalized database object
   */
  protected abstract O filterSingleObject(I obj);

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  protected abstract SimpleTypeInformation<? super I> getInputTypeRestriction();

  /**
   * Get the output type from the input type after conversion.
   * 
   * @param in input type restriction
   * @return output type restriction
   */
  protected abstract SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<I> in);

  /**
   * Return "true" when the normalization needs initialization (two-pass filtering!).
   * 
   * @param in Input type information
   * @return true or false
   */
  protected boolean prepareStart(SimpleTypeInformation<I> in) {
    return false;
  }

  /**
   * Process a single object during initialization.
   * 
   * @param obj Object to process
   */
  protected void prepareProcessInstance(I obj) {
    throw new AbortException("ProcessInstance not implemented, but prepareStart true?");
  }

  /**
   * Complete the initialization phase.
   */
  protected void prepareComplete() {
    // optional - default NOOP.
  }

  @Override
  public String toString() {
    return getClass().getName();
  }
}