/*
 * Copyright (c) 2017-2022 Works Applications Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.worksap.nlp.sudachi.dictionary;

import com.worksap.nlp.sudachi.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A classifier of the categories of characters.
 */
public class CharacterCategory {

    static class Range {
        int low;
        int high;
        EnumSet<CategoryType> categories = EnumSet.noneOf(CategoryType.class);

        boolean contains(int cp) {
            return cp >= low && cp <= high;
        }

        int containingLength(String text) {
            for (int i = 0; i < text.length(); i = text.offsetByCodePoints(i, 1)) {
                int c = text.codePointAt(i);
                if (c < low || c > high) {
                    return i;
                }
            }
            return text.length();
        }
    }

    private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");
    private static final Pattern PATTERN_EMPTY_OR_SPACES = Pattern.compile("\\s*");
    private static final Pattern PATTERN_DOUBLE_PERIODS = Pattern.compile("\\.\\.");
    private final List<Range> rangeList = new ArrayList<>();

    /**
     * Returns the set of the category types of the character (Unicode code point).
     *
     * @param codePoint
     *            the code point value of the character
     * @return the set of the category types of the character
     */
    public EnumSet<CategoryType> getCategoryTypes(int codePoint) {
        EnumSet<CategoryType> categories = EnumSet.noneOf(CategoryType.class);
        for (Range range : rangeList) {
            if (range.contains(codePoint)) {
                categories.addAll(range.categories);
            }
        }

        if (categories.isEmpty()) {
            categories.add(CategoryType.DEFAULT);
        }
        return categories;
    }

    /**
     * Reads the definitions of the character categories from the file which is
     * specified by {@code charDef}. If {@code charDef} is {@code null}, uses the
     * default definitions.
     *
     * <p>
     * The following is the format of definitions.
     *
     * <pre>
     * {@code
     * 0x0020 SPACE              # a white space
     * 0x0041..0x005A ALPHA      # Latin alphabets
     * 0x4E00 KANJINUMERIC KANJI # Kanji numeric and Kanji
     * }
     * </pre>
     * <p>
     * Lines that do not start with "0x" are ignored.
     *
     * @param charDef
     *            the file of the definitions of character categories.
     * @throws IOException
     *             if the definition file is not available.
     * @deprecated use {@link #load(Config.Resource)} instead. Will be removed with
     *             1.0 release.
     */
    @Deprecated
    public void readCharacterDefinition(String charDef) throws IOException {
        try (InputStream in = (charDef != null) ? new FileInputStream(charDef)
                : CharacterCategory.class.getClassLoader().getResourceAsStream("char.def")) {
            readCharacterDefinition(in);
        }
    }

    public void readCharacterDefinition(InputStream in) throws IOException {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#") || PATTERN_EMPTY_OR_SPACES.matcher(line).matches()) {
                continue;
            }
            String[] cols = PATTERN_SPACES.split(line);
            if (cols.length < 2) {
                throw new IllegalArgumentException("invalid format at line " + reader.getLineNumber());
            }
            if (cols[0].startsWith("0x")) {
                Range range = new Range();
                String[] r = PATTERN_DOUBLE_PERIODS.split(cols[0]);
                range.low = range.high = Integer.decode(r[0]);
                if (r.length > 1) {
                    range.high = Integer.decode(r[1]);
                }
                if (range.low > range.high) {
                    throw new IllegalArgumentException("invalid range at line " + reader.getLineNumber());
                }
                for (int i = 1; i < cols.length; i++) {
                    if (cols[i].startsWith("#")) {
                        break;
                    }
                    CategoryType type;
                    try {
                        type = CategoryType.valueOf(cols[i]);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                cols[i] + " is invalid type at line " + reader.getLineNumber(), e);
                    }
                    range.categories.add(type);
                }
                rangeList.add(range);
            }
        }
    }

    public static CharacterCategory load(Config.Resource<CharacterCategory> resource) throws IOException {
        return resource.consume(res -> {
            CharacterCategory result = new CharacterCategory();
            try (InputStream is = res.asInputStream()) {
                result.readCharacterDefinition(is);
            }
            return result;
        });
    }
}
