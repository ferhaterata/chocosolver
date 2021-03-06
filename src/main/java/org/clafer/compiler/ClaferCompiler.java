package org.clafer.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.clafer.ast.AstAbstractClafer;
import org.clafer.ast.AstClafer;
import org.clafer.ast.AstConcreteClafer;
import org.clafer.ast.AstModel;
import org.clafer.ast.AstRef;
import org.clafer.ast.AstStringClafer;
import org.clafer.ast.AstUtil;
import org.clafer.ast.analysis.UnsatAnalyzer;
import org.clafer.ast.compiler.AstCompiler;
import org.clafer.ast.compiler.AstSolutionMap;
import org.clafer.collection.Either;
import org.clafer.collection.Maybe;
import org.clafer.common.UnsatisfiableException;
import org.clafer.common.Util;
import org.clafer.graph.GraphUtil;
import org.clafer.graph.KeyGraph;
import org.clafer.graph.Vertex;
import org.clafer.ir.IrIntConstant;
import org.clafer.ir.IrIntVar;
import org.clafer.ir.IrModule;
import org.clafer.ir.IrSetConstant;
import org.clafer.ir.IrSetVar;
import org.clafer.ir.IrStringVar;
import org.clafer.ir.compiler.IrCompiler;
import org.clafer.ir.compiler.IrSolutionMap;
import org.clafer.objective.Objective;
import org.clafer.scope.Scopable;
import solver.Solver;
import solver.search.strategy.IntStrategyFactory;
import solver.search.strategy.SetStrategyFactory;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.variables.IntVar;
import solver.variables.SetVar;

/**
 * Compiles from AST -> Choco
 *
 * @author jimmy
 */
public class ClaferCompiler {

    private ClaferCompiler() {
    }

    private static SetVar[] getSetVars(AstModel model, ClaferSolutionMap map) {
        KeyGraph<AstClafer> dependency = new KeyGraph<>();
        for (AstAbstractClafer abstractClafer : model.getAbstracts()) {
            Vertex<AstClafer> node = dependency.getVertex(abstractClafer);
            for (AstClafer sub : abstractClafer.getSubs()) {
                node.addNeighbour(dependency.getVertex(sub));
            }
            if (abstractClafer.hasRef()) {
                node.addNeighbour(dependency.getVertex(abstractClafer.getRef().getTargetType()));
            }
        }
        for (AstConcreteClafer concreteClafer : AstUtil.getConcreteClafers(model)) {
            Vertex<AstClafer> node = dependency.getVertex(concreteClafer);
            if (concreteClafer.hasParent()) {
                node.addNeighbour(dependency.getVertex(concreteClafer.getParent()));
            }
            if (concreteClafer.hasRef()) {
                node.addNeighbour(dependency.getVertex(concreteClafer.getRef().getTargetType()));
            }
        }
        List<SetVar> vars = new ArrayList<>();
        for (Set<AstClafer> component : GraphUtil.computeStronglyConnectedComponents(dependency)) {
            for (AstClafer clafer : component) {
                if (clafer instanceof AstConcreteClafer) {
                    for (IrSetVar setVar : map.getAstSolution().getSiblingVars(clafer)) {
                        if (!(setVar instanceof IrSetConstant)) {
                            Either<int[], SetVar> var = map.getIrSolution().getVar(setVar);
                            if (var.isRight()) {
                                vars.add(var.getRight());
                            }
                        }
                    }
                }
            }
        }
        return vars.toArray(new SetVar[vars.size()]);
    }

    private static IntVar[] getIntVars(AstModel model, ClaferSolutionMap map) {
        List<IntVar> vars = new ArrayList<>();
        for (AstClafer clafer : AstUtil.getClafers(model)) {
            if (clafer.hasRef()) {
                AstRef ref = clafer.getRef();
                if (ref.getTargetType() instanceof AstStringClafer) {
                    for (IrStringVar stringVar : map.getAstSolution().getRefStrings(ref)) {
                        Either<Integer, IntVar> lengthVar
                                = map.getIrSolution().getVar(stringVar.getLengthVar());
                        if (lengthVar.isRight()) {
                            vars.add(lengthVar.getRight());
                        }
                        for (IrIntVar charVar : stringVar.getCharVars()) {
                            Either<Integer, IntVar> var = map.getIrSolution().getVar(charVar);
                            if (var.isRight()) {
                                vars.add(var.getRight());
                            }
                        }
                    }
                } else {
                    for (IrIntVar intVar : map.getAstSolution().getRefVars(ref)) {
                        if (!(intVar instanceof IrIntConstant)) {
                            Either<Integer, IntVar> var = map.getIrSolution().getVar(intVar);
                            if (var.isRight()) {
                                vars.add(var.getRight());
                            }
                        }
                    }
                }
            }
        }
        return vars.toArray(new IntVar[vars.size()]);
    }

    @SafeVarargs
    private static void set(Solver solver, Maybe<AbstractStrategy<?>>... strategies) {
        AbstractStrategy<?>[] strats = Maybe.filterJust(strategies);
        if (strats.length > 0) {
            solver.set(strats);
        }
    }

    private static Maybe<AbstractStrategy<?>> setStrategy(SetVar[] vars, ClaferOptions options) {
        if (vars.length == 0) {
            return Maybe.nothing();
        }
        if (options.isPreferSmallerInstances()) {
            return Maybe.<AbstractStrategy<?>>just(SetStrategyFactory.remove_first(vars));
        }
        return Maybe.<AbstractStrategy<?>>just(SetStrategyFactory.force_first(vars));
    }

    private static Maybe<AbstractStrategy<?>> firstFailInDomainMax(IntVar[] vars) {
        if (vars.length == 0) {
            return Maybe.nothing();
        }
        return Maybe.<AbstractStrategy<?>>just(IntStrategyFactory.minDom_UB(vars));
    }

    private static Maybe<AbstractStrategy<?>> firstFailInDomainMin(IntVar[] vars) {
        if (vars.length == 0) {
            return Maybe.nothing();
        }
        return Maybe.<AbstractStrategy<?>>just(IntStrategyFactory.minDom_LB(vars));
    }

    public static ClaferSolver compile(AstModel in, Scopable scope) {
        return compile(in, scope, ClaferOptions.Default);
    }

    public static ClaferSolver compile(AstModel in, Scopable scope, ClaferOptions options) {
        try {
            Solver solver = new Solver();
            IrModule module = new IrModule();

            AstSolutionMap astSolution = AstCompiler.compile(in, scope.toScope(), module,
                    options.isFullSymmetryBreaking());
            IrSolutionMap irSolution = IrCompiler.compile(module, solver, options.isFullOptimizations());
            ClaferSolutionMap solution = new ClaferSolutionMap(astSolution, irSolution);

            set(solver,
                    setStrategy(getSetVars(in, solution), options),
                    firstFailInDomainMin(getIntVars(in, solution)));
            return new ClaferSolver(solver, solution);
        } catch (UnsatisfiableException e) {
            return new ClaferSolver();
        }
    }

    public static ClaferOptimizer compile(AstModel in, Scopable scope, Objective... objectives) {
        return compile(in, scope, objectives, ClaferOptions.Default);
    }

    public static ClaferOptimizer compile(AstModel in, Scopable scope, Objective[] objectives, ClaferOptions options) {
        try {
            Solver solver = new Solver();
            IrModule module = new IrModule();

            AstSolutionMap astSolution = AstCompiler.compile(
                    in, scope.toScope(), objectives, module,
                    options.isFullSymmetryBreaking());
            IrSolutionMap irSolution = IrCompiler.compile(module, solver, options.isFullOptimizations());
            ClaferSolutionMap solution = new ClaferSolutionMap(astSolution, irSolution);

            IrIntVar[] objectiveIrVars = new IrIntVar[objectives.length];
            for (int i = 0; i < objectiveIrVars.length; i++) {
                objectiveIrVars[i] = astSolution.getObjectiveVar(objectives[i]);
            }
            Either<Integer, IntVar>[] objectiveVars = irSolution.getVars(objectiveIrVars);

            boolean[] maximizes = new boolean[objectives.length];
            for (int i = 0; i < maximizes.length; i++) {
                maximizes[i] = objectives[i].isMaximize();
            }

            set(solver,
                    setStrategy(getSetVars(in, solution), options),
                    //                firstFailInDomainMax(objectiveVars),
                    firstFailInDomainMin(getIntVars(in, solution)));
            return maximizes.length == 1
                    ? new ClaferSingleObjectiveOptimizer(solver, solution, maximizes[0], objectiveVars[0])
                    : new ClaferMultiObjectiveOptimizerGIA(solver, solution, maximizes, objectiveVars);
        } catch (UnsatisfiableException e) {
            return new ClaferUnsatOptimizer();
        }
    }

    public static ClaferUnsat compileUnsat(AstModel in, Scopable scope) {
        return compileUnsat(in, scope.toScope(), ClaferOptions.Default);
    }

    public static ClaferUnsat compileUnsat(AstModel in, Scopable scope, ClaferOptions options) {
        Solver solver = new Solver();
        IrModule module = new IrModule();

        AstSolutionMap astSolution = AstCompiler.compile(in, scope.toScope(), module,
                Util.cons(new UnsatAnalyzer(), AstCompiler.DefaultAnalyzers),
                options.isFullSymmetryBreaking());
        IrSolutionMap irSolution = IrCompiler.compile(module, solver, options.isFullOptimizations());
        ClaferSolutionMap solution = new ClaferSolutionMap(astSolution, irSolution);

        set(solver,
                firstFailInDomainMax(Either.filterRight(irSolution.getVars(astSolution.getSoftVars()))),
                setStrategy(getSetVars(in, solution), options),
                firstFailInDomainMin(getIntVars(in, solution)));
        return new ClaferUnsat(solver, solution);
    }

//    public static ClaferSolver compilePartial(AstModel in, ScopeBuilder scope, AstConcreteClafer... concretize) {
//        return compilePartial(in, scope.toScope(), concretize);
//    }
//
//    public static ClaferSolver compilePartial(AstModel in, Scope scope, AstConcreteClafer... concretize) {
//        final Set<AstConcreteClafer> transitiveConcretize = new HashSet<>();
//        for (AstConcreteClafer clafer : concretize) {
//            concretize(clafer, transitiveConcretize);
//        }
//        final ClaferSolver solver = compile(in, scope);
//        final List<IntVar> intVars = new ArrayList<>();
//        final List<SetVar> setVars = new ArrayList<>();
//        for (AstConcreteClafer clafer : transitiveConcretize) {
//            IrSetVar[] siblingVars = solver.getSolutionMap().getAstSolution().getSiblingVars(clafer);
//            for (IrSetVar siblingVar : siblingVars) {
//                Either<int[], SetVar> var = solver.getSolutionMap().getIrSolution().getSetVar(siblingVar);
//                if (var.isRight()) {
//                    setVars.add(var.getRight());
//                }
//            }
//            AstRef ref = AstUtil.getInheritedRef(clafer);
//            if (ref != null) {
//                IrIntVar[] refVars = solver.getSolutionMap().getAstSolution().getRefVars(ref);
//                for (IrIntVar refVar : refVars) {
//                    Either<Integer, IntVar> var = solver.getSolutionMap().getIrSolution().getIntVar(refVar);
//                    if (var.isRight()) {
//                        intVars.add(var.getRight());
//                    }
//                }
//            }
//        }
//        solver.getInternalSolver().getSearchLoop().plugSearchMonitor(new IMonitorSolution() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onSolution() {
//                List<Constraint> constraints = new ArrayList<>();
//                for (IntVar var : intVars) {
//                    constraints.add(ICF.arithm(var, "!=", var.getValue()));
//                }
//                for (SetVar var : setVars) {
//                    constraints.add(Constraints.notEqual(var, var.getValue()));
//                }
//                solver.getInternalSolver().postCut(Constraints.or(
//                        constraints.toArray(new Constraint[constraints.size()])));
//            }
//        });
//        return solver;
//    }
//
//    private static void concretize(AstClafer clafer, Set<AstConcreteClafer> concretize) {
//        if (clafer instanceof AstAbstractClafer) {
//            concretize((AstAbstractClafer) clafer, concretize);
//        } else {
//            concretize((AstConcreteClafer) clafer, concretize);
//        }
//    }
//
//    private static void concretize(AstConcreteClafer clafer, Set<AstConcreteClafer> concretize) {
//        if (!AstUtil.isRoot(clafer) && concretize.add(clafer)) {
//            concretize(clafer.getParent(), concretize);
//            concretize(clafer.getSuperClafer(), concretize);
//        }
//    }
//
//    private static void concretize(AstAbstractClafer clafer, Set<AstConcreteClafer> concretize) {
//        if (!AstUtil.isTypeRoot(clafer)) {
//            for (AstClafer sub : clafer.getSubs()) {
//                concretize(sub, concretize);
//            }
//        }
//    }
}
