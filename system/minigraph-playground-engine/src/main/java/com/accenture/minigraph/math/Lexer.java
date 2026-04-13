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

        // Strings: single or double-quoted
        if (c == '\'' || c == '"') {
            return stringToken();
        }

        // Numbers: digit or leading '.' followed by digit
        if (Character.isDigit(c) || (c == '.' && peekIsDigit())) {
            return numberToken();
        }

        // Identifiers / keywords: [A-Za-z_][A-Za-z0-9_]*
        if (Character.isLetter(c) || c == '_') {
            return identifierToken(start);
        }

        // Operators / punctuation (handle multi-char first)
        return operatorToken(c, start);
    }

    private Token stringToken() {
        int start = idx;
        char quote = src.charAt(idx++);
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char ch = src.charAt(idx++);
            if (ch == quote) {
                return new Token(TokenType.STRING, sb.toString(), start);
            }
            if (ch == '\\') {
                if (eof()) throw error("Unterminated string literal", start);
                char esc = src.charAt(idx++);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\\'-> sb.append('\\');
                    case '\''-> sb.append('\'');
                    case '"' -> sb.append('"');
                    default  -> sb.append(esc);
                }
            } else {
                sb.append(ch);
            }
        }
        throw error("Unterminated string literal", start);
    }

    private Token identifierToken(int start) {
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

        // integer part
        while (!eof() && Character.isDigit(src.charAt(idx))) idx++;

        // fractional part
        if (!eof() && src.charAt(idx) == '.') {
            handleFractional(start);
        }

        // exponent part
        if (!eof() && (src.charAt(idx) == 'e' || src.charAt(idx) == 'E')) {
            handleExponent();
        }

        String lex = src.substring(start, idx);
        return new Token(TokenType.NUMBER, lex, start);
    }

    private void handleFractional(int start) {
        idx++;
        if (eof() || !Character.isDigit(src.charAt(idx)))
            throw error("Malformed number literal", start);
        while (!eof() && Character.isDigit(src.charAt(idx))) idx++;
    }

    private void handleExponent() {
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