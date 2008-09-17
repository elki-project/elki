package de.lmu.ifi.dbs.elki.algorithm.result.clustering.biclustering;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.output.Format;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A Biclustering result holds a set of biclusters.
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Result
 */
public class Biclustering<V extends RealVector<V, Double>> extends AbstractResult<V> {
    /**
     * Marker for a file name of a cluster.
     */
    public static final String CLUSTER_MARKER = "cluster";

    /**
     * Holds the set of biclusters.
     */
    private List<Bicluster<V>> biclusters;

    /**
     * Provides a Result.
     *
     * @param database the database where this result is defined on
     */
    public Biclustering(Database<V> database) {
        super(database);
        biclusters = new ArrayList<Bicluster<V>>();
    }

    /**
     * Appends the given bicluster to this result.
     *
     * @param bicluster the bicluster to be appended
     */
    public void appendBicluster(Bicluster<V> bicluster) {
        biclusters.add(bicluster);
    }

    /**
     * Returns the bicluster with a given index in the result.
     *
     * @param clusterIndex the index of the cluster in the result - cluster appended first has index 0.
     * @return the bicluster appended as {@code clusterIndex+1}<sup>th</sup> to this result
     */
    public Bicluster<V> getBicluster(int clusterIndex) {
        return biclusters.get(clusterIndex);
    }

    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        int c = 0;
        for (Bicluster<V> bicluster : biclusters) {
            c++;
            String marker = CLUSTER_MARKER + Format.format(c, biclusters.size()) + FILE_EXTENSION;
            outStream.println(marker + ":");
            try {
                write(bicluster, outStream, normalization, settings);
            }
            catch (NonNumericFeaturesException e) {
                throw new UnableToComplyException(e);
            }
            outStream.flush();
        }

    }

    @Override
    public void output(File out, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        int c = 0;
        for (Bicluster<V> bicluster : biclusters) {
            c++;
            String marker = CLUSTER_MARKER + Format.format(c, biclusters.size()) + FILE_EXTENSION;
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
                write(bicluster, markedOut, normalization, settings);
            }
            catch (NonNumericFeaturesException e) {
                throw new UnableToComplyException(e);
            }
            markedOut.flush();
        }
    }

    @SuppressWarnings("unchecked")
    private void write(Bicluster<V> bicluster, PrintStream out, Normalization<V> normalization, List<AttributeSettings> settings) throws NonNumericFeaturesException {
        bicluster.sortIDs();
        writeHeader(out, settings, bicluster.headerInformation());

        Result<V> model = bicluster.model();
        if (model != null) {
            try {
                model.output(out, normalization, null);
            }
            catch (UnableToComplyException e) {
                exception(e.getMessage(), e);
            }
        }

        for (Iterator<V> rows = bicluster.rowIterator(); rows.hasNext();) {
            V mo = rows.next();
            if (normalization != null) {
                mo = normalization.restore(mo);
            }
            out.print(mo.toString());
            Associations associations = db.getAssociations(mo.getID());
            List<AssociationID<?>> keys = new ArrayList<AssociationID<?>>(associations.keySet());
            // Collections.sort does't like AssociationID<?>
            Collections.sort((List) keys);
            for (AssociationID<?> id : keys) {
                if (id == AssociationID.CLASS || id == AssociationID.LABEL || id == AssociationID.LOCAL_DIMENSIONALITY) {
                    out.print(SEPARATOR);
                    out.print(id.getName());
                    out.print("=");
                    out.print(associations.get(id));
                }
            }
            out.println();
        }
        out.flush();
    }

}
