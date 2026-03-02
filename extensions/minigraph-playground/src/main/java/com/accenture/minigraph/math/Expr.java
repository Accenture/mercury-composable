package com.accenture.minigraph.math;

import java.util.List;

abstract class Expr {
    static final class NumberLiteral extends Expr {
        final double value;
        NumberLiteral(double value) { this.value = value; }
    }
    static final class BooleanLiteral extends Expr {
        final boolean value;
        BooleanLiteral(boolean value) { this.value = value; }
    }
    static final class Variable extends Expr {
        final String name;
        Variable(String name) { this.name = name; }
    }
    static final class Unary extends Expr {
        final String op;
        final Expr right;
        Unary(String op, Expr right) { this.op = op; this.right = right; }
    }
    static final class Binary extends Expr {
        final String op;
        final Expr left;
        final Expr right;
        Binary(String op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
    }
    static final class MemberAccess extends Expr {
        final Expr target;
        final String property;
        MemberAccess(Expr target, String property) { this.target = target; this.property = property; }
    }
    static final class Call extends Expr {
        final Expr callee;
        final List<Expr> args;
        Call(Expr callee, List<Expr> args) { this.callee = callee; this.args = args; }
    }
    static final class Conditional extends Expr {
        final Expr test;
        final Expr consequent;
        final Expr alternate;
        Conditional(Expr test, Expr consequent, Expr alternate) {
            this.test = test; this.consequent = consequent; this.alternate = alternate;
        }
    }
}
