package com.accenture.minigraph.math;

enum TokenType {
    // Single-char tokens
    LPAREN, RPAREN, COMMA, DOT,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    QUESTION, COLON, BANG,
    LT, GT,

    // Multi-char tokens
    DOUBLESTAR,      // **
    LTE, GTE,        // <= >=
    EQEQ, BANGEQ,    // == !=
    AMPAMP, BARBAR,  // && ||

    // Literals / identifiers
    NUMBER,          // 123, 3.14, 1e-5
    STRING,          // 'abc' or "abc"
    IDENTIFIER,      // foo, Math, sin
    TRUE, FALSE,     // boolean literals

    EOF
}