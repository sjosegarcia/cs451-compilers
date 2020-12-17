// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

/**
 * An enum of token kinds. Each entry in this enum represents the kind of a token along with its
 * image (string representation).
 */
enum TokenKind {
    // End of file.
    EOF("<End Of File>"),

    // Reserved words.
    ABSTRACT("abstract"), BOOLEAN("boolean"), CHAR("char"), CLASS("class"), ELSE("else"),
    EXTENDS("extends"), IF("if"), IMPORT("import"), INSTANCEOF("instanceof"), INT("int"), LONG("long"), 
    DOUBLE("double"), NEW("new"), PACKAGE("package"), PRIVATE("private"), PROTECTED("protected"),
    PUBLIC("public"), RETURN("return"), STATIC("static"), SUPER("super"), THIS("this"),
    VOID("void"), WHILE("while"), BREAK("break"), CASE("case"), CATCH("catch"), CONTINUE("continue"),
    DEFAULT("default"), DO("do"), FINALLY("finally"), FOR("for"), IMPLEMENTS("implements"),
    INTERFACE("interface"), SWITCH("switch"), THROW("throw"), THROWS("throws"), TRY("try"),

    // Operators.
    ASSIGN("="), DEC("--"), EQUAL("=="), GT(">"), INC("++"), LAND("&&"), LOR("||"), LE("<="), LNOT("!"),
    MINUS("-"), PLUS("+"), PLUS_ASSIGN("+="), STAR("*"), DIV("/"), REM("%"), AND("&"), OR("|"),
    XOR("^"), NOT("~"), ALSHIFT("<<"), ARSHIFT(">>"), LRSHIFT(">>>"), QUESTION("?"), COLON(":"),
    NOT_EQUAL("!="), DIV_ASSIGN("/="), MINUS_ASSIGN("-="), STAR_ASSIGN("*="), REM_ASSIGN("%="),
    ARSHIFT_ASSIGN(">>="), LRSHIFT_ASSIGN(">>>="), GE(">="), LT("<"), ALSHIFT_ASSIGN("<<="), 
    XOR_ASSIGN("^="), OR_ASSIGN("|="), AND_ASSIGN("&="),

    // Separators.
    COMMA(","), DOT("."), LBRACK("["), LCURLY("{"), LPAREN("("), RBRACK("]"), RCURLY("}"),
    RPAREN(")"), SEMI(";"),

    // Identifiers.
    IDENTIFIER("<IDENTIFIER>"),

    // Literals.
    NULL("null"), FALSE("false"), TRUE("true"),
    INT_LITERAL("<INT_LITERAL>"), CHAR_LITERAL("<CHAR_LITERAL>"),
    LONG_LITERAL("<LONG_LITERAL>"), DOUBLE_LITERAL("<DOUBLE_LITERAL>"),
    STRING_LITERAL("<STRING_LITERAL>");

    // The token kind's string representation.
    private String image;

    /**
     * Constructs an instance of TokenKind given its string representation.
     *
     * @param image string representation of the token kind.
     */
    private TokenKind(String image) {
        this.image = image;
    }

    /**
     * Returns the token kind's image.
     *
     * @return the token kind's image.
     */
    public String image() {
        return image;
    }

    /**
     * Returns the string representation (image) of the token kind.
     *
     * @return the string representation (image) of the token kind.
     */
    public String toString() {
        return image;
    }
}

/**
 * A representation of tokens returned by the Scanner method getNextToken(). A token has a kind
 * identifying what kind of token it is, an image for providing any semantic text, and the line in
 * which it occurred in the source file.
 */
public class TokenInfo {
    // Token kind.
    private TokenKind kind;

    // Semantic text (if any). For example, the identifier name when the token kind is IDENTIFIER
    // . For tokens without a semantic text, it is simply its string representation. For example,
    // "+=" when the token kind is PLUS_ASSIGN.
    private String image;

    // Line in which the token occurs in the source file.
    private int line;

    /**
     * Constructs a TokenInfo object given its kind, the semantic text forming the token, and its
     * line number.
     *
     * @param kind  the token's kind.
     * @param image the semantic text forming the token.
     * @param line  the line in which the token occurs in the source file.
     */
    public TokenInfo(TokenKind kind, String image, int line) {
        this.kind = kind;
        this.image = image;
        this.line = line;
    }

    /**
     * Constructs a TokenInfo object given its kind and its line number. Its image is simply the
     * token kind's string representation.
     *
     * @param kind the token's identifying number.
     * @param line the line in which the token occurs in the source file.
     */
    public TokenInfo(TokenKind kind, int line) {
        this(kind, kind.toString(), line);
    }

    /**
     * Returns the token's kind.
     *
     * @return the token's kind.
     */
    public TokenKind kind() {
        return kind;
    }

    /**
     * Returns the semantic text associated with the token.
     *
     * @return the semantic text associated with the token
     */
    public String image() {
        return image;
    }

    /**
     * Returns the line number associated with the token.
     *
     * @return the line number associated with the token.
     */
    public int line() {
        return line;
    }

    /**
     * Returns the token kind's string representation.
     *
     * @return the token kind's string representation.
     */
    public String tokenRep() {
        return kind.toString();
    }

    /**
     * Returns the string representation (image) of the token.
     *
     * @return the string representation (image) of the token.
     */
    public String toString() {
        return image;
    }
}
