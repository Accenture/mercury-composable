package com.accenture.minigraph.math;

final class Lexer {
    private final String src;
    private int idx;

    Lexer(String src) { this.src = src; }

    Token next() {
        skipWhitespace();
        if (eof()) return new Token(TokenType.EOF, "", idx);
        char c = src.charAt(idx);
        int start = idx;
        // Numbers
        if (Character.isDigit(c) || (c == '.' && peekIsDigit())) {
            return numberToken();
        }
        // Identifiers / keywords
        if (Character.isLetter(c) || c == '_') {
            return identiferToken(start);
        }
        // operator
        return operatorToken(c, start);
    }

    private Token identiferToken(int start) {
        idx++;
        while (!eof()) {
            char d = src.charAt(idx);
            if (Character.isLetterOrDigit(d) || d == '_') idx++;
            else break;
        }
        String id = src.substring(start, idx);
        if (id.equals("true"))  return new Token(TokenType.TRUE,  id, start);
        if (id.equals("false")) return new Token(TokenType.FALSE, id, start);
        return new Token(TokenType.IDENTIFIER, id, start);
    }

    private Token operatorToken(char c, int start) {
        // Operators / punctuation (multi-char first)
        switch (c) {
            case '(': idx++; return new Token(TokenType.LPAREN, "(", start);
            case ')': idx++; return new Token(TokenType.RPAREN, ")", start);
            case ',': idx++; return new Token(TokenType.COMMA,  ",", start);
            case '.': idx++; return new Token(TokenType.DOT,    ".", start);
            case '+': idx++; return new Token(TokenType.PLUS,   "+", start);
            case '-': idx++; return new Token(TokenType.MINUS,  "-", start);
            case '*':
                if (peek('*')) { idx += 2; return new Token(TokenType.DOUBLESTAR, "**", start); }
                idx++; return new Token(TokenType.STAR,   "*", start);
            case '/': idx++; return new Token(TokenType.SLASH,  "/", start);
            case '%': idx++; return new Token(TokenType.PERCENT,"%", start);
            case '?': idx++; return new Token(TokenType.QUESTION,"?", start);
            case ':': idx++; return new Token(TokenType.COLON,  ":", start);
            case '!':
                if (peek('=')) { idx += 2; return new Token(TokenType.BANGEQ, "!=", start); }
                idx++; return new Token(TokenType.BANG, "!", start);
            case '&':
                if (peek('&')) { idx += 2; return new Token(TokenType.AMPAMP, "&&", start); }
                break;
            case '|':
                if (peek('|')) { idx += 2; return new Token(TokenType.BARBAR, "||", start); }
                break;
            case '=':
                if (peek('=')) { idx += 2; return new Token(TokenType.EQEQ, "==", start); }
                break;
            case '<':
                if (peek('=')) { idx += 2; return new Token(TokenType.LTE, "<=", start); }
                idx++; return new Token(TokenType.LT, "<", start);
            case '>':
                if (peek('=')) { idx += 2; return new Token(TokenType.GTE, ">=", start); }
                idx++; return new Token(TokenType.GT, ">", start);
            default:
        }
        throw error("Unexpected character: '" + c + "'", start);
    }

    private Token numberToken() {
        int start = idx;
        while (!eof() && Character.isDigit(src.charAt(idx))) idx++;
        if (!eof() && src.charAt(idx) == '.') {
            idx++;
            if (eof() || !Character.isDigit(src.charAt(idx)))
                throw error("Malformed number literal", start);
            while (!eof() && Character.isDigit(src.charAt(idx))) idx++;
        }
        if (!eof() && (src.charAt(idx) == 'e' || src.charAt(idx) == 'E')) {
            skipExponent();
        }
        String lex = src.substring(start, idx);
        return new Token(TokenType.NUMBER, lex, start);
    }

    private void skipExponent() {
        int expPos = idx++;
        if (!eof() && (src.charAt(idx) == '+' || src.charAt(idx) == '-')) idx++;
        if (eof() || !Character.isDigit(src.charAt(idx)))
            throw error("Malformed exponent in number literal", expPos);
        while (!eof() && Character.isDigit(src.charAt(idx))) idx++;
    }

    private void skipWhitespace() {
        while (!eof()) {
            char c = src.charAt(idx);
            if (Character.isWhitespace(c)) idx++;
            else break;
        }
    }

    private boolean peek(char ch) { return (idx + 1 < src.length()) && src.charAt(idx + 1) == ch; }
    private boolean peekIsDigit() { return (idx + 1 < src.length()) && Character.isDigit(src.charAt(idx + 1)); }
    private boolean eof() { return idx >= src.length(); }

    private static RuntimeException error(String msg, int pos) {
        return new ParseException(msg + " at position " + pos);
    }
}
