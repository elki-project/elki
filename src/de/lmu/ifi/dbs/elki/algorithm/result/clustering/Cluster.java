package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;

import java.util.Arrays;

/**
 * todo arthur comment
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */
public class Cluster<O extends DatabaseObject> extends AbstractLoggable implements DatabaseObject {
    private int id;

    private int[] clusterIDs;

    private Result<O> model;

    public Cluster(int[] clusterIDs) {
        super(LoggingConfiguration.DEBUG);
        this.clusterIDs = new int[clusterIDs.length];
        System.arraycopy(clusterIDs, 0, this.clusterIDs, 0, clusterIDs.length);
        Arrays.sort(this.clusterIDs);
    }

    public Cluster(Integer[] clusterIDs) {
        super(LoggingConfiguration.DEBUG);
        this.clusterIDs = new int[clusterIDs.length];
        System.arraycopy(clusterIDs, 0, this.clusterIDs, 0, clusterIDs.length);
        Arrays.sort(this.clusterIDs);
    }

    public Integer getID() {
        return this.id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public int[] getClusterIDs() {
        int[] clusterIDs = new int[this.clusterIDs.length];
        System.arraycopy(this.clusterIDs, 0, clusterIDs, 0, this.clusterIDs.length);
        return clusterIDs;
    }

    public Result<O> getModel() {
        return this.model;
    }

    public void setModel(Result<O> model) {
        this.model = model;
    }

}
