// Copyright 2016 Andreas Signer. All rights reserved.

package com.asigner.kidpython.compiler.ast;

import com.asigner.kidpython.compiler.Position;
import com.asigner.kidpython.compiler.ast.expr.ExprNode;

public class WhileStmt extends Stmt {

    private ExprNode cond;
    private Stmt body;

    public WhileStmt(Position pos, ExprNode cond, Stmt body) {
        super(pos);
        this.cond = cond;
        this.body = body;
    }

    public ExprNode getCond() {
        return cond;
    }

    public Stmt getBody() {
        return body;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

}