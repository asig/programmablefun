package com.asigner.kidpython.compiler.runtime;

import com.asigner.kidpython.compiler.ast.Stmt;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Stack;

public class Environment {

    private static class Frame {
        private final Frame parent;
        private final Map<String, Value> vars;

        public Frame(Frame parent) {
            this.parent = parent;
            this.vars = Maps.newHashMap();
        }

        public Frame getParent() {
            return parent;
        }

        public boolean isDefined(String name) {
            return vars.containsKey(name);
        }

        public Value getVar(String name) {
            return vars.get(name);
        }

        public void setVar(String name, Value value) {
            vars.put(name, value);
        }
    }

    private Frame funcFrame;
    private Frame globalFrame;

    private Stack<Stmt> stack = new Stack<>();
    private Value resultValue;

    public Environment() {
        this.funcFrame = null;
        this.globalFrame = new Frame(null);
    }

    public void enterFunction() {
        Frame newFrame = new Frame(funcFrame);
        funcFrame = newFrame;
    }

    public void leaveFunction(Value resultValue) {
        funcFrame = funcFrame.getParent();
        this.resultValue = resultValue;
    }

    public Value getVar(String name) {
        Value v;
        if (funcFrame != null) {
            v = funcFrame.getVar(name);
            if (v != null) {
                return v;
            }
        }
        v = globalFrame.getVar(name);
        return v;
    }

    public void setVar(String name, Value value) {
        Frame targetFrame ;
        if (globalFrame.isDefined(name)) {
            targetFrame = globalFrame;
        } else {
            targetFrame = funcFrame != null ? funcFrame : globalFrame;
        }
        targetFrame.setVar(name, value);
    }

}