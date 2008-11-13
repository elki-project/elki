package de.lmu.ifi.dbs.elki.result.textwriter;

/**
 * Interface for objects providing a text serialization suiteable for
 * human reading and storage in CSV files.
 * 
 * @author Erich Schubert
 */
public interface TextWriteable {
  /**
   * Write self to the given {@link TextWriterStream}
   * @param out
   */
  public void writeToText(TextWriterStream out);
}
