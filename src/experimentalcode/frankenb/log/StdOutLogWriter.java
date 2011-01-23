/**
 * 
 */
package experimentalcode.frankenb.log;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class StdOutLogWriter implements ILogWriter {

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.log.ILogWriter#putLogLine(java.lang.String)
   */
  @Override
  public void putLogLine(LogLevel level, String formattedMessage) {
    System.out.println(formattedMessage);
  }

}
