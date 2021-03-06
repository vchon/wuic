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


package com.github.wuic.test;

import com.github.wuic.NutType;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.core.CompositeNut;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import com.github.wuic.path.DirectoryPath;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * <p>
 * Utility class tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.4
 */
@RunWith(JUnit4.class)
public class UtilityTest extends WuicTest {

    /**
     * Test when nuts with same name are merged.
     */
    @Test
    public void mergeNutTest() {
        final Nut first = Mockito.mock(Nut.class);
        final Nut second = Mockito.mock(Nut.class);
        final Nut third = Mockito.mock(Nut.class);
        final Nut fourth = Mockito.mock(Nut.class);
        final Nut fifth = Mockito.mock(Nut.class);
        final List<Nut> input = Arrays.asList(first, second, third, fourth, fifth);

        Mockito.when(first.getNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(second.getNutType()).thenReturn(NutType.CSS);
        Mockito.when(third.getNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(fourth.getNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(fifth.getNutType()).thenReturn(NutType.CSS);

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.css");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("bar.js");
        Mockito.when(fifth.getName()).thenReturn("baz.css");

        List<Nut> output = CompositeNut.mergeNuts(input);

        Assert.assertEquals(4, output.size());
        Assert.assertEquals("foo.js", output.get(0).getName());
        Assert.assertEquals("foo.css", output.get(1).getName());
        Assert.assertEquals("2bar.js", output.get(2).getName());
        Assert.assertEquals("baz.css", output.get(3).getName());

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.css");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("bar2.js");
        Mockito.when(fifth.getName()).thenReturn("baz.css");

        output = CompositeNut.mergeNuts(input);

        Assert.assertEquals(5, output.size());
        Assert.assertEquals("foo.js", output.get(0).getName());
        Assert.assertEquals("foo.css", output.get(1).getName());
        Assert.assertEquals("bar.js", output.get(2).getName());
        Assert.assertEquals("bar2.js", output.get(3).getName());
        Assert.assertEquals("baz.css", output.get(4).getName());

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.js");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("baz.css");
        Mockito.when(fifth.getName()).thenReturn("baz.css");

        output = CompositeNut.mergeNuts(input);

        Assert.assertEquals(3, output.size());
        Assert.assertEquals("0foo.js", output.get(0).getName());
        Assert.assertEquals("bar.js", output.get(1).getName());
        Assert.assertEquals("3baz.css", output.get(2).getName());
    }

    /**
     * Test path simplification method.
     */
    @Test
    public void simplifyPathTest() {
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo"), "/foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo/"), "/foo/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo"), "foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo/"), "foo/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo/file"), "/foo/file");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo/file/"), "/foo/file/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo/file"), "foo/file");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo/file/"), "foo/file/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/foo/../.."), "/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/foo/../../"), "/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/foo/../../path/../file"), "file");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/foo/../bar/../baz"), "/baz");
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("bar/foo/../../../"));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot(".."));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("/../"));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("../foo"));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("foo/../bar/../../path"));
    }

    /**
     * Test string merge.
     */
    @Test
    public void mergeTest() {
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", "oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo:", "oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo:", ":oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", "foo", }, ":"), "foo:oof:foo");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", "foo", }, null), "foo:ooffoo");
        Assert.assertEquals(StringUtils.merge(new String[] {":", "oof", }, ":"), ":oof");
        Assert.assertEquals(StringUtils.merge(new String[] {":", ":foo:", ":oof", }, ":"), ":foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {":", "foo:", ":oof:", }, ":"), ":foo:oof:");
        Assert.assertEquals(StringUtils.merge(new String[] {"/opt", "data", }, "/"), "/opt/data");
        Assert.assertEquals(StringUtils.merge(new String[] {"http://", "server:80", "", "root" }, "/"), "http://server:80/root");
    }

    /**
     * Be sure that the {@code Map} used internally keep the order of the keys.
     */
    @Test
    public void orderingKeyMapTest() {
        final Map<String, String> map = CollectionUtils.orderedKeyMap();

        map.put("toto", "");
        map.put("titi", "");
        map.put("tata", "");
        map.put("tutu", "");

        int cpt = 0;

        for (String key : map.keySet()) {
            Assert.assertTrue(cpt == 0 ? "toto".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 1 ? "titi".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 2 ? "tata".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 3 ? "tutu".equals(key) : Boolean.TRUE);
            cpt++;
        }
    }

    /**
     * Makes sure the research is correctly performed.
     * 
     * Actually if you have this directory : /foo/oof/js/path.js
     * You have a classpath where root is /foo i.e path.js is retrieved thanks to /oof/js/path.js
     * Your classpath protocol has base directory /oof
     * We need to be very clear about the path evaluated by the regex
     * For instance, /.*.js should returns /js/path.js since /oof is the base path
     * After, that /oof + /js/path.js will result in the exact classpath entry to retrieve
     *
     * @throws IOException if any I/O error occurs
     * @throws StreamException if error occurs during research
     */
    @Test
    public void fileSearchTest() throws IOException, StreamException {

        // Part 1
        final String nanoTime = String.valueOf(System.nanoTime());
        final String tmp = System.getProperty("java.io.tmpdir");
        final String path = IOUtils.mergePath(tmp, nanoTime, "foo");
        final File basePath = new File(path);
        Assert.assertTrue(basePath.mkdirs());

        final File file = File.createTempFile("file", ".js", basePath);

        final DirectoryPath parent = DirectoryPath.class.cast(IOUtils.buildPath(IOUtils.mergePath(tmp, nanoTime)));
        List<String> list = IOUtils.listFile(parent, Pattern.compile(".*js"));

        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), IOUtils.mergePath("foo", file.getName()));

        // Part 2
        final String str = getClass().getResource("/images").toString();
        final String baseDir = str.substring(str.indexOf(":/") + 1);
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir));
        final List<String> listFiles = IOUtils.listFile(directoryPath, Pattern.compile(".*.png"));
        Assert.assertEquals(40, listFiles.size());

        for (String f : listFiles) {
            Assert.assertEquals(directoryPath.getChild(f).getAbsolutePath(), IOUtils.mergePath(directoryPath.getAbsolutePath(), f));
        }
    }

    /**
     * Test difference between sets.
     */
    @Test
    public void differenceTest() {
        final Set<String> first = new HashSet<String>();
        final Set<String> second = new HashSet<String>();
        Set<String> diff = CollectionUtils.difference(first, second);
        Assert.assertTrue(diff.isEmpty());

        first.addAll(Arrays.asList("a", "b", "c", "d"));
        second.addAll(Arrays.asList("a", "b"));
        diff = CollectionUtils.difference(first, second);
        Assert.assertEquals(diff.size(), 2);

        first.clear();
        second.clear();
        first.addAll(Arrays.asList("a", "b"));
        second.addAll(Arrays.asList("a", "b", "c"));
        diff = CollectionUtils.difference(first, second);
        Assert.assertEquals(diff.size(), 1);
        Assert.assertEquals("c", diff.iterator().next());

        first.add("c");
        diff = CollectionUtils.difference(first, second);
        Assert.assertTrue(diff.isEmpty());
    }

    /**
     * Test javascript import.
     *
     * @throws IOException if test fails
     */
    @Test
    public void htmlJavascriptImportTest() throws IOException {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getName()).thenReturn("foo.js");
        Mockito.when(nut.getNutType()).thenReturn(NutType.JAVASCRIPT);
        Assert.assertTrue(HtmlUtil.writeScriptImport(nut, "myPath").contains("\"myPath/foo.js\""));
    }

    /**
     * Test CSS import.
     *
     * @throws IOException if test fails
     */
    @Test
    public void htmlCssImportTest() throws IOException {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getName()).thenReturn("foo.css");
        Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);
        Assert.assertTrue(HtmlUtil.writeScriptImport(nut, "myPath").contains("\"myPath/foo.css\""));
    }

    /**
     * Test URL generation.
     */
    @Test
    public void getUrltTest() {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);

        // Served nut
        Mockito.when(nut.getName()).thenReturn("foo.css");
        Assert.assertTrue(HtmlUtil.getUrl(nut, "myPath").contains("myPath/foo.css"));

        // Absolute path
        Mockito.when(nut.getName()).thenReturn("http:/domain.fr/foo.css");
        Assert.assertTrue(HtmlUtil.getUrl(nut, "myPath").contains("http:/domain.fr/foo.css"));

        // Proxy path
        Mockito.when(nut.getProxyUri()).thenReturn("http://proxy.fr/foo.css");
        Mockito.when(nut.getName()).thenReturn("http:/domain.fr/foo.css");
        Assert.assertTrue(HtmlUtil.getUrl(nut, "myPath").contains("http://proxy.fr/foo.css"));
    }
}
