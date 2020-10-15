package org.mate.exploration.genetic.chromosome_factory;

import org.mate.MATE;
import org.mate.Properties;
import org.mate.exploration.genetic.chromosome.Chromosome;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.fitness.LineCoveredPercentageFitnessFunction;
import org.mate.interaction.UIAbstractionLayer;
import org.mate.model.TestCase;
import org.mate.ui.Action;
import org.mate.ui.ActionType;
import org.mate.ui.EnvironmentManager;
import org.mate.utils.Randomness;

import java.util.Arrays;
import java.util.List;


public class AndroidRandomChromosomeFactory implements IChromosomeFactory<TestCase> {
    public static final String CHROMOSOME_FACTORY_ID = "android_random_chromosome_factory";

    protected UIAbstractionLayer uiAbstractionLayer;
    private int maxNumEvents;
    private boolean storeCoverage;
    private boolean resetApp;

    public AndroidRandomChromosomeFactory(int maxNumEvents) {
        this(Properties.STORE_COVERAGE, true, maxNumEvents);
    }

    public AndroidRandomChromosomeFactory(boolean storeCoverage, boolean resetApp, int maxNumEvents) {
        this.uiAbstractionLayer = MATE.uiAbstractionLayer;
        this.maxNumEvents = maxNumEvents;
        this.storeCoverage = storeCoverage;
        this.resetApp = resetApp;
    }

    @Override
    public IChromosome<TestCase> createChromosome() {
        if (resetApp) {
            uiAbstractionLayer.resetApp();
        }

        TestCase testCase = TestCase.newInitializedTestCase();
        Chromosome<TestCase> chromosome = new Chromosome<>(testCase);

        LoginStrategy[] loginStrategies = {
                new SuccessfulLogin(true),
                new SuccessfulLogin(false),
                new SuccessfulLogin(true, true),
                new SuccessfulLogin(false, true),
                new MissingDNILogin(),
                new MissingNroTramiteLogin(),
                new MissingSexLogin(),
        };
        LoginStrategy loginStrategy = Randomness.randomElement(Arrays.asList(loginStrategies));

        try {
            for (int i = 0; i < maxNumEvents; i++) {
                if (MATE.timeoutReached.get()) {
                    return null;
                }

                Action selectedAction;
                if (loginStrategy.hasNextAction()) {
                    selectedAction = loginStrategy.getNextAction();
                } else {
                    selectedAction = selectRandomAction();
                }

                if (!testCase.updateTestCase(selectedAction, String.valueOf(i))) {
                    return chromosome;
                }
            }
        } finally {
            //store coverage in an case
            if (storeCoverage) {
                EnvironmentManager.storeCoverageData(chromosome, null);

                MATE.log_acc("Coverage of: " + chromosome.toString() + ": " + EnvironmentManager
                        .getCoverage(chromosome));
                MATE.log_acc("Found crash: " + String.valueOf(chromosome.getValue().getCrashDetected()));

                //TODO: remove hack, when better solution implemented
                LineCoveredPercentageFitnessFunction.retrieveFitnessValues(chromosome);
            }
        }


        return chromosome;
    }

    protected Action selectRandomAction() {
        List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
        return Randomness.randomElement(executableActions);
    }

    private abstract class LoginStrategy {
        protected int totalSteps;
        protected int currentStep;

        private LoginStrategy(int totalSteps) {
            this.totalSteps = totalSteps;
            this.currentStep = 0;
        }

        public abstract Action getNextAction();

        public boolean hasNextAction() {
            return currentStep < totalSteps;
        }

        protected Action getDNIAction(List<Action> executableActions, String dni) {
            for (Action action: executableActions) {
                if (action.getWidget().getHint().equals("DNI")) {
                    action.setExtraInfo(dni);
                    return action;
                }
            }
            return null;
        }

        protected Action getNroTramiteAction(List<Action> executableActions, String nroTramite) {
            for (Action action: executableActions) {
                if (action.getWidget().getHint().equals("Nro de trámite")) {
                    action.setExtraInfo(nroTramite);
                    return action;
                }
            }
            return null;
        }

        protected Action getSexoAction(List<Action> executableActions, String sexo) {
            for (Action action: executableActions) {
                if (action.getWidget().getText().equals(sexo)) {
                    return action;
                }
            }
            return null;
        }

        protected Action getSwipeDownAction(List<Action> executableActions) {
            for (Action action: executableActions) {
                if (action.getActionType().equals(ActionType.SWIPE_DOWN)) {
                    return action;
                }
            }
            return null;
        }

        protected Action getSiguienteAction(List<Action> executableActions) {
            for (Action action: executableActions) {
                if (action.getWidget().getText().equals("SIGUIENTE")) {
                    return action;
                }
            }
            return null;
        }

        protected Action getRestartAppAction() {
            return new Action(ActionType.RESTART);
        }
    }

    private class SuccessfulLogin extends LoginStrategy {
        private boolean restartAppAfterLogin = false;
        private String dni;
        private String nroTramite;
        private String sexo;

        private SuccessfulLogin(int totalSteps) {
            super(totalSteps);
        }

        public SuccessfulLogin(boolean infected) {
            this(infected, false);
        }

        public SuccessfulLogin(boolean infected, boolean restartAppAfterLogin) {
            super(restartAppAfterLogin ? 6: 5);
            this.restartAppAfterLogin = restartAppAfterLogin;
            if (infected) {
                dni = "22222222";
                nroTramite = "222";
                sexo = "Masculino";
            } else {
                dni = "11111111";
                nroTramite = "111";
                sexo = "Femenino";
            }
        }

        @Override
        public Action getNextAction() {
            List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
            Action chosenAction = null;
            if (currentStep == 0) {
                chosenAction = getDNIAction(executableActions, dni);
            } else if (currentStep == 1) {
                chosenAction = getNroTramiteAction(executableActions, nroTramite);
            } else if (currentStep == 2) {
                chosenAction = getSexoAction(executableActions, sexo);
            } else if (currentStep == 3) {
                chosenAction = getSwipeDownAction(executableActions);
            } else if (currentStep == 4) {
                chosenAction = getSiguienteAction(executableActions);
            } else if (restartAppAfterLogin && currentStep == 5) {
                chosenAction = getRestartAppAction();
            }

            if (chosenAction == null) {
                throw new Error("No se encontró la acción correspondiente para el paso" +
                        currentStep + " en el LoginStrategy " + this.getClass().getSimpleName());
            }

            currentStep++;
            return chosenAction;
        }
    }

    private class MissingDNILogin extends LoginStrategy {
        private String nroTramite;
        private String sexo;

        private MissingDNILogin(int totalSteps) {
            super(totalSteps);
        }

        public MissingDNILogin() {
            super(4);
            nroTramite = "222";
            sexo = "Masculino";
        }

        @Override
        public Action getNextAction() {
            List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
            Action chosenAction = null;
            if (currentStep == 0) {
                chosenAction = getNroTramiteAction(executableActions, nroTramite);
            } else if (currentStep == 1) {
                chosenAction = getSexoAction(executableActions, sexo);
            } else if (currentStep == 2) {
                chosenAction = getSwipeDownAction(executableActions);
            } else if (currentStep == 3) {
                chosenAction = getSiguienteAction(executableActions);
            }

            if (chosenAction == null) {
                throw new Error("No se encontró la acción correspondiente para el paso" +
                        currentStep + " en el LoginStrategy " + this.getClass().getSimpleName());
            }

            currentStep++;
            return chosenAction;
        }
    }

    private class MissingNroTramiteLogin extends LoginStrategy {
        private String dni;
        private String sexo;

        private MissingNroTramiteLogin(int totalSteps) {
            super(totalSteps);
        }

        public MissingNroTramiteLogin() {
            super(4);
            dni = "22222222";
            sexo = "Masculino";
        }

        @Override
        public Action getNextAction() {
            List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
            Action chosenAction = null;
            if (currentStep == 0) {
                chosenAction = getDNIAction(executableActions, dni);
            } else if (currentStep == 1) {
                chosenAction = getSexoAction(executableActions, sexo);
            } else if (currentStep == 2) {
                chosenAction = getSwipeDownAction(executableActions);
            } else if (currentStep == 3) {
                chosenAction = getSiguienteAction(executableActions);
            }

            if (chosenAction == null) {
                throw new Error("No se encontró la acción correspondiente para el paso" +
                        currentStep + " en el LoginStrategy " + this.getClass().getSimpleName());
            }

            currentStep++;
            return chosenAction;
        }
    }

    private class MissingSexLogin extends LoginStrategy {
        private String dni;
        private String nroTramite;

        private MissingSexLogin(int totalSteps) {
            super(totalSteps);
        }

        public MissingSexLogin() {
            super(4);
            dni = "22222222";
            nroTramite = "222";
        }

        @Override
        public Action getNextAction() {
            List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
            Action chosenAction = null;
            if (currentStep == 0) {
                chosenAction = getDNIAction(executableActions, dni);
            } else if (currentStep == 1) {
                chosenAction = getNroTramiteAction(executableActions, nroTramite);
            } else if (currentStep == 2) {
                chosenAction = getSwipeDownAction(executableActions);
            } else if (currentStep == 3) {
                chosenAction = getSiguienteAction(executableActions);
            }

            if (chosenAction == null) {
                throw new Error("No se encontró la acción correspondiente para el paso" +
                        currentStep + " en el LoginStrategy " + this.getClass().getSimpleName());
            }

            currentStep++;
            return chosenAction;
        }
    }
}
