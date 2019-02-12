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
package de.lmu.ifi.dbs.elki.datasource;

import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.StreamFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for all database connections. AbstractDatabaseConnection
 * already provides the setting of the database according to parameters.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @assoc - - - ObjectFilter
 */
public abstract class AbstractDatabaseConnection implements DatabaseConnection {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * The filters to invoke
   */
  protected List<ObjectFilter> filters;

  /**
   * Constructor.
   * 
   * @param filters Filters to apply, can be null
   */
  protected AbstractDatabaseConnection(List<ObjectFilter> filters) {
    this.filters = filters;
  }

  /**
   * Transforms the specified list of objects and their labels into a list of
   * objects and their associations.
   * 
   * @param bundle the objects to process
   * @return processed objects
   */
  protected MultipleObjectsBundle invokeBundleFilters(MultipleObjectsBundle bundle) {
    assert (bundle != null);
    if(filters == null) {
      return bundle;
    }
    // We dynamically switch between streaming and bundle operations.
    BundleStreamSource stream = null;
    for(ObjectFilter filter : filters) {
      if(filter instanceof StreamFilter) {
        stream = ((StreamFilter) filter).init(bundle != null ? bundle.asStream() : stream);
        bundle = null; // No longer a bundle
      }
      else {
        bundle = filter.filter(stream != null ? stream.asMultipleObjectsBundle() : bundle);
        stream = null; // No longer a stream
      }
    }
    return bundle != null ? bundle : stream.asMultipleObjectsBundle();
  }

  /**
   * Transforms the specified list of objects and their labels into a list of
   * objects and their associations.
   * 
   * @param stream the objects to process
   * @return processed objects
   */
  protected BundleStreamSource invokeStreamFilters(BundleStreamSource stream) {
    assert (stream != null);
    if(filters == null) {
      return stream;
    }
    // We dynamically switch between streaming and bundle operations.
    MultipleObjectsBundle bundle = null;
    for(ObjectFilter filter : filters) {
      if(filter instanceof StreamFilter) {
        stream = ((StreamFilter) filter).init(bundle != null ? bundle.asStream() : stream);
        bundle = null;
      }
      else {
        bundle = filter.filter(stream != null ? stream.asMultipleObjectsBundle() : bundle);
        stream = null;
      }
    }
    return stream != null ? stream : bundle.asStream();
  }

  /**
   * Get the logger for this database connection.
   * 
   * @return Logger
   */
  protected abstract Logging getLogger();

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Filters to apply to the input data.
     */
    public static final OptionID FILTERS_ID = new OptionID("dbc.filter", "The filters to apply to the input data.");

    /**
     * Parameter to specify the parser to provide a database.
     */
    public static final OptionID PARSER_ID = new OptionID("dbc.parser", "Parser to provide the database.");

    /**
     * Filters
     */
    protected List<ObjectFilter> filters;

    /**
     * Parser to use
     */
    protected Parser parser = null;

    /**
     * Get the filters parameter
     * 
     * @param config Parameterization
     */
    protected void configFilters(Parameterization config) {
      final ObjectListParameter<ObjectFilter> filterParam = new ObjectListParameter<>(FILTERS_ID, ObjectFilter.class, true);
      if(config.grab(filterParam)) {
        filters = filterParam.instantiateClasses(config);
      }
    }

    /**
     * Get the parser parameter
     * 
     * @param config Parameterization
     * @param parserRestrictionClass Restriction class
     * @param parserDefaultValueClass Default value
     */
    protected void configParser(Parameterization config, Class<?> parserRestrictionClass, Class<?> parserDefaultValueClass) {
      ObjectParameter<Parser> parserParam = new ObjectParameter<>(PARSER_ID, parserRestrictionClass, parserDefaultValueClass);
      if(config.grab(parserParam)) {
        parser = parserParam.instantiateClass(config);
      }
    }
  }
}
