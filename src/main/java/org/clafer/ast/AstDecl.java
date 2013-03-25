package org.clafer.ast;

import org.clafer.Check;

/**
 *
 * @author jimmy
 */
public class AstDecl {

    private final boolean disjoint;
    private final AstLocal[] locals;
    private final AstSetExpression body;

    public AstDecl(boolean disjoint, AstLocal[] locals, AstSetExpression body) {
        this.disjoint = disjoint;
        this.locals = Check.notNull(locals);
        if (locals.length < 1) {
            throw new IllegalArgumentException();
        }
        this.body = Check.notNull(body);
    }

    public boolean isDisjoint() {
        return disjoint;
    }

    public AstLocal[] getLocals() {
        return locals;
    }

    public AstSetExpression getBody() {
        return body;
    }

    public AstDecl withBody(AstSetExpression body) {
        return new AstDecl(disjoint, locals, body);
    }
}
