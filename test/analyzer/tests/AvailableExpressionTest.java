package analyzer.tests;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

import analyzer.ast.ParserVisitor;
import analyzer.visitors.AvailableExpressionVisitor;

/**
 * Created: 17-08-02
 * Last Changed: 17-08-02
 * Author: Nicolas Cloutier
 *
 * Description: This test the PrintVisitor. So it will basically test that the tree can be written into valid
 * source code.
 */

@RunWith(Parameterized.class)
public class AvailableExpressionTest extends BaseTest {

    private static String m_test_suite_path = "./test-suite/AvailableExpressionTest/data";

    public AvailableExpressionTest(File file) {
        super(file);
    }

    @Test
    public void run() throws Exception {
        ParserVisitor algorithm = new AvailableExpressionVisitor(m_output);
        runAndAssert(algorithm);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getFiles() {
        return getFiles(m_test_suite_path);
    }

}
