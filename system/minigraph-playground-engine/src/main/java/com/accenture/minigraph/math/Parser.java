package com.accenture.minigraph.math;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Non-recursive expression parser (shunting-yard to AST) with postfix (call/member) support. */
final class Parser {
    private final Lexer lexer;
    private Token lookahead;

    // Precedence (higher binds tighter)
    private static final int BP_EXP     = 70;
    private static final int BP_UNARY   = 60; // prefix + - !
    private static final int BP_MUL     = 50;
    private static final int BP_ADD     = 40;
    private static final int BP_REL     = 30; // < <= > >=
    private static final int BP_EQ      = 20; // == !=
    private static final int BP_AND     = 12; // &&
    private static final int BP_OR      = 10; // ||

    private static final int BP_TERNARY = 5;  // ?:

    private final boolean strictJsExponentiationRule;

    Parser(String src) { this(src, true); }

    Parser(String src, boolean strictJsExponentiationRule) {
        this.lexer = new Lexer(src);
        this.lookahead = lexer.next();
        this.strictJsExponentiationRule = strictJsExponentiationRule;
    }

    Expr parse() {
        Expr expr = parseExpressionUntil(EnumSet.of(TokenType.EOF));
        expect(TokenType.EOF);
        return expr;
    }

    private enum Assoc { LEFT, RIGHT }
    private enum Kind  { BINARY, UNARY, TERNARY_Q, LPAREN }

    private static final class Op {
        final Kind kind;
        final String symbol;
        final int prec;
        final Assoc assoc;
        boolean seenColon; // only for TERNARY_Q
        Op(Kind kind, String symbol, int prec, Assoc assoc) {
            this.kind = kind; this.symbol = symbol; this.prec = prec; this.assoc = assoc;
        }
    }

    private Expr parseExpressionUntil(Set<TokenType> stoppers) {
        Deque<Expr> values = new ArrayDeque<>();
        Deque<Op>   ops    = new ArrayDeque<>();
        var expectOperand = new AtomicBoolean(true);

        var outer = new AtomicBoolean(true);
        while (outer.get()) {
            Token t = lookahead;

            if (stoppers.contains(t.type()) && notContainsLParen(ops)) {
                break;
            }

            switch (t.type()) {
                // ----- primaries -----
                case NUMBER -> parseNumberToken(t, values, expectOperand);
                case STRING -> parseStringToken(t, values, expectOperand);
                case TRUE  -> parseBooleanToken(true, values, expectOperand);
                case FALSE -> parseBooleanToken(false, values, expectOperand);
                case IDENTIFIER -> parseIdentifierToken(t, values, expectOperand);
                case LPAREN -> parseLeftParenthesis(ops, expectOperand);
                case RPAREN -> parseRightParenthesis(t, ops, values, expectOperand);

                // ----- separators -----
                case COMMA -> {
                    if (stoppers.contains(TokenType.COMMA) && notContainsLParen(ops)) {
                        outer.set(false);
                        break;
                    }
                    throw new ParseException("Unexpected ',' at position " + t.position());
                }
                case COLON -> parseColon(t, ops, values, expectOperand);

                // ----- operators -----
                case QUESTION -> parseQuestion(ops, values, expectOperand);
                case PLUS, MINUS, BANG -> parsePlusMinusBang(t, ops, values, expectOperand);

                case STAR, SLASH, PERCENT, DOUBLESTAR, LT, LTE, GT, GTE, EQEQ, BANGEQ, AMPAMP, BARBAR ->
                    parseArithmeticAndLogical(t, ops, values, expectOperand);

                default -> throw new ParseException("Unexpected token: " + t);
            }
        }

        while (!ops.isEmpty()) {
            Op op = ops.pop();
            if (op.kind == Kind.LPAREN) throw new ParseException("Unmatched '('");
            applyOp(values, op);
        }

        if (values.size() != 1) throw new ParseException("Incomplete expression");
        return values.pop();
    }

    private void parseNumberToken(Token t, Deque<Expr> values, AtomicBoolean expectOperand) {
        consume();
        values.push(new Expr.NumberLiteral(parseDouble(t.lexeme(), t.position())));
        expectOperand.set(false);
        parsePostfixChainOnTop(values);
    }

    private void parseStringToken(Token t, Deque<Expr> values, AtomicBoolean expectOperand) {
        consume();
        values.push(new Expr.StringLiteral(t.lexeme()));
        expectOperand.set(false);
        parsePostfixChainOnTop(values);
    }

    private void parseBooleanToken(boolean value, Deque<Expr> values, AtomicBoolean expectOperand) {
        consume();
        values.push(new Expr.BooleanLiteral(value));
        expectOperand.set(false);
        parsePostfixChainOnTop(values);
    }

    private void parseIdentifierToken(Token t, Deque<Expr> values, AtomicBoolean expectOperand) {
        consume();
        values.push(new Expr.Variable(t.lexeme()));
        expectOperand.set(false);
        parsePostfixChainOnTop(values);
    }

    private void parseLeftParenthesis(Deque<Op> ops, AtomicBoolean expectOperand) {
        consume();
        ops.push(new Op(Kind.LPAREN, "(", -1, Assoc.LEFT));
        expectOperand.set(true);
    }

    private void parseRightParenthesis(Token t, Deque<Op> ops, Deque<Expr> values, AtomicBoolean expectOperand) {
        consume();
        while (!ops.isEmpty() && ops.peek().kind != Kind.LPAREN) {
            applyOp(values, ops.pop());
        }
        if (ops.isEmpty() || ops.peek().kind != Kind.LPAREN)
            throw new ParseException("Unmatched ')' at position " + t.position());
        ops.pop();
        expectOperand.set(false);
        parsePostfixChainOnTop(values);
    }

    private void parseColon(Token t, Deque<Op> ops, Deque<Expr> values, AtomicBoolean expectOperand) {
        consume();
        while (!ops.isEmpty() && ops.peek().kind != Kind.TERNARY_Q) {
            applyOp(values, ops.pop());
        }
        if (ops.isEmpty() || ops.peek().kind != Kind.TERNARY_Q)
            throw new ParseException("':' without matching '?' at position " + t.position());
        ops.peek().seenColon = true;
        expectOperand.set(true);
    }

    private void parseQuestion(Deque<Op> ops, Deque<Expr> values, AtomicBoolean expectOperand) {
        // reduce higher‑precedence ops so ternary sees the completed test on the left: (A || B) ? X : Y
        while (!ops.isEmpty()) {
            if (breakOnPrecedence(ops, values)) break;
        }
        consume();
        ops.push(new Op(Kind.TERNARY_Q, "?:", BP_TERNARY, Assoc.RIGHT));
        expectOperand.set(true);
    }

    private void parsePlusMinusBang(Token t, Deque<Op> ops, Deque<Expr> values, AtomicBoolean expectOperand) {
        if (expectOperand.get()) {
            consume();
            Op u = switch (t.type()) {
                case PLUS  -> new Op(Kind.UNARY, "+", BP_UNARY, Assoc.RIGHT);
                case MINUS -> new Op(Kind.UNARY, "-", BP_UNARY, Assoc.RIGHT);
                case BANG  -> new Op(Kind.UNARY, "!", BP_UNARY, Assoc.RIGHT);
                default    -> throw new IllegalStateException();
            };
            ops.push(u);
        } else {
            if (t.type() == TokenType.BANG) {
                throw new ParseException("Unexpected '!' at position " + t.position());
            }
            pushBinaryAndReduce(values, ops, t);
            expectOperand.set(true);
        }
    }

    private void parseArithmeticAndLogical(Token t, Deque<Op> ops, Deque<Expr> values, AtomicBoolean expectOperand) {
        if (expectOperand.get())
            throw new ParseException("Missing left operand before '" + t.lexeme() + "' at position " + t.position());
        // JS rule: UnaryExpression cannot be left operand of '**'
        if (t.type() == TokenType.DOUBLESTAR && strictJsExponentiationRule) {
            Op top = ops.peek();
            if (top != null && top.kind == Kind.UNARY) {
                throw new ParseException("Unary expression cannot be left operand of '**'. Use parentheses, e.g., -(2**2).");
            }
        }
        pushBinaryAndReduce(values, ops, t);
        expectOperand.set(true);
    }

    private boolean breakOnPrecedence(Deque<Op> ops, Deque<Expr> values) {
        if (ops.isEmpty()) return true;
        Op top = ops.peek();
        if (top.kind == Kind.LPAREN) return true;
        if (top.kind == Kind.UNARY) { applyOp(values, ops.pop()); return false; }
        if (top.kind == Kind.TERNARY_Q) return true; // chain ternaries normally
        if (top.prec > BP_TERNARY) { applyOp(values, ops.pop()); return false; }
        return true;
    }

    private boolean notContainsLParen(Deque<Op> ops) {
        for (Op op : ops) if (op.kind == Kind.LPAREN) return false;
        return true;
    }

    private void pushBinaryAndReduce(Deque<Expr> values, Deque<Op> ops, Token t) {
        Op curr = switch (t.type()) {
            case DOUBLESTAR -> new Op(Kind.BINARY, "**", BP_EXP, Assoc.RIGHT);
            case STAR       -> new Op(Kind.BINARY, "*",  BP_MUL, Assoc.LEFT);
            case SLASH      -> new Op(Kind.BINARY, "/",  BP_MUL, Assoc.LEFT);
            case PERCENT    -> new Op(Kind.BINARY, "%",  BP_MUL, Assoc.LEFT);
            case PLUS       -> new Op(Kind.BINARY, "+",  BP_ADD, Assoc.LEFT);
            case MINUS      -> new Op(Kind.BINARY, "-",  BP_ADD, Assoc.LEFT);
            case LT         -> new Op(Kind.BINARY, "<",  BP_REL, Assoc.LEFT);
            case LTE        -> new Op(Kind.BINARY, "<=", BP_REL, Assoc.LEFT);
            case GT         -> new Op(Kind.BINARY, ">",  BP_REL, Assoc.LEFT);
            case GTE        -> new Op(Kind.BINARY, ">=", BP_REL, Assoc.LEFT);
            case EQEQ       -> new Op(Kind.BINARY, "==", BP_EQ,  Assoc.LEFT);
            case BANGEQ     -> new Op(Kind.BINARY, "!=", BP_EQ,  Assoc.LEFT);
            case AMPAMP     -> new Op(Kind.BINARY, "&&", BP_AND, Assoc.LEFT);
            case BARBAR     -> new Op(Kind.BINARY, "||", BP_OR,  Assoc.LEFT);
            default -> throw new IllegalStateException("Not a binary operator: " + t);
        };

        while (!ops.isEmpty()) {
            if (breakOnParenTernary(curr, ops, values)) break;
        }
        ops.push(curr);
        consume();
    }

    private boolean breakOnParenTernary(Op curr, Deque<Op> ops, Deque<Expr> values) {
        if (ops.isEmpty()) return true;
        Op top = ops.peek();
        if (top.kind == Kind.LPAREN) return true;
        if (top.kind == Kind.TERNARY_Q) {
            if (curr.prec <= BP_TERNARY) applyOp(values, ops.pop()); else return true;
        } else if (top.kind == Kind.UNARY) {
            applyOp(values, ops.pop());
        } else {
            boolean takeTop = (top.prec > curr.prec) ||
                    (top.prec == curr.prec && top.assoc == Assoc.LEFT && curr.assoc == Assoc.LEFT);
            if (takeTop) applyOp(values, ops.pop());
            else return true;
        }
        return false;
    }

    private void applyOp(Deque<Expr> values, Op op) {
        switch (op.kind) {
            case UNARY -> {
                Expr r = values.pollFirst();
                if (r == null) throw new ParseException("Missing operand for unary '" + op.symbol + "'");
                values.push(new Expr.Unary(op.symbol, r));
            }
            case BINARY -> {
                Expr r = values.pollFirst();
                Expr l = values.pollFirst();
                if (l == null || r == null) throw new ParseException("Missing operands for binary '" + op.symbol + "'");
                values.push(new Expr.Binary(op.symbol, l, r));
            }
            case TERNARY_Q -> {
                if (!op.seenColon) throw new ParseException("Missing ':' for ternary operator");
                Expr alt = values.pollFirst();
                Expr cons = values.pollFirst();
                Expr test = values.pollFirst();
                if (test == null || cons == null || alt == null) throw new ParseException("Malformed ternary expression");
                values.push(new Expr.Conditional(test, cons, alt));
            }
            case LPAREN -> throw new ParseException("Unmatched '('");
        }
    }

    private void parsePostfixChainOnTop(Deque<Expr> values) {
        while (true) {
            if (lookahead.type() == TokenType.DOT) {
                consume();
                Token id = expect(TokenType.IDENTIFIER);
                Expr target = values.pop();
                values.push(new Expr.MemberAccess(target, id.lexeme()));
            } else if (lookahead.type() == TokenType.LPAREN) {
                consume();
                List<Expr> args = new ArrayList<>();
                if (lookahead.type() != TokenType.RPAREN) {
                    parsePostfixChainParen(args);
                }
                expect(TokenType.RPAREN);
                Expr callee = values.pop();
                values.push(new Expr.Call(callee, args));
            } else {
                break;
            }
        }
    }

    private void parsePostfixChainParen(List<Expr> args) {
        while (true) {
            Expr arg = parseExpressionUntil(EnumSet.of(TokenType.COMMA, TokenType.RPAREN));
            args.add(arg);
            if (lookahead.type() == TokenType.COMMA) { consume(); } else break;
        }
    }

    private Token expect(TokenType type) {
        if (lookahead.type() != type) throw new ParseException("Expected " + type + " but found " + lookahead);
        Token t = lookahead;
        lookahead = lexer.next();
        return t;
    }
    private void consume() { lookahead = lexer.next(); }

    private static double parseDouble(String s, int pos) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) {
            throw new ParseException("Invalid number '" + s + "' at position " + pos);
        }
    }
}