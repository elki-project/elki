package de.lmu.ifi.dbs.elki.datasource;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.HashmapDatabase;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
   * Constructor.
   * 
   * @param database the instance of the database
   * @param classLabelIndex the index of the label to be used as class label,
   *        can be null
   * @param classLabelClass the association of occurring class labels
   * @param externalIdIndex the index of the label to be used as external id,
   *        can be null
   */
  protected AbstractDatabaseConnection(Database database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass, Integer externalIdIndex) {
    this.database = database;
    this.classLabelIndex = classLabelIndex;
    this.classLabelClass = classLabelClass;
    this.externalIdIndex = externalIdIndex;
  }

  /**
   * Transforms the specified list of objects and their labels into a list of
   * objects and their associations.
   * 
   * @param origpkgs the objects to process
   * @return processed objects
   */
  // TODO: this should be done in the parser!
  protected MultipleObjectsBundle transformLabels(MultipleObjectsBundle origpkgs) {
    if(classLabelIndex == null && externalIdIndex == null) {
      return origpkgs;
    }

    // Adjust representations: label transformation
    int llcol = -1;
    for(int i = 0; i < origpkgs.metaLength(); i++) {
      SimpleTypeInformation<?> meta = origpkgs.meta(i);
      if(meta.getRestrictionClass() == LabelList.class) {
        llcol = i;
        break;
      }
    }
    if(llcol >= 0) {
      int inc = (classLabelIndex == null ? 0 : 1) + (externalIdIndex == null ? 0 : 1);
      BundleMeta reps = new BundleMeta(origpkgs.dataLength() + inc);
      ArrayList<Object> data = new ArrayList<Object>(origpkgs.dataLength() * (origpkgs.metaLength() + inc));
      // updated type map:
      for(int i = 0; i < origpkgs.metaLength(); i++) {
        SimpleTypeInformation<?> meta = origpkgs.meta(i);
        if (i == llcol && classLabelIndex != null) {
          reps.add(TypeUtil.CLASSLABEL);
        }
        if (i == llcol && externalIdIndex != null) {
          // TODO: special type for external ID?
          reps.add(TypeUtil.STRING);
        }
        reps.add(meta);
      }
      // copy data
      for(int j = 0; j < origpkgs.dataLength(); j++) {
        for(int i = 0; i < origpkgs.metaLength(); i++) {
          Object d = origpkgs.data(j, i);
          if (i == llcol) {
            LabelList ll = (LabelList) d;
            if (classLabelIndex != null) {
              data.add(ll.get(classLabelIndex));
            }
            if (externalIdIndex != null) {
              data.add(ll.get(externalIdIndex));
            }
            // TODO: remove also from ll?
          }
          data.add(d);
        }
      }
      return new MultipleObjectsBundle(reps, data);
    }
    else {
      // TODO: remove? move into a filter? old style, multiple string
      BundleMeta reps = new BundleMeta(origpkgs.dataLength());
      ArrayList<Object> data = new ArrayList<Object>(origpkgs.dataLength() * origpkgs.metaLength());

      // representations
      int ccol = -1;
      int ecol = -1;
      {
        int lcnt = -1;
        for(int i = 0; i < origpkgs.metaLength(); i++) {
          SimpleTypeInformation<?> meta = origpkgs.meta(i);
          if(meta.getRestrictionClass() == String.class) {
            lcnt += 1;
            if(classLabelIndex != null && classLabelIndex == lcnt) {
              ccol = i;
              // Turn into class label column.
              reps.add(SimpleTypeInformation.get(classLabelClass));
              continue;
            }
            if(externalIdIndex != null && externalIdIndex == lcnt) {
              ecol = i;
              // FIXME: create an "external id" representation
              reps.add(TypeUtil.STRING);
              continue;
            }
          }
          // otherwise, just keep the metadata unchanged
          reps.add(meta);
        }

        if(classLabelIndex != null && classLabelIndex > lcnt) {
          throw new IllegalArgumentException("No class label at index " + classLabelIndex + " specified!");
        }
      }

      for(int j = 0; j < origpkgs.dataLength(); j++) {
        for(int i = 0; i < origpkgs.metaLength(); i++) {
          Object d = origpkgs.data(j, i);
          if(i == ccol) {
            try {
              ClassLabel classLabelAssociation = classLabelClass.newInstance();
              classLabelAssociation.init((String) d);
              data.add(classLabelAssociation);
              continue;
            }
            catch(Exception e) {
              getLogger().exception("Cannot instantiate class label", e);
            }
          }
          if(i == ecol) {
            // FIXME: implement
          }
          // otherwise just keep the data
          data.add(d);
        }
      }
      return new MultipleObjectsBundle(reps, data);
    }
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
    Database database = null;

    Integer classLabelIndex = null;

    Class<? extends ClassLabel> classLabelClass = null;

    Integer externalIdIndex = null;

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
  }
}