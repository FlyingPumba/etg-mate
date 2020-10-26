package org.mate.exploration.login_strategies;

import org.mate.exploration.genetic.chromosome_factory.AndroidRandomChromosomeFactory;
import org.mate.exploration.genetic.chromosome_factory.IChromosomeFactory;
import org.mate.model.TestCase;
import org.mate.ui.Action;

import java.util.List;

public class SuccessfulLogin extends LoginStrategy {
    private boolean restartAppAfterLogin = false;
    private String dni;
    private String nroTramite;
    private String sexo;

    private SuccessfulLogin(IChromosomeFactory<TestCase> chromosomeFactory, int totalSteps) {
        super(chromosomeFactory, totalSteps);
    }

    public SuccessfulLogin(IChromosomeFactory<TestCase> chromosomeFactory, boolean infected) {
        this(chromosomeFactory, infected, false);
    }

    public SuccessfulLogin(IChromosomeFactory<TestCase> chromosomeFactory, boolean infected,
                           boolean restartAppAfterLogin) {
        super(chromosomeFactory, restartAppAfterLogin ? 6: 5);
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
    public Action getNextAction() throws Exception {
        List<Action> executableActions = chromosomeFactory.getUiAbstractionLayer().getExecutableActions();
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
            throw new Exception("No se encontró la acción correspondiente para el paso " +
                    currentStep + " en el LoginStrategy " + this.getClass().getSimpleName());
        }

        currentStep++;
        return chosenAction;
    }
}
