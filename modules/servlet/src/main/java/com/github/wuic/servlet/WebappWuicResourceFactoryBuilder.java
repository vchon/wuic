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


package com.github.wuic.servlet;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.exception.WuicRfPropertyNotSupportedException;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.resource.WuicResourceFactoryBuilder;
import com.github.wuic.resource.impl.AbstractWuicResourceFactory;
import com.github.wuic.resource.impl.AbstractWuicResourceFactoryBuilder;
import com.github.wuic.resource.impl.disk.DiskWuicResourceProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * Builder for resource access on disk.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.0
 */
public class WebappWuicResourceFactoryBuilder extends AbstractWuicResourceFactoryBuilder {

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public WebappWuicResourceFactoryBuilder() {
        this(new WebappWuicResourceFactory(new AbstractWuicResourceFactory.DefaultWuicResourceFactory(null)));
    }

    /**
     * <p>
     * Creates a new instance thanks to an already built factory.
     * </p>
     *
     * @param built the already built factory.
     */
    public WebappWuicResourceFactoryBuilder(final WuicResourceFactory built) {
        super(built);
    }

    /**
     * <p>
     * Creates a new factory supporting regex.
     * </p>
     *
     * @return the regex factory
     */
    @Override
    protected WuicResourceFactoryBuilder newRegexFactoryBuilder() {
        return new WebappWuicResourceFactoryBuilder(
                new WebappWuicResourceFactory(
                        new AbstractWuicResourceFactory.RegexWuicResourceFactory(
                                new DiskWuicResourceProtocol(WuicServlet.servletContext().getRealPath("."))
                        )));
    }


    /**
     * <p>
     * Default webapp factory validating particular properties.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.3.2
     */
    public static class WebappWuicResourceFactory extends AbstractWuicResourceFactory {

        /**
         * Supported properties with their default value.
         */
        private Map<String, Object> supportedProperties;

        /**
         * Delegate concrete implementation.
         */
        private AbstractWuicResourceFactory delegate;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param toDecorate a factory to be decorated
         */
        public WebappWuicResourceFactory(final AbstractWuicResourceFactory toDecorate) {
            super(null);

            delegate = toDecorate;

            // Init default property
            final String path = WuicServlet.servletContext().getRealPath(".");
            supportedProperties = new HashMap<String, Object>();
            supportedProperties.put(ApplicationConfig.WEBAPP_BASE_PATH, path);

            // Set default protocol
            setWuicProtocol(new DiskWuicResourceProtocol(path));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProperty(final String key, final String value) throws WuicRfPropertyNotSupportedException {

            // Try to override an existing property
            if (!supportedProperties.containsKey(key)) {
                throw new WuicRfPropertyNotSupportedException(key, this.getClass());
            } else {
                supportedProperties.put(key, WuicServlet.servletContext().getRealPath(value));
            }

            // Set new protocol with the new property
            setWuicProtocol(new DiskWuicResourceProtocol((String) supportedProperties.get(ApplicationConfig.WEBAPP_BASE_PATH)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Pattern getPattern(final String path) {
            return delegate.getPattern(path);
        }
    }
}