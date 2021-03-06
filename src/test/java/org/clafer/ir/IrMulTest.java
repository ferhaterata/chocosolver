package org.clafer.ir;

import org.clafer.ir.IrQuickTest.Solution;
import static org.clafer.ir.Irs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import solver.constraints.Constraint;
import solver.constraints.ICF;
import solver.variables.IntVar;

/**
 *
 * @author jimmy
 */
@RunWith(IrQuickTest.class)
public class IrMulTest {

    @Test(timeout = 60000)
    public IrBoolExpr setup(IrIntVar multiplicand, IrIntVar multiplier, IrIntVar product) {
        return equal(mul(multiplicand, multiplier), product);
    }

    @Solution
    public Constraint setup(IntVar multiplicand, IntVar multiplier, IntVar product) {
        return ICF.times(multiplicand, multiplier, product);
    }
}
