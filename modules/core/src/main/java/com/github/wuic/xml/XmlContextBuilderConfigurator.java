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


package com.github.wuic.xml;

import com.github.wuic.ContextBuilder;
import com.github.wuic.ContextBuilderConfigurator;
import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.exception.xml.WuicXmlReadException;
import com.github.wuic.nut.NutDaoBuilderFactory;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.UnableToInstantiateException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This configurator implements XML supports for WUIC. It abstracts the way the XML is read and unmarshal with JAXB.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
public abstract class XmlContextBuilderConfigurator extends ContextBuilderConfigurator {

    /**
     * To read wuic.xml content.
     */
    private Unmarshaller unmarshaller;


    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @throws JAXBException if an context can't be initialized
     * @throws WuicXmlReadException if the XML is not well formed
     */
    public XmlContextBuilderConfigurator() throws JAXBException, WuicXmlReadException {
        this(Boolean.TRUE);
    }


    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     * @throws JAXBException if an context can't be initialized
     * @throws WuicXmlReadException if the XML is not well formed
     */
    public XmlContextBuilderConfigurator(final Boolean multiple) throws JAXBException, WuicXmlReadException {
        super(multiple);
        final JAXBContext jc = JAXBContext.newInstance(XmlWuicBean.class);
        unmarshaller = jc.createUnmarshaller();

        try {
            final URL xsd = getClass().getResource("/wuic.xsd");
            unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd));
        } catch (SAXException se) {
            throw new BadArgumentException(new IllegalArgumentException(se));
        }
    }

    /**
     * <p>
     * Configures the heap described in the given bean.
     * </p>
     *
     * @param ctxBuilder the builder to configure
     * @param heap the configuration bean
     * @throws StreamException if any I/O error occurs
     */
    public static void configureHeap(final ContextBuilder ctxBuilder, final XmlHeapBean heap) throws StreamException {
        final String[] paths = heap.getNutPaths() == null ? new String[0] : heap.getNutPaths().toArray(new String[heap.getNutPaths().size()]);
        final String[] nested = configureNestedHeap(ctxBuilder, heap);
        final String[] referenced = getReferencedHeap(heap);

        // Merges referenced and nested heap into one array to give to context builder
        final String[] target = new String[(referenced == null ? 0 : referenced.length) + (nested == null ? 0 : nested.length)];

        // Nested exist
        if (nested != null) {
            System.arraycopy(nested, 0, target, 0, nested.length);
        }

        // Referenced exist
        if (referenced != null) {
            System.arraycopy(referenced, 0, target, nested == null ? 0 : nested.length, referenced.length);
        }

        // The heap is not a composition
        if (target.length == 0) {
            ctxBuilder.heap(heap.getId(), heap.getDaoBuilderId(), paths);
        } else {
            ctxBuilder.heap(heap.getId(), heap.getDaoBuilderId(), target, paths);
        }
    }

    /**
     * <p>
     * Configures the given builder with the specified bean.
     * </p>
     *
     * @param xml the bean
     * @param ctxBuilder the builder
     */
    public static void configureWorkflow(final XmlWuicBean xml, final ContextBuilder ctxBuilder) {
        if (xml.getWorkflows() == null) {
            return;
        }

        // Some additional DAOs where process result is saved
        for (final XmlWorkflowBean workflow : xml.getWorkflows()) {

            // DAO where we can store process result is optional
            if (workflow.getDaoBuilderIds() == null) {
                ctxBuilder.workflow(workflow.getIdPrefix(),
                        workflow.getHeapIdPattern(),
                        workflow.getEngineBuilderIds().toArray(new String[workflow.getEngineBuilderIds().size()]),
                        workflow.getWithoutEngineBuilderIds() == null ?
                                null : workflow.getWithoutEngineBuilderIds().toArray(new String[workflow.getWithoutEngineBuilderIds().size()]),
                        workflow.getUseDefaultEngines());
            } else {
                ctxBuilder.workflow(workflow.getIdPrefix(),
                        workflow.getHeapIdPattern(),
                        workflow.getEngineBuilderIds().toArray(new String[workflow.getEngineBuilderIds().size()]),
                        workflow.getWithoutEngineBuilderIds() == null ?
                                null : workflow.getWithoutEngineBuilderIds().toArray(new String[workflow.getWithoutEngineBuilderIds().size()]),
                        workflow.getUseDefaultEngines(),
                        workflow.getDaoBuilderIds().toArray(new String[workflow.getDaoBuilderIds().size()]));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int internalConfigure(final ContextBuilder ctxBuilder) {
        try {
            // Let's load the wuic.xml file and configure the builder with it
            final XmlWuicBean xml = unmarshal(unmarshaller);

            // The DAOs
            if (xml.getDaoBuilders() != null) {
                for (XmlBuilderBean dao : xml.getDaoBuilders()) {
                    ctxBuilder.nutDaoBuilder(dao.getId(), NutDaoBuilderFactory.getInstance().create(dao.getType()), extractProperties(dao));
                }
            }

            // The heaps
            for (final XmlHeapBean heap : xml.getHeaps()) {
                configureHeap(ctxBuilder, heap);
            }

            // The engines
            if (xml.getEngineBuilders() != null) {
                for (XmlBuilderBean engine : xml.getEngineBuilders()) {
                    ctxBuilder.engineBuilder(engine.getId(), EngineBuilderFactory.getInstance().create(engine.getType()), extractProperties(engine));
                }
            }

            configureWorkflow(xml, ctxBuilder);

            return xml.getPollingInterleaveSeconds();
        } catch (JAXBException je) {
            throw new BadArgumentException(new IllegalArgumentException(je));
        } catch (UnableToInstantiateException utiae) {
            throw new BadArgumentException(new IllegalArgumentException(utiae));
        } catch (BuilderPropertyNotSupportedException bpnse) {
            throw new BadArgumentException(new IllegalArgumentException(bpnse));
        } catch (StreamException se) {
            throw new BadArgumentException(new IllegalArgumentException(se));
        }
    }

    /**
     * <p>
     * Gets nested declaration of a heap inside the given heap and configure the given context builder with them.
     * </p>
     *
     * @param ctxBuilder the context builder
     * @param heap the enclosing heap
     * @return the extracted heaps
     * @throws StreamException if an I/O error occurs
     */
    private static String[] configureNestedHeap(final ContextBuilder ctxBuilder, final XmlHeapBean heap) throws StreamException {
        if (heap.getNestedComposition() == null || heap.getNestedComposition().isEmpty()) {
            return null;
        } else {
            final String[] retval = new String[heap.getNestedComposition().size()];

            for (int i = 0; i < heap.getNestedComposition().size(); i++) {
                final XmlHeapBean nested = heap.getNestedComposition().get(i);
                final String[] paths = nested.getNutPaths() == null ? new String[0] : nested.getNutPaths().toArray(new String[nested.getNutPaths().size()]);
                ctxBuilder.heap(nested.getId(), nested.getDaoBuilderId(), paths);
                retval[i] = nested.getId();
            }

            return retval;
        }
    }

    /**
     * <p>
     * Gets referenced declaration of a heap inside the given heap.
     * </p>
     *
     * @param heap the enclosing heap
     * @return the extracted heaps
     */
    private static String[] getReferencedHeap(final XmlHeapBean heap) {
        if (heap.getReferencedComposition() == null || heap.getReferencedComposition().isEmpty()) {
            return null;
        } else {
            final String[] retval = new String[heap.getReferencedComposition().size()];

            for (int i = 0; i < heap.getReferencedComposition().size(); i++) {
                retval[i] = heap.getReferencedComposition().get(i);
            }

            return retval;
        }
    }

    /**
     * <p>
     * Extracts from the given bean a {@link Map} of properties.
     * </p>
     *
     * @param bean the bean
     * @return the {@link Map} associating the property ID to its value
     */
    private Map<String, Object> extractProperties(final XmlBuilderBean bean) {
        if (bean.getProperties() == null) {
            return new HashMap<String, Object>();
        } else {
            final Map<String, Object> retval = new HashMap<String, Object>(bean.getProperties().size());

            for (final XmlPropertyBean propertyBean : bean.getProperties()) {
                retval.put(propertyBean.getKey(), propertyBean.getValue());
            }

            return retval;
        }
    }

    /**
     * <p>
     * Unmashal the {@link XmlWuicBean} with the given unmarhalled.
     * </p>
     *
     * @param unmarshaller the unmarshaller
     * @return the unmarshalled bean
     * @throws JAXBException if the XML can't be read
     */
    protected abstract XmlWuicBean unmarshal(final Unmarshaller unmarshaller) throws JAXBException;
}
