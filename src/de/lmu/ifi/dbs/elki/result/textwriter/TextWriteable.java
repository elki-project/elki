package de.lmu.ifi.dbs.elki.result.textwriter;

/**
 * Interface for objects providing a text serialization suitable for
 * human reading and storage in CSV files.
 * 
 * @author Erich Schubert
 */
//TODO: split TextWriteable interface into data writing and metadata writing?
public interface TextWriteable {
  /**
   * Write self to the given {@link TextWriterStream}
   * @param out
   */
  public void writeToText(TextWriterStream out, String label);
}
