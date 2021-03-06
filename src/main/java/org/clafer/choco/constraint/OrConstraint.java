package org.clafer.choco.constraint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import solver.constraints.Constraint;
import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.Variable;
import util.ESat;
import static util.ESat.TRUE;
import static util.ESat.UNDEFINED;

/**
 * Boolean or. The difference between this constraint and the one provided by
 * the library is that this one does not reify to boolean variables, thus it can
 * be added after the search has started. The one provided in the library
 * requires dynamic variable addition which is not supported.
 *
 * @author jimmy
 */
public class OrConstraint extends Constraint {

    public OrConstraint(Constraint... constraints) {
        super("or", new PropOr(buildArray(constraints), constraints));
    }

    private static Variable[] buildArray(Constraint... constraints) {
        Set<Variable> vars = new HashSet<>();
        for (Constraint constraint : constraints) {
            for (Propagator propagator : constraint.getPropagators()) {
                vars.addAll(Arrays.asList(propagator.getVars()));
                propagator.setReifiedSilent();
            }
        }
        return vars.toArray(new Variable[vars.size()]);
    }

    private static class PropOr extends Propagator<Variable> {

        private final Constraint[] constraints;

        protected PropOr(Variable[] vars, Constraint[] constraints) {
            super(vars, PropagatorPriority.LINEAR, true);
            this.constraints = constraints;
        }

        @Override
        public int getPropagationConditions(int vIdx) {
            return EventType.ALL_FINE_EVENTS.mask;
        }

        @Override
        public void propagate(int evtmask) throws ContradictionException {
            boolean allFalse = true;
            for (Constraint constraint : constraints) {
                switch (constraint.isSatisfied()) {
                    case TRUE:
                        setPassive();
                        return;
                    case UNDEFINED:
                        allFalse = false;
                        break;
                }
            }
            if (allFalse) {
                contradiction(vars[0], "All unsat.");
            }
        }

        @Override
        public void propagate(int idxVarInProp, int mask) throws ContradictionException {
            forcePropagate(EventType.CUSTOM_PROPAGATION);
        }

        @Override
        public ESat isEntailed() {
            boolean allFalse = true;
            for (Constraint constraint : constraints) {
                switch (constraint.isSatisfied()) {
                    case TRUE:
                        return ESat.TRUE;
                    case UNDEFINED:
                        allFalse = false;
                        break;
                }
            }
            return allFalse ? ESat.FALSE : ESat.UNDEFINED;
        }
    }
}
