package com.accenture.minigraph.math;

import java.util.ArrayList;
import java.util.List;

final class Parser {
    private final Lexer lexer;
    private Token lookahead;
    private final boolean strictJsExponentiationRule;

    // precedence
    private static final int BP_EXP     = 70;
    private static final int BP_MUL     = 50;
    private static final int BP_ADD     = 40;
    private static final int BP_REL     = 30; // < <= > >=
    private static final int BP_EQ      = 20; // == !=
    private static final int BP_AND     = 12; // &&
    private static final int BP_OR      = 10; // ||
    private static final int BP_TERNARY = 5;  // ?:

    Parser(String src) { this(src, true); }

    Parser(String src, boolean strictJsExponentiationRule) {
        this.lexer = new Lexer(src);
        this.lookahead = lexer.next();
        this.strictJsExponentiationRule = strictJsExponentiationRule;
    }

    Expr parse() {
        Expr expr = parseExpression(0);
        expect(TokenType.EOF);
        return expr;
    }

    // The Pratt parser always consumes a token before any recursive call.
    private Expr parseExpression(int minBp) {
        Expr left = parseUnaryOrPrimary();
        while (true) {
            if (lookahead.type == TokenType.QUESTION && BP_TERNARY >= minBp) {
                consume(); // '?'
                Expr consequent = parseExpression(0);
                expect(TokenType.COLON);
                Expr alternate = parseExpression(BP_TERNARY); // right-assoc
                left = new Expr.Conditional(left, consequent, alternate);
            } else {
                TokenType t = lookahead.type;
                OpInfo op = OpInfo.of(t);
                if (op == null || op.leftBp < minBp) break;
                if (strictJsExponentiationRule && t == TokenType.DOUBLESTAR && left instanceof Expr.Unary) {
                    throw new ParseException("Unary expression cannot be left operand of '**'. Use parentheses, e.g., -(2**2).");
                }
                consume(); // operator
                int rightBp = op.rightBp;
                Expr right = parseExpression(rightBp);
                left = new Expr.Binary(op.symbol, left, right);
            }
        }
        return left;
    }

    private Expr parseUnaryOrPrimary() {
        if (lookahead.type == TokenType.PLUS)  { consume(); return new Expr.Unary("+", parseUnaryOrPrimary()); }
        if (lookahead.type == TokenType.MINUS) { consume(); return new Expr.Unary("-", parseUnaryOrPrimary()); }
        if (lookahead.type == TokenType.BANG)  { consume(); return new Expr.Unary("!", parseUnaryOrPrimary()); }
        return parsePostfixChain(parsePrimary());
    }

    private Expr parsePrimary() {
        Token t = lookahead;
        switch (t.type) {
            case NUMBER:
                consume();
                return new Expr.NumberLiteral(parseDouble(t.lexeme, t.position));
            case TRUE:
                consume();
                return new Expr.BooleanLiteral(true);
            case FALSE:
                consume();
                return new Expr.BooleanLiteral(false);
            case IDENTIFIER:
                consume();
                return new Expr.Variable(t.lexeme);
            case LPAREN: {
                consume();
                Expr inner = parseExpression(0);
                expect(TokenType.RPAREN);
                return inner;
            }
            default:
                throw new ParseException("Expected a number, boolean, identifier, or '(' at position " + t.position);
        }
    }

    private Expr parsePostfixChain(Expr base) {
        Expr expr = base;
        while (true) {
            if (lookahead.type == TokenType.LPAREN) {
                consume();
                List<Expr> args = new ArrayList<>();
                if (lookahead.type != TokenType.RPAREN) {
                    do {
                        args.add(parseExpression(0));
                    } while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN);
                expr = new Expr.Call(expr, args);
            } else if (lookahead.type == TokenType.DOT) {
                consume();
                Token id = expect(TokenType.IDENTIFIER);
                expr = new Expr.MemberAccess(expr, id.lexeme);
            } else {
                break;
            }
        }
        return expr;
    }

    private Token expect(TokenType type) {
        if (lookahead.type != type) {
            throw new ParseException("Expected " + type + " but found " + lookahead);
        }
        Token t = lookahead;
        lookahead = lexer.next();
        return t;
    }

    private boolean match(TokenType type) {
        if (lookahead.type == type) {
            lookahead = lexer.next();
            return true;
        }
        return false;
    }

    private void consume() { lookahead = lexer.next(); }

    private static double parseDouble(String s, int pos) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) {
            throw new ParseException("Invalid number '" + s + "' at position " + pos);
        }
    }

    private static final class OpInfo {
        final String symbol;
        final int leftBp;
        final int rightBp;

        private OpInfo(String symbol, int leftBp, int rightBp) {
            this.symbol = symbol; this.leftBp = leftBp; this.rightBp = rightBp;
        }

        static OpInfo of(TokenType t) {
            return switch (t) {
                case DOUBLESTAR -> new OpInfo("**", BP_EXP, BP_EXP - 1); // right-assoc
                case STAR -> new OpInfo("*", BP_MUL, BP_MUL + 1);
                case SLASH -> new OpInfo("/", BP_MUL, BP_MUL + 1);
                case PERCENT -> new OpInfo("%", BP_MUL, BP_MUL + 1);
                case PLUS -> new OpInfo("+", BP_ADD, BP_ADD + 1);
                case MINUS -> new OpInfo("-", BP_ADD, BP_ADD + 1);
                case LT -> new OpInfo("<", BP_REL, BP_REL + 1);
                case LTE -> new OpInfo("<=", BP_REL, BP_REL + 1);
                case GT -> new OpInfo(">", BP_REL, BP_REL + 1);
                case GTE -> new OpInfo(">=", BP_REL, BP_REL + 1);
                case EQEQ -> new OpInfo("==", BP_EQ, BP_EQ + 1);
                case BANGEQ -> new OpInfo("!=", BP_EQ, BP_EQ + 1);
                case AMPAMP -> new OpInfo("&&", BP_AND, BP_AND + 1);
                case BARBAR -> new OpInfo("||", BP_OR, BP_OR + 1);
                default -> null;
            };
        }
    }
}
