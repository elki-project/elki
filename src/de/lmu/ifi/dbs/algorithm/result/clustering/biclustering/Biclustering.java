package de.lmu.ifi.dbs.algorithm.result.clustering.biclustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Arthur Zimek
 */
public class Biclustering<V extends RealVector<V, Double>> extends AbstractResult<V>
{
    /**
     * Marker for a file name of a cluster.
     */
    public static final String CLUSTER_MARKER = "cluster";

    /**
     * Prefix for a cluster label.
     */
    public static final String CLUSTER_LABEL_PREFIX = "C";
    
    private List<Bicluster<V>> biclusters;

    public Biclustering(Database<V> database)
    {
        super(database);
        biclusters = new LinkedList<Bicluster<V>>();
    }
    
    public void appendBicluster(Bicluster<V> bicluster)
    {
        biclusters.add(bicluster);
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        int c = 0;
        for(Bicluster<V> bicluster : biclusters)
        {
            c++;
            String marker = CLUSTER_MARKER + Format.format(c, biclusters.size()) + FILE_EXTENSION;
            outStream.println(marker + ":");
            try
            {
                write(bicluster, outStream, normalization, settings);
            }
            catch(NonNumericFeaturesException e)
            {
                throw new UnableToComplyException(e);
            }
            outStream.flush();
        }

    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(File, Normalization, List)
     */
    @Override
    public void output(File out, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        int c = 0;
        for(Bicluster<V> bicluster : biclusters)
        {
            c++;
            String marker = CLUSTER_MARKER + Format.format(c, biclusters.size()) + FILE_EXTENSION;
            PrintStream markedOut;
            try
            {
                File markedFile = new File(out.getAbsolutePath() + File.separator + marker);
                markedFile.getParentFile().mkdirs();
                markedOut = new PrintStream(new FileOutputStream(markedFile));
            }
            catch(Exception e)
            {
                markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
                markedOut.println(marker + ":");
            }
            try
            {
                write(bicluster, markedOut, normalization, settings);
            }
            catch(NonNumericFeaturesException e)
            {
                throw new UnableToComplyException(e);
            }
            markedOut.flush();
        }
    }
    
    private void write(Bicluster<V> bicluster, PrintStream out, Normalization<V> normalization, List<AttributeSettings> settings) throws NonNumericFeaturesException
    {
        writeHeader(out, settings, bicluster.headerInformation());

        Result<V> model = bicluster.model();
        if(model != null)
        {
            try
            {
                model.output(out, normalization, null);
            }
            catch(UnableToComplyException e)
            {
                exception(e.getMessage(), e);
            }
        }

        for(Iterator<V> rows = bicluster.rowIterator(); rows.hasNext();)
        {
            V mo = rows.next();
            if(normalization != null)
            {
                mo = normalization.restore(mo);
            }
            out.print(mo.toString());
            Map<AssociationID, Object> associations = db.getAssociations(mo.getID());
            List<AssociationID> keys = new ArrayList<AssociationID>(associations.keySet());
            Collections.sort(keys);
            for(AssociationID id : keys)
            {
                if(id == AssociationID.CLASS || id == AssociationID.LABEL || id == AssociationID.LOCAL_DIMENSIONALITY)
                {
                    out.print(SEPARATOR);
                    out.print(id.getName());
                    out.print("=");
                    out.print(associations.get(id));
                }
            }
            out.println();
        }
    }
    
}
