package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

import java.util.regex.Pattern;

/**
 * Abstract implementation of interface {@link MeasurementFunction} that provides some methods
 * valid for any extending class.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used as measurement for comparing database objects
 * @param <O> the type of DatabaseObject for which a measurement is provided for comparison
 */
public abstract class AbstractMeasurementFunction<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractParameterizable implements MeasurementFunction<O, D> {

    /**
     * Indicates an infinity pattern.
     */
    public static final String INFINITY_PATTERN = "inf";

    /**
     * A pattern to define the required input format.
     */
    private Pattern pattern;

    /**
     * The database that holds the objects for which
     * the measurements should be computed.
     */
    private Database<O> database;

    /**
     * Provides an abstract MeasurementFunction based on the given pattern.
     *
     * @param pattern a pattern to define the required input format
     */
    protected AbstractMeasurementFunction(Pattern pattern) {
        super();
        this.pattern = pattern;
    }

    /**
     * Provides an abstract MeasurementFunction.
     * This constructor can be used if the required input pattern is
     * not yet known at instantiation time and will therefore be set later.
     */
    protected AbstractMeasurementFunction() {
        this(null);
    }

    public final String requiredInputPattern() {
        return this.pattern.pattern();
    }

    public void setDatabase(Database<O> database, boolean verbose, boolean time) {
        this.database = database;
    }

    /**
     * Returns the database holding the objects for which the
     * measurements should be computed.
     *
     * @return the database holding the objects for which the
     *         measurements should be computed
     */
    protected Database<O> getDatabase() {
        return database;
    }

    /**
     * Returns true if the given pattern matches the pattern defined, false
     * otherwise.
     *
     * @param pattern the pattern to be matched with the pattern defined
     * @return true if the given pattern matches the defined pattern, false
     *         otherwise
     */
    protected final boolean matches(String pattern) {
        return this.pattern.matcher(pattern).matches();
    }

    /**
     * Sets the required input pattern.
     *
     * @param pattern the pattern to be set
     */
    protected final void setRequiredInputPattern(Pattern pattern) {
        this.pattern = pattern;
    }


    /**
     * Returns the required input pattern.
     */
    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer(super.parameterDescription());
        description.append(optionHandler.usage("Pattern for defining a range: " +
            "\"" + requiredInputPattern() + "\".", false));
        return description.toString();
    }
}
