package org.clafer.ir;

import org.clafer.ir.IrQuickTest.Solution;
import static org.clafer.ir.Irs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import solver.constraints.Constraint;
import solver.constraints.set.SCF;
import solver.variables.BoolVar;
import solver.variables.SetVar;

/**
 *
 * @author jimmy
 */
@RunWith(IrQuickTest.class)
public class IrBoolChannelTest {

    @Test(timeout = 60000)
    public IrBoolExpr setup(IrBoolVar[] bools, IrSetVar set) {
        return boolChannel(bools, set);
    }

    @Solution
    public Constraint setup(BoolVar[] bools, SetVar set) {
        return SCF.bool_channel(bools, set, 0);
    }
}
