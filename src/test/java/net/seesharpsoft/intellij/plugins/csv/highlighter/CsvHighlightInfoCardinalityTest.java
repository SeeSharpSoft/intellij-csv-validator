package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.List;

public class CsvHighlightInfoCardinalityTest extends CsvBasePlatformTestCase {

    private static final int RECORDS = 200;
    private static final int FIELDS_PER_RECORD = 10;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.loadState(new CsvEditorSettings.OptionSet());
        settings.setShowInfoBalloon(true);
        settings.setHighlightTabSeparator(true);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        } finally {
            super.tearDown();
        }
    }

    public void testHighlightingCsvCreatesNoHighlightInfoPerField() {
        assertHighlightInfosDoNotScaleWithFields("large.csv", ",");
    }

    public void testHighlightingTsvCreatesNoHighlightInfoPerFieldOrTabSeparator() {
        assertHighlightInfosDoNotScaleWithFields("large.tsv", "\t");
    }

    /**
     * Per-field or per-separator highlight infos made every reparse of large files evict hundreds of thousands
     * of infos in a non-cancelable read section of the platform, blocking all writes, including table edits.
     */
    private void assertHighlightInfosDoNotScaleWithFields(String fileName, String separator) {
        myFixture.configureByText(fileName, table(separator));

        List<HighlightInfo> infos = myFixture.doHighlighting();

        assertTrue("Highlighting created " + infos.size() + " infos for " + RECORDS * FIELDS_PER_RECORD + " fields",
                infos.size() < RECORDS);
    }

    private static String table(String separator) {
        StringBuilder text = new StringBuilder();
        for (int record = 0; record < RECORDS; record++) {
            for (int field = 0; field < FIELDS_PER_RECORD; field++) {
                if (field > 0) text.append(separator);
                text.append("value").append(record).append('_').append(field);
            }
            text.append('\n');
        }
        return text.toString();
    }
}
