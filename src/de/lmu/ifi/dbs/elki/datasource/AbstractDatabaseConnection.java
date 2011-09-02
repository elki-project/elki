package de.lmu.ifi.dbs.elki.datasource;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Abstract super class for all database connections. AbstractDatabaseConnection
 * already provides the setting of the database according to parameters.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses ObjectFilter
 */
public abstract class AbstractDatabaseConnection implements DatabaseConnection {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * Filters to apply to the input data.
   * <p>
   * Key: {@code -dbc.filter}
   * </p>
   */
  public static final OptionID FILTERS_ID = OptionID.getOrCreateOptionID("dbc.filter", "The filters to apply to the input data.");

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
  protected MultipleObjectsBundle invokeFilters(MultipleObjectsBundle bundle) {
    if(filters != null) {
      for(ObjectFilter filter : filters) {
        bundle = filter.filter(bundle);
      }
    }
    return bundle;
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
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer extends AbstractParameterizer {
    protected List<ObjectFilter> filters;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    protected void configFilters(Parameterization config) {
      final ObjectListParameter<ObjectFilter> filterParam = new ObjectListParameter<ObjectFilter>(FILTERS_ID, ObjectFilter.class, true);
      if(config.grab(filterParam)) {
        filters = filterParam.instantiateClasses(config);
      }
    }
  }
}