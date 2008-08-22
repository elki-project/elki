package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.DistanceParser;
import de.lmu.ifi.dbs.elki.parser.DistanceParsingResult;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Provides a database connection expecting input from standard in.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to be provided by the implementing class as element of the supplied database
 */
public class InputStreamDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {
    /**
     * OptionID for {@link #PARSER_PARAM}
     */
    public static final OptionID PARSER_ID = OptionID.getOrCreateOptionID(
        "dbc.parser",
        "Classname specifying the parser to provide a database " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Parser.class) +
            ". "
    );

    /**
     * Parameter to specify the parser to provide a database,
     * must extend {@link Parser}.
     * <p>Default value: {@link RealVectorLabelParser} </p>
     * <p>Key: {@code -dbc.parser} </p>
     */
    private final ClassParameter<Parser> PARSER_PARAM = new ClassParameter<Parser>(
        PARSER_ID, Parser.class, RealVectorLabelParser.class.getName());

    /**
     * Holds the instance of the parser specified by {@link #PARSER_PARAM}.
     */
    Parser<O> parser;

    /**
     * The input stream to parse from.
     */
    InputStream in = System.in;

    /**
     * Adds parameter
     * {@link #PARSER_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public InputStreamDatabaseConnection() {
        super();
        addOption(PARSER_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection#getDatabase(Normalization)
     */
    @SuppressWarnings("unchecked")
    public Database<O> getDatabase(Normalization<O> normalization) {
        try {
            if (this.debug) {
                debugFine("*** parse");
            }

            // parse
            ParsingResult<O> parsingResult = parser.parse(in);
            // normalize objects and transform labels
            List<ObjectAndAssociations<O>> objectAndAssociationsList = normalizeAndTransformLabels(parsingResult.getObjectAndLabelList(),
                normalization);

            // add precomputed distances
            if (parser instanceof DistanceParser) {
                Map<Integer, Map<Integer, Distance>> distanceCache = ((DistanceParsingResult) parsingResult)
                    .getDistanceCache();
                for (ObjectAndAssociations<O> objectAndAssociations : objectAndAssociationsList) {
                    Map<Integer, Distance> distances = distanceCache.remove(objectAndAssociations.getObject().getID());
                    objectAndAssociations.addAssociation(AssociationID.CACHED_DISTANCES, distances);
                }
            }

            if (this.debug) {
                debugFine("*** insert");
            }

            // insert into database
            database.insert(objectAndAssociationsList);

            return database;
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException(e);
        }
        catch (NonNumericFeaturesException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
     */
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("", false));
        description.append('\n');
        description.append("Parsers available within this framework for database connection ");
        description.append(this.getClass().getName());
        description.append(":");
        description.append('\n');
        // for (PropertyDescription pd :
        // Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix()
        // + PROPERTY_PARSER)))
        // {
        // description.append("Class: ");
        // description.append(pd.getEntry());
        // description.append('\n');
        // description.append(pd.getDescription());
        // description.append('\n');
        // }
        // description.append('\n');
        // description.append("Databases available within this framework for
        // database connection ");
        // description.append(this.getClass().getName());
        // description.append(":");
        // description.append('\n');
        // for (PropertyDescription pd :
        // Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix()
        // + PROPERTY_DATABASE)))
        // {
        // description.append("Class: ");
        // description.append(pd.getEntry());
        // description.append('\n');
        // description.append(pd.getDescription());
        // description.append('\n');
        // }
        return description.toString();
    }

    /**
     * Calls {@link AbstractDatabaseConnection#setParameters(String[]) AbstractDatabaseConnection#setParameters(args)}
     * and instantiates {@link #parser} according to the value of parameter
     * {@link #PARSER_PARAM}.
     * The remaining parameters are passed to the {@link #parser}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // parser
        // noinspection unchecked
        parser = PARSER_PARAM.instantiateClass();
        remainingParameters = parser.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #parser}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(parser.getAttributeSettings());
        return attributeSettings;
    }
}
