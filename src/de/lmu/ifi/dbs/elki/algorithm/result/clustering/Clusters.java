package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.*;

/**
 * Provides a result of a clustering-algorithm that computes several clusters.
 * The result is not to treat noise separately. All objects are assigned to a
 * certain cluster.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */
public class Clusters<O extends DatabaseObject> extends AbstractResult<O> implements ClusteringResult<O> {

    /**
     * Marker for a file name of a cluster.
     */
    public static final String CLUSTER_MARKER = "cluster";

    /**
     * Prefix for cluster or class labels: &quot;C&quot;.
     */
    public static final String CLUSTER_LABEL_PREFIX = "C";

    /**
     * todo comment
     */
    protected Map<Integer, Result<O>> clusterToModel;

    /**
     * An array of clusters, respectively, where each array provides the object
     * ids of its members
     */
    protected Integer[][] clusters;

    /**
     * Provides a result of a clustering-algorithm that computes several
     * clusters and remaining noise.
     *
     * @param clusters an array of clusters where each array provides the object
     *                 ids of its members
     * @param db       the database containing the objects of clusters
     */
    public Clusters(Integer[][] clusters, Database<O> db) {
        super(db);
        this.clusters = clusters;
        clusterToModel = new HashMap<Integer, Result<O>>();
    }


    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult#getClusters()
     */
    @SuppressWarnings("unchecked")
    public Cluster<O>[] getClusters() {
        Cluster<O>[] clusters = new Cluster[this.clusters.length];
        for (int i = 0; i < this.clusters.length; i++) {
            clusters[i] = new Cluster(this.clusters[i]);
            System.arraycopy(this.clusters[i], 0, clusters[i], 0, this.clusters[i].length);
            Result<O> model = clusterToModel.get(i);
            clusters[i].setModel(model);
        }
        return clusters;
    }


    /**
     * @see Result#output(File,Normalization,List)
     */
    @Override
    public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        for (int c = 0; c < this.clusters.length; c++) {
            String marker = CLUSTER_MARKER + format(c + 1, clusters.length - 1);
            PrintStream markedOut;
            try {
                File markedFile = new File(out.getAbsolutePath() + File.separator + marker);
                markedFile.getParentFile().mkdirs();
                markedOut = new PrintStream(new FileOutputStream(markedFile));
            }
            catch (Exception e) {
                markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
                markedOut.println(marker + ":");
            }
            try {
                write(c, markedOut, normalization, settings);
            }
            catch (NonNumericFeaturesException e) {
                throw new UnableToComplyException(e);
            }
            markedOut.flush();
        }

    }

    /**
     * @see Result#output(java.io.PrintStream,
     *de.lmu.ifi.dbs.elki.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        for (int c = 0; c < this.clusters.length; c++) {
            String marker = CLUSTER_MARKER + format(c + 1, clusters.length - 1);
            PrintStream markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
            markedOut.println(marker + ":");
            try {
                write(c, markedOut, normalization, settings);
            }
            catch (NonNumericFeaturesException e) {
                throw new UnableToComplyException(e);
            }
            markedOut.flush();
        }

    }

    /**
     * Returns an integer-string for the given input, that has as many leading
     * zeros as to match the length of the specified maximum.
     *
     * @param input   an integer to be formatted
     * @param maximum the maximum to adapt the format to
     * @return an integer-string for the given input, that has as many leading
     *         zeros as to match the length of the specified maximum
     */
    protected String format(int input, int maximum) {
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        formatter.setMinimumIntegerDigits(Integer.toString(maximum).length());
        return formatter.format(input);
    }

    /**
     * Writes a cluster denoted by its cluster number to the designated print
     * stream.
     *
     * @param clusterIndex  the number of the cluster to be written
     * @param out           the print stream where to write
     * @param normalization a Normalization to restore original values for
     *                      output - may remain null
     * @param settings      the settings to be written into the header
     * @throws NonNumericFeaturesException if feature vector is not compatible
     *                                     with values initialized during normalization
     */
    private void write(int clusterIndex, PrintStream out, Normalization<O> normalization, List<AttributeSettings> settings) throws NonNumericFeaturesException {
        List<String> header = new ArrayList<String>();
        header.add("cluster size = " + clusters[clusterIndex].length);
        writeHeader(out, settings, header);

        Result<O> model = clusterToModel.get(clusterIndex);
        if (model != null) {
            try {
                model.output(out, normalization, null);
            }
            catch (UnableToComplyException e) {
                // todo exception werfen
                e.printStackTrace();
            }
        }
        for (int i = 0; i < clusters[clusterIndex].length; i++) {
            O mo = db.get(clusters[clusterIndex][i]);
            if (normalization != null) {
                mo = normalization.restore(mo);
            }
            out.print(mo.toString());
            Associations associations = db.getAssociations(clusters[clusterIndex][i]);
            List<AssociationID> keys = new ArrayList<AssociationID>(associations.keySet());
            Collections.sort(keys);
            for (AssociationID<?> id : keys) {
                if (isRequiredAssociation(id)) {
                    out.print(SEPARATOR);
                    out.print(id.getName());
                    out.print("=");
                    out.print(associations.get(id).toString().replace(' ', '_'));
                }
            }
            out.println();
        }
    }

    /**
     * Decides whether a given associationID is regarded as required.
     *
     * @param id an associationID to decide whether or not it is required
     * @return true if the associationID is {@link AssociationID#CLASS} or {@link AssociationID#LABEL}, false otherwise
     */
    public boolean isRequiredAssociation(AssociationID<?> id) {
        return id == AssociationID.CLASS || id == AssociationID.LABEL;
    }

    /**
     * Returns the array of clusters where each array provides the object ids of
     * its members.
     *
     * @return the array of clusters and noise
     */
    public Integer[][] getClustersArray() {
        return clusters;
    }

    /**
     * @see ClusteringResult#associate(Class)
     */
    public <L extends ClassLabel<L>> Database<O> associate(Class<L> classLabel) {
        try {
            for (int clusterID = 0; clusterID < clusters.length; clusterID++) {
                L label = Util.instantiate(classLabel, classLabel.getName());
                label.init(canonicalClusterLabel(clusterID));
                for (int idIndex = 0; idIndex < clusters[clusterID].length; idIndex++) {
                    this.db.associate(AssociationID.CLASS, clusters[clusterID][idIndex], label);
                }

            }
            // TODO: returning *this*.db does not conform to general contract
            // given in ClusteringResult#associate(Class)
            return this.db;
        }
        catch (UnableToComplyException e) {
            throw new RuntimeException("This should never happen!");
        }

    }

    /**
     * @see ClusteringResult#clustering(Class)
     */
    public <L extends ClassLabel<L>> Map<L, Database<O>> clustering(Class<L> classLabel) {
        Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
        for (int clusterID = 0; clusterID < clusters.length; clusterID++) {
            List<Integer> ids = Arrays.asList(clusters[clusterID]);
            partitions.put(clusterID, ids);
        }
        Map<L, Database<O>> map = new HashMap<L, Database<O>>();
        try {
            Map<Integer, Database<O>> partitionMap = this.db.partition(partitions);

            for (Integer partitionID : partitionMap.keySet()) {
                L label = Util.instantiate(classLabel, classLabel.getName());
                label.init(canonicalClusterLabel(partitionID));
                map.put(label, partitionMap.get(partitionID));
            }
        }
        catch (UnableToComplyException e) {
            // todo exception werfen
            e.printStackTrace();
        }

        return map;
    }

    /**
     * @see ClusteringResult#appendModel(ClassLabel,
     *de.lmu.ifi.dbs.elki.algorithm.result.Result)
     */
    public <L extends ClassLabel<L>> void appendModel(L clusterID, Result<O> model) {
        clusterToModel.put(classLabelToClusterID(clusterID), model);
    }


    /**
     * @see ClusteringResult#noise()
     */
    public Database<O> noise() {
        try {
            Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
            Integer zero = 0;
            partitions.put(zero, new ArrayList<Integer>());
            return this.db.partition(partitions).get(zero);
        }
        catch (UnableToComplyException e) {
            throw new RuntimeException("This should never happen", e);
        }
    }

    /**
     * todo comment
     */
    protected <L extends ClassLabel<L>> Integer classLabelToClusterID(L classLabel) {
        return Integer.parseInt(classLabel.toString().substring(CLUSTER_LABEL_PREFIX.length())) - 1;
    }

    /**
     * todo comment
     */
    public String canonicalClusterLabel(int clusterID) {
        return CLUSTER_LABEL_PREFIX + Integer.toString(clusterID + 1);
    }

}
