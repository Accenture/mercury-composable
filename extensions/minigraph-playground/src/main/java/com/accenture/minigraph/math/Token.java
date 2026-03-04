package com.accenture.minigraph.math;

record Token(TokenType type, String lexeme, int position) {
    @Override public String toString() {
        return type + "('" + lexeme + "')@" + position;
    }
}