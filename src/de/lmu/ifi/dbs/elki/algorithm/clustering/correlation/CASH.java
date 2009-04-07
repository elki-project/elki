package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash.CASHInterval;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.cash.CASHIntervalSplit;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.ParameterizationFunction;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.model.LinearEquationModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.SequentialDatabase;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.elki.utilities.heap.HeapNode;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides the CASH algorithm, an subspace clustering algorithm based on the hough transform.
 * <p>Reference:
 * E. Achtert, C. Boehm, J. David, P. Kroeger, A. Zimek:
 * Robust clustering in arbitrarily oriented subspaces.
 * <br>In Proc. 8th SIAM Int. Conf. on Data Mining (SDM'08), Atlanta, GA, 2008
 * </p>
 *
 * @author Elke Achtert
 */
//todo elke hierarchy (later)
public class CASH extends AbstractAlgorithm<ParameterizationFunction, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>,ParameterizationFunction>{

    /**
     * OptionID for {@link #MINPTS_PARAM}
     */
    public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID(
        "cash.minpts",
        "Threshold for minimum number of points in a cluster."
    );

    /**
     * Parameter to specify the threshold for minimum number of points in a cluster,
     * must be an integer greater than 0.
     * <p>Key: {@code -cash.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Holds the value of {@link #MINPTS_PARAM}.
     */
    private int minPts;

    /**
     * OptionID for {@link #MAXLEVEL_PARAM}.
     */
    public static final OptionID MAXLEVEL_ID = OptionID.getOrCreateOptionID(
        "cash.maxlevel",
        "The maximum level for splitting the hypercube."
    );

    /**
     * Parameter to specify the maximum level for splitting the hypercube,
     * must be an integer greater than 0.
     * <p>Key: {@code -cash.maxlevel} </p>
     */
    private final IntParameter MAXLEVEL_PARAM = new IntParameter(MAXLEVEL_ID,
        new GreaterConstraint(0));

    /**
     * Holds the value of {@link #MAXLEVEL_PARAM}.
     */
    private int maxLevel;

    /**
     * OptionID for {@link #MINDIM_PARAM}
     */
    public static final OptionID MINDIM_ID = OptionID.getOrCreateOptionID(
        "cash.mindim",
        "The minimum dimensionality of the subspaces to be found."
    );

    /**
     * Parameter to specify the minimum dimensionality of the subspaces to be found,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -cash.mindim} </p>
     */
    private final IntParameter MINDIM_PARAM = new IntParameter(MINDIM_ID,
        new GreaterConstraint(0), 1);

    /**
     * Holds the value of {@link #MINDIM_PARAM}.
     */
    private int minDim;

    /**
     * OptionID for {@link #JITTER_PARAM}
     */
    public static final OptionID JITTER_ID = OptionID.getOrCreateOptionID(
        "cash.jitter",
        "The maximum jitter for distance values."
    );


    /**
     * Parameter to specify the maximum jitter for distance values,
     * must be a double greater than 0.
     * <p>Key: {@code -cash.jitter} </p>
     */
    private final DoubleParameter JITTER_PARAM = new DoubleParameter(JITTER_ID,
        new GreaterConstraint(0));

    /**
     * Holds the value of {@link #JITTER_PARAM}.
     */
    private double jitter;

    /**
     * OptionID for {@link #ADJUST_FLAG}
     */
    public static final OptionID ADJUST_ID = OptionID.getOrCreateOptionID(
        "cash.adjust",
        "Flag to indicate that an adjustment of the applied heuristic for choosing an interval " +
            "is performed after an interval is selected.");

    /**
     * Flag to indicate that an adjustment of the applied heuristic for choosing an interval
     * is performed after an interval is selected.
     * <p>Key: {@code -cash.adjust} </p>
     */
    private final Flag ADJUST_FLAG = new Flag(ADJUST_ID);

    /**
     * Holds the value of {@link #ADJUST_FLAG}.
     */
    private boolean adjust;

    /**
     * The result.
     */
    private Clustering<Model> result;

    /**
     * Holds the dimensionality for noise.
     */
    private int noiseDim;

    /**
     * Holds a set of processed ids.
     */
    private Set<Integer> processedIDs;

    /**
     * The database holding the objects.
     */
    private Database<ParameterizationFunction> database;

    /**
     * Provides a new CASH algorithm,
     * adding parameters
     * {@link #MINPTS_PARAM}, {@link #MAXLEVEL_PARAM}, {@link #MINDIM_PARAM}, {@link #JITTER_PARAM},
     * and flag {@link #ADJUST_FLAG}
     * to the option handler additionally to parameters of super class.
     */
    public CASH() {
        super();

        //parameter minpts
        addOption(MINPTS_PARAM);

        //parameter maxLevel
        addOption(MAXLEVEL_PARAM);

        //parameter minDim
        addOption(MINDIM_PARAM);

        //parameter jitter
        addOption(JITTER_PARAM);

        //flag adjust
        addOption(ADJUST_FLAG);
    }

    /**
     * Performs the CASH algorithm on the given database.
     *
     */
    @Override
    protected Clustering<Model> runInTime(Database<ParameterizationFunction> database) throws IllegalStateException {
        this.database = database;
        if (logger.isVerbose()) {
            StringBuffer msg = new StringBuffer();
            msg.append("\nDB size: ").append(database.size());
            msg.append("\nmin Dim: ").append(minDim);
            logger.verbose(msg.toString());
        }

        try {
            processedIDs = new HashSet<Integer>(database.size());
            noiseDim = database.get(database.iterator().next()).getDimensionality();

            Progress progress = new Progress("Clustering", database.size());
            if (logger.isVerbose()) {
                progress.setProcessed(0);
                logger.progress(progress);
            }

            result = doRun(database, progress);
            
            if (isVerbose()) {
                StringBuffer msg = new StringBuffer();
                for (Cluster<Model> c : result.getAllClusters()) {
                  if (c.getModel() instanceof LinearEquationModel) {
                    LinearEquationModel s = (LinearEquationModel) c.getModel();
                    msg.append("\n Cluster: Dim: "+s.getLes().subspacedim()+" size: "+c.size());
                  } else {
                    msg.append("\n Cluster: "+c.getModel().getClass().getName()+" size: "+c.size());
                  }
                }
                logger.verbose(msg.toString());
            }
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException(e);
        }
        catch (ParameterException e) {
            throw new IllegalStateException(e);
        }
        catch (NonNumericFeaturesException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public Clustering<Model> getResult() {
        return result;
    }

    /**
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    public Description getDescription() {
        return new Description("CASH",
            "Robust clustering in arbitrarily oriented subspaces",
            "Subspace clustering algorithm based on the hough transform.",
            "E. Achtert, C. Boehm, J. David, P. Kroeger, A. Zimek: " +
                "Robust clustering in arbitraily oriented subspaces. " +
                "In Proc. 8th SIAM Int. Conf. on Data Mining (SDM'08), Atlanta, GA, 2008");
    }

    /**
     * Calls the super method
     * and sets additionally the values of the parameters
     * {@link #MINPTS_PARAM}, {@link #MAXLEVEL_PARAM}, {@link #MINDIM_PARAM}, {@link #JITTER_PARAM},
     * and the flag {@link #ADJUST_FLAG}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // minpts
        minPts = MINPTS_PARAM.getValue();

        // maxlevel
        maxLevel = MAXLEVEL_PARAM.getValue();

        // mindim
        minDim = MINDIM_PARAM.getValue();

        // jitter
        jitter = JITTER_PARAM.getValue();

        // adjust
        adjust = ADJUST_FLAG.isSet();

        return remainingParameters;
    }

    /**
     * Runs the CASH algorithm on the specified database, this method is recursively called
     * until only noise is left.
     *
     * @param database the current database to run the CASH algorithm on
     * @param progress the progress object for verbose messages
     * @return a mapping of subspace dimensionalities to clusters
     * @throws UnableToComplyException     if an error according to the database occurs
     * @throws ParameterException          if the parameter setting is wrong
     * @throws NonNumericFeaturesException if non numeric feature vectors are used
     */
    private Clustering<Model> doRun(Database<ParameterizationFunction> database,
                                     Progress progress) throws UnableToComplyException, ParameterException, NonNumericFeaturesException {

      
      Clustering<Model> res = new Clustering<Model>();


        int dim = database.get(database.iterator().next()).getDimensionality();

        // init heap
        DefaultHeap<Integer, CASHInterval> heap = new DefaultHeap<Integer, CASHInterval>(false);
        Set<Integer> noiseIDs = getDatabaseIDs(database);
        initHeap(heap, database, dim, noiseIDs);

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            msg.append("\nXXXX dim ").append(dim);
            msg.append("\nXXXX database.size ").append(database.size());
            msg.append("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            logger.debugFine(msg.toString());
        }
        else if (logger.isVerbose()) {
            StringBuffer msg = new StringBuffer();
            msg.append("XXXX dim ").append(dim).append(" database.size ").append(database.size());
            logger.verbose(msg.toString());
        }

        // get the ''best'' d-dimensional intervals at max level
        while (!heap.isEmpty()) {
            CASHInterval interval = determineNextIntervalAtMaxLevel(heap);
            if (logger.isDebugging()) {
              logger.debugFine("next interval in dim " + dim + ": " + interval);
            }
            else if (logger.isVerbose()) {
              logger.verbose("next interval in dim " + dim + ": " + interval);
            }

            // only noise left
            if (interval == null) {
                break;
            }

            // do a dim-1 dimensional run
            Set<Integer> clusterIDs = new HashSet<Integer>();
            if (dim > minDim + 1) {
                Set<Integer> ids;
                Matrix basis_dim_minus_1;
                if (adjust) {
                    ids = new HashSet<Integer>();
                    basis_dim_minus_1 = runDerivator(database, dim, interval, ids);
                }
                else {
                    ids = interval.getIDs();
                    basis_dim_minus_1 = determineBasis(interval.centroid());
                }

                Database<ParameterizationFunction> db = buildDB(dim, basis_dim_minus_1, ids, database);
                if (db.size() != 0) {
                  // add result of dim-1 to this result
                  Clustering<Model> res_dim_minus_1 = doRun(db, progress);
                  for (Cluster<Model> cluster : res_dim_minus_1.getAllClusters()) {
                    res.addCluster(cluster);
                    noiseIDs.removeAll(cluster.getGroup().getIDs());
                    clusterIDs.addAll(cluster.getGroup().getIDs());
                    processedIDs.addAll(cluster.getGroup().getIDs());
                  }
                }
            }
            // dim == minDim
            else {
                DatabaseObjectGroupCollection<Set<Integer>> group = new DatabaseObjectGroupCollection<Set<Integer>>(interval.getIDs());
                LinearEquationSystem les = runDerivator(this.database, dim - 1, interval.getIDs());
                Cluster<Model> c = new Cluster<Model>(group, new LinearEquationModel(les));
                res.addCluster(c);
                noiseIDs.removeAll(interval.getIDs());
                clusterIDs.addAll(interval.getIDs());
                processedIDs.addAll(interval.getIDs());
            }

            // reorganize heap
            Vector<HeapNode<Integer, CASHInterval>> heapVector = heap.copy();
            heap.clear();
            for (HeapNode<Integer, CASHInterval> heapNode : heapVector) {
                CASHInterval currentInterval = heapNode.getValue();
                currentInterval.removeIDs(clusterIDs);
                if (currentInterval.getIDs().size() >= minPts) {
                    heap.addNode(new DefaultHeapNode<Integer, CASHInterval>(currentInterval.priority(), currentInterval));
                }
            }

            if (isVerbose()) {
                progress.setProcessed(processedIDs.size());
                logger.progress(progress);
            }
        }

        // put noise to clusters
        if (!noiseIDs.isEmpty()) {
            if (dim == noiseDim) {
                DatabaseObjectGroupCollection<Set<Integer>> group = new DatabaseObjectGroupCollection<Set<Integer>>(noiseIDs);
                Cluster<Model> c = new Cluster<Model>(group, true, ClusterModel.CLUSTER);
                res.addCluster(c);
                processedIDs.addAll(noiseIDs);
            }
            else if (noiseIDs.size() >= minPts) {
                // TODO: use different class/model for noise, even when LES was computed?
                DatabaseObjectGroupCollection<Set<Integer>> group = new DatabaseObjectGroupCollection<Set<Integer>>(noiseIDs);
                LinearEquationSystem les = runDerivator(this.database, dim - 1, noiseIDs);
                Cluster<Model> c = new Cluster<Model>(group, new LinearEquationModel(les));
                res.addCluster(c);
                processedIDs.addAll(noiseIDs);
            }
        }

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append("noise fuer dim ").append(dim).append(": ").append(noiseIDs.size());
            
            for (Cluster<Model> c : result.getAllClusters()) {
              if (c.getModel() instanceof LinearEquationModel) {
                LinearEquationModel s = (LinearEquationModel) c.getModel();
                msg.append("\n Cluster: Dim: "+s.getLes().subspacedim()+" size: "+c.size());
              } else {
                msg.append("\n Cluster: "+c.getModel().getClass().getName()+" size: "+c.size());
              }
            }
            logger.debugFine(msg.toString());
        }

        if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            logger.progress(progress);
        }


        return res;
    }

    /**
     * Initializes the heap with the root intervals.
     *
     * @param heap     the heap to be initialized
     * @param database the database storing the paramterization functions
     * @param dim      the dimensionality of the database
     * @param ids      the ids of the database
     */
    private void initHeap(DefaultHeap<Integer, CASHInterval> heap, Database<ParameterizationFunction> database, int dim, Set<Integer> ids) {
        CASHIntervalSplit split = new CASHIntervalSplit(database, minPts);

        // determine minimum and maximum function value of all functions
        double[] minMax = determineMinMaxDistance(database, dim);


        double d_min = minMax[0];
        double d_max = minMax[1];
        double dIntervalLength = d_max - d_min;
        int numDIntervals = (int) Math.ceil(dIntervalLength / jitter);
        double dIntervalSize = dIntervalLength / numDIntervals;
        double[] d_mins = new double[numDIntervals];
        double[] d_maxs = new double[numDIntervals];

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append("d_min ").append(d_min);
            msg.append("\nd_max ").append(d_max);
            msg.append("\nnumDIntervals ").append(numDIntervals);
            msg.append("\ndIntervalSize ").append(dIntervalSize);
            logger.debugFine(msg.toString());
        }
        else if (logger.isVerbose()) {
            StringBuffer msg = new StringBuffer();
            msg.append("d_min ").append(d_min);
            msg.append("\nd_max ").append(d_max);
            msg.append("\nnumDIntervals ").append(numDIntervals);
            msg.append("\ndIntervalSize ").append(dIntervalSize);
            logger.verbose(msg.toString());
        }

        // alpha intervals
        double[] alphaMin = new double[dim - 1];
        double[] alphaMax = new double[dim - 1];
        Arrays.fill(alphaMax, Math.PI);

        for (int i = 0; i < numDIntervals; i++) {
            if (i == 0) {
                d_mins[i] = d_min;
            }
            else {
                d_mins[i] = d_maxs[i - 1];
            }

            if (i < numDIntervals - 1) {
                d_maxs[i] = d_mins[i] + dIntervalSize;
            }
            else {
                d_maxs[i] = d_max - d_mins[i];
            }

            HyperBoundingBox alphaInterval = new HyperBoundingBox(alphaMin, alphaMax);
            Set<Integer> intervalIDs = split.determineIDs(ids, alphaInterval, d_mins[i], d_maxs[i]);
            if (intervalIDs != null && intervalIDs.size() >= minPts) {
                CASHInterval rootInterval = new CASHInterval(alphaMin, alphaMax, split, intervalIDs, 0, 0, d_mins[i], d_maxs[i]);
                heap.addNode(new DefaultHeapNode<Integer, CASHInterval>(rootInterval.priority(), rootInterval));
            }
        }

        if (logger.isDebuggingFiner()) {
            StringBuffer msg = new StringBuffer();
            msg.append("heap.size ").append(heap.size());
            logger.debugFiner(msg.toString());
        }
    }

    /**
     * Builds a dim-1 dimensional database where the objects are projected into the specified subspace.
     *
     * @param dim      the dimensionality of the database
     * @param basis    the basis defining the subspace
     * @param ids      the ids for the new database
     * @param database the database storing the parameterization functions
     * @return a dim-1 dimensional database where the objects are projected into the specified subspace
     * @throws UnableToComplyException if an error according to the database occurs
     */
    private Database<ParameterizationFunction> buildDB(int dim,
                                                       Matrix basis,
                                                       Set<Integer> ids,
                                                       Database<ParameterizationFunction> database) throws UnableToComplyException {
        // build objects and associations
        List<Pair<ParameterizationFunction,Associations>> oaas = new ArrayList<Pair<ParameterizationFunction,Associations>>(ids.size());

        for (Integer id : ids) {
            ParameterizationFunction f = project(basis, database.get(id));

            Associations associations = database.getAssociations(id);
            Pair<ParameterizationFunction,Associations> oaa = new Pair<ParameterizationFunction,Associations>(f, associations);
            oaas.add(oaa);
        }

        // insert into db
        Database<ParameterizationFunction> result = new SequentialDatabase<ParameterizationFunction>();
        result.insert(oaas);

        if (logger.isDebugging()) {
          logger.debugFine("db fuer dim " + (dim - 1) + ": " + result.size());
        }

        return result;
    }

    /**
     * Projects the specified parameterization function into the subspace
     * described by the given basis.
     *
     * @param basis the basis defining he subspace
     * @param f     the parameterization function to be projected
     * @return the projected parameterization function
     */
    private ParameterizationFunction project(Matrix basis, ParameterizationFunction f) {
//    Matrix m = new Matrix(new double[][]{f.getPointCoordinates()}).times(basis);
        Matrix m = f.getRowVector().times(basis);
        ParameterizationFunction f_t = new ParameterizationFunction(m.getColumnPackedCopy());
        f_t.setID(f.getID());
        return f_t;
    }

    /**
     * Determines a basis defining a subspace described by the specified alpha values.
     *
     * @param alpha the alpha values
     * @return a basis defining a subspace described by the specified alpha values
     */
    private Matrix determineBasis(double[] alpha) {
        double[] nn = new double[alpha.length + 1];
        for (int i = 0; i < nn.length; i++) {
            double alpha_i = i == alpha.length ? 0 : alpha[i];
            nn[i] = sinusProduct(0, i, alpha) * StrictMath.cos(alpha_i);
        }
        Matrix n = new Matrix(nn, alpha.length + 1);
        return n.completeToOrthonormalBasis();
    }

    /**
     * Computes the product of all sinus values of the specified angles
     * from start to end index.
     *
     * @param start the index to start
     * @param end   the index to end
     * @param alpha the array of angles
     * @return the product of all sinus values of the specified angles
     *         from start to end index
     */
    private double sinusProduct(int start, int end, double[] alpha) {
        double result = 1;
        for (int j = start; j < end; j++) {
            result *= StrictMath.sin(alpha[j]);
        }
        return result;
    }

    /**
     * Determines the next ''best'' interval at maximum level, i.e. the next interval containing the
     * most unprocessed objects.
     *
     * @param heap the heap storing the intervals
     * @return the next ''best'' interval at maximum level
     */
    private CASHInterval determineNextIntervalAtMaxLevel(DefaultHeap<Integer, CASHInterval> heap) {
        CASHInterval next = doDetermineNextIntervalAtMaxLevel(heap);
        // noise path was chosen
        while (next == null) {
            if (heap.isEmpty()) {
                return null;
            }
            next = doDetermineNextIntervalAtMaxLevel(heap);
        }

        return next;
    }

    /**
     * Recursive helper method to determine the next ''best'' interval at maximum level,
     * i.e. the next interval containing the most unprocessed objects
     *
     * @param heap the heap storing the intervals
     * @return the next ''best'' interval at maximum level
     */
    private CASHInterval doDetermineNextIntervalAtMaxLevel(DefaultHeap<Integer, CASHInterval> heap) {
        CASHInterval interval = heap.getMinNode().getValue();
        int dim = interval.getDimensionality();
        while (true) {
            // max level is reached
            if (interval.getLevel() >= maxLevel && interval.getMaxSplitDimension() == dim) {
                return interval;
            }

            if (heap.size() % 10000 == 0 && logger.isVerbose()) {
                logger.verbose("heap size " + heap.size());
            }

            if (heap.size() >= 40000) {
                logger.warning("Heap size > 40.000!!!");
                heap.clear();
                return null;
            }

            if (logger.isDebuggingFiner()) {
              logger.debugFiner("split " + interval.toString() + " " + interval.getLevel() + "-" + interval.getMaxSplitDimension());
            }
            interval.split();

            // noise
            if (!interval.hasChildren()) {
                return null;
            }

            CASHInterval bestInterval;
            if (interval.getLeftChild() != null && interval.getRightChild() != null) {
                int comp = interval.getLeftChild().compareTo(interval.getRightChild());
                if (comp < 0) {
                    bestInterval = interval.getRightChild();
                    heap.addNode(new DefaultHeapNode<Integer, CASHInterval>(interval.getLeftChild().priority(), interval.getLeftChild()));
                }
                else {
                    bestInterval = interval.getLeftChild();
                    heap.addNode(new DefaultHeapNode<Integer, CASHInterval>(interval.getRightChild().priority(), interval.getRightChild()));
                }
            }
            else if (interval.getLeftChild() == null) {
                bestInterval = interval.getRightChild();
            }
            else {
                bestInterval = interval.getLeftChild();
            }

            interval = bestInterval;
        }
    }

    /**
     * Returns the set of ids belonging to the specified database.
     *
     * @param database the database containing the parameterization functions.
     * @return the set of ids belonging to the specified database
     */
    private Set<Integer> getDatabaseIDs(Database<ParameterizationFunction> database) {
        Set<Integer> result = new HashSet<Integer>(database.size());
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            result.add(it.next());
        }
        return result;
    }

    /**
     * Determines the minimum and maximum function value of all parameterization functions
     * stored in the specified database.
     *
     * @param database       the database containing the parameterization functions.
     * @param dimensionality the dimensionality of the database
     * @return an array containing the minimum and maximum function value of all parameterization functions
     *         stored in the specified database
     */
    private double[] determineMinMaxDistance(Database<ParameterizationFunction> database, int dimensionality) {
        double[] min = new double[dimensionality - 1];
        double[] max = new double[dimensionality - 1];
        Arrays.fill(max, Math.PI);
        HyperBoundingBox box = new HyperBoundingBox(min, max);

        double d_min = Double.POSITIVE_INFINITY;
        double d_max = Double.NEGATIVE_INFINITY;
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer id = it.next();
            ParameterizationFunction f = database.get(id);
            HyperBoundingBox minMax = f.determineAlphaMinMax(box);
            double f_min = f.function(minMax.getMin());
            double f_max = f.function(minMax.getMax());

            d_min = Math.min(d_min, f_min);
            d_max = Math.max(d_max, f_max);
        }
        return new double[]{d_min, d_max};
    }

    /**
     * Runs the derivator on the specified interval and assigns all points
     * having a distance less then the standard deviation of the derivator model
     * to the model to this model.
     *
     * @param database the database containing the parameterization functions
     * @param interval the interval to build the model
     * @param dim      the dimensionality of the database
     * @param ids      an empty set to assign the ids
     * @return a basis of the found subspace
     * @throws UnableToComplyException if an error according to the database occurs
     * @throws ParameterException      if the parameter setting is wrong
     */
    private Matrix runDerivator(Database<ParameterizationFunction> database,
                                int dim,
                                CASHInterval interval,
                                Set<Integer> ids) throws UnableToComplyException, ParameterException {
        // build database for derivator
        Database<DoubleVector> derivatorDB = buildDerivatorDB(database, interval);

        DependencyDerivator<DoubleVector, DoubleDistance> derivator = new DependencyDerivator<DoubleVector, DoubleDistance>();
        // set the parameters
        List<String> parameters = new ArrayList<String>();
        OptionUtil.addParameter(parameters, PCAFilteredRunner.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class.getName());
        OptionUtil.addParameter(parameters, FirstNEigenPairFilter.EIGENPAIR_FILTER_N, Integer.toString(dim - 1));
        derivator.setParameters(parameters.toArray(new String[parameters.size()]));

        derivator.run(derivatorDB);
        CorrelationAnalysisSolution<DoubleVector> model = derivator.getResult();

        Matrix weightMatrix = model.getSimilarityMatrix();
        DoubleVector centroid = new DoubleVector(model.getCentroid());
        DistanceFunction<DoubleVector, DoubleDistance> df = new WeightedDistanceFunction<DoubleVector>(weightMatrix);
        DoubleDistance eps = df.valueOf("0.25");

        ids.addAll(interval.getIDs());
        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer id = it.next();
            DoubleVector v = new DoubleVector(database.get(id).getRowVector().getRowPackedCopy());
            DoubleDistance d = df.distance(v, centroid);
            if (d.compareTo(eps) < 0) {
                ids.add(id);
            }
        }

        Matrix basis = model.getStrongEigenvectors();
        return basis.getMatrix(0, basis.getRowDimensionality() - 1, 0, dim - 2);
    }

    /**
     * Builds a database for the derivator consisting of the ids
     * in the specified interval.
     *
     * @param database the database storing the parameterization functions
     * @param interval the interval to build the database from
     * @return a database for the derivator consisting of the ids
     *         in the specified interval
     * @throws UnableToComplyException if an error according to the database occurs
     */
    private Database<DoubleVector> buildDerivatorDB(Database<ParameterizationFunction> database,
                                                  CASHInterval interval) throws UnableToComplyException {
        // build objects and associations
        List<Pair<DoubleVector, Associations>> oaas = new ArrayList<Pair<DoubleVector, Associations>>(database.size());

        for (Integer id : interval.getIDs()) {
            Associations associations = database.getAssociations(id);
            DoubleVector v = new DoubleVector(database.get(id).getRowVector().getRowPackedCopy());
            Pair<DoubleVector, Associations> oaa = new Pair<DoubleVector, Associations>(v, associations);
            oaas.add(oaa);
        }

        // insert into db
        Database<DoubleVector> result = new SequentialDatabase<DoubleVector>();
        result.insert(oaas);

        if (logger.isDebugging()) {
          logger.debugFine("db fuer derivator : " + result.size());
        }

        return result;
    }
    
    /**
     * Runs the derivator on the specified interval and assigns all points
     * having a distance less then the standard deviation of the derivator model
     * to the model to this model.
     *
     * @param database       the database containing the parameterization functions
     * @param ids            the ids to build the model
     * @param dimensionality the dimensionality of the subspace
     * @return a basis of the found subspace
     */
    private LinearEquationSystem runDerivator(Database<ParameterizationFunction> database,
                                              int dimensionality,
                                              Set<Integer> ids) {
        try {
            // build database for derivator
            Database<DoubleVector> derivatorDB = buildDerivatorDB(database, ids);

            DependencyDerivator<DoubleVector, DoubleDistance> derivator = new DependencyDerivator<DoubleVector, DoubleDistance>();

            List<String> parameters = new ArrayList<String>();
            OptionUtil.addParameter(parameters, PCAFilteredRunner.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class.getName());
            OptionUtil.addParameter(parameters, FirstNEigenPairFilter.EIGENPAIR_FILTER_N, Integer.toString(dimensionality));
            derivator.setParameters(parameters.toArray(new String[parameters.size()]));

            derivator.run(derivatorDB);
            CorrelationAnalysisSolution<DoubleVector> model = derivator.getResult();
            LinearEquationSystem les = model.getNormalizedLinearEquationSystem(null);
            return les;
        }
        catch (ParameterException e) {
            throw new IllegalStateException("Wrong parameter-setting for the derivator: " + e);
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException("Initialization of the database for the derivator failed: " + e);
        }
        catch (NonNumericFeaturesException e) {
            throw new IllegalStateException("Error during normalization" + e);
        }
    }

    /**
     * Builds a database for the derivator consisting of the ids
     * in the specified interval.
     *
     * @param database the database storing the parameterization functions
     * @param ids      the ids to build the database from
     * @return a database for the derivator consisting of the ids
     *         in the specified interval
     * @throws UnableToComplyException if initialization of the database is not possible
     */
    private Database<DoubleVector> buildDerivatorDB(Database<ParameterizationFunction> database,
                                                  Set<Integer> ids) throws UnableToComplyException {
        // build objects and associations
        List<Pair<DoubleVector,Associations>> oaas = new ArrayList<Pair<DoubleVector,Associations>>(database.size());

        for (Integer id : ids) {
            Associations associations = database.getAssociations(id);
            DoubleVector v = new DoubleVector(database.get(id).getRowVector().getRowPackedCopy());
            Pair<DoubleVector,Associations> oaa = new Pair<DoubleVector,Associations>(v, associations);
            oaas.add(oaa);
        }

        // insert into db
        Database<DoubleVector> result = new SequentialDatabase<DoubleVector>();
        result.insert(oaas);

        return result;
    }
}
