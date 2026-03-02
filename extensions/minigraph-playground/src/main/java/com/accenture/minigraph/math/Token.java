package com.accenture.minigraph.math;

final class Token {
    final TokenType type;
    final String lexeme;
    final int position;

    Token(TokenType type, String lexeme, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = position;
    }

    @Override public String toString() {
        return type + "('" + lexeme + "')@" + position;
    }
}
