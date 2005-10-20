package de.lmu.ifi.dbs.distance;

/**
 * The CorrelationDistance is a special Distance that indicates the
 * dissimilarity between correlation connected objects. The CorrelationDistance
 * beween two points is a pair consisting of the correlation dimension of two
 * points and the euclidean distance between the two points.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class CorrelationDistance extends AbstractDistance<CorrelationDistance>
{

    /**
     * Generated SerialVersionUID.
     */
    private static final long serialVersionUID = 2829135841596857929L;

    /**
     * The correlation dimension.
     */
    private int correlationValue;

    /**
     * The euclidean distance.
     */
    private double euklideanValue;

    /**
     * Constructs a new CorrelationDistance object.
     * 
     * @param correlationValue
     *            the correlation dimension to be represented by the
     *            CorrelationDistance
     * @param euklideanValue
     *            the euclidean distance to be represented by the
     *            CorrelationDistance
     */
    public CorrelationDistance(int correlationValue, double euklideanValue)
    {
        this.correlationValue = correlationValue;
        this.euklideanValue = euklideanValue;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        int result;
        long temp;
        result = correlationValue;
        temp = euklideanValue != +0.0d ? Double.doubleToLongBits(euklideanValue) : 0l;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.distance.Distance#plus(Distance)
     */
    public CorrelationDistance plus(CorrelationDistance distance)
    {

        return new CorrelationDistance(this.correlationValue + distance.correlationValue, this.euklideanValue + distance.euklideanValue);
    }

    /**
     * @see de.lmu.ifi.dbs.distance.Distance#minus(Distance)
     */
    public CorrelationDistance minus(CorrelationDistance distance)
    {

        return new CorrelationDistance(this.correlationValue - distance.correlationValue, this.euklideanValue - distance.euklideanValue);
    }

    /**
     * @see de.lmu.ifi.dbs.distance.Distance#description()
     */
    public String description()
    {
        return "CorrelationDistance.correlationValue CorrelationDistance.euklideanValue";
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(CorrelationDistance o)
    {

        if(this.correlationValue < o.correlationValue)
            return -1;
        if(this.correlationValue > o.correlationValue)
            return +1;
        return Double.compare(this.euklideanValue, o.euklideanValue);
    }
}
