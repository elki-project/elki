package de.lmu.ifi.dbs.elki.data.model;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Model for Subspace Clusters.
 * 
 * @author Erich Schubert
 * @author Elke Achtert
 * @param <V> the type of FeatureVector the subspace contains
 * 
 */
public class SubspaceModel<V extends FeatureVector<V, ?>> extends MeanModel<V> implements TextWriteable {
  /**
   * The subspace of the cluster.
   */
  private final Subspace<V> subspace;

  /**
   * Creates a new SubspaceModel for the specified subspace with the given
   * cluster mean.
   * 
   * @param subspace the subspace of the cluster
   * @param mean the cluster mean
   */
  public SubspaceModel(Subspace<V> subspace, V mean) {
    super(mean);
    this.subspace = subspace;
  }

  /**
   * Returns the subspace of this SubspaceModel.
   * 
   * @return the subspace
   */
  public Subspace<V> getSubspace() {
    return subspace;
  }

  /**
   * Returns the BitSet that represents the dimensions of the subspace of this
   * SubspaceModel.
   * 
   * @return the dimensions of the subspace
   */
  public BitSet getDimensions() {
    return subspace.getDimensions();
  }

  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    super.writeToText(out, label);
    out.commentPrintLn("Subspace: " + subspace.toString());
  }
}
