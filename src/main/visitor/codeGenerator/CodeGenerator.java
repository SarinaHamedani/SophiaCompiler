package main.visitor.codeGenerator;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.ast.types.NullType;
import main.ast.types.Type;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.ClassType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.FieldSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;
import main.visitor.typeChecker.ExpressionTypeChecker;

import java.io.*;
import java.util.ArrayList;

public class CodeGenerator extends Visitor<String> {
    ExpressionTypeChecker expressionTypeChecker;
    Graph<String> classHierarchy;
    private String outputPath;
    private FileWriter currentFile;
    private ClassDeclaration currentClass;
    private MethodDeclaration currentMethod;
    private int lastTempValue;
    private String stack_size;

    public CodeGenerator(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
        this.prepareOutputFolder();
        this.lastTempValue = 0;
        this.stack_size = "128";
    }

    private void prepareOutputFolder() {
        this.outputPath = "output/";
        String jasminPath = "utilities/jarFiles/jasmin.jar";
        String listClassPath = "utilities/codeGenerationUtilityClasses/List.j";
        String fptrClassPath = "utilities/codeGenerationUtilityClasses/Fptr.j";
        try{
            File directory = new File(this.outputPath);
            File[] files = directory.listFiles();
            if(files != null)
                for (File file : files)
                    file.delete();
            directory.mkdir();
        }
        catch(SecurityException e) { }
        copyFile(jasminPath, this.outputPath + "jasmin.jar");
        copyFile(listClassPath, this.outputPath + "List.j");
        copyFile(fptrClassPath, this.outputPath + "Fptr.j");
    }

    private void copyFile(String toBeCopied, String toBePasted) {
        try {
            File readingFile = new File(toBeCopied);
            File writingFile = new File(toBePasted);
            InputStream readingFileStream = new FileInputStream(readingFile);
            OutputStream writingFileStream = new FileOutputStream(writingFile);
            byte[] buffer = new byte[1024];
            int readLength;
            while ((readLength = readingFileStream.read(buffer)) > 0)
                writingFileStream.write(buffer, 0, readLength);
            readingFileStream.close();
            writingFileStream.close();
        } catch (IOException e) { }
    }

    private void createFile(String name) {
        try {
            String path = this.outputPath + name + ".j";
            File file = new File(path);
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(path);
            this.currentFile = fileWriter;
        } catch (IOException e) {}
    }

    private void addCommand(String command) {
        try {
            command = String.join("\n\t\t", command.split("\n"));
            if(command.startsWith("Label_"))
                this.currentFile.write("\t" + command + "\n");
            else if(command.startsWith("."))
                this.currentFile.write(command + "\n");
            else
                this.currentFile.write("\t\t" + command + "\n");
            this.currentFile.flush();
        } catch (IOException e) {}
    }

    private String makeTypeSignature(Type t) {
        if(t instanceof IntType) {
            //TODO
        } if(t instanceof BoolType) {
            //TODO
        } if(t instanceof StringType)
            return "Ljava/lang/String;";
        if(t instanceof ClassType)
            return ((ClassType) t).getClassName().getName();
        if(t instanceof ListType)
            return "List";
        if(t instanceof FptrType)
            return "Fptr";
        return null;
    }

    private void initializeVar(VarDeclaration v, int slot) {
        //TODO
    }

    private void addDefaultConstructor() {      // done I think
        addCommand(".method public <init>()V");
        addCommand("aload_0");
        if(currentClass.getParentClassName() == null)
            addCommand("invokenonvirtual java/lang/Object/<init>()V");
        else {
            String parentName = currentClass.getParentClassName().getName();
            addCommand("invokespecial "+parentName+"/<init>()V");
        }
        for(int i = 0; i < currentClass.getFields().size(); i++) {
            initializeVar(currentClass.getFields().get(i).getVarDeclaration(), i+1);
        }
        addCommand("return");
        addCommand(".end method");
        addCommand("");
    }

    private void addStaticMainMethod() {        // done I think
        addCommand(".method public static main([Ljava/lang/String;)V");
        addCommand(".limit stack "+this.stack_size);
        addCommand(".limit locals "+this.stack_size);
        addCommand("new Main");
        addCommand("invokespecial Main/<init>()V");
        addCommand("return");
        addCommand(".end method");
        addCommand("");
    }

    private int slotOf(String identifier) {     // we have to handle the temp variables
        ArrayList <VarDeclaration> locals = new ArrayList<>(currentMethod.getArgs());
        locals.addAll(currentMethod.getLocalVars());
//        if(identifier.equals(""))
//            return locals.size() + 1 + this.lastTempValue;
        for(int i = 0; i < locals.size(); i++)
            if(locals.get(i).getVarName().getName().equals(identifier))
                return i+1;
        return 0;
    }

    @Override
    public String visit(Program program) {      // done
        for(ClassDeclaration c : program.getClasses()) {
            this.currentClass = c;
            this.expressionTypeChecker.setCurrentClass(c);
            c.accept(this);
        }
        return null;
    }

    @Override
    public String visit(ClassDeclaration classDeclaration) {    // done
        createFile(classDeclaration.getClassName().getName());
        addCommand(".class public "+classDeclaration.getClassName().getName());
        if(classDeclaration.getParentClassName() == null)
            addCommand(".super java/lang/Object");
        else
            addCommand(".super "+classDeclaration.getParentClassName().getName());
        addCommand("");
        for(FieldDeclaration f : classDeclaration.getFields())
            f.accept(this);
        if(classDeclaration.getConstructor() == null)
            addDefaultConstructor();
        else {
            this.currentMethod = classDeclaration.getConstructor();
            this.expressionTypeChecker.setCurrentMethod(classDeclaration.getConstructor());
            classDeclaration.getConstructor().accept(this);
        }
        for(MethodDeclaration m : classDeclaration.getMethods()) {
            this.currentMethod = m;
            this.expressionTypeChecker.setCurrentMethod(m);
            m.accept(this);
        }
        return null;
    }

    @Override
    public String visit(ConstructorDeclaration constructorDeclaration) {
        //todo add default constructor or static main method if needed
        if(this.currentClass.getClassName().getName().equals("Main"))
            addStaticMainMethod();
        if(constructorDeclaration.getArgs().size() != 0)
            addDefaultConstructor();
        else {
            String argsSigniture = "";
            for(VarDeclaration v : constructorDeclaration.getArgs()) {
                argsSigniture = argsSigniture.concat(makeTypeSignature(v.getType()));
            }
            addCommand(".method public <init>("+argsSigniture+")V");
            addCommand("aload_0");
            if(currentClass.getParentClassName() == null)
                addCommand("invokenonvirtual java/lang/Object/<init>()V");
            else {
                String parentName = currentClass.getParentClassName().getName();
                addCommand("invokespecial "+parentName+"/<init>()V");
            }
            for(int i = 0; i < currentClass.getFields().size(); i++) {
                initializeVar(currentClass.getFields().get(i).getVarDeclaration(), i+1);
            }
            addCommand("return");
            addCommand(".end method");
            addCommand("");
        }
        this.visit((MethodDeclaration) constructorDeclaration);
        return null;
    }

    @Override
    public String visit(MethodDeclaration methodDeclaration) {
        //todo add method or constructor headers
        if(methodDeclaration instanceof ConstructorDeclaration) {
            //todo call parent constructor
            //todo initialize fields
        }
        //todo visit local vars and body and add return if needed
        return null;
    }

    @Override
    public String visit(FieldDeclaration fieldDeclaration) {
        //todo
        return null;
    }

    @Override
    public String visit(VarDeclaration varDeclaration) {
        //todo
        return null;
    }

    @Override
    public String visit(AssignmentStmt assignmentStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(BlockStmt blockStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(ConditionalStmt conditionalStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(MethodCallStmt methodCallStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(PrintStmt print) {
        //todo
        return null;
    }

    @Override
    public String visit(ReturnStmt returnStmt) {
        Type type = returnStmt.getReturnedExpr().accept(expressionTypeChecker);
        if(type instanceof NullType) {
            addCommand("return");
        }
        else {
            //todo add commands to return
        }
        return null;
    }

    @Override
    public String visit(BreakStmt breakStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(ContinueStmt continueStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(ForeachStmt foreachStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(ForStmt forStmt) {
        //todo
        return null;
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        BinaryOperator operator = binaryExpression.getBinaryOperator();
        Expression op1 = binaryExpression.getFirstOperand();
        Expression op2 = binaryExpression.getSecondOperand();
        String commands = "";
        if (operator == BinaryOperator.add) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "iadd\n";
        }
        else if (operator == BinaryOperator.sub) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "isub\n";
        }
        else if (operator == BinaryOperator.mult) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "imul\n";
        }
        else if (operator == BinaryOperator.div) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "idiv\n";
        }
        else if (operator == BinaryOperator.mod) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "irem\n";
        }
        else if((operator == BinaryOperator.gt) || (operator == BinaryOperator.lt)) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            /*
            String s =  String.valueOf(this.labelNum);
            if (operator == BinaryOperator.gt) {
                commands += "if_icmpgt Label" + s + "\n"; //???
                this.labelNum += 1;
                commands += "iconst_1\n";
                s =  String.valueOf(this.labelNum);
                commands += "goto Label" + s + "\n"; //???
            }

            if (operator == BinaryOperator.lt)
                commands += "if_icmplt\n";
            */
        }
        else if((operator == BinaryOperator.eq) || (operator == BinaryOperator.neq)) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            Type t1 = op1.accept(this.expressionTypeChecker);
            Type t2 = op2.accept(this.expressionTypeChecker);
            /*if (t1 instanceof IntType && t2 instanceof IntType) {
                if (operator == BinaryOperator.eq)
                    commands += "if_icmpeq\n";
                if (operator == BinaryOperator.neq)
                    commands += "if_icmpne\n";
            } else {
                if (operator == BinaryOperator.eq)
                    commands += "if_acmpeq\n";
                if (operator == BinaryOperator.neq)
                    commands += "if_acmpne\n";
            }*/
        }
        else if(operator == BinaryOperator.and) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "ior\n";
        }
        else if(operator == BinaryOperator.or) {
            //todo
            commands += op1.accept(this);
            commands += op2.accept(this);
            commands += "iand\n";
        }
        else if(operator == BinaryOperator.assign) {
            Type firstType = binaryExpression.getFirstOperand().accept(expressionTypeChecker);
            String secondOperandCommands = binaryExpression.getSecondOperand().accept(this);
            if(firstType instanceof ListType) {
                //todo make new list with List copy constructor with the second operand commands
                // (add these commands to secondOperandCommands)
            }
            if(binaryExpression.getFirstOperand() instanceof Identifier) {
                //todo
            }
            else if(binaryExpression.getFirstOperand() instanceof ListAccessByIndex) {
                //todo
            }
            else if(binaryExpression.getFirstOperand() instanceof ObjectOrListMemberAccess) {
                Expression instance = ((ObjectOrListMemberAccess) binaryExpression.getFirstOperand()).getInstance();
                Type memberType = binaryExpression.getFirstOperand().accept(expressionTypeChecker);
                String memberName = ((ObjectOrListMemberAccess) binaryExpression.getFirstOperand()).getMemberName().getName();
                Type instanceType = instance.accept(expressionTypeChecker);
                if(instanceType instanceof ListType) {
                    //todo
                }
                else if(instanceType instanceof ClassType) {
                    //todo
                }
            }
        }
        return commands;
    }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        UnaryOperator operator = unaryExpression.getOperator();
        String commands = "";
        if(operator == UnaryOperator.minus) {
            //todo
            commands += unaryExpression.getOperand();
            commands += "ineg\n";
        }
        else if(operator == UnaryOperator.not) {
            //todo
            commands += unaryExpression.getOperand();
            //commands += "\n"; ????
        }
        else if((operator == UnaryOperator.predec) || (operator == UnaryOperator.preinc)) {
            if(unaryExpression.getOperand() instanceof Identifier) {
                //todo
            }
            else if(unaryExpression.getOperand() instanceof ListAccessByIndex) {
                //todo
            }
            else if(unaryExpression.getOperand() instanceof ObjectOrListMemberAccess) {
                Expression instance = ((ObjectOrListMemberAccess) unaryExpression.getOperand()).getInstance();
                Type memberType = unaryExpression.getOperand().accept(expressionTypeChecker);
                String memberName = ((ObjectOrListMemberAccess) unaryExpression.getOperand()).getMemberName().getName();
                Type instanceType = instance.accept(expressionTypeChecker);
                if(instanceType instanceof ListType) {
                    //todo
                }
                else if(instanceType instanceof ClassType) {
                    //todo
                }
            }
        }
        else if((operator == UnaryOperator.postdec) || (operator == UnaryOperator.postinc)) {
            if(unaryExpression.getOperand() instanceof Identifier) {
                //todo
            }
            else if(unaryExpression.getOperand() instanceof ListAccessByIndex) {
                //todo
            }
            else if(unaryExpression.getOperand() instanceof ObjectOrListMemberAccess) {
                Expression instance = ((ObjectOrListMemberAccess) unaryExpression.getOperand()).getInstance();
                Type memberType = unaryExpression.getOperand().accept(expressionTypeChecker);
                String memberName = ((ObjectOrListMemberAccess) unaryExpression.getOperand()).getMemberName().getName();
                Type instanceType = instance.accept(expressionTypeChecker);
                if(instanceType instanceof ListType) {
                    //todo
                }
                else if(instanceType instanceof ClassType) {
                    //todo
                }
            }
        }
        return commands;
    }

    @Override
    public String visit(ObjectOrListMemberAccess objectOrListMemberAccess) {
        Type memberType = objectOrListMemberAccess.accept(expressionTypeChecker);
        Type instanceType = objectOrListMemberAccess.getInstance().accept(expressionTypeChecker);
        String memberName = objectOrListMemberAccess.getMemberName().getName();
        String commands = "";
        if(instanceType instanceof ClassType) {
            String className = ((ClassType) instanceType).getClassName().getName();
            try {
                SymbolTable classSymbolTable = ((ClassSymbolTableItem) SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + className, true)).getClassSymbolTable();
                try {
                    classSymbolTable.getItem(FieldSymbolTableItem.START_KEY + memberName, true);
                    //todo it is a field
                } catch (ItemNotFoundException memberIsMethod) {
                    //todo it is a method (new instance of Fptr)
                }
            } catch (ItemNotFoundException classNotFound) {
            }
        }
        else if(instanceType instanceof ListType) {
            //todo
        }
        return commands;
    }

    @Override
    public String visit(Identifier identifier) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(ListAccessByIndex listAccessByIndex) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(MethodCall methodCall) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(NewClassInstance newClassInstance) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(ThisClass thisClass) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(ListValue listValue) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(NullValue nullValue) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(IntValue intValue) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(BoolValue boolValue) {
        String commands = "";
        //todo
        return commands;
    }

    @Override
    public String visit(StringValue stringValue) {
        String commands = "";
        //todo
        return commands;
    }

}