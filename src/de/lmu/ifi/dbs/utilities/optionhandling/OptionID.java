package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.ConstantObject;

/**
 * An OptionID is used by option handlers as a unique identifier for specific
 * options.
 * There is no option possible without a specific OptionID defined
 * within this class.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class OptionID extends ConstantObject {
    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.COPAA#PREPROCESSOR_PARAM}
     */
    public static final OptionID COPAA_PREPROCESSOR = new OptionID("copaa.preprocessor",
        "preprocessor to derive partition criterion " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(HiCOPreprocessor.class) +
            ".");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.COPAA#PARTITION_ALGORITHM_PARAM}
     */
    public static final OptionID COPAA_PARTITION_ALGORITHM = new OptionID("copaa.partitionAlgorithm",
        "algorithm to apply to each partition " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Algorithm.class) +
            ".");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.COPAC#PARTITION_DB_PARAM}
     */
    public static final OptionID COPAA_PARTITION_DATABASE = new OptionID("copaa.partitionDB",
        "database class for each partition " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Database.class) +
            ". If this parameter is not set, the databases of the partitions have " +
            "the same class as the original database."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.DBSCAN#EPSILON_PARAM}
     */
    public static final OptionID DBSCAN_EPSILON = new OptionID("dbscan.epsilon",
        "the maximum radius of the neighborhood " +
            "to be considered, must be suitable to " +
            "the distance function specified"
    );


    /**
     * The description of the OptionID.
     */
    private String description;

    /**
     * Provides a new OptionID of the given name and description. <p/> All
     * OptionIDs are unique w.r.t. their name. An OptionID provides
     * additionally a description of the option.
     *
     * @param name        the name of the option
     * @param description the description of the option
     */
    private OptionID(final String name, final String description) {
        super(name);
        this.description = description;
    }


    /**
     * Returns the description of this OptionID.
     *
     * @return the description of this OptionID
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this OptionID.
     *
     * @param description the description to be set
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
