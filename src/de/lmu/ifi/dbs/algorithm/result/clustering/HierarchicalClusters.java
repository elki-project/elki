package de.lmu.ifi.dbs.algorithm.result.clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

/**
 * Provides a result of a clustering algorithm that computes hierarchical
 * clusters.
 *
 * @author Elke Achtert 
 */
public class HierarchicalClusters<C extends HierarchicalCluster<C>, O extends DatabaseObject> extends AbstractResult<O>
{
    /**
     * Indicating the children of a cluster in the string representation.
     */
    public static String CHILDREN = "children: ";

    /**
     * Indicating the parents of a cluster in the string representation.
     */
    public static String PARENTS = "parents: ";

    /**
     * Indicating the level of a cluster in the string representation.
     */
    public static String LEVEL = "level: ";

    /**
     * Indicating the index within the level of a cluster in the string representation.
     */
    public static String LEVEL_INDEX = "level index: ";

    /**
     * The root clusters.
     */
    private List<C> rootClusters;

    /**
     * Provides a result of a clustering algorithm that computes hierarchical
     * clusters from a cluster order.
     *
     * @param rootClusters the root clusters
     * @param db           the database containing the objects of the clusters
     */
    public HierarchicalClusters(List<C> rootClusters, Database<O> db)
    {
        super(db);
        this.rootClusters = rootClusters;
    }

    /**
     * Writes the cluster order to the given stream.
     *
     * @param outStream     the stream to write to
     * @param normalization Normalization to restore original values according to, if this action is supported
     *                      - may remain null.
     * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
     *                      no header will be written
     * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
     *          if any feature vector is not compatible with values initialized during normalization
     */
    public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        try
        {
            for(C rootCluster : rootClusters)
            {
                write(rootCluster, null, outStream, normalization, settings, new ArrayList<String>(), new HashMap<C, Boolean>());
            }
        }
        catch(FileNotFoundException e)
        {
            throw new UnableToComplyException(e);
        }
        catch(NonNumericFeaturesException e)
        {
            throw new UnableToComplyException(e);
        }
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    @Override
    public void output(File dir, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        dir.mkdirs();
        try
        {
            for (C rootCluster : rootClusters)
            {
                File outFile = new File(dir.getAbsolutePath() + File.separator + rootCluster.toString());
                PrintStream outStream = new PrintStream(new FileOutputStream(outFile, false));
                write(rootCluster, dir, outStream, normalization, settings, new ArrayList<String>(), new HashMap<C, Boolean>());
            }
        }
        catch (NonNumericFeaturesException e)
        {
            throw new UnableToComplyException(e);
        }
        catch (FileNotFoundException e)
        {
            throw new UnableToComplyException(e);
        }
        // TODO: possible output to stdout if dir==null
//        try
//        {
//            for(C rootCluster : rootClusters)
//            {
//                String marker = rootCluster.toString();
//                PrintStream markedOut;
//                try
//                {
//                    File markedFile = new File(dir.getAbsolutePath() + File.separator + marker);
//                    markedFile.getParentFile().mkdirs();
//                    markedOut = new PrintStream(new FileOutputStream(markedFile, false));
//                }
//                catch(Exception e)
//                {
//                    markedOut = new PrintStream(
//                                    new FileOutputStream(
//                                                FileDescriptor.out));
//                    markedOut.println(marker + ":");
//                }
//                write(rootCluster, dir, markedOut, normalization, settings, new ArrayList<String>(), new HashMap<C, Boolean>());
//            }
//        }
//        catch(NonNumericFeaturesException e)
//        {
//            throw new UnableToComplyException(e);
//        }
//        catch(FileNotFoundException e)
//        {
//            throw new UnableToComplyException(e);
//        }
    }

    /**
     * Writes a cluster to the designated print stream.
     *
     * @param cluster       the cluster to be written
     * @param dir           the directory where to write
     * @param normalization a Normalization to restore original values for output - may
     *                      remain null
     * @param settings      the settings to be written into the header
     * @param headerInformation list of header informations (each entry written in a separate line
     * @param written       the already written clusters
     * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
     *          if feature vector is not compatible with values initialized
     *          during normalization
     */
    private void write(C cluster, File dir, PrintStream out, Normalization<O> normalization, List<AttributeSettings> settings, List<String> headerInformation, Map<C, Boolean> written) throws NonNumericFeaturesException, FileNotFoundException
    {
        {
            StringBuilder children = new StringBuilder();
            children.append(HierarchicalClusters.CHILDREN);
            for(int i = 0; i < cluster.numChildren(); i++)
            {
                C c = cluster.getChild(i);
                children.append(c);
                if(i < cluster.getChildren().size() - 1)
                {
                    children.append(":");
                }
    
            }
            headerInformation.add(children.toString());
        }
        {
            StringBuilder parents = new StringBuilder();
            parents.append(HierarchicalClusters.PARENTS);
            for(int i = 0; i < cluster.numParents(); i++)
            {
                C c = cluster.getParent(i);
                parents.append(c);
                if(i < cluster.getParents().size() - 1)
                {
                    parents.append(":");
                }
            }
            headerInformation.add(parents.toString());
        }
        headerInformation.add(HierarchicalClusters.LEVEL + cluster.getLevel());
        headerInformation.add(HierarchicalClusters.LEVEL_INDEX + cluster.getLevelIndex());

        writeHeader(out, settings, headerInformation, cluster);
        
        List<Integer> ids = cluster.getIDs();
        for(Integer id : ids)
        {
            O v = db.get(id);
            if(normalization != null)
            {
                v = normalization.restore(v);
            }
            out.println(v.toString() + SEPARATOR + db.getAssociation(AssociationID.LABEL, id));
        }
        out.flush();
        written.put(cluster, true);

        // write the children
        List<C> children = cluster.getChildren();
        for(C child : children)
        {
            Boolean done = written.get(child);
            if(done != null && done)
            {
                continue;
            }

            if(dir != null)
            {
                File outFile = new File(dir.getAbsolutePath() + File.separator + child.toString());
                PrintStream outStream = new PrintStream(new FileOutputStream(outFile, false), true);
                write(child, dir, outStream, normalization, settings, new ArrayList<String>(), written);
            }
            else
            {
                write(child, dir, out, normalization, settings, new ArrayList<String>(), written);
            }
        }
    }

    /**
     * Returns the root clusters.
     *
     * @return the root clusters
     */
    public final List<C> getRootClusters()
    {
        return rootClusters;
    }

    /**
     * Writes a header for the specified cluster providing information concerning the underlying database
     * and the specified parameter-settings. Subclasses may need to overwrite this method.
     *
     * @param out               the print stream where to write
     * @param settings          the settings to be written into the header
     * @param headerInformation additional information to be printed in the header, each entry
     *                          will be printed in one separate line
     * @param cluster           the cluster to write the header for
     */
    protected void writeHeader(PrintStream out, List<AttributeSettings> settings, List<String> headerInformation, C cluster)
    {
        headerInformation.add(cluster.toString());
        writeHeader(out, settings, headerInformation);
    }

}
