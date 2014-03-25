package org.clafer;

import java.util.Arrays;
import org.clafer.ast.AstConcreteClafer;
import org.clafer.ast.AstModel;
import static org.clafer.ast.Asts.*;
import org.clafer.compiler.ClaferCompiler;
import org.clafer.compiler.ClaferOptimizer;
import org.clafer.objective.Objective;
import org.clafer.scope.Scope;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author jimmy
 */
public class MultiObjectiveOptimizationTest {

    @Test
    public void testMaximizeMaximize() {
        AstModel model = newModel();

        AstConcreteClafer a = model.addChild("A").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer b = model.addChild("B").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer c = model.addChild("C").refTo(IntType).withCard(Mandatory);
        model.addConstraint(lessThan(add(joinRef(a), joinRef(b)), constant(2)));
        model.addConstraint(greaterThan(add(joinRef(a), joinRef(b)), constant(-2)));

        ClaferOptimizer search = ClaferCompiler.compile(model, Scope.defaultScope(1).intLow(-2).intHigh(2),
                Objective.maximize(joinRef(a)), Objective.maximize(joinRef(b)));
        while (search.find()) {
            int[] o = search.optimalValues();
            assertTrue(Arrays.toString(o) + " is not optimal",
                    (o[0] == -1 && o[1] == 2)
                    || (o[0] == 0 && o[1] == 1)
                    || (o[0] == 1 && o[1] == 0)
                    || (o[0] == 2 && o[1] == -1));
        }
        assertEquals(20, search.instanceCount());
    }

    @Test
    public void testMaximizeMinimize() {
        AstModel model = newModel();

        AstConcreteClafer a = model.addChild("A").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer b = model.addChild("B").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer c = model.addChild("C").refTo(IntType).withCard(Mandatory);
        model.addConstraint(lessThan(add(joinRef(a), joinRef(b)), constant(2)));
        model.addConstraint(greaterThan(add(joinRef(a), joinRef(b)), constant(-2)));

        ClaferOptimizer search = ClaferCompiler.compile(model, Scope.defaultScope(1).intLow(-2).intHigh(2),
                Objective.maximize(joinRef(a)), Objective.minimize(joinRef(b)));
        while (search.find()) {
            int[] o = search.optimalValues();
            assertTrue(Arrays.toString(o) + " is not optimal",
                    (o[0] == 2 && o[1] == -2));
        }
        assertEquals(5, search.instanceCount());
    }

    @Test
    public void testMinimizeMaximize() {
        AstModel model = newModel();

        AstConcreteClafer a = model.addChild("A").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer b = model.addChild("B").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer c = model.addChild("C").refTo(IntType).withCard(Mandatory);
        model.addConstraint(lessThan(add(joinRef(a), joinRef(b)), constant(2)));
        model.addConstraint(greaterThan(add(joinRef(a), joinRef(b)), constant(-2)));

        ClaferOptimizer search = ClaferCompiler.compile(model, Scope.defaultScope(1).intLow(-2).intHigh(2),
                Objective.minimize(joinRef(a)), Objective.maximize(joinRef(b)));
        while (search.find()) {
            int[] o = search.optimalValues();
            assertTrue(Arrays.toString(o) + " is not optimal",
                    (o[0] == -2 && o[1] == 2));
        }
        assertEquals(5, search.instanceCount());
    }

    @Test
    public void testMinimizeMinimize() {
        AstModel model = newModel();

        AstConcreteClafer a = model.addChild("A").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer b = model.addChild("B").refTo(IntType).withCard(Mandatory);
        AstConcreteClafer c = model.addChild("C").refTo(IntType).withCard(Mandatory);
        model.addConstraint(lessThan(add(joinRef(a), joinRef(b)), constant(2)));
        model.addConstraint(greaterThan(add(joinRef(a), joinRef(b)), constant(-2)));

        ClaferOptimizer search = ClaferCompiler.compile(model, Scope.defaultScope(1).intLow(-2).intHigh(2),
                Objective.minimize(joinRef(a)), Objective.minimize(joinRef(b)));
        while (search.find()) {
            int[] o = search.optimalValues();
            assertTrue(Arrays.toString(o) + " is not optimal",
                    (o[0] == 1 && o[1] == -2)
                    || (o[0] == -0 && o[1] == -1)
                    || (o[0] == -1 && o[1] == -0)
                    || (o[0] == -2 && o[1] == 1));
        }
        assertEquals(20, search.instanceCount());
    }
}
