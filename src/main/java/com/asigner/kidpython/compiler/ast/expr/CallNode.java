// Copyright 2016 Andreas Signer. All rights reserved.

package com.asigner.kidpython.compiler.ast.expr;

import com.asigner.kidpython.compiler.Position;
import com.asigner.kidpython.compiler.runtime.Environment;
import com.asigner.kidpython.compiler.runtime.Value;

import java.util.List;

public class CallNode extends ExprNode {

    private final ExprNode expr;
    private final List<ExprNode> params;

    public CallNode(Position pos, ExprNode expr, List<ExprNode> params) {
        super(pos);
        this.expr = expr;
        this.params = params;
    }

    @Override
    public Value eval(Environment env) {
        // TODO(asigner): Implement me!
        return null;
    }

    public ExprNode getExpr() {
        return expr;
    }

    public List<ExprNode> getParams() {
        return params;
    }

    @Override
    void accept(ExprNodeVisitor visitor) {
        visitor.visit(this);

    }
}
