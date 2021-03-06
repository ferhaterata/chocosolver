package org.clafer.ir;

import org.clafer.ir.IrQuickTest.Solution;
import static org.clafer.ir.Irs.*;
import org.clafer.test.NonEmpty;
import org.clafer.test.SameLength;
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
public class IrSortStringTest {

    @Test(timeout = 60000)
    public IrBoolExpr setup(@NonEmpty @SameLength IrIntVar[][] strings, boolean strict) {
        return strict ? sortStrict(strings) : sort(strings);
    }

    @Solution
    public Constraint setup(IntVar[][] strings, boolean strict) {
        return strict ? ICF.lex_chain_less(strings) : ICF.lex_chain_less_eq(strings);
    }
}
