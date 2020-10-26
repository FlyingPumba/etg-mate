package org.mate.exploration.login_strategies;

import org.mate.exploration.genetic.chromosome_factory.IChromosomeFactory;
import org.mate.model.TestCase;
import org.mate.ui.Action;
import org.mate.ui.ActionType;

import java.util.List;

public abstract class LoginStrategy {
    protected final IChromosomeFactory<TestCase> chromosomeFactory;
    protected int totalSteps;
    protected int currentStep;

    protected LoginStrategy(IChromosomeFactory<TestCase> chromosomeFactory, int totalSteps) {
        this.chromosomeFactory = chromosomeFactory;
        this.totalSteps = totalSteps;
        this.currentStep = 0;
    }

    public abstract Action getNextAction() throws Exception;

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
