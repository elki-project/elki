package de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution;

/**
 * Interface for a simple distribution generator
 * with a PDF, i.e. it can also compute a density
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public interface Distribution {
  /**
   * Generate a new random value
   * @return new generated value
   */
  public double generate();
  /**
   * Return the density of an existing value
   * @param val existing value
   * @return distribution density
   */
  public double explain(double val);
  
  /**
   * Describe the generator
   * @return description
   */
  public String toString();
}
