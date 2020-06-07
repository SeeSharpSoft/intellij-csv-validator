package net.seesharpsoft.intellij.plugins.csv.structureview;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvStructureViewTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/structureview";
    }

    private void doCheckTreeElement(TreeElement element, Class expectedClazz, String expectedText, String expectedLocation) {
        assertInstanceOf(element, expectedClazz);
        assertInstanceOf(element, ItemPresentation.class);

        ItemPresentation presentation = (ItemPresentation) element;
        assertEquals(expectedText, presentation.getPresentableText());
        if (expectedLocation != null) {
            assertEquals(expectedLocation, presentation.getLocationString());
        }
    }

    public void testStructureViewWithoutFileEndLineBreakSupport() {
        myFixture.configureByFile("StructureViewTestData.csv");
        CsvEditorSettings.getInstance().setFileEndLineBreak(false);
        myFixture.testStructureView(structureViewComponent -> {
            StructureViewTreeElement root = structureViewComponent.getTreeModel().getRoot();
            doCheckTreeElement(root, CsvStructureViewElement.File.class, "FirstName, LastName\n" +
                    "Peter,Lustig,42\n" +
                    "Martin\n" +
                    ",Fuchs\n", null);
            assertEquals(3, root.getChildren().length);

            TreeElement header = root.getChildren()[0];
            doCheckTreeElement(
                    header,
                    CsvStructureViewElement.Header.class,
                    "FirstName",
                    "Header (4 entries)"
                    );

            TreeElement field = header.getChildren()[0];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Peter",
                    "(1)"
            );
            field = header.getChildren()[1];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Martin",
                    "(2)"
            );
            field = header.getChildren()[2];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "",
                    "(3)"
            );
            field = header.getChildren()[3];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "",
                    "(4)"
            );

            /**
             * LastName header
             */
            header = root.getChildren()[1];
            doCheckTreeElement(
                    header,
                    CsvStructureViewElement.Header.class,
                    "LastName",
                    "Header (3 entries)"
            );

            field = header.getChildren()[0];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Lustig",
                    "(1)"
            );
            field = header.getChildren()[1];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "<undefined>",
                    "(2)"
            );
            field = header.getChildren()[2];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Fuchs",
                    "(3)"
            );

            /**
             * Empty header
             */
            header = root.getChildren()[2];
            doCheckTreeElement(
                    header,
                    CsvStructureViewElement.Header.class,
                    "<undefined>",
                    "Header (1 entries)"
            );

            field = header.getChildren()[0];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "42",
                    "(1)"
            );
        });
    }

    public void testStructureViewFileEndLineBreakSupport() {
        myFixture.configureByFile("StructureViewTestData.csv");
        CsvEditorSettings.getInstance().setFileEndLineBreak(true);
        myFixture.testStructureView(structureViewComponent -> {
            StructureViewTreeElement root = structureViewComponent.getTreeModel().getRoot();
            doCheckTreeElement(root, CsvStructureViewElement.File.class, "FirstName, LastName\n" +
                    "Peter,Lustig,42\n" +
                    "Martin\n" +
                    ",Fuchs\n", null);
            assertEquals(3, root.getChildren().length);

            TreeElement header = root.getChildren()[0];
            doCheckTreeElement(
                    header,
                    CsvStructureViewElement.Header.class,
                    "FirstName",
                    "Header (3 entries)"
            );

            TreeElement field = header.getChildren()[0];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Peter",
                    "(1)"
            );
            field = header.getChildren()[1];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Martin",
                    "(2)"
            );
            field = header.getChildren()[2];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "",
                    "(3)"
            );

            /**
             * LastName header
             */
            header = root.getChildren()[1];
            doCheckTreeElement(
                    header,
                    CsvStructureViewElement.Header.class,
                    "LastName",
                    "Header (3 entries)"
            );

            field = header.getChildren()[0];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Lustig",
                    "(1)"
            );
            field = header.getChildren()[1];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "<undefined>",
                    "(2)"
            );
            field = header.getChildren()[2];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "Fuchs",
                    "(3)"
            );

            /**
             * Empty header
             */
            header = root.getChildren()[2];
            doCheckTreeElement(
                    header,
                    CsvStructureViewElement.Header.class,
                    "<undefined>",
                    "Header (1 entries)"
            );

            field = header.getChildren()[0];
            doCheckTreeElement(
                    field,
                    CsvStructureViewElement.Field.class,
                    "42",
                    "(1)"
            );
        });
    }
}
