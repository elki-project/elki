package de.lmu.ifi.dbs.elki.datasource;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
   * Optional parameter that specifies the index of the label to be used as
   * class label, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.classLabelIndex}
   * </p>
   */
  public static final OptionID CLASS_LABEL_INDEX_ID = OptionID.getOrCreateOptionID("dbc.classLabelIndex", "The index of the label to be used as class label.");

  /**
   * Parameter to specify the class of occurring class labels.
   * <p>
   * Key: {@code -dbc.classLabelClass}
   * </p>
   */
  public static final OptionID CLASS_LABEL_CLASS_ID = OptionID.getOrCreateOptionID("dbc.classLabelClass", "Class label class to use.");

  /**
   * Optional parameter that specifies the index of the label to be used as
   * external Id, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.externalIdIndex}
   * </p>
   */
  public static final OptionID EXTERNALID_INDEX_ID = OptionID.getOrCreateOptionID("dbc.externalIdIndex", "The index of the label to be used as external Id.");

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
   * The index of the label to be used as class label, null if no class label is
   * specified.
   */
  protected Integer classLabelIndex;

  /**
   * The class label class to use.
   */
  private Class<? extends ClassLabel> classLabelClass;

  /**
   * The index of the label to be used as external Id, null if no external id
   * index is specified.
   */
  protected Integer externalIdIndex;

  /**
   * The filters to invoke
   */
  protected List<ObjectFilter> filters;

  /**
   * Constructor.
   * 
   * @param database the instance of the database
   * @param classLabelIndex the index of the label to be used as class label,
   *        can be null
   * @param classLabelClass the association of occurring class labels
   * @param externalIdIndex the index of the label to be used as external id,
   *        can be null
   * @param filters Filters to apply, can be null
   */
  protected AbstractDatabaseConnection(Database database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass, Integer externalIdIndex, List<ObjectFilter> filters) {
    this.database = database;
    this.classLabelIndex = classLabelIndex;
    this.classLabelClass = classLabelClass;
    this.externalIdIndex = externalIdIndex;
    this.filters = filters;
  }

  /**
   * Transforms the specified list of objects and their labels into a list of
   * objects and their associations.
   * 
   * @param origpkgs the objects to process
   * @return processed objects
   */
  protected MultipleObjectsBundle transformLabels(MultipleObjectsBundle origpkgs) {
    if(filters != null) {
      for(ObjectFilter filter : filters) {
        origpkgs = filter.filter(origpkgs);
      }
    }
    if(classLabelIndex == null && externalIdIndex == null) {
      return origpkgs;
    }

    // Prepare bundle for expansion
    BundleMeta reps = new BundleMeta(origpkgs.metaLength() + 2);
    List<List<Object>> columns = new ArrayList<List<Object>>(origpkgs.metaLength() + 2);
    // Adjust representations: label transformation
    for(int i = 0; i < origpkgs.metaLength(); i++) {
      SimpleTypeInformation<?> meta = origpkgs.meta(i);
      // Skip non-label columns
      if(meta.getRestrictionClass() != LabelList.class) {
        reps.add(meta);
        columns.add(origpkgs.getColumn(i));
        continue;
      }
      // We split the label column into up to three parts
      List<Object> clscol = null;
      if(classLabelIndex != null) {
        reps.add(TypeUtil.CLASSLABEL);
        clscol = new ArrayList<Object>(origpkgs.dataLength());
        columns.add(clscol);
      }
      List<Object> eidcol = null;
      if(externalIdIndex != null) {
        // TODO: special type for external ID?
        reps.add(TypeUtil.STRING);
        eidcol = new ArrayList<Object>(origpkgs.dataLength());
        columns.add(eidcol);
      }
      List<Object> lblcol = new ArrayList<Object>(origpkgs.dataLength());
      reps.add(meta);
      columns.add(lblcol);

      // Split the column
      for(Object obj : origpkgs.getColumn(i)) {
        if(obj != null) {
          LabelList ll = (LabelList) obj;
          if(classLabelIndex != null) {
            try {
              ClassLabel lbl = classLabelClass.newInstance();
              lbl.init(ll.get(classLabelIndex));
              clscol.add(lbl);
            }
            catch(Exception e) {
              throw new AbortException("Cannot initialize class labels.");
            }
          }
          if(externalIdIndex != null) {
            eidcol.add(ll.get(externalIdIndex));
          }
          // Remove in appropriate sequence
          if(classLabelIndex != null && externalIdIndex != null) {
            ll.remove(Math.max(classLabelIndex, externalIdIndex));
            ll.remove(Math.min(classLabelIndex, externalIdIndex));
          }
          else {
            if(classLabelIndex != null) {
              ll.remove(externalIdIndex);
            }
            if(externalIdIndex != null) {
              ll.remove(externalIdIndex);
            }
          }
          lblcol.add(ll);
        }
        else {
          if(classLabelIndex != null) {
            clscol.add(null);
          }
          if(externalIdIndex != null) {
            eidcol.add(null);
          }
          lblcol.add(null);
        }
      }
    }
    return new MultipleObjectsBundle(reps, columns);
  }

  /**
   * Get the logger for this database connection.
   * 
   * @return
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

    protected Integer classLabelIndex = null;

    protected Class<? extends ClassLabel> classLabelClass = null;

    protected Integer externalIdIndex = null;

    protected List<ObjectFilter> filters;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    protected void configExternalId(Parameterization config) {
      final IntParameter externalIdIndexParam = new IntParameter(EXTERNALID_INDEX_ID, new GreaterEqualConstraint(0), true);
      if(config.grab(externalIdIndexParam)) {
        externalIdIndex = externalIdIndexParam.getValue();
      }
    }

    protected void configClassLabel(Parameterization config) {
      // parameter class label index
      final IntParameter classLabelIndexParam = new IntParameter(CLASS_LABEL_INDEX_ID, new GreaterEqualConstraint(0), true);
      final ObjectParameter<ClassLabel> classlabelClassParam = new ObjectParameter<ClassLabel>(CLASS_LABEL_CLASS_ID, ClassLabel.class, SimpleClassLabel.class);

      config.grab(classLabelIndexParam);
      config.grab(classlabelClassParam);
      if(classLabelIndexParam.isDefined() && classlabelClassParam.isDefined()) {
        classLabelIndex = classLabelIndexParam.getValue();
        classLabelClass = classlabelClassParam.getValue();
      }
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