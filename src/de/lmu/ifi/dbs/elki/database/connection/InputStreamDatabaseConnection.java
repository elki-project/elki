package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.InputStream;
import java.util.List;

/**
 * Provides a database connection expecting input from an input stream such as stdin.
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
        "Parser to provide the database."
    );

    /**
     * Parameter to specify the parser to provide a database,
     * must extend {@link Parser}.
     * <p>Default value: {@link DoubleVectorLabelParser} </p>
     * <p>Key: {@code -dbc.parser} </p>
     */
    private final ClassParameter<Parser<O>> PARSER_PARAM = new ClassParameter<Parser<O>>(
        PARSER_ID, Parser.class, DoubleVectorLabelParser.class.getName());

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

    public Database<O> getDatabase(Normalization<O> normalization) {
        try {
            if (logger.isDebugging()) {
              logger.debugFine("*** parse");
            }

            // parse
            ParsingResult<O> parsingResult = parser.parse(in);
            // normalize objects and transform labels
            List<Pair<O, Associations>> objectAndAssociationsList = normalizeAndTransformLabels(parsingResult.getObjectAndLabelList(),
                normalization);

            if (logger.isDebugging()) {
              logger.debugFine("*** insert");
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

    @Override
    public String shortDescription() {
        StringBuffer description = new StringBuffer();
        description.append(this.getClass().getName());
        description.append(" parses an input stream such as STDIN into a database.\n");
        return description.toString();
    }

    /**
     * Calls the super method
     * and instantiates {@link #parser} according to the value of parameter
     * {@link #PARSER_PARAM}.
     * The remaining parameters are passed to the {@link #parser}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // parser
        parser = PARSER_PARAM.instantiateClass();
        addParameterizable(parser);
        remainingParameters = parser.setParameters(remainingParameters);
        
        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }
}
