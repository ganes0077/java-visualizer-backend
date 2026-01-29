package com.codeanalyzer.backend.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import org.springframework.stereotype.Service;

@Service
public class InstrumentationService {

    public String instrument(String originalCode) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(originalCode);
            cu.addImport("com.codeanalyzer.backend.service.Tracer");

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                String methodName = method.getNameAsString();
                BlockStmt originalBody = method.getBody().orElse(null);

                if (originalBody != null) {
                    BlockStmt newMethodBody = new BlockStmt();
                    newMethodBody.addStatement(StaticJavaParser.parseStatement("Tracer.enterMethod(\"" + methodName + "\");"));

                    BlockStmt userLogic = new BlockStmt();
                    originalBody.getStatements().forEach(stmt -> userLogic.addStatement(stmt));
                    
                    instrumentBlock(userLogic, methodName); // Recursive instrumentation

                    TryStmt tryStmt = new TryStmt();
                    tryStmt.setTryBlock(userLogic);
                    BlockStmt finallyBlock = new BlockStmt();
                    finallyBlock.addStatement(StaticJavaParser.parseStatement("Tracer.exitMethod();"));
                    tryStmt.setFinallyBlock(finallyBlock);

                    newMethodBody.addStatement(tryStmt);
                    method.setBody(newMethodBody);
                }
            });
            return cu.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return originalCode;
        }
    }

    private void instrumentBlock(BlockStmt block, String methodName) {
        NodeList<Statement> newStmts = new NodeList<>();

        for (Statement stmt : block.getStatements()) {
            newStmts.add(stmt); // Add original line

            // 1. RECURSION FOR LOOPS & IFs
            if (stmt.isForStmt()) {
                Statement body = stmt.asForStmt().getBody();
                BlockStmt bodyBlock = body.isBlockStmt() ? body.asBlockStmt() : new BlockStmt(new NodeList<>(body));
                instrumentBlock(bodyBlock, methodName);
                stmt.asForStmt().setBody(bodyBlock);
            } 
            else if (stmt.isWhileStmt()) {
                Statement body = stmt.asWhileStmt().getBody();
                BlockStmt bodyBlock = body.isBlockStmt() ? body.asBlockStmt() : new BlockStmt(new NodeList<>(body));
                instrumentBlock(bodyBlock, methodName);
                stmt.asWhileStmt().setBody(bodyBlock);
            }
            else if (stmt.isIfStmt()) {
                Statement thenStmt = stmt.asIfStmt().getThenStmt();
                BlockStmt thenBlock = thenStmt.isBlockStmt() ? thenStmt.asBlockStmt() : new BlockStmt(new NodeList<>(thenStmt));
                instrumentBlock(thenBlock, methodName);
                stmt.asIfStmt().setThenStmt(thenBlock);
                stmt.asIfStmt().getElseStmt().ifPresent(elseStmt -> {
                    BlockStmt elseBlock = elseStmt.isBlockStmt() ? elseStmt.asBlockStmt() : new BlockStmt(new NodeList<>(elseStmt));
                    instrumentBlock(elseBlock, methodName);
                    stmt.asIfStmt().setElseStmt(elseBlock);
                });
            }

            // 2. INJECT TRACERS (Variable Tracking)
            if (stmt.isExpressionStmt()) {
                Expression expr = stmt.asExpressionStmt().getExpression();

                // Case A: int[] arr = {1, 2};
                if (expr.isVariableDeclarationExpr()) {
                    VariableDeclarationExpr vde = expr.asVariableDeclarationExpr();
                    for (VariableDeclarator var : vde.getVariables()) {
                        injectSnapshot(newStmts, stmt, methodName, var.getNameAsString());
                    }
                } 
                // Case B: arr = new int[5];  OR  arr[0] = 10;
                else if (expr.isAssignExpr()) {
                    AssignExpr assign = expr.asAssignExpr();
                    Expression target = assign.getTarget();

                    // If it is an array access (numbers[i] = ...), dig down to find "numbers"
                    while (target.isArrayAccessExpr()) {
                        target = target.asArrayAccessExpr().getName();
                    }
                    
                    // Now target is the variable name (e.g., "numbers" or "a")
                    injectSnapshot(newStmts, stmt, methodName, target.toString());
                }
            }
        }
        block.setStatements(newStmts);
    }

    private void injectSnapshot(NodeList<Statement> stmts, Statement originalStmt, String methodName, String varName) {
        int lineNo = originalStmt.getBegin().map(pos -> pos.line).orElse(0);
        // We capture the variable 'varName' so if 'arr[0]' changed, we snapshot 'arr'
        String code = String.format("Tracer.snapshot(%d, \"%s\", \"%s\", %s);", lineNo, methodName, varName, varName);
        stmts.add(StaticJavaParser.parseStatement(code));
    }
}