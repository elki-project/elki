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
 * @param <V> the type of FeatureVector the subspace contains
 * 
 */
public class SubspaceModel<V extends FeatureVector<V, ?>> extends BaseModel implements TextWriteable {
  /**
   * The subspace of the cluster.
   */
  private final Subspace<V> subspace;

  /**
   * Creates a new SubspaceModel for the specified subspace.
   * 
   * @param subspace the subspace of the cluster
   */
  public SubspaceModel(Subspace<V> subspace) {
    super();
    this.subspace = subspace;
  }

  /**
   * Creates a new SubspaceModel for a subspace of the specified dimensions.
   * 
   * @param dimensions the dimensions of the subspace
   */
  public SubspaceModel(BitSet dimensions) {
    super();
    this.subspace = new Subspace<V>(dimensions);
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
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(TextWriterStream.SER_MARKER + " " + getClass().getName());
    out.commentPrintLn("Subspace: " + subspace.toString());
  }
}
