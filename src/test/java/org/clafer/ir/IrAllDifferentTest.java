package org.clafer.ir;

import org.clafer.ir.IrQuickTest.Solution;
import static org.clafer.ir.Irs.*;
import org.clafer.test.NonEmpty;
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
public class IrAllDifferentTest {

    @Test(timeout = 60000)
    public IrBoolExpr setup(@NonEmpty IrIntVar[] ints) {
        return allDifferent(ints);
    }

    @Solution
    public Constraint setup(IntVar[] ints) {
        return ICF.alldifferent(ints);
    }
}
