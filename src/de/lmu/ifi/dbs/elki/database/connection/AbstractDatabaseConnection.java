package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.database.SequentialDatabase;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.ObjectAndLabels;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.NotEqualValueGlobalConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract super class for all database connections. AbstractDatabaseConnection
 * already provides the setting of the database according to parameters.
 *
 * @author Elke Achtert
 */
public abstract class AbstractDatabaseConnection<O extends DatabaseObject> extends AbstractParameterizable implements DatabaseConnection<O> {
    /**
     * A sign to separate components of a label.
     */
    public static final String LABEL_CONCATENATION = " ";

    /**
     * OptionID for {@link #DATABASE_PARAM}
     */
    public static final OptionID DB_CONNECTION_DATABASE = OptionID.getOrCreateOptionID(
        "dbconnection.database",
        "Classname specifying the database to be provided by the parse method " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Database.class) +
            ". "
    );

    /**
     * Parameter to specify the database to be provided by the parse method.
     * <p>Default value: {@link SequentialDatabase} </p>
     * <p>Key: {@code -dbconnection.database} </p>
     */
    private final ClassParameter<Database> DATABASE_PARAM = new ClassParameter<Database>(
        DB_CONNECTION_DATABASE, Database.class, SequentialDatabase.class.getName());

    /**
     * Option string for parameter externalIDIndex.
     */
    public static final String EXTERNAL_ID_INDEX_P = "externalIDIndex";

    /**
     * Description for parameter classLabelIndex.
     */
    public static final String EXTERNAL_ID_INDEX_D = "a positive integer specifiying the index of the label to be used as a external id.";

    /**
     * Option string for parameter classLabelIndex.
     */
    public static final String CLASS_LABEL_INDEX_P = "classLabelIndex";

    /**
     * Description for parameter classLabelIndex.
     */
    public static final String CLASS_LABEL_INDEX_D = "a positive integer specifiying the index of the label to be used as class label.";

    /**
     * Option string for parameter classLabelClass.
     */
    public static final String CLASS_LABEL_CLASS_P = "classLabelClass";

    /**
     * Description for parameter classLabelClass.
     */
    public static final String CLASS_LABEL_CLASS_D = "a class as association of occuring class labels "
        + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(ClassLabel.class) + ". Default: " + SimpleClassLabel.class.getName();

    /**
     * The index of the external id label, null if no external id label is
     * specified.
     */
    private Integer externalIDIndex;

    /**
     * The index of the class label, null if no class label is specified.
     */
    protected Integer classLabelIndex;

    /**
     * The class name for a class label.
     */
    private String classLabelClass;

    /**
     * The database.
     */
    Database<O> database;

    /**
     * True, if an external label needs to be set. Default is false.
     */
    boolean forceExternalID = false;

    /**
     * AbstractDatabaseConnection already provides the setting of the database
     * according to parameters.
     */
    protected AbstractDatabaseConnection() {
        super();

        // parameter 'database'
        addOption(DATABASE_PARAM);

        // parameter 'class label index'
        IntParameter classLabelIndex = new IntParameter(CLASS_LABEL_INDEX_P, CLASS_LABEL_INDEX_D, new GreaterEqualConstraint(0));
        classLabelIndex.setOptional(true);
        optionHandler.put(classLabelIndex);

        // parameter 'class label class'
        ClassParameter<ClassLabel<?>> classLabelClass = new ClassParameter(CLASS_LABEL_CLASS_P, CLASS_LABEL_CLASS_D, ClassLabel.class);
        classLabelClass.setDefaultValue(SimpleClassLabel.class.getName());
        optionHandler.put(classLabelClass);

        // parameter 'external ID index'
        IntParameter ex = new IntParameter(EXTERNAL_ID_INDEX_P, EXTERNAL_ID_INDEX_D, new GreaterEqualConstraint(Integer.valueOf(0)));
        ex.setOptional(true);
        optionHandler.put(ex);

        // global parameter constraints
        ArrayList<NumberParameter<Integer>> globalConstraints = new ArrayList<NumberParameter<Integer>>();
        globalConstraints.add(classLabelIndex);
        globalConstraints.add(ex);
        optionHandler.setGlobalParameterConstraint(new NotEqualValueGlobalConstraint<Integer>(globalConstraints));
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = optionHandler.grabOptions(args);

        // database
        // noinspection unchecked
        database = DATABASE_PARAM.instantiateClass();

        if (optionHandler.isSet(CLASS_LABEL_INDEX_P)) {
            classLabelIndex = (Integer) optionHandler.getOptionValue(CLASS_LABEL_INDEX_P);

            classLabelClass = (String) optionHandler.getOptionValue(CLASS_LABEL_CLASS_P);
        }
        else if (!((Parameter) optionHandler.getOption(CLASS_LABEL_CLASS_P)).tookDefaultValue()) {
            // throws an exception if the class label class is set but no class
            // label index!
            classLabelIndex = (Integer) optionHandler.getOptionValue(CLASS_LABEL_INDEX_P);
            classLabelClass = (String) optionHandler.getOptionValue(CLASS_LABEL_CLASS_P);
        }

        if (optionHandler.isSet(EXTERNAL_ID_INDEX_P) || forceExternalID) {
            // throws an exception if forceExternalID is true but
            // externalIDIndex is not set!
            externalIDIndex = (Integer) optionHandler.getOptionValue(EXTERNAL_ID_INDEX_P);
        }

        remainingParameters = database.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> result = new ArrayList<AttributeSettings>();
        result.addAll(database.getAttributeSettings());
        return result;
    }

    /**
     * Normalizes and transforms the specified list of objects and their labels
     * into a list of objects and their associtaions.
     *
     * @param objectAndLabelsList the list of object and their labels to be transformed
     * @param normalization       the normalization to be applied
     * @return a list of normalized objects and their associations
     * @throws NonNumericFeaturesException if any exception occurs during normalization
     */
    protected List<ObjectAndAssociations<O>> normalizeAndTransformLabels(List<ObjectAndLabels<O>> objectAndLabelsList,
                                                                         Normalization<O> normalization) throws NonNumericFeaturesException {
        List<ObjectAndAssociations<O>> objectAndAssociationsList = transformLabels(objectAndLabelsList);

        if (normalization == null) {
            return objectAndAssociationsList;
        }
        else {
            return normalization.normalizeObjects(objectAndAssociationsList);
        }
    }

    /**
     * Transforms the specified list of objects and their labels into a list of
     * objects and their associtaions.
     *
     * @param objectAndLabelsList the list of object and their labels to be transformed
     * @return a list of objects and their associations
     */
    private List<ObjectAndAssociations<O>> transformLabels(List<ObjectAndLabels<O>> objectAndLabelsList) {
        List<ObjectAndAssociations<O>> result = new ArrayList<ObjectAndAssociations<O>>();

        for (ObjectAndLabels<O> objectAndLabels : objectAndLabelsList) {
            List<String> labels = objectAndLabels.getLabels();
            if (classLabelIndex != null && classLabelIndex - 1 > labels.size()) {
                throw new IllegalArgumentException("No class label at index " + (classLabelIndex) + " specified!");
            }

            if (externalIDIndex != null && externalIDIndex - 1 > labels.size()) {
                throw new IllegalArgumentException("No external id label at index " + (externalIDIndex) + " specified!");
            }

            String classLabel = null;
            String externalIDLabel = null;
            StringBuffer label = new StringBuffer();
            for (int i = 0; i < labels.size(); i++) {
                String l = labels.get(i).trim();
                if (l.length() == 0)
                    continue;

                if (classLabelIndex != null && i == classLabelIndex - 1) {
                    classLabel = l;
                }
                else if (externalIDIndex != null && i == externalIDIndex - 1) {
                    externalIDLabel = l;
                }
                else {
                    if (label.length() == 0) {
                        label.append(l);
                    }
                    else {
                        label.append(LABEL_CONCATENATION);
                        label.append(l);
                    }
                }
            }

            Associations associationMap = new Associations();
            if (label.length() != 0)
                associationMap.put(AssociationID.LABEL, label.toString());

            if (classLabel != null) {
                try {
                    ClassLabel classLabelAssociation = (ClassLabel) Class.forName(classLabelClass).newInstance();
                    classLabelAssociation.init(classLabel);
                    associationMap.put(AssociationID.CLASS, classLabelAssociation);
                }
                catch (InstantiationException e) {
                    IllegalStateException ise = new IllegalStateException(e);
                    ise.fillInStackTrace();
                    throw ise;
                }
                catch (IllegalAccessException e) {
                    IllegalStateException ise = new IllegalStateException(e);
                    ise.fillInStackTrace();
                    throw ise;
                }
                catch (ClassNotFoundException e) {
                    IllegalStateException ise = new IllegalStateException(e);
                    ise.fillInStackTrace();
                    throw ise;
                }
            }

            if (externalIDLabel != null) {
                associationMap.put(AssociationID.EXTERNAL_ID, externalIDLabel);
            }

            result.add(new ObjectAndAssociations<O>(objectAndLabels.getObject(), associationMap));
        }
        return result;
    }
}