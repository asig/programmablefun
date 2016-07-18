package com.asigner.kidpython.ide;

import com.asigner.kidpython.compiler.CodeGenerator;
import com.asigner.kidpython.compiler.Error;
import com.asigner.kidpython.compiler.Parser;
import com.asigner.kidpython.compiler.ast.Stmt;
import com.asigner.kidpython.compiler.runtime.Instruction;
import com.asigner.kidpython.compiler.runtime.NativeFunctions;
import com.asigner.kidpython.compiler.runtime.VirtualMachine;
import com.asigner.kidpython.ide.console.ConsoleComposite;
import com.asigner.kidpython.ide.turtle.TurtleCanvas;
import com.asigner.kidpython.ide.util.AnsiEscapeCodes;
import com.asigner.kidpython.ide.util.SWTResources;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import java.io.PrintWriter;
import java.util.List;

public class App {

    protected Shell shell;

    private TurtleCanvas turtleCanvas;
    private SourceCodeComposite sourceCodeComposite;
    private ConsoleComposite consoleComposite;

    private PrintWriter consoleOut;
    private VirtualMachine virtualMachine;
    private NativeFunctions nativeFunctions;

    // VirtualMachine toolbar
    BaseAction vmStartAction;
    BaseAction vmPauseAction;
    BaseAction vmResumeAction;
    BaseAction vmStopAction;
    BaseAction vmStepIntoAction;
    BaseAction vmStepOverAction;

    private CoolBarManager coolBarManager;

    /**
     * Launch the application.
     * @param args
     */
    public static void main(String[] args) {
        try {
            App window = new App();
            window.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public App() {
    }

    /**
     * Open the window.
     */
    public void open() {
        Display display = Display.getDefault();
        createContents();
        shell.open();
        shell.layout();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        Display display = Display.getDefault();
        shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);
        shell.setLayout(new GridLayout(1, false));
        Image icon = new Image(display, App.class.getResourceAsStream("icons/icon.png"));
        shell.setImage(icon);

        Menu menuBar = new Menu(shell, SWT.BAR);
        MenuItem cascadeFileMenu = new MenuItem(menuBar, SWT.CASCADE);
        cascadeFileMenu.setText("&File");

        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        cascadeFileMenu.setMenu(fileMenu);

        MenuItem exitItem = new MenuItem(fileMenu, SWT.PUSH);
        exitItem.setText("&Exit");
        shell.setMenuBar(menuBar);

        exitItem.addListener(SWT.Selection, event -> {
            shell.getDisplay().dispose();
            System.exit(0);
        });

        createActions();
        createToolbar();

        SashForm sashForm = new SashForm(shell, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // Upper part of toplevel sash
        SashForm sashForm2 = new SashForm(sashForm, SWT.HORIZONTAL);
        sashForm2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        sourceCodeComposite = new SourceCodeComposite(sashForm2, SWT.NONE);
        turtleCanvas = new TurtleCanvas(sashForm2, SWT.NONE);

        // Lower part of toplevel sash
        consoleComposite = new ConsoleComposite(sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        sashForm.setWeights(new int[]{3, 1});

        shell.setText("Simple menu");
        shell.setSize(578, 390);
        shell.layout();
        shell.setMaximized(true);
        shell.open();

        consoleOut = new PrintWriter(consoleComposite.getOutputStream(), true);
        nativeFunctions = new NativeFunctions(turtleCanvas, consoleComposite);
        virtualMachine = new VirtualMachine(consoleComposite.getOutputStream(), consoleComposite.getInputStream(), nativeFunctions);

        updateVmButtons();

        virtualMachine.addListener(new VirtualMachine.EventListener() {
            @Override
            public void vmStateChanged() {
                showVmStateMessage();
                updateVmButtons();
            }

            @Override
            public void newStatementReached(Stmt stmt) {

            }

            @Override
            public void programSet() {
                updateVmButtons();
            }

            @Override
            public void reset() {
                updateVmButtons();
            }
        });
    }

    private void updateVmButtons() {
        shell.getDisplay().syncExec(() -> {
            VirtualMachine.State state = virtualMachine.getState();
            switch (state) {
                case RUNNING:
                    vmStartAction.setEnabled(false);
                    vmPauseAction.setEnabled(true);
                    vmResumeAction.setEnabled(false);
                    vmStopAction.setEnabled(true);
                    vmStepIntoAction.setEnabled(false);
                    vmStepOverAction.setEnabled(false);
                    break;
                case STOPPED:
                    vmStartAction.setEnabled(true);
                    vmPauseAction.setEnabled(false);
                    vmResumeAction.setEnabled(false);
                    vmStopAction.setEnabled(false);
                    vmStepIntoAction.setEnabled(true);
                    vmStepOverAction.setEnabled(true);
                    break;
                case PAUSED:
                    vmStartAction.setEnabled(false);
                    vmPauseAction.setEnabled(false);
                    vmResumeAction.setEnabled(true);
                    vmStopAction.setEnabled(true);
                    vmStepIntoAction.setEnabled(true);
                    vmStepOverAction.setEnabled(true);
                    break;
            }
        });
    }

    private void showVmStateMessage() {
        VirtualMachine.State state = virtualMachine.getState();
        switch (state) {
            case RUNNING:
                status("Programmausführung gestartet.");
                break;
            case STOPPED:
                status("Programmausführung gestoppt.");
                break;
            case PAUSED:
                status("Programmausführung pausiert.");
                break;
        }
    }

    private void status(String s) {
        consoleOut.println(AnsiEscapeCodes.FG_BLUE + s + AnsiEscapeCodes.FG_BLACK);
    }

    private void createActions() {
        vmStartAction = new BaseAction("Run", SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/nav_go@2x.png"), () -> {
            runCode(sourceCodeComposite.getText());
        });
        vmPauseAction = new BaseAction("Pause", SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/suspend_co@2x.png"), () -> virtualMachine.pause() );
        vmResumeAction = new BaseAction("Resume", SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/resume_co@2x.png"), () -> virtualMachine.start() );
        vmStopAction = new BaseAction("Stop", SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/stop@2x.png"), () -> virtualMachine.stop());
        vmStepIntoAction = new BaseAction("Step Into", SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/stepinto_co@2x.png"), this::stepInto);
        vmStepOverAction = new BaseAction("Step Over", SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/stepover_co@2x.png"), this::stepOver);
    }

    private void createToolbar() {
        coolBarManager = new CoolBarManager(SWT.FLAT);
        final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.NO_FOCUS);
        coolBarManager.add(toolBarManager);
        toolBarManager.add(vmStartAction);

        toolBarManager.add(vmPauseAction);
        toolBarManager.add(vmResumeAction);
        toolBarManager.add(vmStopAction);
        toolBarManager.add(vmStepIntoAction);
        toolBarManager.add(vmStepOverAction);

//        final ToolBarManager toolBarManager_1 = new ToolBarManager(SWT.FLAT | SWT.NO_FOCUS);
//        coolBarManager.add(toolBarManager_1);
//        toolBarManager_1.add(new TestAction("TEST2", ImageDescriptor.createFromImage(SWTResources.getImage("/com/asigner/kidpython/ide/toolbar/nav_go@2x.png"))));

        coolBarManager.createControl(shell);
    }

    private void runCode(String source) {
        sourceCodeComposite.clearErrors();
        Parser p = new Parser(source);
        Stmt stmt = p.parse();
        if (stmt == null) {
            consoleOut.print(AnsiEscapeCodes.BOLD);
            consoleOut.print(AnsiEscapeCodes.FG_YELLOW);
            consoleOut.print(AnsiEscapeCodes.BG_RED);
            consoleOut.print("Fehler beim Compilieren:");
            consoleOut.println(AnsiEscapeCodes.RESET);
            for (Error e : p.getErrors()) {
                consoleOut.println(e);
            }
            sourceCodeComposite.setErrors(p.getErrors());
            return;
        }

        CodeGenerator codeGen = new CodeGenerator(stmt);
        List<Instruction> program = codeGen.generate();
        int i = 0;
        for (Instruction instr : program) {
            System.out.println(String.format("%04d: %s", i++, instr));
        }
        System.out.flush();

        virtualMachine.setProgram(program);
        virtualMachine.start();
    }

    private void stepInto() {
        // Implement me
    }

    private void stepOver() {
        // Implement me
    }
}
