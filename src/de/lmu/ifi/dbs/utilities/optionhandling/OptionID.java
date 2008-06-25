package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.evaluation.procedure.EvaluationProcedure;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.ConstantObject;
import de.lmu.ifi.dbs.varianceanalysis.EigenPairFilter;

/**
 * An OptionID is used by option handlers as a unique identifier for specific
 * options.
 * There is no option possible without a specific OptionID defined
 * within this class.
 *
 * @author Elke Achtert
 */
public class OptionID extends ConstantObject<OptionID> {

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#HELP_FLAG}
     */
    public static final OptionID HELP = new OptionID("h",
        "Flag to obtain help-message, either for the main-routine or for any specified algorithm. " +
            "Causes immediate stop of the program.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#HELP_LONG_FLAG}
     */
    public static final OptionID HELP_LONG = new OptionID("help",
        "Flag to obtain help-message, either for the main-routine or for any specified algorithm. " +
            "Causes immediate stop of the program.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#ALGORITHM_PARAM}
     */
    public static final OptionID ALGORITHM = new OptionID("algorithm",
        "Classname of an algorithm " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Algorithm.class) +
            ". Either full name to identify classpath or only classname, if its package is " +
            Algorithm.class.getPackage().getName() + "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#DESCRIPTION_PARAM}
     */
    public static final OptionID DESCRIPTION = new OptionID("description",
        "Name of a class to obtain a description - for classes that implement " +
            Parameterizable.class.getName() +
            " -- no further processing will be performed."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#DATABASE_CONNECTION_PARAM}
     */
    public static final OptionID DATABASE_CONNECTION = new OptionID("dbc",
        "Classname of a database connection " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DatabaseConnection.class) +
            ". Either full name to identify classpath or only classname, if its package is " +
            DatabaseConnection.class.getPackage().getName() + "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#OUTPUT_PARAM}
     * todo richtige beschreibung? oder sind es directories?
     */
    public static final OptionID OUTPUT = new OptionID("out",
        "Name of the file to write the obtained results in. " +
            "If an algorithm requires several outputfiles, the given filename will be used " +
            "as prefix followed by automatically created markers. " +
            "If this parameter is omitted, per default the output will sequentially be given to STDOUT."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#NORMALIZATION_PARAM}
     */
    public static final OptionID NORMALIZATION = new OptionID("norm",
        "Classname of a normalization in order to use a database with normalized values " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Normalization.class) +
            ". "
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KDDTask#NORMALIZATION_PARAM}
     */
    public static final OptionID NORMALIZATION_UNDO = new OptionID("normUndo",
        "Flag to revert result to original values - " +
            "invalid option if no normalization has been performed.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm#DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID ALGORITHM_DISTANCEFUNCTION = new OptionID("algorithm.distancefunction",
        "Classname of the distance function to determine the distance between database objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#VERBOSE_FLAG}
     */
    public static final OptionID ALGORITHM_VERBOSE = new OptionID("verbose",
        "Flag to allow verbose messages while performing the algorithm.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#TIME_FLAG}
     */
    public static final OptionID ALGORITHM_TIME = new OptionID("time",
        "Flag to request output of performance time.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.APRIORI#MINFREQ_PARAM}
     */
    public static final OptionID APRIORI_MINFREQ = new OptionID("apriori.minfreq",
        "Threshold for minimum frequency as percentage value " +
            "(alternatively to parameter apriori.minsupp)."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.APRIORI#MINSUPP_PARAM}
     */
    public static final OptionID APRIORI_MINSUPP = new OptionID("apriori.minsupp",
        "Threshold for minimum support as minimally required number of transactions " +
            "(alternatively to parameter apriori.minfreq" +
            " - setting apriori.minsupp is slightly preferable over setting " +
            "apriori.minfreq in terms of efficiency)."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.cash.CASH#ADJUST_FLAG}
     */
    public static final OptionID CASH_ADJUST = new OptionID("cash.adjust",
        "Flag to indicate that an adjustment of the applied heuristic for choosing an interval " +
            "is performed after an interval is selected.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.cash.CASH#JITTER_PARAM}
     */
    public static final OptionID CASH_JITTER = new OptionID("cash.jitter",
        "The maximum jitter for distance values."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.cash.CASH#MAXLEVEL_PARAM}
     */
    public static final OptionID CASH_MAXLEVEL = new OptionID("cash.maxlevel",
        "The maximum level for splitting the hypercube."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.cash.CASH#MINDIM_PARAM}
     */
    public static final OptionID CASH_MINDIM = new OptionID("cash.mindim",
        "The minimum dimensionality of the subspaces to be found."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.cash.CASH#MINPTS_PARAM}
     */
    public static final OptionID CASH_MINPTS = new OptionID("cash.minpts",
        "Threshold for minimum number of points in a cluster."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.classifier.DistanceBasedClassifier#DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID CLASSIFIER_DISTANCEFUNCTION = new OptionID("classifier.distancefunction",
        "Classname of the distance function to determine the distance between database objects " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.classifier.AbstractClassifier#EVALUATION_PROCEDURE_PARAM}
     */
    public static final OptionID CLASSIFIER_EVALUATION_PROCEDURE = new OptionID("classifier.eval",
        "Classname of the evaluation-procedure to use for evaluation " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(EvaluationProcedure.class) + "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.classifier.AbstractClassifier#HOLDOUT_PARAM}
     */
    public static final OptionID CLASSIFIER_HOLDOUT = new OptionID("classifier.holdout",
        "<Classname of the holdout for evaluation  " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Holdout.class) + "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.clique.CLIQUE#PRUNE_FLAG}
     */
    public static final OptionID CLIQUE_PRUNE = new OptionID("clique.prune",
        "Flag to indicate that only subspaces with large coverage " +
            "(i.e. the fraction of the database that is covered by the dense units) " +
            "are selected, the rest will be pruned."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.clique.CLIQUE#TAU_PARAM}
     */
    public static final OptionID CLIQUE_TAU = new OptionID("clique.tau",
        "The density threshold for the selectivity of a unit, where the selectivity is" +
            "the fraction of total feature vectors contained in this unit."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.clique.CLIQUE#XSI_PARAM}
     */
    public static final OptionID CLIQUE_XSI = new OptionID("clique.xsi",
        "The number of intervals (units) in each dimension."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.COPAA#PREPROCESSOR_PARAM}
     */
    public static final OptionID COPAA_PREPROCESSOR = new OptionID("copaa.preprocessor",
        "Classname of the preprocessor to derive partition criterion " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(HiCOPreprocessor.class) +
            ".");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.COPAA#PARTITION_ALGORITHM_PARAM}
     */
    public static final OptionID COPAA_PARTITION_ALGORITHM = new OptionID("copaa.partitionAlgorithm",
        "Classname of the algorithm to apply to each partition " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Algorithm.class) +
            ".");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.COPAC#PARTITION_DB_PARAM}
     */
    public static final OptionID COPAA_PARTITION_DATABASE = new OptionID("copaa.partitionDB",
        "Classname of the database for each partition " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Database.class) +
            ". If this parameter is not set, the databases of the partitions have " +
            "the same class as the original database."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.CoDeC#CLASSLABEL_PARAM}
     */
    public static final OptionID CODEC_CLASSLABEL = new OptionID("codec.classlabel",
        "Classname of the designated classLabel class " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(ClassLabel.class) +
            "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.CoDeC#CLUSTERING_ALGORITHM_PARAM}
     */
    public static final OptionID CODEC_CLUSTERING_ALGORITHM = new OptionID("codec.clusteringAlgorithm",
        "Classname of the clustering algorithm to use to derive cluster " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
            "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.CoDeC#EVALUATE_AS_CLASSIFIER_FLAG}
     */
    public static final OptionID CODEC_EVALUATE_AS_CLASSIFIER = new OptionID("codec.classify",
        "Flag to demand evaluation of the cluster-models as classifier.");

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.DBSCAN#EPSILON_PARAM}
     */
    public static final OptionID DBSCAN_EPSILON = new OptionID("dbscan.epsilon",
        "The maximum radius of the neighborhood to be considered."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.DBSCAN#MINPTS_PARAM}
     */
    public static final OptionID DBSCAN_MINPTS = new OptionID("dbscan.minpts",
        " Threshold for minimum number of points in the epsilon-neighborhood of a point."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.DeLiClu#MINPTS_PARAM}
     */
    public static final OptionID DELICLU_MINPTS = new OptionID("deliclu.minpts",
        "Threshold for minimum number of points in the epsilon-neighborhood of a point."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.DependencyDerivator#OUTPUT_ACCURACY_PARAM}
     */
    public static final OptionID DEPENDENCY_DERIVATOR_ACCURACY = new OptionID("derivator.accuracy",
        "Threshold for output accuracy fraction digits."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.DependencyDerivator#SAMPLE_SIZE_PARAM}
     */
    public static final OptionID DEPENDENCY_DERIVATOR_SAMPLE_SIZE = new OptionID("derivator.sampleSize",
        "Threshold for the size of the random sample to use. " +
            "Default value is size of the complete dataset."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.DependencyDerivator#RANDOM_SAMPLE_FLAG}
     */
    public static final OptionID DEPENDENCY_DERIVATOR_RANDOM_SAMPLE = new OptionID("derivator.randomSample",
        "Flag to use random sample (use knn query around centroid, if flag is not set)."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.DiSH#EPSILON_PARAM}
     */
    public static final OptionID DISH_EPSILON = new OptionID("dish.epsilon",
        "The maximum radius of the neighborhood " +
            "to be considered in each dimension for determination of " +
            "the preference vector."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.DiSH#MU_PARAM}
     */
    public static final OptionID DISH_MU = new OptionID("dish.mu",
        "The minimum number of points as a smoothing factor to avoid the single-link-effekt."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.EM#DELTA_PARAM}
     */
    public static final OptionID EM_DELTA = new OptionID("em.delta",
        "The termination criterion for maximization of E(M): " +
            "E(M) - E(M') < em.delta"
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.EM#K_PARAM}
     */
    public static final OptionID EM_K = new OptionID("em.k",
        "The number of clusters to find."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.hierarchical.FracClus#NUMBER_OF_SUPPORTERS_PARAM}
     */
    public static final OptionID FRACLUS_NUMBER_OF_SUPPORTERS = new OptionID("fraclus.supporters",
        "The number of supporters."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.FractalDimensionTest#ID1_PARAM}
     */
    public static final OptionID FRACTAL_DIMENSION_TEST_ID1 = new OptionID("fractaldimensiontest.id1",
        "The first id."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.FractalDimensionTest#ID1_PARAM}
     */
    public static final OptionID FRACTAL_DIMENSION_TEST_ID2 = new OptionID("fractaldimensiontest.id2",
        "The second id."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.KMeans#K_PARAM}
     */
    public static final OptionID KMEANS_K = new OptionID("kmeans.k",
        "The number of clusters to find."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.classifier.KNNClassifier#K_PARAM}
     */
    public static final OptionID KNN_CLASSIFIER_K = new OptionID("knnclassifier.k",
        "The number of neighbors to take into account for classification."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KNNDistanceOrder#K_PARAM}
     */
    public static final OptionID KNN_DISTANCE_ORDER_K = new OptionID("knndistanceorder.k",
        "Specifies the distance of the k-distant object to be assessed."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KNNDistanceOrder#PERCENTAGE_PARAM}
     */
    public static final OptionID KNN_DISTANCE_ORDER_PERCENTAGE = new OptionID("knndistanceorder.percentage",
        "The average percentage of distances randomly choosen to be provided in the result."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor#K_PARAM}
     */
    public static final OptionID KNN_HICO_PREPROCESSOR_K = new OptionID("hicopreprocessor.k",
        "<The number of nearest neighbors considered in the PCA. " +
            "If this parameter is not set, k ist set to three " +
            "times of the dimensionality of the database objects."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.KNNJoin#K_PARAM}
     */
    public static final OptionID KNN_JOIN_K = new OptionID("knnjoin.k",
        "Specifies the k-nearest neighbors to be assigned."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.ProjectedDBSCAN#MINPTS_PARAM}
     */
    public static final OptionID PROJECTED_DBSCAN_MINPTS = new OptionID("projdbscan.minpts",
        "Threshold for minimum number of points in " +
            "the epsilon-neighborhood of a point."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.OPTICS#EPSILON_PARAM}
     */
    public static final OptionID OPTICS_EPSILON = new OptionID("optics.epsilon",
        "The maximum radius of the neighborhood to be considered."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.OPTICS#MINPTS_PARAM}
     */
    public static final OptionID OPTICS_MINPTS = new OptionID("optics.minpts",
        "Threshold for minimum number of points in " +
            "the epsilon-neighborhood of a point."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.ORCLUS#ALPHA_PARAM}
     */
    public static final OptionID ORCLUS_ALPHA = new OptionID("orclus.alpha",
        "The factor for reducing the number of current clusters in each iteration."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.varianceanalysis.AbstractPCA#EIGENPAIR_FILTER_PARAM}
     */
    public static final OptionID PCA_EIGENPAIR_FILTER = new OptionID("pca.filter",
        "Classname of the filter to determine the strong and weak eigenvectors " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(EigenPairFilter.class) +
            "."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.PROCLUS#M_I_PARAM}
     */
    public static final OptionID PROCLUS_M_I = new OptionID("proclus.mi",
        "The multiplier for the initial number of medoids."
    );

    /**
     * OptionID for {@link de.lmu.ifi.dbs.algorithm.clustering.SUBCLU#MINPTS_PARAM}
     */
    public static final OptionID SUBCLU_MINPTS = new OptionID("subclu.minpts",
        "Threshold for minimum number of points in the epsilon-neighborhood of a point."
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

    /**
     * Gets or creates the OptionID for the given class and given name.
     * The OptionID usually is named as the classes name (lowercase) as name-prefix
     * and the given name as suffix of the complete name, separated by a dot.
     * For example, the parameter {@code epsilon} for the class {@link de.lmu.ifi.dbs.algorithm.clustering.DBSCAN}
     * will be named {@code dbscan.epsilon}.
     *
     * @param name        the name
     * @param description the description is also set if the named OptionID does exist already
     * @return the OptionID for the given name
     */
    public static OptionID getOrCreateOptionID(final String name, final String description) {
        OptionID optionID = getOptionID(name);
        if (optionID == null) {
            optionID = new OptionID(name, description);
        }
        else {
            optionID.setDescription(description);
        }
        return optionID;
    }

    /**
     * Returns the OptionID for the given name
     * if it exists, null otherwise.
     *
     * @param name name of the desired OptionID
     * @return the OptionID for the given name
     */
    public static OptionID getOptionID(final String name) {
        return OptionID.lookup(OptionID.class, name);
    }
}
