package org.clafer.choco.constraint.propagator;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.SetVar;
import solver.variables.delta.ISetDeltaMonitor;
import util.ESat;
import util.procedure.IntProcedure;

/**
 *
 * @author jimmy
 */
public class PropMask extends Propagator<SetVar> {

    private final SetVar set;
    private final SetVar masked;
    private final ISetDeltaMonitor setD;
    private final ISetDeltaMonitor maskedD;
    // Inclusive
    private final int from;
    // Exclusive
    private final int to;

    public PropMask(SetVar set, SetVar masked, int from, int to) {
        super(new SetVar[]{set, masked}, PropagatorPriority.UNARY, true);

        if (from > to) {
            throw new IllegalArgumentException();
        }

        this.set = set;
        this.masked = masked;
        this.setD = set.monitorDelta(aCause);
        this.maskedD = masked.monitorDelta(aCause);
        this.from = from;
        this.to = to;
    }

    private boolean isSetVar(int idx) {
        return idx == 0;
    }

    private boolean isMaskedVar(int idx) {
        return idx == 1;
    }

    @Override
    protected int getPropagationConditions(int vIdx) {
        return EventType.ADD_TO_KER.mask + EventType.REMOVE_FROM_ENVELOPE.mask;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = set.getKernelFirst(); i != SetVar.END; i = set.getKernelNext()) {
            if (i >= from && i < to) {
                masked.addToKernel(i - from, aCause);
            }
        }
        for (int i = set.getEnvelopeFirst(); i != SetVar.END; i = set.getEnvelopeNext()) {
            if (i >= from && i < to && !masked.envelopeContains(i - from)) {
                set.removeFromEnvelope(i, aCause);
            }
        }
        for (int i = masked.getKernelFirst(); i != SetVar.END; i = masked.getKernelNext()) {
            set.addToKernel(i + from, aCause);
        }
        for (int i = masked.getEnvelopeFirst(); i != SetVar.END; i = masked.getEnvelopeNext()) {
            if (i < 0 || i >= to - from || !set.envelopeContains(i + from)) {
                masked.removeFromEnvelope(i, aCause);
            }
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (isSetVar(idxVarInProp)) {
            setD.freeze();
            setD.forEach(pickMaskedOnSetKer, EventType.ADD_TO_KER);
            setD.forEach(pruneMaskedOnSetEnv, EventType.REMOVE_FROM_ENVELOPE);
            setD.unfreeze();
        } else {
            assert isMaskedVar(idxVarInProp);
            maskedD.freeze();
            maskedD.forEach(pickSetOnMaskedKer, EventType.ADD_TO_KER);
            maskedD.forEach(pruneSetOnMaskedEnv, EventType.REMOVE_FROM_ENVELOPE);
            maskedD.unfreeze();
        }
    }
    private final IntProcedure pickMaskedOnSetKer = new IntProcedure() {
        @Override
        public void execute(int ker) throws ContradictionException {
            if (ker >= from && ker < to) {
                masked.addToKernel(ker - from, aCause);
            }
        }
    };
    private final IntProcedure pruneMaskedOnSetEnv = new IntProcedure() {
        @Override
        public void execute(int env) throws ContradictionException {
            if (env >= from && env < to) {
                masked.removeFromEnvelope(env - from, aCause);
            }
        }
    };
    private final IntProcedure pickSetOnMaskedKer = new IntProcedure() {
        @Override
        public void execute(int ker) throws ContradictionException {
            assert ker < to - from;
            set.addToKernel(ker + from, aCause);
        }
    };
    private final IntProcedure pruneSetOnMaskedEnv = new IntProcedure() {
        @Override
        public void execute(int env) throws ContradictionException {
            assert env < to - from;
            set.removeFromEnvelope(env + from, aCause);
        }
    };

    @Override
    public ESat isEntailed() {
        for (int i = set.getKernelFirst(); i != SetVar.END; i = set.getKernelNext()) {
            if (i >= from && i < to && !masked.envelopeContains(i - from)) {
                return ESat.FALSE;
            }
        }
        for (int i = masked.getKernelFirst(); i != SetVar.END; i = masked.getKernelNext()) {
            if (i < 0 || i >= to - from || !set.envelopeContains(i + from)) {
                return ESat.FALSE;
            }
        }
        return set.isInstantiated() && masked.isInstantiated() ? ESat.TRUE : ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return "mask(" + set + ", " + masked + ", " + from + ", " + to + ")";
    }
}
