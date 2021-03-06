package org.clafer.ir;

import org.clafer.ir.IrQuickTest.Solution;
import static org.clafer.ir.Irs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import solver.constraints.Constraint;
import solver.constraints.ICF;
import solver.variables.BoolVar;

/**
 *
 * @author jimmy
 */
@RunWith(IrQuickTest.class)
public class IrIfOnlyIfTest {

    @Test(timeout = 60000)
    public IrBoolExpr setup(IrBoolVar left, IrBoolVar right) {
        return ifOnlyIf(left, right);
    }

    @Solution
    public Constraint setup(BoolVar left, BoolVar right) {
        return ICF.arithm(left, "=", right);
    }
}
