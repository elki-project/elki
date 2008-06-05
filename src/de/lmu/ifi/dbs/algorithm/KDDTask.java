package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides a KDDTask that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.database.connection.DatabaseConnection DatabaseConnection}.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDTask<O extends DatabaseObject> extends AbstractParameterizable {
    /**
     * The String for calling this class' main routine on command line
     * interface.
     */
    private static final String CALL = "java " + KDDTask.class.getName();

    /**
     * The newline string according to system.
     */
    private static final String NEWLINE = System.getProperty("line.separator");

    /**
     * Flag to obtain help-message.
     * <p>Key: {@code -h} </p>
     */
    private final Flag HELP_FLAG = new Flag(OptionID.HELP);

    /**
     * Flag to obtain help-message.
     * <p>Key: {@code -help} </p>
     */
    private final Flag HELP_LONG_FLAG = new Flag(OptionID.HELP_LONG);

    /**
     * Parameter to specify the algorithm to be applied,
     * must extend {@link de.lmu.ifi.dbs.algorithm.Algorithm}.
     * <p>Key: {@code -algorithm} </p>
     */
    private final ClassParameter<Algorithm> ALGORITHM_PARAM =
        new ClassParameter<Algorithm>(OptionID.ALGORITHM, Algorithm.class);

    /**
     * Optional Parameter to specify a class to obtain a description for,
     * must extend {@link de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable}.
     * <p>Key: {@code -description} </p>
     */
    private final ClassParameter<Parameterizable> DESCRIPTION_PARAM =
        new ClassParameter<Parameterizable>(OptionID.DESCRIPTION, Parameterizable.class, true);

    /**
     * Parameter to specify the database connection to be used,
     * must extend {@link de.lmu.ifi.dbs.database.connection.DatabaseConnection}.
     * <p>Key: {@code -dbc} </p>
     * <p>Default value: {@link FileBasedDatabaseConnection} </p>
     */
    private final ClassParameter<DatabaseConnection> DATABASE_CONNECTION_PARAM =
        new ClassParameter<DatabaseConnection>(OptionID.DATABASE_CONNECTION,
            DatabaseConnection.class, FileBasedDatabaseConnection.class.getName());

    /**
     * Optional Parameter to specify the file to write the obtained results in.
     * If this parameter is omitted, per default the output will sequentially be given to STDOUT.
     * <p>Key: {@code -out} </p>
     */
    private final FileParameter OUTPUT_PARAM = new FileParameter(OptionID.OUTPUT,
        FileParameter.FILE_OUT, true);

    /**
     * Optional Parameter to specify a normalization in order to use a database with normalized values.
     * <p>Key: {@code -norm} </p>
     */
    private final ClassParameter<Normalization> NORMALIZATION_PARAM =
        new ClassParameter<Normalization>(OptionID.NORMALIZATION, Normalization.class, true);

    /**
     * Flag to revert result to original values -
     * invalid option if no normalization has been performed..
     * <p>Key: {@code -normUndo} </p>
     */
    private final Flag NORMALIZATION_UNDO_FLAG = new Flag(OptionID.NORMALIZATION_UNDO);

    /**
     * Holds the algorithm to run.
     */
    private Algorithm<O> algorithm;

    /**
     * Holds the database connection to have the algorithm run with.
     */
    private DatabaseConnection<O> databaseConnection;

    /**
     * Holds the file to print results to.
     */
    private File out;

    /**
     * Whether KDDTask has been properly initialized for calling the
     * {@link #run() run()}-method.
     */
    private boolean initialized = false;

    /**
     * A normalization - per default no normalization is used.
     */
    private Normalization<O> normalization = null;

    /**
     * Whether to undo normalization for result.
     */
    private boolean normalizationUndo = false;

    /**
     * Provides a KDDTask.
     */
    public KDDTask() {

        // parameter algorithm
        addOption(ALGORITHM_PARAM);

        // help flag
        addOption(HELP_FLAG);
        addOption(HELP_LONG_FLAG);

        // decription parameter
        addOption(DESCRIPTION_PARAM);

        // parameter database connection
        addOption(DATABASE_CONNECTION_PARAM);

        // parameter output file
        addOption(OUTPUT_PARAM);

        // parameter normalization
        addOption(NORMALIZATION_PARAM);

        // normalization-undo flag
        addOption(NORMALIZATION_UNDO_FLAG);

        optionHandler.setProgrammCall(CALL);
        if (this.debug) {
            debugFinest("Root logger level: " + Logger.getLogger("").getLevel().getName() + "\n");
        }
    }

    /**
     * Returns a description for printing on command line interface.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description() {
        return optionHandler.usage("");
    }

    /**
     * Returns a usage message with the specified message as leading line, and
     * information as provided by optionHandler. If an algorithm is specified,
     * the description of the algorithm is returned.
     *
     * @param message a message to be include in the usage message
     * @return a usage message with the specified message as leading line, and
     *         information as provided by optionHandler
     */
    public String usage(String message) {
        StringBuffer usage = new StringBuffer();
        usage.append(message);
        usage.append(NEWLINE);
        usage.append(optionHandler.usage("", false));
        usage.append(NEWLINE);
        if (algorithm != null) {
            usage.append(OptionHandler.OPTION_PREFIX);
            usage.append(ALGORITHM_PARAM.getName());
            usage.append(" ");
            usage.append(algorithm.description());
            usage.append(NEWLINE);
        }
        return usage.toString();
    }

    /**
     * Sets the options accordingly to the specified list of parameters.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        if (args.length == 0) {
            throw new AbortException("No options specified. Try flag -h to gain more information.");
        }
        String[] remainingParameters = optionHandler.grabOptions(args);

        // help
        if (isSet(HELP_FLAG) || isSet(HELP_LONG_FLAG)) {
            throw new AbortException(description());
        }

        // description
        if (isSet(DESCRIPTION_PARAM)) {
            String descriptionClass = getParameterValue(DESCRIPTION_PARAM);
            Parameterizable p;
            try {
                try {
                    p = Util.instantiate(Algorithm.class, descriptionClass);
                }
                catch (UnableToComplyException e) {
                    p = Util.instantiate(Parameterizable.class, descriptionClass);
                }
            }
            catch (UnableToComplyException e) {
                exception(e.getMessage(), e);
                throw new WrongParameterValueException(DESCRIPTION_PARAM.getName(),
                    descriptionClass, DESCRIPTION_PARAM.getDescription(), e);
            }
            if (p instanceof Algorithm) {
                Algorithm<?> a = (Algorithm<?>) p;
                throw new AbortException(a.getDescription().toString() + '\n' + a.description());
            }
            else {
                throw new AbortException(p.description());
            }
        }

        // algorithm
        String algorithmClass = getParameterValue(ALGORITHM_PARAM);
        try {
            // noinspection unchecked
            algorithm = Util.instantiate(Algorithm.class, algorithmClass);
        }
        catch (UnableToComplyException e) {
            exception(e.getMessage(), e);
            throw new WrongParameterValueException(ALGORITHM_PARAM.getName(),
                algorithmClass,
                ALGORITHM_PARAM.getDescription(),
                e);
        }

        // database connection
        String databaseConnectionClass = getParameterValue(DATABASE_CONNECTION_PARAM);

        try {
            // noinspection unchecked
            databaseConnection = Util.instantiate(DatabaseConnection.class, databaseConnectionClass);
        }
        catch (UnableToComplyException e) {
            exception(e.getMessage(), e);
            throw new WrongParameterValueException(DATABASE_CONNECTION_PARAM.getName(),
                databaseConnectionClass, DATABASE_CONNECTION_PARAM.getDescription(), e);
        }

        // output
        if (isSet(OUTPUT_PARAM)) {
            out = getParameterValue(OUTPUT_PARAM);
        }

        // normalization
        if (isSet(NORMALIZATION_PARAM)) {
            String normalizationClass = getParameterValue(NORMALIZATION_PARAM);
            try {
                // noinspection unchecked
                normalization = Util.instantiate(Normalization.class, normalizationClass);
            }
            catch (UnableToComplyException e) {
                exception(e.getMessage(), e);
                throw new WrongParameterValueException(NORMALIZATION_PARAM.getName(),
                    normalizationClass, NORMALIZATION_PARAM.getDescription(), e);
            }
            normalizationUndo = isSet(NORMALIZATION_UNDO_FLAG);
            remainingParameters = normalization.setParameters(remainingParameters);
        }
        else if (isSet(NORMALIZATION_UNDO_FLAG)) {
            throw new WrongParameterValueException("Illegal parameter setting: Flag " +
                NORMALIZATION_UNDO_FLAG +
                " is set, but no normalization is specified.");
        }

        remainingParameters = algorithm.setParameters(remainingParameters);
        remainingParameters = databaseConnection.setParameters(remainingParameters);

        initialized = true;
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        attributeSettings.addAll(databaseConnection.getAttributeSettings());
        if (normalization != null) {
            attributeSettings.addAll(normalization.getAttributeSettings());
        }
        attributeSettings.addAll(algorithm.getAttributeSettings());

        return attributeSettings;
    }

    /**
     * Method to run the specified algorithm using the specified database
     * connection.
     *
     * @return the result of the specified algorithm
     * @throws IllegalStateException if initialization has not been done properly (i.e.
     *                               {@link #setParameters(String[]) setParameters(String[])} has
     *                               not been called before calling this method)
     */
    public Result<O> run() throws IllegalStateException {
        if (initialized) {
            algorithm.run(databaseConnection.getDatabase(normalization));
            try {
                Result<O> result = algorithm.getResult();

                List<AttributeSettings> settings = getAttributeSettings();
                if (normalizationUndo) {
                    result.output(out, normalization, settings);
                }
                else {
                    result.output(out, null, settings);
                }
                return result;
            }
            catch (UnableToComplyException e) {
                throw new IllegalStateException("Error in restoring result to original values.", e);
            }
        }
        else {
            throw new IllegalStateException("KDD-Task was not properly initialized. Need to set parameters first.");
        }
    }

    // public Logger getLogger() {
    // return logger;
    // }

    /**
     * Runs a KDD task accordingly to the specified parameters.
     *
     * @param args parameter list according to description
     */
    public static void main(String[] args) {
        LoggingConfiguration.configureRootFinally(LoggingConfiguration.CLI);
        // noinspection unchecked
        KDDTask<? extends DatabaseObject> kddTask = new KDDTask();
        try {
            kddTask.setParameters(args);
            kddTask.run();
        }
        catch (AbortException e) {
            kddTask.verbose(kddTask.usage(e.getMessage() + "\n\nUSAGE:"));
        }
        catch (ParameterException e) {
            kddTask.warning(kddTask.usage(e.getMessage() + "\n\nUSAGE:\n"));
        }
        catch (Exception e) // any other exception
        {
            kddTask.exception(e.getMessage(), e);
        }
    }

}