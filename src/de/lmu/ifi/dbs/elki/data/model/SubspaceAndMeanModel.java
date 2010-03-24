package de.lmu.ifi.dbs.elki.data.model;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Model for Subspace Clusters that additionally stores a mean vector.
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
   * Creates a new SubspaceModel for the specified subspace.
   * 
   * @param subspace the subspaces of the cluster
   * @param mean the mean of the cluster
   */
  public SubspaceAndMeanModel(BitSet dimensions, V mean) {
    super(dimensions);
    this.mean = mean;
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Mean: " + mean.toString());
  }

}
