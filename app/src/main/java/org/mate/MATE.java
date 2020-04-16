package org.mate;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.mate.exceptions.AUTCrashException;
import org.mate.exploration.genetic.algorithm.MOSA;
import org.mate.exploration.genetic.algorithm.Mio;
import org.mate.exploration.genetic.algorithm.NSGAII;
import org.mate.exploration.genetic.algorithm.RandomWalk;
import org.mate.exploration.genetic.builder.GeneticAlgorithmBuilder;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.chromosome_factory.AndroidRandomChromosomeFactory;
import org.mate.exploration.genetic.chromosome_factory.AndroidSuiteRandomChromosomeFactory;
import org.mate.exploration.genetic.core.GenericGeneticAlgorithm;
import org.mate.exploration.genetic.core.IGeneticAlgorithm;
import org.mate.exploration.genetic.crossover.TestCaseMergeCrossOverFunction;
import org.mate.exploration.genetic.crossover.UniformSuiteCrossoverFunction;
import org.mate.exploration.genetic.fitness.ActivityFitnessFunction;
import org.mate.exploration.genetic.fitness.AmountCrashesFitnessFunction;
import org.mate.exploration.genetic.fitness.AndroidStateFitnessFunction;
import org.mate.exploration.genetic.fitness.IFitnessFunction;
import org.mate.exploration.genetic.fitness.LineCoveredPercentageFitnessFunction;
import org.mate.exploration.genetic.fitness.StatementCoverageFitnessFunction;
import org.mate.exploration.genetic.fitness.TestLengthFitnessFunction;
import org.mate.exploration.genetic.mutation.CutPointMutationFunction;
import org.mate.exploration.genetic.mutation.SapienzSuiteMutationFunction;
import org.mate.exploration.genetic.selection.FitnessProportionateSelectionFunction;
import org.mate.exploration.genetic.selection.FitnessSelectionFunction;
import org.mate.exploration.genetic.selection.RandomSelectionFunction;
import org.mate.exploration.genetic.termination.IterTerminationCondition;
import org.mate.exploration.genetic.termination.NeverTerminationCondition;
import org.mate.exploration.heuristical.HeuristicExploration;
import org.mate.exploration.heuristical.RandomExploration;
import org.mate.interaction.DeviceMgr;
import org.mate.interaction.UIAbstractionLayer;
import org.mate.model.IGUIModel;
import org.mate.model.TestCase;
import org.mate.model.TestSuite;
import org.mate.model.graph.GraphGUIModel;
import org.mate.state.IScreenState;
import org.mate.state.ScreenStateFactory;
import org.mate.ui.Action;
import org.mate.ui.EnvironmentManager;
import org.mate.utils.TimeoutRun;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * Created by marceloe on 07/03/17.
 */

public class MATE {

    public static UiDevice device;
    public static UIAbstractionLayer uiAbstractionLayer;
    public static String packageName;
    public static IGUIModel guiModel;
    private Vector<Action> actions;
    private DeviceMgr deviceMgr;
    public static long total_time;
    public static long RANDOM_LENGH;
    private long runningTime = new Date().getTime();
    public static long TIME_OUT;
    public Instrumentation instrumentation;

    private GraphGUIModel completeModel;

    public static String logMessage;

    public static final AtomicBoolean timeoutReached = new AtomicBoolean(false);


    //public static Vector<String> checkedWidgets = new Vector<String>();
    public static Set<String> visitedActivities = new HashSet<String>();

    public MATE() {
        try (FileInputStream fis = InstrumentationRegistry.getInstrumentation().getTargetContext().openFileInput("port");
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            EnvironmentManager.port = Integer.valueOf(reader.readLine());
            MATE.log_acc("Using server port: " + EnvironmentManager.port);
        } catch (IOException e) {
            //ignore: use default port if file does not exists
        }

        //get timeout from server using EnvironmentManager
        long timeout = EnvironmentManager.getTimeout();
        if (timeout == 0)
            timeout = 60; //set default - 60 minutes
        MATE.TIME_OUT = timeout * 60 * 1000;
        MATE.log("TIMEOUT : " + timeout);

        //get random length = number of actions before restarting the app
        long rlength = EnvironmentManager.getRandomLength();
        if (rlength == 0)
            rlength = 1000; //default
        MATE.RANDOM_LENGH = rlength;
        MATE.log("RANDOM length by server: " + MATE.RANDOM_LENGH);

        logMessage = "";

        //Defines the class that represents the device
        //Instrumentation instrumentation =  getInstrumentation();
        instrumentation = getInstrumentation();
        device = UiDevice.getInstance(instrumentation);

        //get the name of the package of the app currently running
        packageName = device.getCurrentPackageName();

        // fetch emulator before calling handleAuth method, otherwise it will throw an exception on
        // MATE server when sending a null emulator
        EnvironmentManager.detectEmulator(packageName);

        //checks whether user needs to authorize access to something on the device/emulator
        handleAuth(device);
        MATE.log("Package name: " + packageName);

        //list the activities of the app under test
        listActivities(instrumentation.getContext());

    }

    public void testApp(String explorationStrategy) {

        String emulator = EnvironmentManager.detectEmulator(packageName);
        MATE.log("EMULATOR: " + emulator);

        runningTime = new Date().getTime();
        try {
            if (emulator != null && !emulator.equals("")) {
                this.deviceMgr = new DeviceMgr(packageName);

                if (explorationStrategy.equals("OnePlusOneNew")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);

                    IGeneticAlgorithm<TestCase> onePlusOneNew = new GeneticAlgorithmBuilder()
                            .withAlgorithm(org.mate.exploration.genetic.algorithm.OnePlusOne.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withSelectionFunction(FitnessSelectionFunction.SELECTION_FUNCTION_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withFitnessFunction(AndroidStateFitnessFunction.FITNESS_FUNCTION_ID)
                            .withTerminationCondition(IterTerminationCondition.TERMINATION_CONDITION_ID)
                            .build();
                    onePlusOneNew.run();
                } else if (explorationStrategy.equals("NSGA-II")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log_acc("Activities");
                    for (String s : EnvironmentManager.getActivityNames()) {
                        MATE.log_acc("\t" + s);
                    }

                    IGeneticAlgorithm<TestCase> nsga = new GeneticAlgorithmBuilder()
                            .withAlgorithm(NSGAII.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withSelectionFunction(FitnessSelectionFunction.SELECTION_FUNCTION_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withFitnessFunction(ActivityFitnessFunction.FITNESS_FUNCTION_ID)
                            .withFitnessFunction(AndroidStateFitnessFunction.FITNESS_FUNCTION_ID)
                            .withTerminationCondition(IterTerminationCondition.TERMINATION_CONDITION_ID)
                            .build();
                    nsga.run();
                } else if (explorationStrategy.equals("GenericGeneticAlgorithm")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log_acc("Activities");
                    for (String s : EnvironmentManager.getActivityNames()) {
                        MATE.log_acc("\t" + s);
                    }

                    final IGeneticAlgorithm<TestCase> genericGA = new GeneticAlgorithmBuilder()
                            .withAlgorithm(GenericGeneticAlgorithm.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withSelectionFunction(FitnessProportionateSelectionFunction.SELECTION_FUNCTION_ID)
                            .withCrossoverFunction(TestCaseMergeCrossOverFunction.CROSSOVER_FUNCTION_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withFitnessFunction(StatementCoverageFitnessFunction.FITNESS_FUNCTION_ID)
                            .withTerminationCondition(NeverTerminationCondition.TERMINATION_CONDITION_ID)
                            .withPopulationSize(50)
                            .withBigPopulationSize(100)
                            .withMaxNumEvents(50)
                            .withPMutate(0.3)
                            .withPCrossover(0.7)
                            .build();
                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            genericGA.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    if (Properties.STORE_COVERAGE) {
                        EnvironmentManager.storeCoverageData(genericGA, null);
                        MATE.log_acc("Total coverage: " + EnvironmentManager.getCombinedCoverage());
                    }
                } else if (explorationStrategy.equals("Sapienz")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log_acc("Activities");
                    for (String s : EnvironmentManager.getActivityNames()) {
                        MATE.log_acc("\t" + s);
                    }

                    final IGeneticAlgorithm<TestSuite> nsga = new GeneticAlgorithmBuilder()
                            .withAlgorithm(NSGAII.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidSuiteRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withCrossoverFunction(UniformSuiteCrossoverFunction.CROSSOVER_FUNCTION_ID)
                            .withSelectionFunction(RandomSelectionFunction.SELECTION_FUNCTION_ID)
                            .withMutationFunction(SapienzSuiteMutationFunction.MUTATION_FUNCTION_ID)
                            .withFitnessFunction(StatementCoverageFitnessFunction.FITNESS_FUNCTION_ID)
                            .withFitnessFunction(AmountCrashesFitnessFunction.FITNESS_FUNCTION_ID)
                            .withFitnessFunction(TestLengthFitnessFunction.FITNESS_FUNCTION_ID)
                            .withTerminationCondition(NeverTerminationCondition.TERMINATION_CONDITION_ID)
                            .withPopulationSize(50)
                            .withBigPopulationSize(100)
                            .withMaxNumEvents(50)
                            .withPMutate(1)
                            .withPInnerMutate(0.3)
                            .withPCrossover(0.7)
                            .withNumTestCases(5)
                            .build();

                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            nsga.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    if (Properties.STORE_COVERAGE) {
                        EnvironmentManager.storeCoverageData(nsga, null);
                        MATE.log_acc("Total coverage: " + EnvironmentManager.getCombinedCoverage());
                    }
                } else if (explorationStrategy.equals("HeuristicRandom")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log_acc("Activities");
                    for (String s : EnvironmentManager.getActivityNames()) {
                        MATE.log_acc("\t" + s);
                    }

                    final HeuristicExploration heuristicExploration = new HeuristicExploration(50);

                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            heuristicExploration.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    if (Properties.STORE_COVERAGE) {
                        EnvironmentManager.storeCoverageData(heuristicExploration, null);
                        MATE.log_acc("Total coverage: " + EnvironmentManager.getCombinedCoverage());
                    }
                } else if (explorationStrategy.equals("RandomExploration")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log_acc("Activities");
                    for (String s : EnvironmentManager.getActivityNames()) {
                        MATE.log_acc("\t" + s);
                    }

                    final RandomExploration randomExploration = new RandomExploration(50);

                    timeoutReached.set(false);

                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            randomExploration.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    timeoutReached.set(true);

                    if (Properties.STORE_COVERAGE) {
                        EnvironmentManager.storeCoverageData(randomExploration, null);
                        MATE.log_acc("Total coverage: " + EnvironmentManager.getCombinedCoverage());
                    }

                    randomExploration.takeScreenshotsToRepresentativeIndividuals();
                    List<IChromosome<TestCase>> individuals = randomExploration.getRepresentativeIndividual();
                    IFitnessFunction<TestCase> fitnessFunction = randomExploration.getFitnessFunctions();

                    List<TestCase> testCases = new ArrayList<>();
                    for (IChromosome<TestCase> individual : individuals) {
                        TestCase testCase = individual.getValue();

                        testCase.setCoverage(fitnessFunction.getFitness(individual));
                        testCase.setChromosomeHash(individual.toString());

                        testCases.add(testCase);
                    }

                    // write test cases to JSON and save in Server
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonString = mapper.writeValueAsString(testCases);
                    EnvironmentManager.storeJsonTestCases(jsonString);
                } else if (explorationStrategy.equals(MOSA.ALGORITHM_NAME)) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);

                    final GeneticAlgorithmBuilder builder = new GeneticAlgorithmBuilder()
                            .withAlgorithm(MOSA.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withCrossoverFunction(TestCaseMergeCrossOverFunction.CROSSOVER_FUNCTION_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withSelectionFunction(RandomSelectionFunction.SELECTION_FUNCTION_ID) //todo: use better selection function
                            .withTerminationCondition(NeverTerminationCondition.TERMINATION_CONDITION_ID)
                            .withPopulationSize(50)
                            .withBigPopulationSize(100)
                            .withMaxNumEvents(50)
                            .withPMutate(0.3)
                            .withPCrossover(0.7);

                    // add specific fitness functions for all activities of the Application Under Test
                    MATE.log_acc("Retrieving source lines...");
                    List<String> lines = EnvironmentManager.getSourceLines();
                    MATE.log_acc("Retrieved " + lines.size() + " lines.");
                    MATE.log_acc("Processing lines...");
                    int count = 1;
                    for (String line : lines) {
                        if (count % (lines.size() / 10) == 0) {
                            MATE.log_acc(Math.ceil(count * 100.0 / lines.size()) + "%");
                        }
                        builder.withFitnessFunction(LineCoveredPercentageFitnessFunction.FITNESS_FUNCTION_ID, line);
                        count++;
                    }
                    MATE.log_acc("done processing lines");

                    final IGeneticAlgorithm<TestCase> mosa = builder.build();
                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            mosa.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    if (Properties.STORE_COVERAGE) {
                        EnvironmentManager.storeCoverageData(mosa, null);
                        MATE.log_acc("Total coverage: " + EnvironmentManager.getCombinedCoverage());
                    }
                } else if (explorationStrategy.equals("Mio")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);

                    final GeneticAlgorithmBuilder builder = new GeneticAlgorithmBuilder()
                            .withAlgorithm(Mio.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withCrossoverFunction(TestCaseMergeCrossOverFunction.CROSSOVER_FUNCTION_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withSelectionFunction(RandomSelectionFunction.SELECTION_FUNCTION_ID) //todo: use better selection function
                            .withTerminationCondition(NeverTerminationCondition.TERMINATION_CONDITION_ID)
                            .withPopulationSize(50)
                            .withBigPopulationSize(100)
                            .withMaxNumEvents(50)
                            .withPMutate(0.3)
                            .withPCrossover(0.7)
                            .withPSampleRandom(0.5)
                            .withFocusedSearchStart(0.5);

                    // add specific fitness functions for all activities of the Application Under Test
                    MATE.log_acc("Retrieving source lines...");
                    List<String> lines = EnvironmentManager.getSourceLines();
                    MATE.log_acc("Retrieved " + lines.size() + " lines.");
                    MATE.log_acc("Processing lines...");
                    int count = 1;
                    for (String line : lines) {
                        if (count % (lines.size() / 10) == 0) {
                            MATE.log_acc(Math.ceil(count * 100.0 / lines.size()) + "%");
                        }
                        builder.withFitnessFunction(LineCoveredPercentageFitnessFunction.FITNESS_FUNCTION_ID, line);
                        count++;
                    }
                    MATE.log_acc("done processing lines");

                    final IGeneticAlgorithm<TestCase> mio = builder.build();
                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            mio.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    if (Properties.STORE_COVERAGE) {
                        EnvironmentManager.storeCoverageData(mio, null);
                        MATE.log_acc("Total coverage: " + EnvironmentManager.getCombinedCoverage());
                    }
                } else if (explorationStrategy.equals("RandomWalk")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log("Starting random walk now ...");

                    final GeneticAlgorithmBuilder builder = new GeneticAlgorithmBuilder()
                            .withAlgorithm(RandomWalk.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withTerminationCondition(NeverTerminationCondition.TERMINATION_CONDITION_ID)
                            .withFitnessFunction(StatementCoverageFitnessFunction.FITNESS_FUNCTION_ID)
                            .withMaxNumEvents(50);

                    final IGeneticAlgorithm<TestCase> randomWalk = builder.build();
                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            randomWalk.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    List<IChromosome<TestCase>> survivors = randomWalk.getGenerationSurvivors();

                    List<TestCase> testCases = new ArrayList<>();
                    for (IChromosome<TestCase> survivor : survivors) {
                        TestCase testCase = survivor.getValue();

                        List<IFitnessFunction<TestCase>> fitnessFunctions = randomWalk.getFitnessFunctions();
                        IFitnessFunction<TestCase> fitnessFunction = fitnessFunctions.get(0);
                        testCase.setCoverage(fitnessFunction.getFitness(survivor));
                        testCase.setChromosomeHash(survivor.toString());

                        testCases.add(testCase);
                    }

                    // write test cases to JSON and save in Server
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonString = mapper.writeValueAsString(testCases);
                    EnvironmentManager.storeJsonTestCases(jsonString);

                } else if (explorationStrategy.equals("RandomWalkActivityCoverage")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log("Starting random walk now ...");

                    final GeneticAlgorithmBuilder builder = new GeneticAlgorithmBuilder()
                            .withAlgorithm(RandomWalk.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withTerminationCondition(IterTerminationCondition.TERMINATION_CONDITION_ID)
                            .withFitnessFunction(ActivityFitnessFunction.FITNESS_FUNCTION_ID)
                            .withMaxNumEvents(50);


                    final IGeneticAlgorithm<TestCase> randomWalk = builder.build();
                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            randomWalk.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);

                    List<IChromosome<TestCase>> survivors = randomWalk.getGenerationSurvivors();

                    List<TestCase> testCases = new ArrayList<>();
                    for (IChromosome<TestCase> survivor : survivors) {
                        testCases.add(survivor.getValue());
                    }

                    // write test cases to JSON and save in Server
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonString = mapper.writeValueAsString(testCases);
                    EnvironmentManager.storeJsonTestCases(jsonString);

                } else if (explorationStrategy.equals("RandomWalkStateCoverage")) {
                    uiAbstractionLayer = new UIAbstractionLayer(deviceMgr, packageName);
                    MATE.log("Starting random walk now ...");

                    final GeneticAlgorithmBuilder builder = new GeneticAlgorithmBuilder()
                            .withAlgorithm(RandomWalk.ALGORITHM_NAME)
                            .withChromosomeFactory(AndroidRandomChromosomeFactory.CHROMOSOME_FACTORY_ID)
                            .withMutationFunction(CutPointMutationFunction.MUTATION_FUNCTION_ID)
                            .withTerminationCondition(NeverTerminationCondition.TERMINATION_CONDITION_ID)
                            .withFitnessFunction(AndroidStateFitnessFunction.FITNESS_FUNCTION_ID)
                            .withMaxNumEvents(50);


                    final IGeneticAlgorithm<TestCase> randomWalk = builder.build();
                    TimeoutRun.timeoutRun(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            randomWalk.run();
                            return null;
                        }
                    }, MATE.TIME_OUT);
                }
            } else
                MATE.log("Emulator is null");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            EnvironmentManager.releaseEmulator();
            //EnvironmentManager.deleteAllScreenShots(packageName);
        }

    }

    private void checkVisitedActivities(String explorationStrategy) {
        Set<String> visitedActivities = new HashSet<String>();
        for (IScreenState scnd : guiModel.getStates()) {
            visitedActivities.add(scnd.getActivityName());
        }

        MATE.log(explorationStrategy + " visited activities " + visitedActivities.size());
        for (String act : visitedActivities)
            MATE.log("   " + act);
    }

    public void handleAuth(UiDevice device) {

        if (this.packageName != null && this.packageName.contains("com.google.android.packageinstaller")) {
            long timeA = new Date().getTime();


            boolean goOn = true;
            while (goOn) {

                DeviceMgr dmgr = new DeviceMgr("");
                IScreenState screenState = ScreenStateFactory.getScreenState("ActionsScreenState");
                Vector<Action> actions = screenState.getActions();
                for (Action action : actions) {
                    if (action.getWidget().getId().contains("allow")) {
                        try {
                            dmgr.executeAction(action);
                        } catch (AUTCrashException e) {
                            MATE.log_acc(e.getStackTrace().toString());
                            e.printStackTrace();
                        }
                        break;
                    }
                }

                this.packageName = device.getCurrentPackageName();
                MATE.log("new package name: " + this.packageName);
                long timeB = new Date().getTime();
                if (timeB - timeA > 30000)
                    goOn = false;
                if (!this.packageName.contains("com.android.packageinstaller"))
                    goOn = false;
            }
        }


    }

    public static void log(String msg) {
        Log.i("apptest", msg);
    }

    public static void logsum(String msg) {
        Log.e("acc", msg);
        logMessage += msg + "\n";

    }

    public static void log_acc(String msg) {
        Log.e("acc", msg);
        logMessage += msg + "\n";
    }

    public static void log_vin(String msg) {
        Log.i("vinDebug", msg);
        logMessage += msg + "\n";
    }

    public void listActivities(Context context) {

        //list all activities of the application being executed
        PackageManager pm = (PackageManager) context.getPackageManager();
        try {
            PackageInfo pinfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = pinfo.activities;
            for (int i = 0; i < activities.length; i++) {
                log("Activity " + (i + 1) + ": " + activities[i].name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public IGUIModel getGuiModel() {
        return guiModel;
    }

    public static void logactivity(String activityName) {
        Log.i("acc", "ACTIVITY_VISITED: " + activityName);
    }
}
