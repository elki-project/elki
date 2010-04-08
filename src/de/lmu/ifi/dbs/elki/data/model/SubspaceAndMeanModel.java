package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Model for Subspace Clusters that additionally stores a mean vector of the cluster.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector the subspace contains
 * 
 */
public class SubspaceAndMeanModel<V extends FeatureVector<V, ?>> extends SubspaceModel<V> {

  /**
   * Cluster mean.
   */
  private V mean;

  /**
   * Creates a new SubspaceAndMeanModel for the specified subspace
   * with the given mean vector.
   * 
   * @param subspace the subspace of the cluster
   * @param mean the mean of the cluster
   */
  public SubspaceAndMeanModel(Subspace<V> subspace, V mean) {
    super(subspace);
    this.mean = mean;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Cluster Mean: " + mean.toString());
  }

}
