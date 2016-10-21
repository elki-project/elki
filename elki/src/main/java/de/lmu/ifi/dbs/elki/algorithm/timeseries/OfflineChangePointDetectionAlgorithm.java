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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import java.util.*;

public class OfflineChangePointDetectionAlgorithm extends AbstractAlgorithm<ChangePointDetectionResult> {

    private int confidence, bootstrapSteps;

    public OfflineChangePointDetectionAlgorithm(int confidence, int bootstrapSteps) {
        this.confidence = confidence;
        this.bootstrapSteps = bootstrapSteps;
    }

    public ChangePointDetectionResult run(Database database, Relation<DoubleVector> relation, Relation<LabelList> labellist) {

        List<ChangePoints> result = new ArrayList<>();

        for(DBIDIter realtion_iter = relation.getDBIDs().iter(); realtion_iter.valid(); realtion_iter.advance()) {

            result.add(new ChangePoints(
                    multipleChangepointsWithConfidence(relation.get(realtion_iter).getValues(), confidence, bootstrapSteps)));
        }

        return new ChangePointDetectionResult("Change Point List", "changepoints", result, labellist);
    }

    private double[] likelihoodRatioChangeInMean(double[] values){
        double[] result = new double[values.length];

        // vector containing means for all different vector lengths, last index contains mean over all elements
        double[] meansLeft = new double[values.length];
        Mean currentMeanLeft = new Mean();
        for(int i = 0; i < meansLeft.length; i++){
            currentMeanLeft.put(values[i]);
            meansLeft[i] = currentMeanLeft.getMean();
        }
        // first index contains mean over all elements coming from the other side
        double[] meansRight = new double[values.length];
        Mean currentMeanRight = new Mean();
        for(int i = meansRight.length-1; i >= 0; i--){
            currentMeanRight.put(values[i]);
            meansRight[i] = currentMeanRight.getMean();
        }

        result[0] = -(VMath.sumElements(VMath.square(VMath.minus(values, meansRight[0]))));
        for(int i = 1; i < values.length; i++){
            result[i] = -(  (VMath.squareSum(VMath.minus(Arrays.copyOfRange(values, 0, i), meansLeft[i-1])))
                        +   (VMath.squareSum(VMath.minus(Arrays.copyOfRange(values, i, values.length), meansRight[i])))
                        );
        }

        return result;
    }

    // DOES NOT WORK - REASON NOT YET INVESTIGATED
    private double[] likelihoodRatioChangeInMeanOptimised(double[] values){
        double[] result = new double[values.length];

        // vector containing means for all different vector lengths, last index contains mean over all elements
        double[] meansLeft = new double[values.length];
        Mean currentMeanLeft = new Mean();
        for(int i = 0; i < meansLeft.length; i++){
            currentMeanLeft.put(values[i]);
            meansLeft[i] = currentMeanLeft.getMean();
        }
        // first index contains mean over all elements coming from the other side
        double[] meansRight = new double[values.length];
        Mean currentMeanRight = new Mean();
        for(int i = meansRight.length-1; i >= 0; i--){
            currentMeanRight.put(values[i]);
            meansRight[i] = currentMeanRight.getMean();
        }

        result[0]=0;
        double tmpMeanDif;
        for(int i = 1; i < values.length; i++) {
            tmpMeanDif = meansLeft[i-1] - meansRight[i];
            result[i]= -i*(values.length - i)*tmpMeanDif*tmpMeanDif;
        }

        return result;

    }

    private List<ChangePoint> multipleChangepointsWithConfidence(double[] values, int confidence, int bootstrapSteps){
        List<ChangePoint> result = new ArrayList<>();
        result = multipleChangepointsWithConfidence(result, values, confidence, bootstrapSteps, 0);
        return result;
    }

    private List<ChangePoint> multipleChangepointsWithConfidence(List<ChangePoint> result, double[] values, int confidence, int bootstrapSteps, int tmpArraryStartIndex){
        double tmpConf = confidenceLevel(values, bootstrapSteps);
        int tmpMaxPos = tmpArraryStartIndex + getMaximumIndex(likelihoodRatioChangeInMean(values)); // return the detected changepoint

        if(!(tmpConf < confidence || values.length <=3 || (tmpMaxPos - tmpArraryStartIndex + 1 == values.length))){ // cannot split up arrays of size 3, that would make every element a change point
            multipleChangepointsWithConfidence(result
                                                    , Arrays.copyOfRange(values, 0, tmpMaxPos - tmpArraryStartIndex)
                                                    , confidence
                                                    , bootstrapSteps
                                                    , tmpArraryStartIndex);
            multipleChangepointsWithConfidence(result
                                                    , Arrays.copyOfRange(values, tmpMaxPos - tmpArraryStartIndex + 1, values.length)
                                                    , confidence
                                                    , bootstrapSteps
                                                    , tmpMaxPos);
            result.add(new ChangePoint(tmpMaxPos, tmpConf));
        }

        return result;
    }

    //TRY OUT RESULT HANDLING
    private ChangePoint singleChangepointWithConfidence(double[] values, int bootstrapSteps){
        double conf = confidenceLevel(values, bootstrapSteps);
        double index = getMaximumIndex(likelihoodRatioChangeInMean(values));
        return new ChangePoint(index, conf);
    }


    private double confidenceLevel(double[] values, int steps){
        double estimator = getBootstrapEstimator(likelihoodRatioChangeInMean(values));
        int x = 0;
        for(int i=0; i < steps; i++){
            double[] tmpValues = shuffleVector(values);
            double tmpEstimator = getBootstrapEstimator(likelihoodRatioChangeInMean(tmpValues));
            if (tmpEstimator < estimator){
                x += 1;
            }
        }
        return 100 * ((double)x/(double)steps);
    }

    private double getBootstrapEstimator(double[] values){
        return getMaximum(values)- getMinimum(values);
    }

    // move to VMath??
    private double[] shuffleVector(double[] values)
    {
        double[] result= VMath.copy(values);
        Random rnd = new Random();
        for (int i = result.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            double tmp = result[index];
            result[index] = result[i];
            result[i] = tmp;
        }

        return result;
    }

    // move to VMath??
    private int getMaximumIndex(double[] values){
        int result = 0;
        for (int i = 0; i < values.length; i++){
            if ((values[i] >= values[result])){
                result = i;
            }
        }
        return result;
    }

    // move to VMath??
    private double getMaximum(double[] values){
        double result = values[0];
        for(double value : values){
            if(value > result){
                result = value;
            }
        }
        return result;
    }

    // move to VMath??
    private double getMinimum(double[] values){
        double result = values[0];
        for(double value : values){
            if(value < result){
                result = value;
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

        public static final OptionID CONFIDENCE_ID = new OptionID("changepointdetection.confidence", //
                "Confidence level for terminating");

        public static final OptionID BOOTSTRAP_ID = new OptionID("changepointdetection.bootstrapsteps", //
                "Steps for bootstrapping");

        private int confidence, bootstrap_steps;

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);
            IntParameter confidence_parameter = new IntParameter(CONFIDENCE_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
            if(config.grab(confidence_parameter)) {
                confidence = confidence_parameter.getValue();
            }
            IntParameter bootstrap_steps_parameter = new IntParameter(BOOTSTRAP_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
            if(config.grab(bootstrap_steps_parameter)) {
                bootstrap_steps = bootstrap_steps_parameter.getValue();
            }
        }

        @Override
        protected OfflineChangePointDetectionAlgorithm makeInstance() {
            return new OfflineChangePointDetectionAlgorithm(confidence, bootstrap_steps);
        }
    }
}
