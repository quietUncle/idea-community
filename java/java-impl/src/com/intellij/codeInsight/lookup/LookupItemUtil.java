package com.intellij.codeInsight.lookup;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.JavaMethodCallElement;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 06.02.2003
 * Time: 16:05:20
 * To change this template use Options | File Templates.
 */
public class LookupItemUtil{
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.lookup.LookupItemUtil");

  private LookupItemUtil() {
  }

  @Nullable
  public static LookupItem addLookupItem(Collection<LookupItem> set, @NotNull Object object) {
    return addLookupItem(set, object, new CamelHumpMatcher(""));
  }

  @Nullable
  public static LookupItem addLookupItem(Collection<LookupItem> set, @NotNull Object object, PrefixMatcher matcher) {
    if (object instanceof PsiType) {
      PsiType psiType = (PsiType)object;
      for (final LookupItem lookupItem : set) {
        Object o = lookupItem.getObject();
        if (o.equals(psiType)) {
          return lookupItem;
        }
      }
    }

    for (LookupItem lookupItem : set) {
      if(lookupItem.getObject().equals(lookupItem)) return null;
    }
    LookupItem item = objectToLookupItem(object);
    if (matcher.prefixMatches(item)) {
      return set.add(item) ? item : null;
    }
    return null;
  }

  public static List<LookupItem> addLookupItems(Set<LookupItem> set, Object[] objects, PrefixMatcher matcher) {
    final ArrayList<LookupItem> list = new ArrayList<LookupItem>(objects.length);
    for (Object object : objects) {
      LOG.assertTrue(object != null, "Lookup item can't be null!");
      ContainerUtil.addIfNotNull(addLookupItem(set, object, matcher), list);
    }
    return list;
  }

  /**
   * @deprecated
   * @see LookupElementFactory
  */
  public static LookupItem objectToLookupItem(Object object) {
    if (object instanceof LookupItem) return (LookupItem)object;
    if (object instanceof PsiClass) {
      return new JavaPsiClassReferenceElement((PsiClass)object).setTailType(TailType.NONE);
    }
    if (object instanceof PsiMethod) {
      return new JavaMethodCallElement((PsiMethod)object);
    }

    String s = null;
    LookupItem item = new LookupItem(object, "");
    if (object instanceof PsiElement){
      s = PsiUtilBase.getName((PsiElement) object);
    }
    if (object instanceof PsiEnumConstant) {
      item.addLookupStrings(((PsiEnumConstant)object).getName());
    }
    TailType tailType = TailType.NONE;
    if (object instanceof PsiPackage) {
      tailType = TailType.DOT;
      s = StringUtil.notNullize(s);
    }
    else if (object instanceof PsiKeyword) {
      return new KeywordLookupItem((PsiKeyword)object).setTailType(tailType);
    }
    else if (object instanceof PsiExpression) {
      return new ExpressionLookupItem((PsiExpression) object).setTailType(tailType);
    }
    else if (object instanceof PsiType) {
      return PsiTypeLookupItem.createLookupItem((PsiType)object);
    }
    else if (object instanceof PsiMetaData) {
      s = ((PsiMetaData)object).getName();
    }
    else if (object instanceof String) {
      s = (String)object;
    }
    else if (object instanceof Template) {
      s = ((Template) object).getKey();
    }
    else if (object instanceof PresentableLookupValue) {
      s = ((PresentableLookupValue)object).getPresentation();
    }

    if (object instanceof LookupValueWithUIHint && ((LookupValueWithUIHint) object).isBold() || object instanceof PsiKeyword) {
      item.setBold();
    }

    if (s == null) {
      LOG.assertTrue(false, "Null string for object: " + object + " of class " + (object != null ?object.getClass():null));
    }
    if (object instanceof LookupValueWithTail) {
      item.setAttribute(LookupItem.TAIL_TEXT_ATTR, " " + ((LookupValueWithTail)object).getTailText());
    }
    item.setLookupString(s);

    item.setAttribute(CompletionUtil.TAIL_TYPE_ATTR, tailType);
    return item;
  }

}