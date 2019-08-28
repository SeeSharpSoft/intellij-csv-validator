package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

import java.util.Arrays;


public class TableDataChangeEventTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureByFiles("AnyFile.csv");
    }
    @Test
    public void testInstanceShouldBeCreatedWithGivenValues() {
        Object[][] values = { { "Test", 1 }, { 2, 5} };
        TableDataChangeEvent event = new TableDataChangeEvent(this, values);

        assertEquals(this, event.getSource());
        assertTrue(Arrays.deepEquals(values, event.getValue()));
    }

    @Test
    public void testGetValueShouldReturnCopyOfValues() {
        Object[][] values = { { "Test", 1 }, { 2, 5} };
        TableDataChangeEvent event = new TableDataChangeEvent(this, values);

        assertNotSame(values, event.getValue());
    }
}
