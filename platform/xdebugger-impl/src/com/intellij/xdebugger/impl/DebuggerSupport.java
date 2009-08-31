package com.intellij.xdebugger.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.AbstractDebuggerSession;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.DebuggerToggleActionHandler;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointPanelProvider;
import com.intellij.xdebugger.impl.evaluate.quick.common.QuickEvaluateHandler;
import com.intellij.xdebugger.impl.settings.DebuggerSettingsPanelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public abstract class DebuggerSupport {
  private static final ExtensionPointName<DebuggerSupport> EXTENSION_POINT = ExtensionPointName.create("com.intellij.xdebugger.debuggerSupport");

  @NotNull 
  public static DebuggerSupport[] getDebuggerSupports() {
    return Extensions.getExtensions(EXTENSION_POINT);
  }

  @NotNull
  public abstract BreakpointPanelProvider<?> getBreakpointPanelProvider();

  @NotNull
  public abstract DebuggerSettingsPanelProvider getSettingsPanelProvider();

  @NotNull
  public abstract DebuggerActionHandler getStepOverHandler();

  @NotNull
  public abstract DebuggerActionHandler getStepIntoHandler();

  @NotNull
  public abstract DebuggerActionHandler getSmartStepIntoHandler();

  @NotNull
  public abstract DebuggerActionHandler getStepOutHandler();

  @NotNull
  public abstract DebuggerActionHandler getForceStepOverHandler();

  @NotNull
  public abstract DebuggerActionHandler getForceStepIntoHandler();


  @NotNull
  public abstract DebuggerActionHandler getRunToCursorHandler();

  @NotNull
  public abstract DebuggerActionHandler getForceRunToCursorHandler();


  @NotNull
  public abstract DebuggerActionHandler getResumeActionHandler();

  @NotNull
  public abstract DebuggerActionHandler getPauseHandler();


  @NotNull
  public abstract DebuggerActionHandler getToggleLineBreakpointHandler();


  @NotNull
  public abstract DebuggerActionHandler getShowExecutionPointHandler();

  @NotNull
  public abstract DebuggerActionHandler getEvaluateHandler();

  @NotNull
  public abstract QuickEvaluateHandler getQuickEvaluateHandler();

  @NotNull
  public abstract DebuggerToggleActionHandler getMuteBreakpointsHandler();

  @Nullable
  public abstract AbstractDebuggerSession getCurrentSession(@NotNull Project project);
}