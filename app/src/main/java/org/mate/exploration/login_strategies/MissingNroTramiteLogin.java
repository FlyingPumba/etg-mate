package org.mate.exploration.login_strategies;

import org.mate.exploration.genetic.chromosome_factory.IChromosomeFactory;
import org.mate.model.TestCase;
import org.mate.ui.Action;

import java.util.List;

public class MissingNroTramiteLogin extends LoginStrategy {
    private String dni;
    private String sexo;

    private MissingNroTramiteLogin(IChromosomeFactory<TestCase> chromosomeFactory, int totalSteps) {
        super(chromosomeFactory, totalSteps);
    }

    public MissingNroTramiteLogin(IChromosomeFactory<TestCase> chromosomeFactory) {
        super(chromosomeFactory, 4);
        dni = "22222222";
        sexo = "Masculino";
    }

    @Override
    public Action getNextAction() throws Exception {
        List<Action> executableActions = chromosomeFactory.getUiAbstractionLayer().getExecutableActions();
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
            throw new Exception("No se encontró la acción correspondiente para el paso " +
                    currentStep + " en el LoginStrategy " + this.getClass().getSimpleName());
        }

        currentStep++;
        return chosenAction;
    }
}
