package org.clafer.collection;

import org.clafer.common.Check;
import org.clafer.common.Util;

/**
 * A sum type. Does not allow nulls.
 *
 * @param <A> the type of left
 * @param <B> the type of right
 * @author jimmy
 */
public class Either<A, B> {

    private final A left;
    private final B right;

    private Either(A left, B right) {
        this.left = left;
        this.right = right;
    }

    public boolean isLeft() {
        return left != null;
    }

    public A getLeft() {
        assert isLeft();
        return left;
    }

    public boolean isRight() {
        return right != null;
    }

    public B getRight() {
        assert isRight();
        return right;
    }

    public static <A, B> Either<A, B> left(A left) {
        return new Either<A, B>(Check.notNull(left), null);
    }

    public static <A, B> Either<A, B> right(B right) {
        return new Either<A, B>(null, Check.notNull(right));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Either<?, ?>) {
            Either<?, ?> other = (Either<?, ?>) obj;
            return Util.equals(left, other.left) && Util.equals(right, other.right);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.hashCode(left) ^ Util.hashCode(right);
    }

    @Override
    public String toString() {
        return isLeft() ? "Left " + left : "Right " + right;
    }
}
