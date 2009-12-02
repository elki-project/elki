package de.lmu.ifi.dbs.elki.data.model;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;

/**
 * Simple model for Axis-Parallel Subspace Clusters.
 * Where the Subspace is modeled by a BitSet of dimensions.
 * 
 * @author Erich Schubert
 *
 */
public class AxesModel extends BaseModel implements TextWriteable{
  /**
   * Storage of BitSet for subspaces
   */
  private BitSet subspaces;

  /**
   * Constructor
   * 
   * @param subspaces Subspaces
   */
  public AxesModel(BitSet subspaces) {
    super();
    this.subspaces = subspaces;
  }

  /**
   * Access subspaces bitset.
   * 
   * @return bit set
   */
  public BitSet getSubspaces() {
    return subspaces;
  }

  /**
   * Access subspaces bitset.
   * 
   * @param subspaces
   */
  public void setSubspaces(BitSet subspaces) {
    this.subspaces = subspaces;
  }  
  
  /**
   * Implementation of {@link TextWriteable} interface.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if (label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(TextWriterStream.SER_MARKER+" " + getClass().getName());
    out.commentPrintLn("Subspace: "+subspaces);
  }
}
