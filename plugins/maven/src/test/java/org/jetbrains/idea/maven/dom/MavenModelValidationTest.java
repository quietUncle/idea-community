/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.maven.dom;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

public class MavenModelValidationTest extends MavenDomWithIndicesTestCase {
  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();
    importProject("<groupId>test</groupId>" +
                  "<artifactId>project</artifactId>" +
                  "<version>1</version>");
  }

  public void testUnderstandingProjectSchemaWithoutNamespace() throws Exception {
    VfsUtil.saveText(myProjectPom,
                     "<project>" +
                     "  <dep<caret>" +
                     "</project>");

    assertCompletionVariants(myProjectPom, "dependencies", "dependencyManagement");
  }

  public void testUnderstandingProfilesSchemaWithoutNamespace() throws Exception {
    VirtualFile profiles = createProfilesXml("<profile>" +
                                             "  <<caret>" +
                                             "</profile>");

    assertCompletionVariantsInclude(profiles, "id", "activation");
  }

  public void testUnderstandingSettingsSchemaWithoutNamespace() throws Exception {
    VirtualFile settings = updateSettingsXml("<profiles>" +
                                             "  <profile>" +
                                             "    <<caret>" +
                                             "  </profile>" +
                                             "</profiles>");

    assertCompletionVariantsInclude(settings, "id", "activation");
  }

  public void testAbsentModelVersion() throws Throwable {
    VfsUtil.saveText(myProjectPom,
                     "<<error descr=\"'modelVersion' child tag should be defined\">project</error> xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
                     "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                     "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" +
                     "  <artifactId>foo</artifactId>" +
                     "</project>");
    checkHighlighting();
  }

  public void testAbsentArtifactId() throws Throwable {
    VfsUtil.saveText(myProjectPom,
                     "<<error descr=\"'artifactId' child tag should be defined\">project</error> xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
                     "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                     "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" +
                     "  <modelVersion>4.0.0</modelVersion>" +
                     "</project>");
    checkHighlighting();
  }

  public void testUnknownModelVersion() throws Throwable {
    VfsUtil.saveText(myProjectPom,
                     "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
                     "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                     "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" +
                     "  <modelVersion><error descr=\"Unsupported model version. Only version 4.0.0 is supported.\">666</error></modelVersion>" +
                     "  <artifactId>foo</artifactId>" +
                     "</project>");
    checkHighlighting();
  }

  public void testEmptyValues() throws Throwable {
    createProjectPom("<<error>groupId</error>></groupId>" +
                     "<<error>artifactId</error>></artifactId>" +
                     "<<error>version</error>></version>");
    checkHighlighting();
  }
}