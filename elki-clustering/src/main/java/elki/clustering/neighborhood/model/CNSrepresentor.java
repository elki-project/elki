package elki.clustering.neighborhood.model;

import elki.database.ids.DBIDs;

public class CNSrepresentor {
    public int size;
    public double[] cnsMean;

    public DBIDs cnsElements;

    public CNSrepresentor(double[] cnsMean, int size, DBIDs cnsElements) {
        this.cnsMean = cnsMean;
        this.size = size;
        this.cnsElements = cnsElements;
    }

}
