// Copyright 2016 Andreas Signer. All rights reserved.

package com.asigner.kidpython.ide.console;

import java.io.IOException;
import java.io.OutputStream;

public class ConsoleOutputStream extends OutputStream {
    private final ConsoleComposite console;

    public ConsoleOutputStream(ConsoleComposite console) {
        this.console = console;
    }

    @Override
    public void write(int b) throws IOException {
        console.write((char)b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        console.write(new String(b, off, len, "UTF-8"));
    }
}