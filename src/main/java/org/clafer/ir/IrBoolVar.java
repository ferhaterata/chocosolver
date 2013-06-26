package org.clafer.ir;

import org.clafer.common.Check;

/**
 *
 * @author jimmy
 */
public class IrBoolVar extends IrAbstractBool implements IrVar {

    private final String name;
    private final boolean decision;

    IrBoolVar(String name, IrBoolDomain domain) {
        this(name, domain, true);
    }

    IrBoolVar(String name, IrBoolDomain domain, boolean decision) {
        super(domain);
        this.name = Check.notNull(name);
        this.decision = decision;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IrBoolVar asDecision() {
        return new IrBoolVar(name, getDomain(), true);
    }

    @Override
    public IrBoolVar asNoDecision() {
        return new IrBoolVar(name, getDomain(), false);
    }

    @Override
    public boolean isDecision() {
        return decision;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}