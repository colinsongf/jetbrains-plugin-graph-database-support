package com.neueda.jetbrains.plugin.graphdb.test.integration.neo4j.tests.cypher.editor;

import com.intellij.codeInsight.hint.ParameterInfoComponent;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoUIContextEx;
import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext;
import com.intellij.testFramework.utils.parameterInfo.MockUpdateParameterInfoContext;
import com.neueda.jetbrains.plugin.graphdb.language.cypher.editor.CypherParameterInfoHandler;
import com.neueda.jetbrains.plugin.graphdb.language.cypher.references.CypherInvocation;
import com.neueda.jetbrains.plugin.graphdb.test.integration.neo4j.util.base.BaseIntegrationTest;

public class ArgumentHintTest extends BaseIntegrationTest {

    private CypherParameterInfoHandler parameterInfoHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parameterInfoHandler = new CypherParameterInfoHandler();

        dataSource().neo4j31();
    }


    public void testSpecialAll() {
        doTest("RETURN ALL (x <caret>IN nodes(p) WHERE x.age > 30)",
                "<html>(<b>variable IN list WHERE predicate :: ANY</b>)</html>");
    }

    public void testSpecialAny() {
        doTest("RETURN ANY (x IN a.array WHERE x = 'one'<caret>)",
                "<html>(<b>variable IN list WHERE predicate :: ANY</b>)</html>");
    }

    public void testSpecialExists() {
        doTest("RETURN EXISTS(<caret>true)",
                "<html>(<b>pattern :: ANY</b>)</html>");
    }

    public void testSpecialNone() {
        doTest("RETURN NONE (<caret>x IN nodes(p) WHERE x.age = 25)",
                "<html>(<b>variable in list WHERE predicate :: ANY</b>)</html>");
    }

    public void testSpecialSingle() {
        doTest("RETURN SINGLE (<caret>var IN nodes(p) WHERE var.eyes = 'blue')",
                "<html>(<b>variable in list WHERE predicate :: ANY</b>)</html>");
    }

    public void testSpecialFilter() {
        doTest("RETURN filter(<caret>x IN a.array WHERE size(x)= 3)",
                "<html>(<b>variable IN list WHERE predicate :: ANY</b>)</html>");
    }

    public void testSpecialExtract() {
        doTest("RETURN extract(<caret>n IN nodes(p)| n.age) AS extracted",
                "<html>(<b>variable IN list | expression :: ANY</b>)</html>");
    }

    public void testSpecialReduce() {
        doTest("RETURN reduce(<caret>totalAge = 0, n IN nodes(p)| totalAge + n.age) ",
                "<html>(<b>accumulator = initial :: ANY</b>, variable IN list | expression :: ANY)</html>");
    }

    public void testBuiltIn() {
        doTest("return toFloat(<caret>\"12\")",
                "<html>(<b>expression :: STRING</b>)</html>");
    }

    public void testProcedure() {
        doTest("CALL db.resampleIndex(<caret>\"test\");",
                "<html>(<b>index :: STRING?</b>)</html>");
    }

    public void testUserDefinedFunction() {
        doTest("RETURN com.neueda.jetbrains.plugin.graphdb.test.database.neo4j_3_1.secondTestFunction(<caret>\"test\");",
                "<html>(<b>param :: STRING?</b>)</html>");
    }

    public void testNoParams() {
        doTest("RETURN e(<caret>)",
                null);
    }

    public void testUnknownFunction() {
        doTest("return unknown(<caret>)",
                null);
    }

    public void testSecondArgument() {
        doTest("RETURN reduce(totalAge = 0, n IN nodes(p)| totalAge + n.age<caret>)",
                "<html>(accumulator = initial :: ANY,<b> variable IN list | expression :: ANY</b>)</html>");
    }

    private void doTest(String query, String expectedHighlight) {
        myFixture.configureByText("test.cyp", query);
        Object[] itemsToShow = getItemsToShow();
        int paramIdx = getHighlightedItem();
        String presentation = getPresentation(itemsToShow, paramIdx);

        assertEquals(1, itemsToShow.length);
        assertEquals(expectedHighlight, presentation);
    }

    private Object[] getItemsToShow() {
        CreateParameterInfoContext createCtx = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
        CypherInvocation psiElement = parameterInfoHandler.findElementForParameterInfo(createCtx);
        assertNotNull(psiElement);
        parameterInfoHandler.showParameterInfo(psiElement, createCtx);
        return createCtx.getItemsToShow();
    }


    private int getHighlightedItem() {
        MockUpdateParameterInfoContext updateCtx = new MockUpdateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
        CypherInvocation psiElement = parameterInfoHandler.findElementForUpdatingParameterInfo(updateCtx);
        assertNotNull(psiElement);
        parameterInfoHandler.updateParameterInfo(psiElement, updateCtx);
        return updateCtx.getCurrentParameter();
    }

    private String getPresentation(Object[] itemsToShow, int paramIdx) {
        ParameterInfoUIContextEx uiCtx =
                ParameterInfoComponent.createContext(itemsToShow, myFixture.getEditor(), parameterInfoHandler, paramIdx);
        return parameterInfoHandler.getPresentation((CypherInvocation) itemsToShow[0], uiCtx);
    }

}
