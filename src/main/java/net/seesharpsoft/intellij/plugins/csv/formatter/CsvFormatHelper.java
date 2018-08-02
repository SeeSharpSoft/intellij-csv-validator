package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvElementType;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CsvFormatHelper {

    private static final String WHITE_SPACE_PATTERN_STRING = "[ \\f]+";
    private static final Pattern BEGIN_WHITE_SPACE_PATTERN = Pattern.compile("^" + WHITE_SPACE_PATTERN_STRING);
    private static final Pattern END_WHITE_SPACE_PATTERN = Pattern.compile(WHITE_SPACE_PATTERN_STRING + "$");

    private static final int HEX_RADIX = 16;
    private static final int[][] WIDTH_DOUBLE_CHARCODE_RANGES;
    private static final int[][] AMBIGUOUS_DOUBLE_CHARCODE_RANGES;
    private static final int[][] WIDTH_ZERO_CHARCODE_RANGES = {
            {0x0300, 0x036F}, {0x0483, 0x0486}, {0x0488, 0x0489},
            {0x0591, 0x05BD}, {0x05BF, 0x05BF}, {0x05C1, 0x05C2},
            {0x05C4, 0x05C5}, {0x05C7, 0x05C7}, {0x0600, 0x0603},
            {0x0610, 0x0615}, {0x064B, 0x065E}, {0x0670, 0x0670},
            {0x06D6, 0x06E4}, {0x06E7, 0x06E8}, {0x06EA, 0x06ED},
            {0x070F, 0x070F}, {0x0711, 0x0711}, {0x0730, 0x074A},
            {0x07A6, 0x07B0}, {0x07EB, 0x07F3}, {0x0901, 0x0902},
            {0x093C, 0x093C}, {0x0941, 0x0948}, {0x094D, 0x094D},
            {0x0951, 0x0954}, {0x0962, 0x0963}, {0x0981, 0x0981},
            {0x09BC, 0x09BC}, {0x09C1, 0x09C4}, {0x09CD, 0x09CD},
            {0x09E2, 0x09E3}, {0x0A01, 0x0A02}, {0x0A3C, 0x0A3C},
            {0x0A41, 0x0A42}, {0x0A47, 0x0A48}, {0x0A4B, 0x0A4D},
            {0x0A70, 0x0A71}, {0x0A81, 0x0A82}, {0x0ABC, 0x0ABC},
            {0x0AC1, 0x0AC5}, {0x0AC7, 0x0AC8}, {0x0ACD, 0x0ACD},
            {0x0AE2, 0x0AE3}, {0x0B01, 0x0B01}, {0x0B3C, 0x0B3C},
            {0x0B3F, 0x0B3F}, {0x0B41, 0x0B43}, {0x0B4D, 0x0B4D},
            {0x0B56, 0x0B56}, {0x0B82, 0x0B82}, {0x0BC0, 0x0BC0},
            {0x0BCD, 0x0BCD}, {0x0C3E, 0x0C40}, {0x0C46, 0x0C48},
            {0x0C4A, 0x0C4D}, {0x0C55, 0x0C56}, {0x0CBC, 0x0CBC},
            {0x0CBF, 0x0CBF}, {0x0CC6, 0x0CC6}, {0x0CCC, 0x0CCD},
            {0x0CE2, 0x0CE3}, {0x0D41, 0x0D43}, {0x0D4D, 0x0D4D},
            {0x0DCA, 0x0DCA}, {0x0DD2, 0x0DD4}, {0x0DD6, 0x0DD6},
            {0x0E31, 0x0E31}, {0x0E34, 0x0E3A}, {0x0E47, 0x0E4E},
            {0x0EB1, 0x0EB1}, {0x0EB4, 0x0EB9}, {0x0EBB, 0x0EBC},
            {0x0EC8, 0x0ECD}, {0x0F18, 0x0F19}, {0x0F35, 0x0F35},
            {0x0F37, 0x0F37}, {0x0F39, 0x0F39}, {0x0F71, 0x0F7E},
            {0x0F80, 0x0F84}, {0x0F86, 0x0F87}, {0x0F90, 0x0F97},
            {0x0F99, 0x0FBC}, {0x0FC6, 0x0FC6}, {0x102D, 0x1030},
            {0x1032, 0x1032}, {0x1036, 0x1037}, {0x1039, 0x1039},
            {0x1058, 0x1059}, {0x1160, 0x11FF}, {0x135F, 0x135F},
            {0x1712, 0x1714}, {0x1732, 0x1734}, {0x1752, 0x1753},
            {0x1772, 0x1773}, {0x17B4, 0x17B5}, {0x17B7, 0x17BD},
            {0x17C6, 0x17C6}, {0x17C9, 0x17D3}, {0x17DD, 0x17DD},
            {0x180B, 0x180D}, {0x18A9, 0x18A9}, {0x1920, 0x1922},
            {0x1927, 0x1928}, {0x1932, 0x1932}, {0x1939, 0x193B},
            {0x1A17, 0x1A18}, {0x1B00, 0x1B03}, {0x1B34, 0x1B34},
            {0x1B36, 0x1B3A}, {0x1B3C, 0x1B3C}, {0x1B42, 0x1B42},
            {0x1B6B, 0x1B73}, {0x1DC0, 0x1DCA}, {0x1DFE, 0x1DFF},
            {0x200B, 0x200F}, {0x202A, 0x202E}, {0x2060, 0x2063},
            {0x206A, 0x206F}, {0x20D0, 0x20EF}, {0x302A, 0x302F},
            {0x3099, 0x309A}, {0xA806, 0xA806}, {0xA80B, 0xA80B},
            {0xA825, 0xA826}, {0xFB1E, 0xFB1E}, {0xFE00, 0xFE0F},
            {0xFE20, 0xFE23}, {0xFEFF, 0xFEFF}, {0xFFF9, 0xFFFB},
            {0x10A01, 0x10A03}, {0x10A05, 0x10A06}, {0x10A0C, 0x10A0F},
            {0x10A38, 0x10A3A}, {0x10A3F, 0x10A3F}, {0x1D167, 0x1D169},
            {0x1D173, 0x1D182}, {0x1D185, 0x1D18B}, {0x1D1AA, 0x1D1AD},
            {0x1D242, 0x1D244}, {0xE0001, 0xE0001}, {0xE0020, 0xE007F},
            {0xE0100, 0xE01EF}
    };

    static {
        final List<String> wideLines = new ArrayList<>();
        final List<String> ambiguousLines = new ArrayList<>();
        try (InputStream is = CsvFormatHelper.class.getClassLoader().getResourceAsStream("misc/EastAsianDoubleWidth.csv")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            reader.lines().forEach(line -> {
                        if (line.endsWith("W") || line.endsWith("F")) {
                            wideLines.add(line.substring(0, line.length() - 2));
                        }
                        if (line.endsWith("A")) {
                            ambiguousLines.add(line.substring(0, line.length() - 2));
                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        WIDTH_DOUBLE_CHARCODE_RANGES = convertRangeTextToRangeArray(wideLines);
        AMBIGUOUS_DOUBLE_CHARCODE_RANGES = convertRangeTextToRangeArray(ambiguousLines);
    }

    private static int[][] convertRangeTextToRangeArray(List<String> wideLines) {
        int[][] targetArray = new int[wideLines.size()][2];
        for (int i = 0; i < targetArray.length; ++i) {
            String[] split = wideLines.get(i).split("\\.\\.");
            targetArray[i][0] = Integer.parseInt(split[0], HEX_RADIX);
            if (split.length == 1) {
                targetArray[i][1] = targetArray[i][0];
            } else {
                targetArray[i][1] = Integer.parseInt(split[1], HEX_RADIX);
            }
        }
        return targetArray;
    }

    private static boolean binarySearch(int[][] ranges, int charCode) {
        int min = 0;
        int mid;
        int max = ranges.length - 1;

        if (charCode < ranges[0][0] || charCode > ranges[max][1]) {
            return false;
        }
        while (max >= min) {
            mid = (min + max) / 2;
            if (charCode > ranges[mid][1]) {
                min = mid + 1;
            } else if (charCode < ranges[mid][0]) {
                max = mid - 1;
            } else {
                return true;
            }
        }

        return false;
    }

    public static int charWidth(int charCode, boolean ambiguousWide) {
        if (charCode <= 0 || binarySearch(WIDTH_ZERO_CHARCODE_RANGES, charCode)) {
            return 0;
        }
        if (binarySearch(WIDTH_DOUBLE_CHARCODE_RANGES, charCode) || (ambiguousWide && binarySearch(AMBIGUOUS_DOUBLE_CHARCODE_RANGES, charCode))) {
            return 2;
        }
        return 1;
    }

    public static int charWidth(CharSequence s, boolean ambiguousWide) {
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            result += charWidth(s.charAt(i), ambiguousWide);
        }
        return result;
    }

    public static int getTextLength(ASTNode node, CodeStyleSettings codeStyleSettings) {
        CsvCodeStyleSettings csvCodeStyleSettings = codeStyleSettings.getCustomSettings(CsvCodeStyleSettings.class);
        String text = node.getText();
        int length = 0;
        if (csvCodeStyleSettings.TABULARIZE && !csvCodeStyleSettings.WHITE_SPACES_OUTSIDE_QUOTES && text.startsWith("\"")) {
            text = text.substring(1, text.length() - 1);
            text = BEGIN_WHITE_SPACE_PATTERN.matcher(text).replaceFirst("");
            text = END_WHITE_SPACE_PATTERN.matcher(text).replaceFirst("");
            length += 2;
        }
        length += csvCodeStyleSettings.ENABLE_WIDE_CHARACTER_DETECTION ? charWidth(text, csvCodeStyleSettings.TREAT_AMBIGUOUS_CHARACTERS_AS_WIDE) : text.length();

        return length;
    }

    public static ASTNode getRoot(final ASTNode node) {
        ASTNode currentNode = node;
        ASTNode parent;
        while ((parent = currentNode.getTreeParent()) != null) {
            currentNode = parent;
        }
        return currentNode;
    }

    public static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        CsvCodeStyleSettings csvCodeStyleSettings = settings.getCustomSettings(CsvCodeStyleSettings.class);
        SpacingBuilder builder = new SpacingBuilder(settings, CsvLanguage.INSTANCE);
        if (csvCodeStyleSettings.TRIM_LEADING_WHITE_SPACES || csvCodeStyleSettings.TABULARIZE) {
            builder
                    .after(CsvTypes.COMMA).spaceIf(csvCodeStyleSettings.SPACE_AFTER_SEPARATOR)
                    .after(CsvTypes.CRLF).spaces(0)
                    .after(CsvElementType.DOCUMENT_START).spaces(0);
            if (csvCodeStyleSettings.TABULARIZE && !csvCodeStyleSettings.WHITE_SPACES_OUTSIDE_QUOTES) {
                builder.before(CsvTypes.QUOTE).spaces(0);
            }
        } else if (csvCodeStyleSettings.SPACE_AFTER_SEPARATOR) {
            builder.after(CsvTypes.COMMA).spaces(1);
        }

        if (csvCodeStyleSettings.TRIM_TRAILING_WHITE_SPACES || csvCodeStyleSettings.TABULARIZE) {
            builder
                    .before(CsvTypes.COMMA).spaceIf(csvCodeStyleSettings.SPACE_BEFORE_SEPARATOR)
                    .before(CsvTypes.CRLF).spaces(0);
            if (csvCodeStyleSettings.TABULARIZE && !csvCodeStyleSettings.WHITE_SPACES_OUTSIDE_QUOTES) {
                builder.after(CsvTypes.QUOTE).spaces(0);
            }
        } else if (csvCodeStyleSettings.SPACE_BEFORE_SEPARATOR) {
            builder.before(CsvTypes.COMMA).spaces(1);
        }

        return builder;
    }

    public static Map<Integer, CsvColumnInfo<ASTNode>> createColumnInfoMap(ASTNode root, CodeStyleSettings settings) {
        Map<Integer, CsvColumnInfo<ASTNode>> columnInfoMap = new HashMap<>();
        ASTNode child = root.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CsvTypes.RECORD) {
                Integer column = 0;
                ASTNode subChild = child.getFirstChildNode();
                while (subChild != null) {
                    if (subChild.getElementType() == CsvTypes.FIELD) {
                        int length = getTextLength(subChild, settings);
                        if (!columnInfoMap.containsKey(column)) {
                            columnInfoMap.put(column, new CsvColumnInfo(column, length));
                        } else if (columnInfoMap.get(column).getMaxLength() < length) {
                            columnInfoMap.get(column).setMaxLength(length);
                        }
                        columnInfoMap.get(column).addElement(subChild);
                        ++column;
                    }
                    subChild = subChild.getTreeNext();
                }
            }
            child = child.getTreeNext();
        }
        return columnInfoMap;
    }

    public static boolean isQuotedField(@Nullable CsvBlock block) {
        if (block != null && block.getNode().getElementType() == CsvTypes.FIELD) {
            List<Block> subBlocks = block.buildChildren();
            if (subBlocks.size() > 0) {
                AbstractBlock abstractBlock = (AbstractBlock) subBlocks.get(0);
                return abstractBlock.getNode().getElementType() == CsvTypes.QUOTE;
            }
        }
        return false;
    }

    private CsvFormatHelper() {
        // static utility class
    }
}
