package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.algorithm.timeseries.ChangePoints;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

import java.util.List;

public class ChangePointDetectionResult extends BasicResult implements TextWriteable {

    private List<ChangePoints> results;
    public ChangePointDetectionResult(String name, String shortname, List<ChangePoints> results) {
        super(name, shortname);
        this.results = results;
    }

    public void writeToText(TextWriterStream out, String label) {
        for(ChangePoints cp : results) {
            out.inlinePrintNoQuotes(cp.appendTo(new StringBuilder()));
            out.flush();
        }
    }
}
