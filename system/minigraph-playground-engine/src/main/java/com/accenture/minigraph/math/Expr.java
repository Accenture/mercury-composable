package com.accenture.minigraph.math;

import java.util.List;

public sealed interface Expr
        permits Expr.NumberLiteral, Expr.StringLiteral, Expr.BooleanLiteral,
        Expr.Variable, Expr.Unary, Expr.Binary,
        Expr.MemberAccess, Expr.Call, Expr.Conditional {

    record NumberLiteral(double value) implements Expr {}
    record StringLiteral(String value) implements Expr {}
    record BooleanLiteral(boolean value) implements Expr {}
    record Variable(String name) implements Expr {}
    record Unary(String op, Expr right) implements Expr {}
    record Binary(String op, Expr left, Expr right) implements Expr {}
    record MemberAccess(Expr target, String property) implements Expr {}
    record Call(Expr callee, List<Expr> args) implements Expr {}
    record Conditional(Expr test, Expr consequent, Expr alternate) implements Expr {}
}