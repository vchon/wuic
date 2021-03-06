/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.engine.LineInspector;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This class inspects CSS files to extract nuts referenced with @import statement to process it. Then it adapts
 * the path of those processed nuts to be accessible when exposed to the browser through WUIC uri
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.3
 */
public class CGCssUrlLineInspector implements LineInspector {

    /**
     * Possible value for CSS file name.
     */
    private static final String STRING_LITERAL_REGEX = "(\"(?:\\.|[^\\\"])*\"|'(?:\\.|[^\\'])*')";

    /**
     * Url pattern.
     */
    private static final String URL_REGEX = String.format("(?:url\\(\\s*(%s|[^)]*)\\s*\\))", STRING_LITERAL_REGEX);

    /**
     * Finds the @import or background URL statements within CSS script. The pattern describes a string :
     * <ul>
     * <li>starting with @import</li>
     * <li>followed any set of characters</li>
     * <li>followed by either a double quote or single quote</li>
     * <li>followed by any set of character until a repetition of the previously viewed quote is found</li>
     * </ul>
     *
     * OR
     *
     * <ul>
     * <li>stating with @import</li>
     * <li>followed by a set of characters with the term 'url' inside them</li>
     * <li>followed by a '('</li>
     * <li>followed by a set of characters</li>
     * <li>followed by a set of characters with ')' at the beginning and a ';' at the end</li>
     * </ul>
     *
     * OR
     * <ul>
     * <li>starting with background</li>
     * <li>followed by a string ending with 'url('</li>
     * <li>followed by either a single quote or a double quote (optional)</li>
     * <li>followed by everything which does not contain a repetition of the quote previously found</li>
     * </ul>
     */
    private static final Pattern CSS_URL_PATTERN = Pattern.compile(
            String.format("(?:@import.*?(%s|%s))|(?:background.*?(%s))", URL_REGEX, STRING_LITERAL_REGEX, URL_REGEX));

    /**
     * Three groups could contain the name, test the second one if first returns null.
     */
    private static final int [] GROUP_INDEXES = new int[] {
            NumberUtils.SIX, // background case
            NumberUtils.TWO, // import with url case
            1,               // import without url case
    };

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public Pattern getPattern() {
        return CSS_URL_PATTERN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut appendTransformation(final Matcher matcher,
                                    final StringBuilder replacement,
                                    final String heapPath,
                                    final NutDao dao) throws StreamException {
        // Search the right group
        int i = 0;
        int groupIndex;
        String rawPath;
        Nut retval = null;

        do {
            groupIndex = GROUP_INDEXES[i++];
            rawPath = matcher.group(groupIndex);
        } while (rawPath == null && i < GROUP_INDEXES.length);

        // Compute once operations performed multiple times
        final int start = matcher.start(groupIndex) - matcher.start();
        final String group = matcher.group();
        String referencedPath = rawPath;

        // Quotes must be removed
        if (referencedPath.charAt(0) == '\'' || referencedPath.charAt(0) == '"') {
            referencedPath = referencedPath.substring(1, referencedPath.length() - 1);
        }

        // Ignore absolute CSS
        final Boolean isAbsolute = referencedPath.startsWith("http://") || referencedPath.startsWith("/");

        log.info("url statement found for nut {}", referencedPath);

        // Rewrite the statement from its beginning to the beginning of the nut name
        replacement.append(group.substring(0, start));

        // Write path to nut
        replacement.append("\"");

        // Don't change nut if absolute
        if (isAbsolute) {
            log.warn("{} is referenced as an absolute file and won't be processed by WUIC. You should only use relative URL reachable by nut DAO.", referencedPath);
            replacement.append(referencedPath);
        } else {
            // Extract the nut
            final Set<Nut> nuts = dao.create(referencedPath, NutDao.PathFormat.RELATIVE_FILE).keySet();

            if (!nuts.isEmpty()) {
                retval = nuts.iterator().next();

                // Use proxy URI if DAO provide it
                final String proxy = retval.getProxyUri();

                if (proxy == null) {
                    replacement.append(IOUtils.mergePath("/", heapPath, retval.getName().replace("../", "a/../")));
                } else {
                    replacement.append(proxy);
                }
            } else {
                log.warn("{} is referenced as a relative file but not found with in the DAO. Keeping same value...", referencedPath);
                replacement.append(referencedPath);
            }
        }

        replacement.append("\"");
        replacement.append(group.substring(start + matcher.group(groupIndex).length()));

        // Return null means we don't change the original nut
        return retval;
    }
}
