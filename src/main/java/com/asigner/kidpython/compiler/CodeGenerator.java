// Copyright 2016 Andreas Signer. All rights reserved.

package com.asigner.kidpython.compiler;

import com.asigner.kidpython.compiler.ast.AssignmentStmt;
import com.asigner.kidpython.compiler.ast.EmptyStmt;
import com.asigner.kidpython.compiler.ast.EvalStmt;
import com.asigner.kidpython.compiler.ast.ForEachStmt;
import com.asigner.kidpython.compiler.ast.ForStmt;
import com.asigner.kidpython.compiler.ast.IfStmt;
import com.asigner.kidpython.compiler.ast.NodeVisitor;
import com.asigner.kidpython.compiler.ast.RepeatStmt;
import com.asigner.kidpython.compiler.ast.ReturnStmt;
import com.asigner.kidpython.compiler.ast.Stmt;
import com.asigner.kidpython.compiler.ast.WhileStmt;
import com.asigner.kidpython.compiler.ast.expr.BinOpNode;
import com.asigner.kidpython.compiler.ast.expr.CallNode;
import com.asigner.kidpython.compiler.ast.expr.ConstNode;
import com.asigner.kidpython.compiler.ast.expr.ExprNode;
import com.asigner.kidpython.compiler.ast.expr.MakeFuncNode;
import com.asigner.kidpython.compiler.ast.expr.MakeIterNode;
import com.asigner.kidpython.compiler.ast.expr.MakeListNode;
import com.asigner.kidpython.compiler.ast.expr.MakeMapNode;
import com.asigner.kidpython.compiler.ast.expr.MapAccessNode;
import com.asigner.kidpython.compiler.ast.expr.UnOpNode;
import com.asigner.kidpython.compiler.ast.expr.VarNode;
import com.asigner.kidpython.compiler.runtime.BooleanValue;
import com.asigner.kidpython.compiler.runtime.FuncValue;
import com.asigner.kidpython.compiler.runtime.Instruction;
import com.asigner.kidpython.compiler.runtime.VarRefValue;
import com.asigner.kidpython.util.Pair;
import com.google.common.collect.Lists;

import java.util.List;

import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.ADD;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.ASSIGN;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.B;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.BF;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.CALL;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.DIV;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.EQ;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.GE;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.GT;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.LE;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.LT;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.MKFIELDREF;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.MKITER;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.MKLIST;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.MKMAP;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.MUL;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.NE;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.NEG;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.NOT;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.POP;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.PUSH;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.RET;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.STOP;
import static com.asigner.kidpython.compiler.runtime.Instruction.OpCode.SUB;

public class CodeGenerator implements NodeVisitor {

    private final Stmt stmt;

    private List<Instruction> instrs;

    public CodeGenerator(Stmt stmt) {
        this.stmt = stmt;
        this.instrs = Lists.newLinkedList();
    }

    public List<Instruction> generate() {
        generateStmtBlock(stmt);
        emit(new Instruction(stmt, STOP));
        return instrs;
    }

    private void generateStmtBlock(Stmt stmt) {
        while (stmt != null) {
            stmt.accept(this);
            stmt = stmt.getNext();
        }
    }

    @Override
    public void visit(AssignmentStmt stmt) {
        stmt.getVar().accept(this);
        stmt.getExpr().accept(this);
        emit(new Instruction(stmt, ASSIGN));
    }

    @Override
    public void visit(EmptyStmt stmt) {
    }

    @Override
    public void visit(EvalStmt stmt) {
        stmt.getExpr().accept(this);
        emit(new Instruction(stmt, POP));
    }

    @Override
    public void visit(ForEachStmt stmt) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void visit(ForStmt stmt) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void visit(IfStmt stmt) {
        stmt.getCond().accept(this);
        int branchIfFalsePc = emit(new Instruction(stmt, BF, 0)); // patch later
        generateStmtBlock(stmt.getTrueBranch());
        int branchPc = emit(new Instruction(stmt, B, 0)); // patch later
        int falseStart = instrs.size();
        generateStmtBlock(stmt.getFalseBranch());
        int pc = instrs.size();
        patch(branchIfFalsePc, new Instruction(stmt, BF, falseStart));
        patch(branchPc, new Instruction(stmt, B, pc));
    }

    @Override
    public void visit(RepeatStmt stmt) {
        int startPc = instrs.size();
        generateStmtBlock(stmt.getBody());
        stmt.getCond().accept(this);
        emit(new Instruction(stmt, BF, startPc));
    }

    @Override
    public void visit(ReturnStmt stmt) {
        stmt.getExpr().accept(this);
        emit(new Instruction(stmt, RET));
    }

    @Override
    public void visit(WhileStmt stmt) {
        int startPc = instrs.size();
        stmt.getCond().accept(this);
        int jumpPc = instrs.size();
        emit(new Instruction(stmt, BF, 0)); // will be patched afterwards
        generateStmtBlock(stmt.getBody());
        emit(new Instruction(stmt, B, startPc)); // will be patched afterwards
        patch(jumpPc, new Instruction(stmt, BF, instrs.size()));
    }

    @Override
    public void visit(BinOpNode node) {
        if (node.getOp() == BinOpNode.Op.AND) {
            // IF a THEN b ELSE false
            node.getLeft().accept(this);
            int bf = emit(new Instruction(node, BF, 0));
            node.getRight().accept(this);
            int endOfTrue = emit(new Instruction(node, B, 0));
            int falseStart = emit(new Instruction(node, PUSH, new BooleanValue(false)));
            int end = instrs.size();

            patch(bf, new Instruction(node, BF, falseStart));
            patch(endOfTrue, new Instruction(node, B, end));
        } else if (node.getOp() == BinOpNode.Op.OR) {
            // IF a THEN true ELSE b
            node.getLeft().accept(this);
            int bf = emit(new Instruction(node, BF, 0));
            emit(new Instruction(node, PUSH, new BooleanValue(true)));
            int endOfTrue = emit(new Instruction(node, B, 0));
            int falseStart = instrs.size();
            node.getRight().accept(this);
            int end = instrs.size();

            patch(bf, new Instruction(node, BF, falseStart));
            patch(endOfTrue, new Instruction(node, B, end));
        } else {
            node.getLeft().accept(this);
            node.getRight().accept(this);
            Instruction.OpCode opCode;
            switch (node.getOp()) {
                case EQ:
                    opCode = EQ;
                    break;
                case NE:
                    opCode = NE;
                    break;
                case LE:
                    opCode = LE;
                    break;
                case LT:
                    opCode = LT;
                    break;
                case GE:
                    opCode = GE;
                    break;
                case GT:
                    opCode = GT;
                    break;

                case ADD:
                    opCode = ADD;
                    break;
                case SUB:
                    opCode = SUB;
                    break;
                case MUL:
                    opCode = MUL;
                    break;
                case DIV:
                    opCode = DIV;
                    break;

                default:
                    throw new IllegalStateException("Unknown BinOpNode op " + node.getOp());
            }
            emit(new Instruction(node, opCode));
        }
    }


    @Override
    public void visit(UnOpNode node) {
        node.getExpr().accept(this);
        Instruction.OpCode opCode;
        switch(node.getOp()) {
            case NEG: opCode = NEG; break;
            case NOT: opCode = NOT; break;

            default: throw new IllegalStateException("Unknown UnOpNode op " + node.getOp());
        }
        emit(new Instruction(node, opCode));
    }

    @Override
    public void visit(CallNode node) {
        node.getExpr().accept(this);
        List<ExprNode> params = node.getParams();
        for (ExprNode p : params) {
            p.accept(this);
        }
        emit(new Instruction(node, CALL, params.size()));
    }

    @Override
    public void visit(ConstNode node) {
        emit(new Instruction(node, PUSH, node.getVal()));
    }

    @Override
    public void visit(MakeFuncNode node) {
        // jump over function for now.
        int jumpOverFunc = emit(new Instruction(node, B, 0));
        int startPc = instrs.size();
        generateStmtBlock(node.getBody());
        patch(jumpOverFunc, new Instruction(node, B, instrs.size()));
        emit(new Instruction(node, PUSH, new FuncValue(startPc, node.getParams())));
    }

    @Override
    public void visit(MakeIterNode node) {
        node.getNode().accept(this);
        emit(new Instruction(node, MKITER));
    }

    @Override
    public void visit(MakeListNode node) {
        for (ExprNode elem : node.getElements()) {
            elem.accept(this);
        }
        emit(new Instruction(node, MKLIST, node.getElements().size()));
    }

    @Override
    public void visit(MakeMapNode node) {
        for (Pair<ExprNode, ExprNode> elem : node.getElements()) {
            elem.getFirst().accept(this);
            elem.getSecond().accept(this);
        }
        emit(new Instruction(node, MKMAP, node.getElements().size()));
    }

    @Override
    public void visit(MapAccessNode node) {
        node.getMapExpr().accept(this);
        node.getKeyExpr().accept(this);
        emit(new Instruction(node, MKFIELDREF));
    }

    @Override
    public void visit(VarNode node) {
        emit(new Instruction(node, PUSH, new VarRefValue(node.getVar())));
    }

    private int emit(Instruction instr) {
        instrs.add(instr);
        return instrs.size() - 1;
    }

    private void patch(int pos, Instruction instr) {
        instrs.set(pos, instr);
    }
}