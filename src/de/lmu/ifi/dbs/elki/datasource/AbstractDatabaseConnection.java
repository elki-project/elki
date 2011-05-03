package de.lmu.ifi.dbs.elki.datasource;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
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
 */
public abstract class AbstractDatabaseConnection implements DatabaseConnection {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * Parameter to specify the database to be provided by the parse method.
   * <p>
   * Key: {@code -dbc.database}
   * </p>
   */
  public static final OptionID DATABASE_ID = OptionID.getOrCreateOptionID("dbc.database", "Database class to be provided by the parse method.");

  /**
   * Filters to apply to the input data.
   * <p>
   * Key: {@code -dbc.filter}
   * </p>
   */
  public static final OptionID FILTERS_ID = OptionID.getOrCreateOptionID("dbc.filter", "The filters to apply to the input data.");

  /**
   * The database provided by the parse method.
   */
  Database database;

  /**
   * The filters to invoke
   */
  protected List<ObjectFilter> filters;

  /**
   * Constructor.
   * 
   * @param database the instance of the database
   * @param filters Filters to apply, can be null
   */
  protected AbstractDatabaseConnection(Database database, List<ObjectFilter> filters) {
    this.database = database;
    this.filters = filters;
  }

  /**
   * Transforms the specified list of objects and their labels into a list of
   * objects and their associations.
   * 
   * @param bundle the objects to process
   * @return processed objects
   */
  protected MultipleObjectsBundle transformLabels(MultipleObjectsBundle bundle) {
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
    protected Database database = null;

    protected List<ObjectFilter> filters;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    protected void configDatabase(Parameterization config) {
      // parameter database
      final ObjectParameter<Database> dbParam = new ObjectParameter<Database>(DATABASE_ID, Database.class, HashmapDatabase.class);
      if(config.grab(dbParam)) {
        database = dbParam.instantiateClass(config);
      }
    }

    protected void configFilters(Parameterization config) {
      final ObjectListParameter<ObjectFilter> filterParam = new ObjectListParameter<ObjectFilter>(FILTERS_ID, ObjectFilter.class, true);
      if(config.grab(filterParam)) {
        filters = filterParam.instantiateClasses(config);
      }
    }
  }
}