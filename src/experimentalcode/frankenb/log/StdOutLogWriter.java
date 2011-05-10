package experimentalcode.frankenb.log;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class StdOutLogWriter implements ILogWriter {

  @Override
  public void putLogLine(LogLevel level, String formattedMessage) {
    System.out.println(formattedMessage);
  }

}
