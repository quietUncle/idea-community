/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.psi.templateLanguages;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.InjectableLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.LanguagePerFileMappings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

/**
 * @author peter
 */
@State(
    name = "TemplateDataLanguageMappings",
    storages = {
        @Storage(id = "default", file = "$PROJECT_FILE$"),
        @Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/templateLanguages.xml", scheme = StorageScheme.DIRECTORY_BASED)
})
public class TemplateDataLanguageMappings extends LanguagePerFileMappings<Language> {

  public static TemplateDataLanguageMappings getInstance(final Project project) {
    return ServiceManager.getService(project, TemplateDataLanguageMappings.class);
  }

  public TemplateDataLanguageMappings(final Project project) {
    super(project);
  }

  protected String serialize(final Language language) {
    return language.getID();
  }

  public List<Language> getAvailableValues() {
    return getTemplateableLanguages();
  }

  @Override
  public Language getMapping(VirtualFile file) {
    final Language mapping = super.getMapping(file);
    if (mapping != null) {
      return mapping;
    }
    return TemplateDataLanguagePatterns.getInstance().getTemplateDataLanguageByFileName(file);
  }

  public static List<Language> getTemplateableLanguages() {
    return ContainerUtil.findAll(Language.getRegisteredLanguages(), new Condition<Language>() {
      public boolean value(final Language language) {
        if (language == Language.ANY) return false;
        if (language instanceof TemplateLanguage || language instanceof DependentLanguage || language instanceof InjectableLanguage) return false;
        if (language.getBaseLanguage() != null) return value(language.getBaseLanguage());
        return true;
      }
    });
  }

}