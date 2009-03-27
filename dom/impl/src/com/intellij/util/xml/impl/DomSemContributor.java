/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.util.xml.impl;

import com.intellij.patterns.ElementPattern;
import static com.intellij.patterns.XmlPatterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;
import com.intellij.util.NullableFunction;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.EvaluatedXmlName;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.CustomDomChildrenDescription;
import com.intellij.util.xml.reflect.DomChildrenDescription;
import com.intellij.util.xml.reflect.DomCollectionChildDescription;
import com.intellij.util.xml.reflect.DomFixedChildDescription;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author peter
 */
public class DomSemContributor extends SemContributor {
  private final SemService mySemService;
  private final DomManagerImpl myDomManager;

  public DomSemContributor(SemService semService, DomManager domManager) {
    mySemService = semService;
    myDomManager = (DomManagerImpl)domManager;
  }

  public void registerSemProviders(SemRegistrar registrar) {
    registrar.registerSemElementProvider(DomManagerImpl.FILE_DESCRIPTION_KEY, xmlFile(), new NullableFunction<XmlFile, FileDescriptionCachedValueProvider>() {
      public FileDescriptionCachedValueProvider fun(XmlFile xmlFile) {
        return new FileDescriptionCachedValueProvider(myDomManager, xmlFile);
      }
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_HANDLER_KEY, xmlTag().withParent(psiElement(XmlDocument.class).withParent(xmlFile())), new NullableFunction<XmlTag, DomInvocationHandler>() {
      public DomInvocationHandler fun(XmlTag xmlTag) {
        final FileDescriptionCachedValueProvider provider =
          mySemService.getSemElement(DomManagerImpl.FILE_DESCRIPTION_KEY, xmlTag.getContainingFile());
        assert provider != null;
        final DomFileElementImpl element = provider.getFileElement();
        if (element != null) {
          final DomRootInvocationHandler handler = element.getRootHandler();
          if (handler.getXmlTag() == xmlTag) {
            xmlTag.putUserData(DomManagerImpl.CACHED_DOM_HANDLER, handler);
            return handler;
          }
        }
        return null;
      }
    });

    final ElementPattern<XmlTag> nonRootTag = xmlTag().withParent(xmlTag());
    registrar.registerSemElementProvider(DomManagerImpl.DOM_INDEXED_HANDLER_KEY, nonRootTag, new NullableFunction<XmlTag, IndexedElementInvocationHandler>() {
      public IndexedElementInvocationHandler fun(XmlTag tag) {
        final XmlTag parentTag = PhysicalDomParentStrategy.getParentTag(tag);
        assert parentTag != null;
        DomInvocationHandler parent = mySemService.getSemElement(DomManagerImpl.DOM_HANDLER_KEY, parentTag);
        if (parent == null) return null;

        final String localName = tag.getLocalName();
        final String namespace = tag.getNamespace();

        final DomFixedChildDescription description =
          findChildrenDescription(parent.getGenericInfo().getFixedChildrenDescriptions(), tag, parent);

        if (description != null) {

          final int totalCount = description.getCount();

          int index = 0;
          PsiElement current = tag;
          while (true) {
            current = current.getPrevSibling();
            if (current == null) {
              break;
            }
            if (current instanceof XmlTag) {
              final XmlTag xmlTag = (XmlTag)current;
              if (localName.equals(xmlTag.getName()) && namespace.equals(xmlTag.getNamespace())) {
                index++;
                if (index >= totalCount) {
                  return null;
                }
              }
            }
          }

          final IndexedElementInvocationHandler handler =
            new IndexedElementInvocationHandler(parent.createEvaluatedXmlName(description.getXmlName()), (FixedChildDescriptionImpl)description, index,
                                                new PhysicalDomParentStrategy(tag), myDomManager, namespace);
          tag.putUserData(DomManagerImpl.CACHED_DOM_HANDLER, handler);
          return handler;
        }
        return null;
      }
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_COLLECTION_HANDLER_KEY, nonRootTag, new NullableFunction<XmlTag, CollectionElementInvocationHandler>() {
      public CollectionElementInvocationHandler fun(XmlTag tag) {
        final XmlTag parentTag = PhysicalDomParentStrategy.getParentTag(tag);
        assert parentTag != null;
        DomInvocationHandler parent = mySemService.getSemElement(DomManagerImpl.DOM_HANDLER_KEY, parentTag);
        if (parent == null) return null;

        final DomCollectionChildDescription description = findChildrenDescription(parent.getGenericInfo().getCollectionChildrenDescriptions(), tag, parent);
        if (description != null) {
          final CollectionElementInvocationHandler handler =
            new CollectionElementInvocationHandler(description.getType(), tag, (AbstractCollectionChildDescription)description, parent);
          tag.putUserData(DomManagerImpl.CACHED_DOM_HANDLER, handler);
          return handler;
        }
        return null;
      }
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_CUSTOM_HANDLER_KEY, nonRootTag, new NullableFunction<XmlTag, CollectionElementInvocationHandler>() {
      public CollectionElementInvocationHandler fun(XmlTag tag) {
        if (StringUtil.isEmpty(tag.getName())) return null;

        final XmlTag parentTag = PhysicalDomParentStrategy.getParentTag(tag);
        assert parentTag != null;
        DomInvocationHandler parent = mySemService.getSemElement(DomManagerImpl.DOM_HANDLER_KEY, parentTag);
        if (parent == null) return null;

        final CustomDomChildrenDescription customDescription = parent.getGenericInfo().getCustomNameChildrenDescription();
        if (customDescription == null) return null;

        if (mySemService.getSemElement(DomManagerImpl.DOM_INDEXED_HANDLER_KEY, tag) == null &&
            mySemService.getSemElement(DomManagerImpl.DOM_COLLECTION_HANDLER_KEY, tag) == null) {
            final CollectionElementInvocationHandler handler =
              new CollectionElementInvocationHandler(customDescription.getType(), tag, (AbstractCollectionChildDescription)customDescription, parent);
            tag.putUserData(DomManagerImpl.CACHED_DOM_HANDLER, handler);
            return handler;
        }

        return null;
      }
    });

    registrar.registerSemElementProvider(DomManagerImpl.DOM_ATTRIBUTE_HANDLER_KEY, xmlAttribute(), new NullableFunction<XmlAttribute, AttributeChildInvocationHandler>() {
      public AttributeChildInvocationHandler fun(XmlAttribute attribute) {
        final XmlTag tag = PhysicalDomParentStrategy.getParentTag(attribute);
        final DomInvocationHandler handler = mySemService.getSemElement(DomManagerImpl.DOM_HANDLER_KEY, tag);
        if (handler == null) return null;

        final String localName = attribute.getLocalName();
        for (final AttributeChildDescriptionImpl description : handler.getGenericInfo().getAttributeChildrenDescriptions()) {
          if (description.getXmlName().getLocalName().equals(localName)) {
            final EvaluatedXmlName evaluatedXmlName = handler.createEvaluatedXmlName(description.getXmlName());

            String ns = evaluatedXmlName.getNamespace(tag, handler.getFile());
            //see XmlTagImpl.getAttribute(localName, namespace)
            if (ns.equals(tag.getNamespace()) && localName.equals(attribute.getName()) ||
                ns.equals(attribute.getNamespace())) {
              final AttributeChildInvocationHandler attributeHandler =
                new AttributeChildInvocationHandler(evaluatedXmlName, description, myDomManager,
                                                    new PhysicalDomParentStrategy(attribute));
              attribute.putUserData(DomManagerImpl.CACHED_DOM_HANDLER, attributeHandler);
              return attributeHandler;
            }
          }
        }
        return null;
      }
    });

  }

  @Nullable
  private static <T extends DomChildrenDescription> T findChildrenDescription(Collection<T> descriptions, XmlTag tag, DomInvocationHandler parent) {
    final String localName = tag.getLocalName();
    final String namespace = tag.getNamespace();
    final String qName = tag.getName();

    final XmlFile file = parent.getFile();

    for (final T description : descriptions) {
      final XmlName xmlName = description.getXmlName();

      final EvaluatedXmlName evaluatedXmlName = parent.createEvaluatedXmlName(xmlName);
      if (DomImplUtil.isNameSuitable(evaluatedXmlName, localName, qName, namespace, file)) {
        return description;
      }
    }
    return null;
  }
}