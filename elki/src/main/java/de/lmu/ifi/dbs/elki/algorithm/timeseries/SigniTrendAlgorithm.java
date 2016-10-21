package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.result.ChangePointDetectionResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import java.util.*;

public class SigniTrendAlgorithm extends AbstractAlgorithm<ChangePointDetectionResult> {

    private double bias, halflife, minsigma;

    public SigniTrendAlgorithm(double bias, double halflife, double minsigma) {
        this.bias = bias;
        this.halflife = halflife;
        this.minsigma = minsigma;
    }

    public ChangePointDetectionResult run(Database database, Relation<DoubleVector> relation, Relation<LabelList> labellist) {

        List<ChangePoints> result = new ArrayList<>();

        for(DBIDIter realtion_iter = relation.getDBIDs().iter(); realtion_iter.valid(); realtion_iter.advance()) {

            result.add(new ChangePoints(detectTrend(relation.get(realtion_iter).getValues())));
        }

        return new ChangePointDetectionResult("Change Point List", "changepoints", result, labellist);
    }

    private List<ChangePoint> detectTrend(double[] values){
        List<ChangePoint> result = new ArrayList<>();

        double norm = 1/values.length;
        double beta = bias * norm;
        double alpha = 1 - Math.exp(Math.log(0.5)/halflife);

        double ewma = 0;
        double ewmavar = 0;
        double[] sigmar = new double[values.length];
        sigmar[0] = 0;

        for(int i = 1; i < values.length; i++){
            double tmpx = values[i] * norm;
            double delta = tmpx - ewma;
            ewma += alpha * delta;
            ewmavar = (1 - alpha) * (ewmavar + alpha * delta * delta);
            sigmar[i] = (tmpx - Math.max(beta, ewma)) / (Math.sqrt(ewmavar) + beta);
        }

        for(int i = 0; i < sigmar.length; i++){
            if(sigmar[i] > minsigma){
                result.add(new ChangePoint(i, sigmar[i]));
            }
        }

        return result;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH, TypeUtil.LABELLIST);
    }

    @Override
    protected Logging getLogger() {
        return null;
    }

    public static class Parameterizer extends AbstractParameterizer {

        public static final OptionID BIAS_ID = new OptionID("signitrend.bias", //
                "Bias");

        public static final OptionID HALFLIFE_ID = new OptionID("signitred.halflife", //
                "Half time");

        public static final OptionID MINSIGMA_ID = new OptionID("signitred.minsigma", //
                "Minimal Sigma");

        private double bias, halflife, minsigma;

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);
            DoubleParameter bias_parameter = new DoubleParameter(BIAS_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
            if(config.grab(bias_parameter)) {
                bias = bias_parameter.getValue();
            }
            DoubleParameter halflife_parameter = new DoubleParameter(HALFLIFE_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
            if(config.grab(halflife_parameter)) {
                halflife = halflife_parameter.getValue();
            }
            DoubleParameter minsigma_parameter = new DoubleParameter(MINSIGMA_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
            if(config.grab(minsigma_parameter)) {
                minsigma = minsigma_parameter.getValue();
            }
        }

        @Override
        protected SigniTrendAlgorithm makeInstance() {
            return new SigniTrendAlgorithm(bias, halflife, minsigma);
        }
    }
}

