package org.clafer.ir;

import org.clafer.domain.Domain;
import static org.clafer.domain.Domains.*;
import org.clafer.domain.EnumDomain;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.clafer.common.UnsatisfiableException;
import org.clafer.common.Util;
import org.clafer.domain.BoolDomain;

/**
 * Import this class to access all IR building functions.
 * <pre>
 * import static org.clafer.ast.Asts.*;
 * </pre>
 *
 * @author jimmy
 */
public class Irs {

    private Irs() {
    }

    /**
     *******************
     *
     * Boolean
     *
     *******************
     */
    public static final IrBoolVar True = new IrBoolConstant(true);
    public static final IrBoolVar False = new IrBoolConstant(false);

    public static IrBoolVar constant(boolean value) {
        return value ? True : False;
    }

    public static IrBoolVar bool(String name) {
        return new IrBoolVar(name, TrueFalseDomain);
    }

    public static IrBoolExpr not(IrBoolExpr proposition) {
        Boolean constant = IrUtil.getConstant(proposition);
        if (constant != null) {
            // Reverse the boolean
            return constant ? False : True;
        }
        return proposition.negate();
    }

    public static IrBoolExpr and(Collection<? extends IrBoolExpr> operands) {
        return and(operands.toArray(new IrBoolExpr[operands.size()]));
    }

    public static IrBoolExpr and(IrBoolExpr... operands) {
        List<IrBoolExpr> flatten = new ArrayList<>(operands.length);
        for (IrBoolExpr operand : operands) {
            if (operand instanceof IrAnd) {
                // Invariant: No nested IrAnd
                flatten.addAll(Arrays.asList(((IrAnd) operand).getOperands()));
            } else {
                flatten.add(operand);
            }
        }
        List<IrBoolExpr> filter = new ArrayList<>(flatten.size());
        for (IrBoolExpr operand : flatten) {
            if (IrUtil.isFalse(operand)) {
                return False;
            }
            if (!IrUtil.isTrue(operand)) {
                filter.add(operand);
            }
        }
        switch (filter.size()) {
            case 0:
                return True;
            case 1:
                return filter.get(0);
            default:
                return new IrAnd(filter.toArray(new IrBoolExpr[filter.size()]), TrueFalseDomain);
        }
    }

    public static IrBoolExpr lone(Collection<? extends IrBoolExpr> operands) {
        return lone(operands.toArray(new IrBoolExpr[operands.size()]));
    }

    public static IrBoolExpr lone(IrBoolExpr... operands) {
        List<IrBoolExpr> filter = new ArrayList<>(operands.length);
        int count = 0;
        for (IrBoolExpr operand : operands) {
            if (IrUtil.isTrue(operand)) {
                count++;
                if (count > 1) {
                    return False;
                }
            } else if (!IrUtil.isFalse(operand)) {
                filter.add(operand);
            }
        }
        assert count == 0 || count == 1;
        switch (filter.size()) {
            case 0:
                return True;
            case 1:
                return count == 0 ? True : not(filter.get(0));
            default:
                IrBoolExpr[] f = filter.toArray(new IrBoolExpr[filter.size()]);
                return count == 0
                        ? new IrLone(f, TrueFalseDomain)
                        : not(or(f));
        }
    }

    public static IrBoolExpr one(Collection<? extends IrBoolExpr> operands) {
        return one(operands.toArray(new IrBoolExpr[operands.size()]));
    }

    public static IrBoolExpr one(IrBoolExpr... operands) {
        List<IrBoolExpr> filter = new ArrayList<>(operands.length);
        int count = 0;
        for (IrBoolExpr operand : operands) {
            if (IrUtil.isTrue(operand)) {
                count++;
                if (count > 1) {
                    return False;
                }
            } else if (!IrUtil.isFalse(operand)) {
                filter.add(operand);
            }
        }
        assert count == 0 || count == 1;
        switch (filter.size()) {
            case 0:
                return count == 0 ? False : True;
            case 1:
                return count == 0 ? filter.get(0) : not(filter.get(0));
            case 2:
                return count == 0
                        ? xor(filter.get(0), filter.get(1))
                        : and(not(filter.get(0)), not(filter.get(1)));
            default:
                IrBoolExpr[] f = filter.toArray(new IrBoolExpr[filter.size()]);
                return count == 0
                        ? new IrOne(f, TrueFalseDomain)
                        : not(or(f));
        }
    }

    public static IrBoolExpr or(Collection<? extends IrBoolExpr> operands) {
        return or(operands.toArray(new IrBoolExpr[operands.size()]));
    }

    public static IrBoolExpr or(IrBoolExpr... operands) {
        List<IrBoolExpr> flatten = new ArrayList<>(operands.length);
        for (IrBoolExpr operand : operands) {
            if (operand instanceof IrOr) {
                // Invariant: No nested IrOr
                flatten.addAll(Arrays.asList(((IrOr) operand).getOperands()));
            } else {
                flatten.add(operand);
            }
        }
        List<IrBoolExpr> filter = new ArrayList<>(flatten.size());
        for (IrBoolExpr operand : flatten) {
            if (IrUtil.isTrue(operand)) {
                return True;
            }
            if (!IrUtil.isFalse(operand)) {
                filter.add(operand);
            }
        }
        switch (filter.size()) {
            case 0:
                return False;
            case 1:
                return filter.get(0);
            default:
                return new IrOr(filter.toArray(new IrBoolExpr[filter.size()]), TrueFalseDomain);
        }
    }

    public static IrBoolExpr implies(IrBoolExpr antecedent, IrBoolExpr consequent) {
        if (antecedent.equals(consequent)) {
            return True;
        }
        if (IrUtil.isTrue(antecedent)) {
            return consequent;
        }
        if (IrUtil.isFalse(antecedent)) {
            return True;
        }
        if (IrUtil.isTrue(consequent)) {
            return True;
        }
        if (IrUtil.isFalse(consequent)) {
            return not(antecedent);
        }
        if (consequent instanceof IrImplies) {
            // a => (b => c) <=> !a or !b or c
            IrImplies consequentImplies = (IrImplies) consequent;
            return or(not(antecedent),
                    not(consequentImplies.getAntecedent()),
                    consequentImplies.getConsequent());
        }
        return new IrImplies(antecedent, consequent, TrueFalseDomain);
    }

    public static IrBoolExpr notImplies(IrBoolExpr antecedent, IrBoolExpr consequent) {
        if (antecedent.equals(consequent)) {
            return False;
        }
        if (IrUtil.isTrue(antecedent)) {
            return not(consequent);
        }
        if (IrUtil.isFalse(antecedent)) {
            return False;
        }
        if (IrUtil.isTrue(consequent)) {
            return False;
        }
        if (IrUtil.isFalse(consequent)) {
            return antecedent;
        }
        return new IrNotImplies(antecedent, consequent, TrueFalseDomain);
    }

    public static IrBoolExpr ifThenElse(IrBoolExpr antecedent, IrBoolExpr consequent, IrBoolExpr alternative) {
        if (IrUtil.isTrue(antecedent)) {
            return consequent;
        }
        if (IrUtil.isFalse(antecedent)) {
            return alternative;
        }
        if (IrUtil.isTrue(consequent)) {
            return or(antecedent, alternative);
        }
        if (IrUtil.isFalse(consequent)) {
            return and(antecedent.negate(), alternative);
        }
        if (IrUtil.isTrue(alternative)) {
            return or(antecedent.negate(), consequent);
        }
        if (IrUtil.isFalse(alternative)) {
            return and(antecedent, consequent);
        }
        return new IrIfThenElse(antecedent, consequent, alternative, TrueFalseDomain);
    }

    public static IrBoolExpr ifOnlyIf(IrBoolExpr left, IrBoolExpr right) {
        if (left.equals(right)) {
            return True;
        }
        if (IrUtil.isTrue(left)) {
            return right;
        }
        if (IrUtil.isFalse(left)) {
            return not(right);
        }
        if (IrUtil.isTrue(right)) {
            return left;
        }
        if (IrUtil.isFalse(right)) {
            return not(left);
        }
        if (left instanceof IrNot) {
            return xor(((IrNot) left).getExpr(), right);
        }
        if (right instanceof IrNot) {
            return xor(left, ((IrNot) right).getExpr());
        }
        return new IrIfOnlyIf(left, right, TrueFalseDomain);
    }

    public static IrBoolExpr xor(IrBoolExpr left, IrBoolExpr right) {
        if (IrUtil.isTrue(left)) {
            return not(right);
        }
        if (IrUtil.isFalse(left)) {
            return right;
        }
        if (IrUtil.isTrue(right)) {
            return not(left);
        }
        if (IrUtil.isFalse(right)) {
            return left;
        }
        if (left instanceof IrNot) {
            return ifOnlyIf(((IrNot) left).getExpr(), right);
        }
        if (right instanceof IrNot) {
            return ifOnlyIf(left, ((IrNot) right).getExpr());
        }
        return new IrXor(left, right, TrueFalseDomain);
    }

    public static IrBoolExpr within(IrIntExpr value, Domain range) {
        Domain domain = value.getDomain();
        if (range.isBounded()
                && domain.getLowBound() >= range.getLowBound()
                && domain.getHighBound() <= range.getHighBound()) {
            return True;
        }
        if (domain.getLowBound() > range.getHighBound()
                || domain.getHighBound() < range.getLowBound()) {
            return False;
        }
        if (range.size() == 1) {
            return equal(value, range.getLowBound());
        }
        Domain diff = domain.difference(range);
        switch (diff.size()) {
            case 0:
                return True;
            case 1:
                return notEqual(value, diff.getLowBound());
            default:
                return new IrWithin(value, range, TrueFalseDomain);
        }
    }

    public static IrBoolExpr compare(int left, IrCompare.Op op, IrIntExpr right) {
        return compare(constant(left), op, right);
    }

    public static IrBoolExpr compare(IrIntExpr left, IrCompare.Op op, int right) {
        return compare(left, op, constant(right));
    }

    public static IrBoolExpr compare(IrIntExpr left, IrCompare.Op op, IrIntExpr right) {
        Domain leftDomain = left.getDomain();
        Domain rightDomain = right.getDomain();
        switch (op) {
            case Equal:
                if (left.equals(right)) {
                    return True;
                }
                if (!leftDomain.intersects(rightDomain)) {
                    return False;
                }
                if (left instanceof IrBoolExpr && right instanceof IrBoolExpr) {
                    return ifOnlyIf((IrBoolExpr) left, (IrBoolExpr) right);
                }
                break;
            case NotEqual:
                if (left.equals(right)) {
                    return False;
                }
                if (!leftDomain.intersects(rightDomain)) {
                    return True;
                }
                if (left instanceof IrBoolExpr && right instanceof IrBoolExpr) {
                    return xor((IrBoolExpr) left, (IrBoolExpr) right);
                }
                break;
            case LessThan:
                if (left.equals(right)) {
                    return False;
                }
                if (leftDomain.getHighBound() < rightDomain.getLowBound()) {
                    return True;
                }
                if (leftDomain.getLowBound() >= rightDomain.getHighBound()) {
                    return False;
                }
                if (left instanceof IrBoolExpr && right instanceof IrBoolExpr) {
                    return not(implies((IrBoolExpr) right, (IrBoolExpr) left));
                }
                if (left instanceof IrMinus && right instanceof IrMinus) {
                    return greaterThan(((IrMinus) left).getExpr(), ((IrMinus) right).getExpr());
                }
                break;
            case LessThanEqual:
                if (left.equals(right)) {
                    return True;
                }
                if (leftDomain.getHighBound() <= rightDomain.getLowBound()) {
                    return True;
                }
                if (leftDomain.getLowBound() > rightDomain.getHighBound()) {
                    return False;
                }
                if (leftDomain.getLowBound() == rightDomain.getHighBound()) {
                    return equal(left, right);
                }
                if (left instanceof IrBoolExpr && right instanceof IrBoolExpr) {
                    return implies((IrBoolExpr) left, (IrBoolExpr) right);
                }
                if (left instanceof IrMinus && right instanceof IrMinus) {
                    return greaterThanEqual(((IrMinus) left).getExpr(), ((IrMinus) right).getExpr());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown op: " + op);
        }
        return new IrCompare(left, op, right, TrueFalseDomain);
    }

    public static IrBoolExpr compare(IrStringExpr left, IrStringCompare.Op op, IrStringExpr right) {
        if (left.equals(right)) {
            switch (op) {
                case Equal:
                case LessThanEqual:
                    return True;
                case NotEqual:
                case LessThan:
                    return False;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return new IrStringCompare(left, op, right, TrueFalseDomain);
    }

    public static IrBoolExpr equal(int left, IrIntExpr right) {
        return equal(constant(left), right);
    }

    public static IrBoolExpr equal(IrIntExpr left, int right) {
        return equal(left, constant(right));
    }

    public static IrBoolExpr equal(IrIntExpr left, IrIntExpr right) {
        return compare(left, IrCompare.Op.Equal, right);
    }

    public static IrBoolExpr equal(IrIntExpr[] left, IrIntExpr[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException();
        }
        IrBoolExpr[] ands = new IrBoolExpr[left.length];
        for (int i = 0; i < ands.length; i++) {
            ands[i] = equal(left[i], right[i]);
        }
        return and(ands);
    }

    public static IrBoolExpr equality(IrSetExpr left, IrSetEquality.Op op, IrSetExpr right) {
        switch (op) {
            case Equal:
                if (left.equals(right)) {
                    return True;
                }
                if (!left.getKer().isSubsetOf(right.getEnv())
                        || !right.getKer().isSubsetOf(left.getEnv())) {
                    return False;
                }
                if (!left.getCard().intersects(right.getCard())) {
                    return False;
                }
                Domain constant = IrUtil.getConstant(left);
                if (constant != null) {
                    /*
                     * The idea is that integer constraints are easier to
                     * optimize than set constraints. If the expression is a top
                     * level expression than the cardinality propagator will
                     * optimize this expression away anyways.
                     */
                    if (constant.isEmpty()) {
                        return equal(card(right), 0);
                    }
                    if (constant.size() == right.getEnv().size()) {
                        return constant.isSubsetOf(right.getEnv())
                                ? equal(card(right), constant.size()) : False;
                    }
                }
                constant = IrUtil.getConstant(right);
                if (constant != null) {
                    if (constant.isEmpty()) {
                        return equal(card(left), 0);
                    }
                    if (constant.size() == left.getEnv().size()) {
                        return constant.isSubsetOf(left.getEnv())
                                ? equal(card(left), constant.size()) : False;
                    }
                }
                IrIntExpr leftInt = IrUtil.asInt(left);
                if (leftInt != null) {
                    IrIntExpr rightInt = IrUtil.asInt(right);
                    if (rightInt != null) {
                        return equal(leftInt, rightInt);
                    }
                }
                break;
            case NotEqual:
                if (left.equals(right)) {
                    return False;
                }
                if (!left.getKer().isSubsetOf(right.getEnv())
                        || !right.getKer().isSubsetOf(left.getEnv())) {
                    return True;
                }
                if (!left.getCard().intersects(right.getCard())) {
                    return True;
                }
                constant = IrUtil.getConstant(left);
                if (constant != null) {
                    if (constant.isEmpty()) {
                        return notEqual(card(right), 0);
                    }
                    if (constant.size() == right.getEnv().size()) {
                        return constant.isSubsetOf(right.getEnv())
                                ? notEqual(card(right), constant.size()) : True;
                    }
                }
                constant = IrUtil.getConstant(right);
                if (constant != null) {
                    if (constant.isEmpty()) {
                        return notEqual(card(left), 0);
                    }
                    if (constant.size() == left.getEnv().size()) {
                        return constant.isSubsetOf(left.getEnv())
                                ? notEqual(card(left), constant.size()) : True;
                    }
                }
                leftInt = IrUtil.asInt(left);
                if (leftInt != null) {
                    IrIntExpr rightInt = IrUtil.asInt(right);
                    if (rightInt != null) {
                        return notEqual(leftInt, rightInt);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        return new IrSetEquality(left, op, right, TrueFalseDomain);
    }

    public static IrBoolExpr equal(IrSetExpr left, IrSetExpr right) {
        return equality(left, IrSetEquality.Op.Equal, right);
    }

    public static IrBoolExpr equal(IrStringExpr left, IrStringExpr right) {
        return compare(left, IrStringCompare.Op.Equal, right);
    }

    public static IrBoolExpr notEqual(int left, IrIntExpr right) {
        return notEqual(constant(left), right);
    }

    public static IrBoolExpr notEqual(IrIntExpr left, int right) {
        return notEqual(left, constant(right));
    }

    public static IrBoolExpr notEqual(IrIntExpr left, IrIntExpr right) {
        return compare(left, IrCompare.Op.NotEqual, right);
    }

    public static IrBoolExpr notEqual(IrSetExpr left, IrSetExpr right) {
        return equality(left, IrSetEquality.Op.NotEqual, right);
    }

    public static IrBoolExpr notEqual(IrStringExpr left, IrStringExpr right) {
        return compare(left, IrStringCompare.Op.NotEqual, right);
    }

    public static IrBoolExpr lessThan(int left, IrIntExpr right) {
        return lessThan(constant(left), right);
    }

    public static IrBoolExpr lessThan(IrIntExpr left, int right) {
        return lessThan(left, constant(right));
    }

    public static IrBoolExpr lessThan(IrIntExpr left, IrIntExpr right) {
        return compare(left, IrCompare.Op.LessThan, right);
    }

    public static IrBoolExpr lessThanEqual(int left, IrIntExpr right) {
        return lessThanEqual(constant(left), right);
    }

    public static IrBoolExpr lessThanEqual(IrIntExpr left, int right) {
        return lessThanEqual(left, constant(right));
    }

    public static IrBoolExpr lessThanEqual(IrIntExpr left, IrIntExpr right) {
        return compare(left, IrCompare.Op.LessThanEqual, right);
    }

    public static IrBoolExpr greaterThan(int left, IrIntExpr right) {
        return greaterThan(constant(left), right);
    }

    public static IrBoolExpr greaterThan(IrIntExpr left, int right) {
        return greaterThan(left, constant(right));
    }

    public static IrBoolExpr greaterThan(IrIntExpr left, IrIntExpr right) {
        return compare(right, IrCompare.Op.LessThan, left);
    }

    public static IrBoolExpr greaterThanEqual(int left, IrIntExpr right) {
        return greaterThanEqual(constant(left), right);
    }

    public static IrBoolExpr greaterThanEqual(IrIntExpr left, int right) {
        return greaterThanEqual(left, constant(right));
    }

    public static IrBoolExpr greaterThanEqual(IrIntExpr left, IrIntExpr right) {
        return compare(right, IrCompare.Op.LessThanEqual, left);
    }

    public static IrBoolExpr member(IrIntExpr element, IrSetExpr set) {
        if (element.getDomain().isSubsetOf(set.getKer())) {
            return True;
        }
        if (!element.getDomain().intersects(set.getEnv())) {
            return False;
        }
        if (IrUtil.isConstant(set)) {
            return within(element, set.getEnv());
        }
        return new IrMember(element, set, TrueFalseDomain);
    }

    public static IrBoolExpr notMember(IrIntExpr element, IrSetExpr set) {
        if (!element.getDomain().intersects(set.getEnv())) {
            return True;
        }
        if (element.getDomain().isSubsetOf(set.getKer())) {
            return False;
        }
        if (IrUtil.isConstant(set)) {
            return within(element, set.getEnv()).negate();
        }
        return new IrNotMember(element, set, TrueFalseDomain);
    }

    public static IrBoolExpr subsetEq(IrSetExpr subset, IrSetExpr superset) {
        if (subset.getEnv().isSubsetOf(superset.getKer())) {
            return True;
        }
        if (subset.getCard().getLowBound() == superset.getCard().getHighBound()) {
            return equal(subset, superset);
        }
        return new IrSubsetEq(subset, superset, TrueFalseDomain);
    }

    public static IrBoolExpr boolChannel(IrBoolExpr[] bools, IrSetExpr set) {
        if (set.getEnv().isEmpty()
                || (set.getEnv().getLowBound() >= 0 && set.getEnv().getHighBound() < bools.length)) {
            {
                Domain constant = IrUtil.getConstant(set);
                if (constant != null) {
                    IrBoolExpr[] ands = new IrBoolExpr[bools.length];
                    for (int i = 0; i < ands.length; i++) {
                        ands[i] = equal(bools[i], constant.contains(i) ? True : False);
                    }
                    return and(ands);
                }
                if (bools.length == 1) {
                    return equal(bools[0], card(set));
                }
            }
            TIntHashSet values = new TIntHashSet();
            Domain env = set.getEnv();
            Domain ker = set.getKer();
            boolean entailed = true;
            for (int i = 0; i < bools.length; i++) {
                Boolean constant = IrUtil.getConstant(bools[i]);
                if (Boolean.TRUE.equals(constant)) {
                    if (values != null) {
                        values.add(i);
                    }
                    if (!env.contains(i)) {
                        return False;
                    }
                    if (!ker.contains(i)) {
                        entailed = false;
                    }
                } else if (Boolean.FALSE.equals(constant)) {
                    if (ker.contains(i)) {
                        return False;
                    }
                    if (env.contains(i)) {
                        entailed = false;
                    }
                } else {
                    values = null;
                    entailed = false;
                }
            }
            if (entailed) {
                return True;
            }
            if (values != null) {
                return equal(set, constant(enumDomain(values)));
            }
        }
        return new IrBoolChannel(bools, set, TrueFalseDomain);
    }

    public static IrBoolExpr intChannel(IrIntExpr[] ints, IrSetExpr[] sets) {
        if (ints.length == 0) {
            IrBoolExpr[] empty = new IrBoolExpr[sets.length];
            for (int i = 0; i < empty.length; i++) {
                empty[i] = equal(sets[i], EmptySet);
            }
            return and(empty);
        }
        if (sets.length == 0) {
            return ints.length == 0 ? True : False;
        }
        boolean entailed = true;
        for (int i = 0; i < ints.length; i++) {
            Integer constant = IrUtil.getConstant(ints[i]);
            if (constant != null) {
                if (constant < 0 || constant >= sets.length) {
                    return False;
                }
                IrSetExpr set = sets[constant];
                if (!set.getEnv().contains(i)) {
                    return False;
                } else if (!set.getKer().contains(i)) {
                    entailed = false;
                }
            } else {
                entailed = false;
            }
        }
        for (int i = 0; i < sets.length; i++) {
            Domain constant = IrUtil.getConstant(sets[i]);
            if (constant != null) {
                TIntIterator iter = constant.iterator();
                while (iter.hasNext()) {
                    int j = iter.next();
                    if (j < 0 || j >= ints.length) {
                        return False;
                    }
                    IrIntExpr iexpr = ints[j];
                    if (!iexpr.getDomain().contains(i)) {
                        return False;
                    } else if (iexpr.getDomain().size() != 1) {
                        entailed = false;
                    }
                }
            } else {
                entailed = false;
            }
        }
        if (entailed) {
            return True;
        }
        return new IrIntChannel(ints, sets, TrueFalseDomain);
    }

    public static IrBoolExpr sort(IrIntExpr... array) {
        if (array.length <= 1) {
            return True;
        }
        IrBoolExpr[] sort = new IrBoolExpr[array.length - 1];
        for (int i = 0; i < array.length - 1; i++) {
            sort[i] = lessThanEqual(array[i], array[i + 1]);
        }
        return and(sort);
    }

    public static IrBoolExpr sortStrict(IrIntExpr... array) {
        if (array.length <= 1) {
            return True;
        }
        IrBoolExpr[] sort = new IrBoolExpr[array.length - 1];
        for (int i = 0; i < array.length - 1; i++) {
            sort[i] = lessThan(array[i], array[i + 1]);
        }
        return and(sort);
    }

    public static IrBoolExpr sort(IrSetExpr... sets) {
        List<IrSetExpr> filter = new ArrayList<>(sets.length);
        boolean fixedCard = true;
        for (IrSetExpr set : sets) {
            if (!set.getEnv().isEmpty()) {
                filter.add(set);
                fixedCard = fixedCard && set.getCard().size() == 1;
            }
        }
        if (filter.isEmpty()) {
            return True;
        }
        if (filter.size() == 1) {
            Domain env = filter.get(0).getEnv();
            Domain ker = filter.get(0).getKer();
            if (env.getLowBound() == 0) {
                int i;
                for (i = 0; i < env.getHighBound(); i++) {
                    if (!ker.contains(i)) {
                        break;
                    }
                }
                if (i == env.getHighBound()) {
                    // env = [0,1,...,n]
                    // ker = [0,1,...,n] or [0,1,...,n-1]
                    return True;
                }
            }
        }
        if (fixedCard) {
            List<IrBoolExpr> ands = new ArrayList<>();
            int i = 0;
            for (IrSetExpr set : filter) {
                assert set.getCard().size() == 1;
                int card = set.getCard().getLowBound();
                ands.add(equal(set, constant(Util.fromTo(i, i + card))));
                i += card;
            }
            return and(ands);
        }
        return new IrSortSets(filter.toArray(new IrSetExpr[filter.size()]), TrueFalseDomain);
    }

    private static IrBoolExpr sortStrings(IrIntExpr[][] strings, boolean strict) {
        if (strings.length < 2) {
            return True;
        }
        boolean[] filter = new boolean[strings.length];
        for (int i = 0; i < strings.length - 1; i++) {
            switch (IrUtil.compareString(strings[i], strings[i + 1])) {
                case EQ:
                case LE:
                    if (!strict) {
                        break;
                    }
                // fallthrough
                case GT:
                case GE:
                case UNKNOWN:
                    filter[i] = true;
                    filter[i + 1] = true;
            }
        }
        List<IrIntExpr[]> filterStrings = new ArrayList<>(strings.length);
        for (int i = 0; i < filter.length; i++) {
            if (filter[i]) {
                filterStrings.add(strings[i]);
            }
        }
        IrIntExpr[][] fstrings = filterStrings.toArray(new IrIntExpr[filterStrings.size()][]);

        if (fstrings.length < 2) {
            return True;
        }

        IrIntExpr[] array = new IrIntExpr[fstrings.length];
        for (int i = 0; i < fstrings.length; i++) {
            IrIntExpr[] string = fstrings[i];
            if (string.length != 1) {
                return new IrSortStrings(fstrings, strict, TrueFalseDomain);
            }
            array[i] = fstrings[i][0];
        }
        return strict ? sortStrict(array) : sort(array);
    }

    public static IrBoolExpr sort(IrIntExpr[]  
        ... strings) {
        return sortStrings(strings, false);
    }

    public static IrBoolExpr sortStrict(IrIntExpr[]  
        ... strings) {
        return sortStrings(strings, true);
    }

    public static IrBoolExpr sortChannel(IrIntExpr[][] strings, IrIntExpr[] ints) {
        if (strings.length != ints.length) {
            throw new IllegalArgumentException();
        }
        for (int i = 1; i < strings.length; i++) {
            if (strings[0].length != strings[i].length) {
                throw new IllegalArgumentException();
            }
        }
        List<IrIntExpr[]> filterStrings = new ArrayList<>(strings.length);
        List<IrIntExpr> filterInts = new ArrayList<>(ints.length);
        for (int i = 0; i < ints.length; i++) {
            boolean equivalence = false;
            for (int j = i + 1; j < ints.length; j++) {
                if (IrUtil.Ordering.EQ.equals(IrUtil.compare(ints[i], ints[j]))
                        && IrUtil.Ordering.EQ.equals(IrUtil.compareString(strings[i], strings[j]))) {
                    equivalence = true;
                    break;
                }
            }
            if (!equivalence) {
                filterStrings.add(strings[i]);
                filterInts.add(ints[i]);
            }
        }
        assert !filterInts.isEmpty();
        if (filterInts.size() == 1) {
            return equal(filterInts.get(0), 0);
        }
        IrIntExpr[][] fstrings = filterStrings.toArray(new IrIntExpr[filterStrings.size()][]);
        IrIntExpr[] fints = filterInts.toArray(new IrIntExpr[filterInts.size()]);
        int[] constant = IrUtil.getConstant(fints);
        if (constant != null) {
            IrIntExpr[][] partialOrdering = new IrIntExpr[constant.length][];
            for (int i = 0; i < constant.length; i++) {
                int val = constant[i];
                if (val < 0 || val >= constant.length) {
                    return False;
                }
                if (partialOrdering[constant[i]] != null) {
                    throw new IllegalStateException();
                }
                partialOrdering[constant[i]] = fstrings[i];
            }
            return sortStrict(partialOrdering);
        }
        return new IrSortStringsChannel(fstrings, fints, TrueFalseDomain);
    }

    public static IrBoolExpr allDifferent(IrIntExpr[] ints) {
        if (ints.length < 2) {
            return True;
        }
        if (ints.length == 2) {
            return notEqual(ints[0], ints[1]);
        }
        Domain domain = ints[0].getDomain();
        int size = ints[0].getDomain().size();
        for (int i = 1; i < ints.length; i++) {
            domain = domain.union(ints[i].getDomain());
            size += ints[i].getDomain().size();
            if (size != domain.size()) {
                return new IrAllDifferent(ints, TrueFalseDomain);
            }
        }
        return True;
    }

    public static IrBoolExpr selectN(IrBoolExpr[] bools, IrIntExpr n) {
        if (bools.length == 1) {
            if (bools[0].equals(n)) {
                return True;
            }
        }
        if (n.getDomain().getLowBound() > bools.length
                || n.getDomain().getHighBound() < 0) {
            return False;
        }
        boolean entailed = true;
        Domain nDomain = n.getDomain();
        for (int i = 0; i < bools.length; i++) {
            Boolean constant = IrUtil.getConstant(bools[i]);
            if (Boolean.TRUE.equals(constant)) {
                if (i >= nDomain.getHighBound()) {
                    return False;
                }
                if (i >= nDomain.getLowBound()) {
                    entailed = false;
                }
            } else if (Boolean.FALSE.equals(constant)) {
                if (i < nDomain.getLowBound()) {
                    return False;
                }
                if (i < nDomain.getHighBound()) {
                    entailed = false;
                }
            } else {
                entailed = false;
            }
        }
        if (entailed
                && n.getDomain().getLowBound() >= 0
                && n.getDomain().getHighBound() <= bools.length) {
            return True;
        }
        Integer constant = IrUtil.getConstant(n);
        if (constant != null) {
            IrBoolExpr[] ands = new IrBoolExpr[bools.length];
            System.arraycopy(bools, 0, ands, 0, constant);
            for (int i = constant; i < bools.length; i++) {
                ands[i] = not(bools[i]);
            }
            return and(ands);
        }
        return new IrSelectN(bools, n, TrueFalseDomain);
    }

    public static IrBoolExpr acyclic(IrIntExpr[] edges) {
        if (edges.length == 0) {
            return True;
        }
        return new IrAcyclic(edges, TrueFalseDomain);
    }

    public static IrBoolExpr unreachable(IrIntExpr[] edges, int from, int to) {
        return new IrUnreachable(edges, from, to, TrueFalseDomain);
    }

    public static IrBoolExpr filterString(IrSetExpr set, IrIntExpr[] string, IrIntExpr[] result) {
        if (set.getEnv().isEmpty()) {
            return filterString(set, 0, new IrIntExpr[0], result);
        }
        int offset = set.getEnv().getLowBound();
        int end = set.getEnv().getHighBound();
        return filterString(set, offset,
                Arrays.copyOfRange(string, offset, end + 1),
                result);
    }

    public static IrBoolExpr filterString(IrSetExpr set, int offset, IrIntExpr[] string, IrIntExpr[] result) {
        Domain constant = IrUtil.getConstant(set);
        if (constant != null) {
            int[] array = constant.getValues();
            IrBoolExpr[] ands = new IrBoolExpr[result.length];
            for (int i = 0; i < array.length; i++) {
                ands[i] = equal(string[array[i] - offset], result[i]);
            }
            for (int i = array.length; i < result.length; i++) {
                ands[i] = equal(result[i], -1);
            }
            return and(ands);
        }
        IrIntExpr[] filterString = Arrays.copyOf(string, string.length);
        IrIntExpr[] filterResult = Arrays.copyOf(result, result.length);
        TIntIterator iter = set.getEnv().iterator();
        int i = 0;
        while (iter.hasNext()) {
            int env = iter.next();
            int x = env - offset;
            if (!set.getKer().contains(env) || !filterString[x].equals(filterResult[i])) {
                break;
            }
            filterString[x] = Zero;
            filterResult[i] = Zero;
            i++;
        }
        int cut = filterResult.length;
        while (cut > 0 && Integer.valueOf(-1).equals(IrUtil.getConstant(filterResult[cut - 1]))) {
            cut--;
        }
        if (cut != filterResult.length) {
            filterResult = Arrays.copyOf(filterResult, cut);
        }
        return new IrFilterString(set, offset, filterString, filterResult, TrueFalseDomain);
    }

    /*
     * TODO STRING
     */
    public static IrBoolExpr prefix(IrStringExpr prefix, IrStringExpr word) {
        if (prefix.getLength().getHighBound() == 0) {
            return True;
        }
        if (prefix.getLength().getLowBound() >= word.getLength().getHighBound()) {
            return equal(prefix, word);
        }
        return new IrPrefix(prefix, word, TrueFalseDomain);
    }

    /*
     * TODO STRING
     */
    public static IrBoolExpr suffix(IrStringExpr suffix, IrStringExpr word) {
        if (suffix.getLength().getHighBound() == 0) {
            return True;
        }
        if (suffix.getLength().getLowBound() >= word.getLength().getHighBound()) {
            return equal(suffix, word);
        }
        return new IrSuffix(suffix, word, TrueFalseDomain);
    }
    /**
     *******************
     *
     * Integer
     *
     *******************
     */
    public static IrIntVar Zero = False;
    public static IrIntVar One = True;

    public static IrIntVar constant(int value) {
        switch (value) {
            case 0:
                return Zero;
            case 1:
                return One;
            default:
                return new IrIntConstant(value);
        }
    }

    public static IrIntVar domainInt(String name, Domain domain) {
        if (domain.size() == 1) {
            return constant(domain.getLowBound());
        }
        if (domain instanceof BoolDomain) {
            return new IrBoolVar(name, (BoolDomain) domain);
        }
        return new IrIntVar(name, domain);
    }

    public static IrIntVar boundInt(String name, int low, int high) {
        return domainInt(name, boundDomain(low, high));
    }

    public static IrIntVar enumInt(String name, int... values) {
        return domainInt(name, enumDomain(values));
    }

    public static IrIntExpr minus(IrIntExpr expr) {
        Integer constant = IrUtil.getConstant(expr);
        if (constant != null) {
            return constant(-constant);
        }
        if (expr instanceof IrMinus) {
            IrMinus minus = (IrMinus) expr;
            return minus.getExpr();
        }
        return new IrMinus(expr, expr.getDomain().minus());

    }

    public static IrIntExpr card(IrSetExpr set) {
        if (set instanceof IrSetVar) {
            IrSetVar var = (IrSetVar) set;
            return var.getCardVar();
        }
        Domain domain = set.getCard();
        if (domain.size() == 1) {
            return constant(domain.getLowBound());
        }
        return new IrCard(set, domain);
    }

    public static IrIntExpr add(int addend1, IrIntExpr addend2) {
        return add(constant(addend1), addend2);
    }

    public static IrIntExpr add(IrIntExpr addend1, int addend2) {
        return add(addend1, constant(addend2));
    }

    public static IrIntExpr add(Collection<? extends IrIntExpr> addends) {
        return add(addends.toArray(new IrIntExpr[addends.size()]));
    }

    public static IrIntExpr add(IrIntExpr... addends) {
        int constants = 0;
        List<IrIntExpr> filter = new ArrayList<>(addends.length);
        for (IrIntExpr addend : addends) {
            if (addend instanceof IrAdd) {
                IrAdd add = (IrAdd) addend;
                // Invariant: No nested IrAdd or constants
                filter.addAll(Arrays.asList(add.getAddends()));
                constants += add.getOffset();
            } else {
                Integer constant = IrUtil.getConstant(addend);
                if (constant != null) {
                    constants += constant;
                } else {
                    filter.add(addend);
                }
            }
        }
        if (filter.isEmpty()) {
            return constant(constants);
        }
        if (filter.size() == 1) {
            IrIntExpr first = filter.get(0);
            if (constants == 0) {
                return first;
            }
            return new IrAdd(new IrIntExpr[]{first}, constants,
                    first.getDomain().offset(constants));
        }
        int low = constants;
        int high = constants;
        for (IrIntExpr addend : filter) {
            low += addend.getDomain().getLowBound();
            high += addend.getDomain().getHighBound();
        }
        Domain domain = boundDomain(low, high);
        return new IrAdd(filter.toArray(new IrIntExpr[filter.size()]), constants, domain);
    }

    public static IrIntExpr sub(int minuend, IrIntExpr subtrahend) {
        return sub(constant(minuend), subtrahend);
    }

    public static IrIntExpr sub(IrIntExpr minuend, int subtrahend) {
        return sub(minuend, constant(subtrahend));
    }

    public static IrIntExpr sub(Collection<? extends IrIntExpr> subtrahends) {
        return sub(subtrahends.toArray(new IrIntExpr[subtrahends.size()]));
    }

    public static IrIntExpr sub(IrIntExpr... subtrahends) {
        if (subtrahends.length == 0) {
            return Zero;
        }
        IrIntExpr[] flip = new IrIntExpr[subtrahends.length];
        flip[0] = subtrahends[0];
        for (int i = 1; i < flip.length; i++) {
            flip[i] = minus(subtrahends[i]);
        }
        return add(flip);
    }

    public static IrIntExpr mul(int multiplicand, IrIntExpr multiplier) {
        return mul(constant(multiplicand), multiplier);
    }

    public static IrIntExpr mul(IrIntExpr multiplicand, int multiplier) {
        return mul(multiplicand, constant(multiplier));
    }

    public static IrIntExpr mul(IrIntExpr multiplicand, IrIntExpr multiplier) {
        Integer multiplicandConstant = IrUtil.getConstant(multiplicand);
        Integer multiplierConstant = IrUtil.getConstant(multiplier);
        if (multiplicandConstant != null) {
            switch (multiplicandConstant) {
                case -1:
                    return minus(multiplier);
                case 0:
                    return multiplicand;
                case 1:
                    return multiplier;
            }
        }
        if (multiplierConstant != null) {
            switch (multiplierConstant) {
                case -1:
                    return minus(multiplicand);
                case 0:
                    return multiplier;
                case 1:
                    return multiplicand;
            }
        }
        if (multiplicandConstant != null && multiplierConstant != null) {
            return constant(multiplicandConstant * multiplierConstant);
        }
        int low1 = multiplicand.getDomain().getLowBound();
        int high1 = multiplicand.getDomain().getHighBound();
        int low2 = multiplier.getDomain().getLowBound();
        int high2 = multiplier.getDomain().getHighBound();
        int min = Util.min(low1 * low2, low1 * high2, high1 * low2, high1 * high2);
        int max = Util.max(low1 * low2, low1 * high2, high1 * low2, high1 * high2);
        return new IrMul(multiplicand, multiplier, boundDomain(min, max));
    }

    public static IrIntExpr div(int dividend, IrIntExpr divisor) {
        return div(constant(dividend), divisor);
    }

    public static IrIntExpr div(IrIntExpr dividend, int divisor) {
        return div(dividend, constant(divisor));
    }

    public static IrIntExpr div(IrIntExpr dividend, IrIntExpr divisor) {
        Integer dividendConstant = IrUtil.getConstant(dividend);
        Integer divisorConstant = IrUtil.getConstant(divisor);
        if (dividendConstant != null && dividendConstant == 0) {
            return dividend;
        }
        if (divisorConstant != null && divisorConstant == 1) {
            return dividend;
        }
        if (dividendConstant != null && divisorConstant != null) {
            return constant(dividendConstant / divisorConstant);
        }
        int low1 = dividend.getDomain().getLowBound();
        int high1 = dividend.getDomain().getHighBound();
        int low2 = divisor.getDomain().getLowBound();
        int high2 = divisor.getDomain().getHighBound();
        int min = Util.min(low1, -low1, high1, -high1);
        int max = Util.max(low1, -low1, high1, -high1);
        return new IrDiv(dividend, divisor, boundDomain(min, max));
    }

    public static IrIntExpr element(IrIntExpr[] array, IrIntExpr index) {
        IrIntExpr[] $array = index.getDomain().getHighBound() + 1 < array.length
                ? Arrays.copyOf(array, index.getDomain().getHighBound() + 1)
                : array.clone();
        for (int i = 0; i < $array.length; i++) {
            if (!index.getDomain().contains(i)) {
                $array[i] = Zero;
            }
        }

        Integer constant = IrUtil.getConstant(index);
        if (constant != null) {
            return $array[constant];
        }
        TIntIterator iter = index.getDomain().iterator();
        assert iter.hasNext();

        Domain domain = $array[iter.next()].getDomain();
        int low = domain.getLowBound();
        int high = domain.getHighBound();
        while (iter.hasNext()) {
            int val = iter.next();
            if (val < $array.length) {
                domain = $array[val].getDomain();
                // TODO Use IrUtil.union
                low = Math.min(low, domain.getLowBound());
                high = Math.max(high, domain.getHighBound());
            }
        }
        domain = boundDomain(low, high);
        return new IrElement($array, index, domain);
    }

    public static IrIntExpr count(int value, IrIntExpr[] array) {
        List<IrIntExpr> filter = new ArrayList<>();
        int count = 0;
        for (IrIntExpr i : array) {
            Integer constant = IrUtil.getConstant(i);
            if (constant != null) {
                if (constant == value) {
                    count++;
                }
            } else if (i.getDomain().contains(value)) {
                filter.add(i);
            }
        }
        switch (filter.size()) {
            case 0:
                return constant(count);
            case 1:
                return add(equal(value, filter.get(0)), count);
            default:
                return add(
                        new IrCount(value, filter.toArray(new IrIntExpr[filter.size()]), boundDomain(0, filter.size())),
                        count);
        }
    }

    public static IrIntExpr sum(IrSetExpr set) {
        int sum = Util.sum(set.getKer().iterator());
        int count = set.getKer().size();

        // Calculate low
        int low = sum;
        int lowCount = count;
        TIntIterator envIter = set.getEnv().iterator();
        while (lowCount < set.getCard().getHighBound() && envIter.hasNext()) {
            int env = envIter.next();
            if (env >= 0 && lowCount >= set.getCard().getLowBound()) {
                break;
            }
            if (!set.getKer().contains(env)) {
                low += env;
                lowCount++;
            }
        }

        // Calculate high
        int high = sum;
        int highCount = count;
        envIter = set.getEnv().iterator(false);
        while (highCount < set.getCard().getHighBound() && envIter.hasNext()) {
            int env = envIter.next();
            if (env <= 0 && highCount >= set.getCard().getLowBound()) {
                break;
            }
            if (!set.getKer().contains(env)) {
                high += env;
                highCount++;
            }
        }

        return new IrSetSum(set, boundDomain(low, high));
    }

    public static IrIntExpr ternary(IrBoolExpr antecedent, IrIntExpr consequent, IrIntExpr alternative) {
        if (IrUtil.isTrue(antecedent)) {
            return consequent;
        }
        if (IrUtil.isFalse(antecedent)) {
            return alternative;
        }
        if (consequent.equals(alternative)) {
            return consequent;
        }
        Integer consequentConstant = IrUtil.getConstant(consequent);
        Integer alternativeConstant = IrUtil.getConstant(alternative);
        if (consequentConstant != null && consequentConstant.equals(alternativeConstant)) {
            return constant(consequentConstant);
        }
        Domain domain = consequent.getDomain().union(alternative.getDomain());
        return new IrTernary(antecedent, consequent, alternative, domain);
    }

    public static IrIntExpr length(IrStringExpr string) {
        if (string instanceof IrStringVar) {
            return ((IrStringVar) string).getLengthVar();
        }
        return new IrLength(string, string.getLength());
    }

    /**
     *******************
     *
     * Set
     *
     *******************
     */
    public static final IrSetVar EmptySet = new IrSetConstant(EmptyDomain);

    public static IrSetVar set(String name, int lowEnv, int highEnv) {
        return set(name, boundDomain(lowEnv, highEnv));
    }

    public static IrSetVar set(String name, int lowEnv, int highEnv, int lowKer, int highKer) {
        return set(name, boundDomain(lowEnv, highEnv), boundDomain(lowKer, highKer));
    }

    public static IrSetVar set(String name, int lowEnv, int highEnv, int[] ker) {
        return set(name, boundDomain(lowEnv, highEnv), enumDomain(ker));
    }

    public static IrSetVar set(String name, int[] env) {
        return set(name, enumDomain(env));
    }

    public static IrSetVar set(String name, int[] env, int lowKer, int highKer) {
        return set(name, enumDomain(env), boundDomain(lowKer, highKer));
    }

    public static IrSetVar set(String name, int[] env, int[] ker) {
        return set(name, enumDomain(env), enumDomain(ker));
    }

    public static IrSetVar set(String name, Domain env) {
        return set(name, env, EmptyDomain);
    }

    public static IrSetVar set(String name, Domain env, Domain ker) {
        return set(name, env, ker, boundDomain(ker.size(), env.size()));
    }

    public static IrSetVar set(String name, Domain env, Domain ker, Domain card) {
        return set(name, env, ker, domainInt("|" + name + "|", card));
    }

    public static IrSetVar set(String name, Domain env, Domain ker, IrIntVar card) {
        return IrUtil.asConstant(new IrSetVar(name, env, ker, card));
    }

    public static IrSetVar constant(int[] value) {
        return constant(enumDomain(value));
    }

    public static IrSetVar constant(TIntCollection value) {
        return constant(enumDomain(value));
    }

    public static IrSetVar constant(TIntSet value) {
        return constant(enumDomain(value));
    }

    public static IrSetVar constant(Domain value) {
        if (value.isEmpty()) {
            return EmptySet;
        }
        return new IrSetConstant(value);
    }

    public static IrSetExpr singleton(IrIntExpr value) {
        Integer constant = IrUtil.getConstant(value);
        if (constant != null) {
            return constant(new int[]{constant});
        }
        return new IrSingleton(value, value.getDomain(), EmptyDomain);
    }

    public static IrSetExpr arrayToSet(IrIntExpr[] array, Integer globalCardinality) {
        switch (array.length) {
            case 0:
                return EmptySet;
            case 1:
                return singleton(array[0]);
            default:
                Domain env = array[0].getDomain();
                for (int i = 1; i < array.length; i++) {
                    env = env.union(array[i].getDomain());
                }
                TIntSet values = new TIntHashSet();
                for (IrIntExpr i : array) {
                    Integer constant = IrUtil.getConstant(i);
                    if (constant != null) {
                        values.add(constant);
                    }
                }
                Domain ker = enumDomain(values);
                int lowCard = Math.max(
                        globalCardinality == null ? 1 : divRoundUp(array.length, globalCardinality),
                        ker.size());
                int highCard = Math.min(array.length, env.size());
                if (lowCard > highCard) {
                    assert globalCardinality > 0;
                    throw new UnsatisfiableException();
                }
                Domain card = boundDomain(lowCard, highCard);
                return IrUtil.asConstant(new IrArrayToSet(array, env, ker, card, globalCardinality));
        }
    }

    /**
     * Relational join.
     *
     * Union{for all i in take} children[i]
     *
     * @param take
     * @param children
     * @param injective
     * @return the join expression take.children
     */
    public static IrSetExpr joinRelation(IrSetExpr take, IrSetExpr[] children, boolean injective) {
        if (take.getEnv().isEmpty()) {
            return EmptySet;
        }
        IrSetExpr[] $children = take.getEnv().getHighBound() + 1 < children.length
                ? Arrays.copyOf(children, take.getEnv().getHighBound() + 1)
                : children.clone();
        for (int i = 0; i < $children.length; i++) {
            if (!take.getEnv().contains(i)) {
                $children[i] = EmptySet;
            }
        }

        IrIntExpr[] ints = IrUtil.asInts(children);
        if (ints != null) {
            return joinFunction(take, ints, injective ? 1 : 0);
        }

        Domain constant = IrUtil.getConstant(take);
        if (constant != null) {
            int[] array = constant.getValues();
            IrSetExpr[] to = new IrSetExpr[array.length];
            for (int i = 0; i < to.length; i++) {
                to[i] = $children[array[i]];
            }
            return union(to, injective);
        }

        // Compute env
        TIntIterator iter = take.getEnv().iterator();
        Domain env;
        if (iter.hasNext()) {
            Domain domain = $children[iter.next()].getEnv();
            while (iter.hasNext()) {
                domain = domain.union($children[iter.next()].getEnv());
            }
            env = domain;
        } else {
            env = EmptyDomain;
        }

        // Compute ker
        iter = take.getKer().iterator();
        Domain ker;
        if (iter.hasNext()) {
            Domain domain = $children[iter.next()].getKer();
            while (iter.hasNext()) {
                domain = domain.union($children[iter.next()].getKer());
            }
            ker = domain;
        } else {
            ker = EmptyDomain;
        }

        // Compute card
        Domain takeEnv = take.getEnv();
        Domain takeKer = take.getKer();
        Domain takeCard = take.getCard();
        int index = 0;
        int[] childrenLowCards = new int[takeEnv.size() - takeKer.size()];
        int[] childrenHighCards = new int[takeEnv.size() - takeKer.size()];
        int cardLow = 0, cardHigh = 0;

        iter = takeEnv.iterator();
        while (iter.hasNext()) {
            int val = iter.next();
            Domain childDomain = $children[val].getCard();
            if (takeKer.contains(val)) {
                cardLow = injective
                        ? cardLow + childDomain.getLowBound()
                        : Math.max(cardLow, childDomain.getLowBound());
                cardHigh = injective
                        ? cardHigh + childDomain.getHighBound()
                        : Math.max(cardHigh, childDomain.getHighBound());
            } else {
                childrenLowCards[index] = childDomain.getLowBound();
                childrenHighCards[index] = childDomain.getHighBound();
                index++;
            }
        }
        assert index == childrenLowCards.length;
        assert index == childrenHighCards.length;

        Arrays.sort(childrenLowCards);
        Arrays.sort(childrenHighCards);

        for (int i = 0; i < takeCard.getLowBound() - takeKer.size(); i++) {
            cardLow = injective
                    ? cardLow + childrenLowCards[i]
                    : Math.max(cardLow, childrenLowCards[i]);
        }
        for (int i = 0; i < takeCard.getHighBound() - takeKer.size(); i++) {
            cardHigh = injective
                    ? cardHigh + childrenHighCards[childrenHighCards.length - 1 - i]
                    : Math.max(cardHigh, childrenHighCards[childrenHighCards.length - 1 - i]);
        }
        cardLow = Math.max(cardLow, ker.size());
        cardHigh = Math.min(cardHigh, env.size());
        Domain card = boundDomain(cardLow, cardHigh);

        return new IrJoinRelation(take, $children, env, ker, card, injective);
    }

    public static IrSetExpr joinFunction(IrSetExpr take, IrIntExpr[] refs, Integer globalCardinality) {
        if (take.getEnv().isEmpty()) {
            return EmptySet;
        }
        IrIntExpr[] $refs = take.getEnv().getHighBound() + 1 < refs.length
                ? Arrays.copyOf(refs, take.getEnv().getHighBound() + 1)
                : refs.clone();
        for (int i = 0; i < $refs.length; i++) {
            if (!take.getEnv().contains(i)) {
                $refs[i] = Zero;
            }
        }

        Domain constant = IrUtil.getConstant(take);
        if (constant != null) {
            int[] array = constant.getValues();
            IrIntExpr[] to = new IrIntExpr[array.length];
            for (int i = 0; i < to.length; i++) {
                to[i] = $refs[array[i]];
            }
            return arrayToSet(to, globalCardinality);
        }

        // Compute env
        TIntIterator iter = take.getEnv().iterator();
        Domain env;
        if (iter.hasNext()) {
            Domain domain = $refs[iter.next()].getDomain();
            while (iter.hasNext()) {
                domain = domain.union($refs[iter.next()].getDomain());
            }
            env = domain;
        } else {
            env = EmptyDomain;
        }

        // Compute ker
        iter = take.getKer().iterator();
        TIntHashSet values = new TIntHashSet(0);
        while (iter.hasNext()) {
            Integer constantRef = IrUtil.getConstant($refs[iter.next()]);
            if (constantRef != null) {
                values.add(constantRef);
            }
        }
        Domain ker = values.isEmpty() ? EmptyDomain : new EnumDomain(values.toArray());

        // Compute card
        Domain takeCard = take.getCard();
        int lowTakeCard = takeCard.getLowBound();
        int highTakeCard = takeCard.getHighBound();
        Domain card;
        if (globalCardinality == null) {
            card = lowTakeCard == 0
                    ? boundDomain(Math.max(0, ker.size()), Math.min(highTakeCard, env.size()))
                    : boundDomain(Math.max(1, ker.size()), Math.min(highTakeCard, env.size()));
        } else {
            card = boundDomain(
                    divRoundUp(Math.max(lowTakeCard, ker.size()), globalCardinality),
                    Math.min(highTakeCard, env.size()));
        }

        return new IrJoinFunction(take, $refs, env, ker, card, globalCardinality);
    }

    private static int divRoundUp(int a, int b) {
        assert a >= 0;
        assert b > 0;

        return (a + b - 1) / b;
    }

    public static IrSetExpr difference(IrSetExpr minuend, IrSetExpr subtrahend) {
        Domain env = minuend.getEnv().difference(subtrahend.getKer());
        Domain ker = minuend.getKer().difference(subtrahend.getEnv());
        int low = Math.max(0, minuend.getCard().getLowBound() - subtrahend.getCard().getHighBound());
        int high = minuend.getCard().getHighBound();
        Domain card = boundDomain(Math.max(low, ker.size()), Math.min(high, env.size()));
        return new IrSetDifference(minuend, subtrahend, env, ker, card);
    }

    public static IrSetExpr intersection(IrSetExpr... operands) {
        List<IrSetExpr> flatten = new ArrayList<>(operands.length);
        for (IrSetExpr operand : operands) {
            if (operand instanceof IrSetIntersection) {
                // Invariant: No nested IrSetIntersection
                flatten.addAll(Arrays.asList(((IrSetIntersection) operand).getOperands()));
            } else {
                flatten.add(operand);
            }
        }
        TIntSet constants = null;
        List<IrSetExpr> filter = new ArrayList<>();
        for (IrSetExpr operand : flatten) {
            Domain constant = IrUtil.getConstant(operand);
            if (constant == null) {
                filter.add(operand);
            } else {
                if (constants == null) {
                    constants = new TIntHashSet(constant.getValues());
                } else {
                    constants.retainAll(constant.getValues());
                }
            }
        }
        if (constants != null) {
            filter.add(constant(constants));
        }
        IrSetExpr[] ops = filter.toArray(new IrSetExpr[filter.size()]);
        switch (ops.length) {
            case 0:
                return EmptySet;
            case 1:
                return ops[0];
            default:
                Domain env = ops[0].getEnv();
                Domain ker = ops[0].getKer();
                int low = 0;
                int high = ops[0].getCard().getHighBound();
                for (int i = 1; i < ops.length; i++) {
                    env = env.intersection(ops[i].getEnv());
                    ker = ker.intersection(ops[i].getKer());
                    high = Math.max(high, ops[i].getCard().getHighBound());
                }
                Domain card = boundDomain(
                        Math.max(low, ker.size()),
                        Math.min(high, env.size()));
                return new IrSetIntersection(ops, env, ker, card);
        }
    }

    public static IrSetExpr union(IrSetExpr... operands) {
        return union(operands, false);
    }

    public static IrSetExpr union(IrSetExpr[] operands, boolean disjoint) {
        List<IrSetExpr> flatten = new ArrayList<>(operands.length);
        for (IrSetExpr operand : operands) {
            if (operand instanceof IrSetUnion) {
                // Invariant: No nested IrSetUnion
                flatten.addAll(Arrays.asList(((IrSetUnion) operand).getOperands()));
            } else {
                flatten.add(operand);
            }
        }
        TIntSet constants = new TIntHashSet();
        List<IrSetExpr> filter = new ArrayList<>();
        for (IrSetExpr operand : flatten) {
            Domain constant = IrUtil.getConstant(operand);
            if (constant == null) {
                filter.add(operand);
            } else {
                constant.transferTo(constants);
            }
        }
        if (!constants.isEmpty()) {
            filter.add(constant(constants));
        }
        IrSetExpr[] ops = filter.toArray(new IrSetExpr[filter.size()]);
        switch (ops.length) {
            case 0:
                return EmptySet;
            case 1:
                return ops[0];
            default:
                Domain env = ops[0].getEnv();
                Domain ker = ops[0].getKer();
                Domain operandCard = ops[0].getCard();
                int low = operandCard.getLowBound();
                int high = operandCard.getHighBound();
                for (int i = 1; i < ops.length; i++) {
                    env = env.union(ops[i].getEnv());
                    ker = ker.union(ops[i].getKer());
                    operandCard = ops[i].getCard();
                    low = disjoint
                            ? low + operandCard.getLowBound()
                            : Math.max(low, operandCard.getLowBound());
                    high += operandCard.getHighBound();
                }
                Domain card = boundDomain(
                        Math.max(low, ker.size()),
                        Math.min(high, env.size()));
                return IrUtil.asConstant(new IrSetUnion(ops, env, ker, card, disjoint));
        }
    }

    public static IrSetExpr offset(IrSetExpr set, int offset) {
        if (offset == 0) {
            return set;
        }
        if (set instanceof IrOffset) {
            IrOffset nested = (IrOffset) set;
            return offset(nested.getSet(), offset + nested.getOffset());
        }
        Domain env = set.getEnv().offset(offset);
        Domain ker = set.getKer().offset(offset);
        Domain card = set.getCard();
        return IrUtil.asConstant(new IrOffset(set, offset, env, ker, card));
    }

    public static IrSetExpr mask(IrSetExpr set, int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException();
        }
        if (from == to) {
            return EmptySet;
        }
        if (from <= set.getEnv().getLowBound() && to > set.getEnv().getHighBound()) {
            return offset(set, -from);
        }
        Domain env = set.getEnv().offset(-from).boundBetween(0, to - from - 1);
        Domain ker = set.getKer().offset(-from).boundBetween(0, to - from - 1);
        Domain card = boundDomain(ker.size(), Math.min(env.size(), set.getCard().getHighBound()));
        return IrUtil.asConstant(new IrMask(set, from, to, env, ker, card));
    }

    public static IrSetExpr ternary(IrBoolExpr antecedent, IrSetExpr consequent, IrSetExpr alternative) {
        if (IrUtil.isTrue(antecedent)) {
            return consequent;
        }
        if (IrUtil.isFalse(antecedent)) {
            return alternative;
        }
        if (consequent.equals(alternative)) {
            return consequent;
        }
        Domain env = consequent.getEnv().union(alternative.getEnv());
        Domain ker = consequent.getKer().intersection(alternative.getKer());
        Domain card = consequent.getCard().union(alternative.getCard());
        return new IrSetTernary(antecedent, consequent, alternative, env, ker, card);
    }

    /**
     *******************
     *
     * String
     *
     *******************
     */
    public static IrStringVar EmptyString = new IrStringConstant("");

    public static IrStringVar constant(String value) {
        if (value.length() == 0) {
            return EmptyString;
        }
        return new IrStringConstant(value);
    }

    public static IrStringVar string(String name, IrIntVar[] chars, IrIntVar length) {
        if (length instanceof IrConstant) {
            char[] string = new char[chars.length];
            for (int i = 0; string != null && i < string.length; i++) {
                if (chars[i] instanceof IrConstant) {
                    string[i] = (char) IrUtil.getConstant(chars[i]).intValue();
                } else {
                    string = null;
                }
            }
            if (string != null) {
                return constant(new String(string, 0, IrUtil.getConstant(length)));
            }
        }
        int lengthHigh = length.getDomain().getHighBound();
        if (lengthHigh < 0) {
            throw new IllegalArgumentException();
        }
        int maxLength = chars.length - 1;
        while (maxLength >= lengthHigh && Zero.equals(chars[maxLength])) {
            maxLength--;
        }
        if (maxLength + 1 < chars.length) {
            chars = Arrays.copyOf(chars, maxLength + 1);
        }
        return new IrStringVar(name, chars, length);
    }

    public static IrStringVar string(String name, Domain charDomain, int maxLength) {
        Domain domain = charDomain.insert(0);
        IrIntVar[] chars = new IrIntVar[maxLength];
        for (int i = 0; i < maxLength; i++) {
            chars[i] = domainInt(name + "[" + i + "]", domain);
        }
        return string(name, chars, boundInt("|" + name + "|", 0, maxLength));
    }

    public static IrStringExpr element(IrStringExpr[] array, IrIntExpr index) {
        IrStringExpr[] $array = index.getDomain().getHighBound() + 1 < array.length
                ? Arrays.copyOf(array, index.getDomain().getHighBound() + 1)
                : array.clone();
        for (int i = 0; i < $array.length; i++) {
            if (!index.getDomain().contains(i)) {
                $array[i] = EmptyString;
            }
        }

        Integer constant = IrUtil.getConstant(index);
        if (constant != null) {
            return $array[constant];
        }
        TIntIterator iter = index.getDomain().iterator();
        assert iter.hasNext();

        Domain length = EmptyDomain;
        Domain[] chars = new Domain[IrUtil.maxLength($array)];
        Arrays.fill(chars, EmptyDomain);
        while (iter.hasNext()) {
            int val = iter.next();
            if (val < $array.length) {
                IrStringExpr string = $array[val];
                length = length.union(string.getLength());
                for (int i = 0; i < string.getChars().length; i++) {
                    chars[i] = chars[i].union(string.getChars()[i]);
                }
            }
        }
        for (int i = 0; i < chars.length; i++) {
            if (i < length.getLowBound()) {
                chars[i] = chars[i].remove(0);
            } else {
                assert i < length.getHighBound();
                chars[i] = chars[i].insert(0);
            }
        }
        return new IrStringElement($array, index, chars, length);
    }

    public static IrStringExpr concat(IrStringExpr left, IrStringExpr right) {
        Domain leftLength = left.getLength();
        Domain[] leftChars = left.getChars();
        Domain rightLength = right.getLength();
        Domain[] rightChars = right.getChars();

        if (leftLength.getHighBound() == 0) {
            return right;
        }
        if (rightLength.getHighBound() == 0) {
            return left;
        }

        Domain length;
        if (leftLength.size() == 1) {
            length = rightLength.offset(leftLength.getLowBound());
        } else if (rightLength.size() == 1) {
            length = leftLength.offset(rightLength.getLowBound());
        } else {
            length = boundDomain(
                    leftLength.getLowBound() + rightLength.getLowBound(),
                    leftLength.getHighBound() + rightLength.getHighBound());
        }
        Domain[] charDomains = new Domain[length.getHighBound()];
        int i;
        for (i = 0; i < leftLength.getLowBound(); i++) {
            charDomains[i] = leftChars[i];
        }
        Domain union = EmptyDomain;
        for (; i < leftLength.getHighBound(); i++) {
            int j = i - leftLength.getLowBound();
            if (j < rightChars.length) {
                union = union.union(rightChars[j]);
            }
            charDomains[i] = leftChars[i].union(union);
        }
        for (; i < charDomains.length; i++) {
            int j = i - leftLength.getLowBound();
            int k = i - leftLength.getHighBound();
            assert k < rightChars.length;
            charDomains[i] = union(rightChars, k, Math.min(j + 1, rightChars.length));
        }
        for (i = length.getLowBound(); i < charDomains.length; i++) {
            charDomains[i] = charDomains[i].insert(0);
        }
        return IrUtil.asConstant(new IrConcat(left, right, charDomains, length));
    }

    private static Domain union(Domain[] domains, int start, int end) {
        assert start < end;
        Domain union = domains[start];
        for (int i = start + 1; i < end; i++) {
            union = union.union(domains[i]);
        }
        return union;
    }
}
