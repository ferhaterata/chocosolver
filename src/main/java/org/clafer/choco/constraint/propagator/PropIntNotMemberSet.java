package org.clafer.choco.constraint.propagator;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IntVar;
import solver.variables.SetVar;
import solver.variables.Variable;
import solver.variables.delta.ISetDeltaMonitor;
import util.ESat;
import util.procedure.IntProcedure;

/**
 * Missing from the library.
 * 
 * @author jimmy
 */
public class PropIntNotMemberSet extends Propagator<Variable> {

    private IntVar element;
    private SetVar set;
    private ISetDeltaMonitor setD;

    public PropIntNotMemberSet(IntVar element, SetVar set) {
        super(new Variable[]{element, set}, PropagatorPriority.BINARY, true);
        this.element = element;
        this.set = set;
        this.setD = set.monitorDelta(aCause);
    }

    private boolean isElementVar(int idx) {
        return idx == 0;
    }

    private boolean isSetVar(int idx) {
        return idx == 1;
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if (isElementVar(vIdx)) {
            return EventType.INSTANTIATE.mask;
        }
        assert isSetVar(vIdx);
        return EventType.ADD_TO_KER.mask;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = set.getKernelFirst(); i != SetVar.END; i = set.getKernelNext()) {
            element.removeValue(i, aCause);
        }
        if (element.isInstantiated()) {
            set.removeFromEnvelope(element.getValue(), aCause);
            setPassive();
        } else if (set.isInstantiated()) {
            setPassive();
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (isElementVar(idxVarInProp)) {
            assert element.isInstantiated();
            set.removeFromEnvelope(element.getValue(), aCause);
            setPassive();
        } else {
            assert isSetVar(idxVarInProp);
            setD.freeze();
            setD.forEach(pruneElementOnSetKer, EventType.ADD_TO_KER);
            setD.unfreeze();
            if (element.isInstantiated()) {
                set.removeFromEnvelope(element.getValue(), aCause);
                setPassive();
            } else if (set.isInstantiated()) {
                setPassive();
            }
        }
    }
    private final IntProcedure pruneElementOnSetKer = new IntProcedure() {

        @Override
        public void execute(int setKer) throws ContradictionException {
            element.removeValue(setKer, aCause);
        }
    };

    @Override
    public ESat isEntailed() {
        if (element.isInstantiated()) {
            if (!set.envelopeContains(element.getValue())) {
                return ESat.TRUE;
            }
            return set.isInstantiated() ? ESat.FALSE : ESat.UNDEFINED;
        }
        if (PropUtil.isDomSubsetKer(element, set)) {
            return ESat.FALSE;
        }
        return PropUtil.isDomIntersectEnv(element, set) ? ESat.UNDEFINED : ESat.TRUE;
    }

    @Override
    public String toString() {
        return element + " not in " + set;
    }
}
