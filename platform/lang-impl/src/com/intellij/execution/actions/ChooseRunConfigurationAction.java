package com.intellij.execution.actions;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.list.PopupListElementRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author spleaner
 */
public class ChooseRunConfigurationAction extends AnAction {
  private static final Icon EDIT_ICON = IconLoader.getIcon("/actions/editSource.png");
  private static final Icon SAVE_ICON = IconLoader.getIcon("/runConfigurations/saveTempConfig.png");

  private Executor myCurrentExecutor;
  private boolean myEditConfiguration;

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(PlatformDataKeys.PROJECT);
    assert project != null;

    final Executor executor = getDefaultExecutor();
    assert executor != null;

    final RunListPopup popup = new RunListPopup(project, new ConfigurationListPopupStep(this, project, String.format("%s", executor.getActionName())));
    registerActions(popup);

    final String adText = getAdText(getAlternateExecutor());
    if (adText != null) {
      popup.setAdText(adText);
    }

    popup.showCenteredInCurrentWindow(project);
  }

  protected String getAdKey() {
    return "run.configuration.alternate.action.ad";
  }

  protected static boolean canRun(@NotNull final Executor executor, final RunnerAndConfigurationSettingsImpl settings) {
    return ExecutionUtil.getRunner(executor.getId(), settings) != null;
  }

  @Nullable
  protected String getAdText(final Executor alternateExecutor) {
    final PropertiesComponent properties = PropertiesComponent.getInstance();
    if (alternateExecutor != null && !properties.isTrueValue(getAdKey())) {
      return String
        .format("Hold %s to %s", KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke("SHIFT")), alternateExecutor.getActionName());
    }

    if (!properties.isTrueValue("run.configuration.edit.ad")) {
      return String.format("Press %s to Edit", KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke("F4")));
    }

    if (!properties.isTrueValue("run.configuration.delete.ad")) {
      return String.format("Press %s to Delete configuration", KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke("DELETE")));
    }

    return null;
  }

  private void registerActions(final RunListPopup popup) {
    popup.registerAction("alternateExecutor", KeyStroke.getKeyStroke("shift pressed SHIFT"), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        myCurrentExecutor = getAlternateExecutor();
        updatePresentation(popup);
      }
    });

    popup.registerAction("restoreDefaultExecutor", KeyStroke.getKeyStroke("released SHIFT"), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        myCurrentExecutor = getDefaultExecutor();
        updatePresentation(popup);
      }
    });


    popup.registerAction("invokeAction", KeyStroke.getKeyStroke("shift ENTER"), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        popup.handleSelect(true);
      }
    });

    popup.registerAction("editConfiguration", KeyStroke.getKeyStroke("F4"), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        myEditConfiguration = true;
        try {
          popup.handleSelect(true);
        }
        finally {
          myEditConfiguration = false;
        }
      }
    });


    popup.registerAction("deleteConfiguration", KeyStroke.getKeyStroke("DELETE"), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        popup.removeSelected();
      }
    });

    popup.registerAction("0Action", KeyStroke.getKeyStroke("0"), createNumberAction(0, popup, getDefaultExecutor()));
    popup.registerAction("0Action_", KeyStroke.getKeyStroke("shift pressed 0"), createNumberAction(0, popup, getAlternateExecutor()));
    popup.registerAction("1Action", KeyStroke.getKeyStroke("1"), createNumberAction(1, popup, getDefaultExecutor()));
    popup.registerAction("1Action_", KeyStroke.getKeyStroke("shift pressed 1"), createNumberAction(1, popup, getAlternateExecutor()));
    popup.registerAction("2Action", KeyStroke.getKeyStroke("2"), createNumberAction(2, popup, getDefaultExecutor()));
    popup.registerAction("2Action_", KeyStroke.getKeyStroke("shift pressed 2"), createNumberAction(2, popup, getAlternateExecutor()));
    popup.registerAction("3Action", KeyStroke.getKeyStroke("3"), createNumberAction(3, popup, getDefaultExecutor()));
    popup.registerAction("3Action_", KeyStroke.getKeyStroke("shift pressed 3"), createNumberAction(3, popup, getAlternateExecutor()));
  }

  private void updatePresentation(ListPopupImpl listPopup) {
    final Executor executor = getCurrentExecutor();
    if (executor != null) {
      listPopup.setCaption(executor.getActionName());
    }
  }

  static void execute(final ItemWrapper itemWrapper, final Executor executor) {
    if (executor == null) {
      return;
    }

    final DataContext dataContext = DataManager.getInstance().getDataContext();
    final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project != null) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          itemWrapper.perform(project, executor, dataContext);
        }
      });
    }
  }

  void editConfiguration(@NotNull final Project project, @NotNull final RunnerAndConfigurationSettingsImpl configuration) {
    final Executor executor = getCurrentExecutor();
    assert executor != null;

    PropertiesComponent.getInstance().setValue("run.configuration.edit.ad", Boolean.toString(true));
    if (RunDialog.editConfiguration(project, configuration, "Edit configuration settings", executor.getActionName(), executor.getIcon())) {
      RunManagerEx.getInstanceEx(project).setSelectedConfiguration(configuration);
      ExecutionUtil.executeConfiguration(project, configuration, executor, DataManager.getInstance().getDataContext());
    }
  }

  private static void deleteConfiguration(final Project project, @NotNull final RunnerAndConfigurationSettingsImpl configurationSettings) {
    final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
    manager.removeConfiguration(configurationSettings);
  }

  @Nullable
  protected Executor getDefaultExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }

  @Nullable
  protected Executor getAlternateExecutor() {
    return ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
  }

  @Nullable
  protected Executor getCurrentExecutor() {
    return myCurrentExecutor == null ? getDefaultExecutor() : myCurrentExecutor;
  }

  @Override
  public void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Project project = e.getData(PlatformDataKeys.PROJECT);

    presentation.setEnabled(true);
    if (project == null || project.isDisposed()) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }

    if (null == getDefaultExecutor()) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }

    presentation.setEnabled(true);
    presentation.setVisible(true);
  }

  private static Action createNumberAction(final int number, final ListPopupImpl listPopup, final Executor executor) {
    return new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        for (Object item : listPopup.getListStep().getValues()) {
          if (item instanceof ItemWrapper && ((ItemWrapper)item).getMnemonic() == number) {
            listPopup.cancel();
            execute((ItemWrapper)item, executor);
          }
        }
      }
    };
  }

  private abstract static class ItemWrapper<T> {
    private T myValue;
    private boolean myDynamic = false;
    private int myMnemonic = -1;

    protected ItemWrapper(@Nullable final T value) {
      myValue = value;
    }

    public int getMnemonic() {
      return myMnemonic;
    }

    public void setMnemonic(int mnemonic) {
      myMnemonic = mnemonic;
    }

    public T getValue() {
      return myValue;
    }

    public boolean isDynamic() {
      return myDynamic;
    }

    public void setDynamic(final boolean b) {
      myDynamic = b;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ItemWrapper)) return false;

      ItemWrapper that = (ItemWrapper)o;

      if (myValue != null ? !myValue.equals(that.myValue) : that.myValue != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myValue != null ? myValue.hashCode() : 0;
    }

    public abstract Icon getIcon();

    public abstract String getText();

    public abstract void perform(@NotNull final Project project, @NotNull final Executor executor, @NotNull final DataContext context);

    @Nullable
    public ConfigurationType getType() {
      return null;
    }

    public boolean available(Executor executor) {
      return false;
    }

    public boolean hasActions() {
      return false;
    }

    public PopupStep getNextStep(Project project, ChooseRunConfigurationAction action) {
      return PopupStep.FINAL_CHOICE;
    }

    public static ItemWrapper wrap(@NotNull final Project project,
                                   @NotNull final RunnerAndConfigurationSettingsImpl settings,
                                   final boolean dynamic) {
      final ItemWrapper result = wrap(project, settings);
      result.setDynamic(dynamic);
      return result;
    }

    public static ItemWrapper wrap(@NotNull final Project project, @NotNull final RunnerAndConfigurationSettingsImpl settings) {
      return new ItemWrapper<RunnerAndConfigurationSettingsImpl>(settings) {
        @Override
        public void perform(@NotNull Project project, @NotNull Executor executor, @NotNull DataContext context) {
          RunManagerEx.getInstanceEx(project).setSelectedConfiguration(getValue());
          ExecutionUtil.executeConfiguration(project, getValue(), executor, DataManager.getInstance().getDataContext());
        }

        @Override
        public ConfigurationType getType() {
          return getValue().getType();
        }

        @Override
        public Icon getIcon() {
          return ExecutionUtil.getConfigurationIcon(project, getValue());
        }

        @Override
        public String getText() {
          if (getMnemonic() != -1) {
            return String.format("%s", getValue().getName());
          }

          return getValue().getName();
        }

        @Override
        public boolean hasActions() {
          return true;
        }

        @Override
        public boolean available(Executor executor) {
          return null != ExecutionUtil.getRunner(executor.getId(), getValue());
        }

        @Override
        public PopupStep getNextStep(@NotNull final Project project, @NotNull final ChooseRunConfigurationAction action) {
          return new ConfigurationActionsStep(project, action, getValue(), isDynamic());
        }
      };
    }

    public boolean canBeDeleted() {
      return !isDynamic() && getValue() != null;
    }
  }

  private static final class ConfigurationListPopupStep extends BaseListPopupStep<ItemWrapper> {
    private Project myProject;
    private ChooseRunConfigurationAction myAction;
    private int myDefaultConfiguration = -1;

    private ConfigurationListPopupStep(@NotNull final ChooseRunConfigurationAction action,
                                       @NotNull final Project project,
                                       @NotNull final String title) {
      super(title, createSettingsList(project));
      myProject = project;
      myAction = action;

      if (-1 == getDefaultOptionIndex()) {
        myDefaultConfiguration = getDynamicIndex();
      }
    }

    private int getDynamicIndex() {
      int i = 0;
      for (final ItemWrapper wrapper : getValues()) {
        if (wrapper.isDynamic()) {
          return i;
        }
        i++;
      }

      return -1;
    }

    @Override
    public boolean isAutoSelectionEnabled() {
      return false;
    }

    private static ItemWrapper[] createSettingsList(@NotNull final Project project) {
      final RunManagerEx manager = RunManagerEx.getInstanceEx(project);

      final List<ItemWrapper> result = new ArrayList<ItemWrapper>();

      final RunnerAndConfigurationSettingsImpl selectedConfiguration = manager.getSelectedConfiguration();
      final List<RunnerAndConfigurationSettingsImpl> contextConfigurations = populateWithDynamicRunners(result, project, manager, selectedConfiguration);

      final ConfigurationType[] factories = manager.getConfigurationFactories();
      for (final ConfigurationType factory : factories) {
        final RunnerAndConfigurationSettingsImpl[] configurations = manager.getConfigurationSettings(factory);
        for (final RunnerAndConfigurationSettingsImpl configuration : configurations) {
          if (!contextConfigurations.contains(configuration)) { // exclude context configuration
            final ItemWrapper wrapped = ItemWrapper.wrap(project, configuration);
            if (configuration == selectedConfiguration) {
              wrapped.setMnemonic(1);
            }

            result.add(wrapped);
          }
        }
      }

      //noinspection unchecked
      final ItemWrapper edit = new ItemWrapper(null) {
        @Override
        public Icon getIcon() {
          return EDIT_ICON;
        }

        @Override
        public String getText() {
          return "Edit configurations...";
        }

        @Override
        public void perform(@NotNull final Project project, @NotNull final Executor executor, @NotNull DataContext context) {
          final EditConfigurationsDialog dialog = new EditConfigurationsDialog(project) {
            @Override
            protected void init() {
              setOKButtonText(executor.getStartActionText());
              setOKButtonIcon(executor.getIcon());

              super.init();
            }
          };

          dialog.show();
          if (dialog.isOK()) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                final RunnerAndConfigurationSettings configuration = RunManager.getInstance(project).getSelectedConfiguration();
                if (configuration instanceof RunnerAndConfigurationSettingsImpl) {
                  if (canRun(executor, (RunnerAndConfigurationSettingsImpl) configuration)) {
                    ExecutionUtil.executeConfiguration(project, (RunnerAndConfigurationSettingsImpl) configuration, executor, DataManager.getInstance().getDataContext());
                  }
                }
              }
            });
          }
        }

        @Override
        public boolean available(Executor executor) {
          return true;
        }
      };

      edit.setMnemonic(0);
      result.add(0, edit);

      return result.toArray(new ItemWrapper[result.size()]);
    }

    @NotNull
    private static List<RunnerAndConfigurationSettingsImpl> populateWithDynamicRunners(final List<ItemWrapper> result,
                                                                                 final Project project,
                                                                                 final RunManagerEx manager,
                                                                                 final RunnerAndConfigurationSettingsImpl selectedConfiguration) {

      final ArrayList<RunnerAndConfigurationSettingsImpl> contextConfigurations = new ArrayList<RunnerAndConfigurationSettingsImpl>();
      final DataContext dataContext = DataManager.getInstance().getDataContext();
      final ConfigurationContext context = new ConfigurationContext(dataContext);

      final List<RuntimeConfigurationProducer> producers = PreferedProducerFind.findPreferredProducers(context.getLocation(), context, false);
      if (producers == null) return Collections.emptyList();

      Collections.sort(producers, new Comparator<RuntimeConfigurationProducer>() {
        public int compare(final RuntimeConfigurationProducer p1, final RuntimeConfigurationProducer p2) {
          return p1.getConfigurationType().getDisplayName().compareTo(p2.getConfigurationType().getDisplayName());
        }
      });

      final RunnerAndConfigurationSettingsImpl[] preferred = {null};

      int i = 2; // selectedConfiguration == null ? 1 : 2;
      for (final RuntimeConfigurationProducer producer : producers) {
        final RunnerAndConfigurationSettingsImpl configuration = producer.getConfiguration();
        if (configuration != null) {
          if (selectedConfiguration != null && configuration.equals(selectedConfiguration)) continue;
          contextConfigurations.add(configuration);

          if (preferred[0] == null) {
            preferred[0] = configuration;
          }

          //noinspection unchecked
          final ItemWrapper wrapper = new ItemWrapper(configuration) {
            @Override
            public Icon getIcon() {
              return IconLoader.getTransparentIcon(ExecutionUtil.getConfigurationIcon(project, configuration), 0.3f);
            }

            @Override
            public String getText() {
              return String.format("%s", configuration.getName());
            }

            @Override
            public boolean available(Executor executor) {
              return canRun(executor, configuration);
            }

            @Override
            public void perform(@NotNull Project project, @NotNull Executor executor, @NotNull DataContext context) {
              manager.setTemporaryConfiguration(configuration);
              RunManagerEx.getInstanceEx(project).setSelectedConfiguration(configuration);
              ExecutionUtil.executeConfiguration(project, configuration, executor, DataManager.getInstance().getDataContext());
            }

            @Override
            public PopupStep getNextStep(@NotNull final Project project, @NotNull final ChooseRunConfigurationAction action) {
              return new ConfigurationActionsStep(project, action, configuration, isDynamic());
            }

            @Override
            public boolean hasActions() {
              return true;
            }
          };

          wrapper.setDynamic(true);
          wrapper.setMnemonic(i);
          result.add(wrapper);
          i++;
        }
      }

      return contextConfigurations;
    }

    @Override
    public ListSeparator getSeparatorAbove(ItemWrapper value) {
      final List<ItemWrapper> configurations = getValues();
      final int index = configurations.indexOf(value);
      if (index > 0 && index <= configurations.size() - 1) {
        final ItemWrapper aboveConfiguration = index == 0 ? null : configurations.get(index - 1);

        if (aboveConfiguration != null && aboveConfiguration.isDynamic() != value.isDynamic()) {
          return new ListSeparator();
        }

        final ConfigurationType currentType = value.getType();
        final ConfigurationType aboveType = aboveConfiguration == null ? null : aboveConfiguration.getType();
        if (aboveType != currentType && currentType != null) {
          return new ListSeparator(); // new ListSeparator(currentType.getDisplayName());
        }
      }

      return null;
    }

    @Override
    public boolean isSpeedSearchEnabled() {
      return true;
    }

    @Override
    public int getDefaultOptionIndex() {
      final RunnerAndConfigurationSettings currentConfiguration = RunManager.getInstance(myProject).getSelectedConfiguration();
      if (currentConfiguration == null && myDefaultConfiguration != -1) {
        return myDefaultConfiguration;
      }

      return currentConfiguration instanceof RunnerAndConfigurationSettingsImpl ? getValues()
        .indexOf(ItemWrapper.wrap(myProject, (RunnerAndConfigurationSettingsImpl)currentConfiguration)) : -1;
    }

    @Override
    public PopupStep onChosen(final ItemWrapper wrapper, boolean finalChoice) {
      if (myAction.myEditConfiguration) {
        final Object o = wrapper.getValue();
        if (o instanceof RunnerAndConfigurationSettingsImpl) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              myAction.editConfiguration(myProject, (RunnerAndConfigurationSettingsImpl)o);
            }
          });

          return FINAL_CHOICE;
        }
      }

      final Executor executor = myAction.getCurrentExecutor();
      assert executor != null;

      if (finalChoice && wrapper.available(executor)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (myAction.getCurrentExecutor() == myAction.getAlternateExecutor()) {
              PropertiesComponent.getInstance().setValue(myAction.getAdKey(), Boolean.toString(true));
            }

            wrapper.perform(myProject, executor, DataManager.getInstance().getDataContext());
          }
        });

        return FINAL_CHOICE;
      }
      else {
        return wrapper.getNextStep(myProject, myAction);
      }
    }

    @Override
    public boolean hasSubstep(ItemWrapper selectedValue) {
      return selectedValue.hasActions();
    }

    @NotNull
    @Override
    public String getTextFor(ItemWrapper value) {
      return value.getText();
    }

    @Override
    public Icon getIconFor(ItemWrapper value) {
      return value.getIcon();
    }
  }

  private static final class ConfigurationActionsStep extends BaseListPopupStep<ActionWrapper> {
    private ConfigurationActionsStep(@NotNull final Project project,
                                     ChooseRunConfigurationAction action,
                                     @NotNull final RunnerAndConfigurationSettingsImpl settings, final boolean dynamic) {
      super("Actions", buildActions(project, action, settings, dynamic));
    }

    private static ActionWrapper[] buildActions(@NotNull final Project project,
                                                final ChooseRunConfigurationAction action,
                                                @NotNull final RunnerAndConfigurationSettingsImpl settings,
                                                final boolean dynamic) {
      final List<ActionWrapper> result = new ArrayList<ActionWrapper>();
      final Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
      for (final Executor executor : executors) {
        final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), settings.getConfiguration());
        if (runner != null) {
          result.add(new ActionWrapper(executor.getActionName(), executor.getIcon()) {
            @Override
            public void perform() {
              final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
              if (dynamic) manager.setTemporaryConfiguration(settings);
              manager.setSelectedConfiguration(settings);
              ExecutionUtil.executeConfiguration(project, settings, executor, DataManager.getInstance().getDataContext());
            }
          });
        }
      }

      result.add(new ActionWrapper("Edit...", EDIT_ICON) {
        @Override
        public void perform() {
          final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
          if (dynamic) manager.setTemporaryConfiguration(settings);
          action.editConfiguration(project, settings);
        }
      });

      if (RunManager.getInstance(project).isTemporary(settings.getConfiguration()) || dynamic) {
        result.add(new ActionWrapper("Save temp configuration", SAVE_ICON) {
          @Override
          public void perform() {
            final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
            if (dynamic) manager.setTemporaryConfiguration(settings);
            manager.makeStable(settings.getConfiguration());
          }
        });
      }

      return result.toArray(new ActionWrapper[result.size()]);
    }

    @Override
    public PopupStep onChosen(final ActionWrapper selectedValue, boolean finalChoice) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          selectedValue.perform();
        }
      });

      return FINAL_CHOICE;
    }

    @Override
    public Icon getIconFor(ActionWrapper aValue) {
      return aValue.getIcon();
    }

    @NotNull
    @Override
    public String getTextFor(ActionWrapper value) {
      return value.getName();
    }
  }

  private abstract static class ActionWrapper {
    private String myName;
    private Icon myIcon;

    private ActionWrapper(String name, Icon icon) {
      myName = name;
      myIcon = icon;
    }

    public abstract void perform();

    public String getName() {
      return myName;
    }

    public Icon getIcon() {
      return myIcon;
    }
  }

  private static class RunListElementRenderer extends PopupListElementRenderer {
    private JLabel myLabel;
    private ListPopupImpl myPopup1;

    private RunListElementRenderer(ListPopupImpl popup) {
      super(popup);

      myPopup1 = popup;
    }

    @Override
    protected JComponent createItemComponent() {
      if (myLabel == null) {
        myLabel = new JLabel();
        myLabel.setPreferredSize(new JLabel("8.").getPreferredSize());
      }
      
      final JComponent result = super.createItemComponent();
      result.add(myLabel, BorderLayout.WEST);
      return result;
    }

    @Override
    protected void customizeComponent(JList list, Object value, boolean isSelected) {
      super.customizeComponent(list, value, isSelected);

      ListPopupStep<Object> step = myPopup1.getListStep();
      boolean isSelectable = step.isSelectable(value);
      myLabel.setEnabled(isSelectable);

      if (isSelected) {
        setSelected(myLabel);
      } else {
        setDeselected(myLabel);
      }

      if (value instanceof ItemWrapper) {
        final int mnemonic = ((ItemWrapper)value).getMnemonic();
        if (mnemonic != -1) {
          myLabel.setText(mnemonic + ".");
          myLabel.setDisplayedMnemonicIndex(0);
        } else {
          myLabel.setText("");
        }
      }
    }
  }

  private static class RunListPopup extends ListPopupImpl {
    private Project myProject_;

    public RunListPopup(final Project project, ListPopupStep step) {
      super(step);
      myProject_ = project;
    }

    @Override
    protected ListCellRenderer getListElementRenderer() {
      return new RunListElementRenderer(this);
    }

    public void removeSelected() {
      final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
      if (!propertiesComponent.isTrueValue("run.configuration.delete.ad")) propertiesComponent.setValue("run.configuration.delete.ad", Boolean.toString(true));

      final int index = getSelectedIndex();
      if (index == -1) {
        return;
      }

      final Object o = getListModel().get(index);
      if (o != null && o instanceof ItemWrapper && ((ItemWrapper)o).canBeDeleted()) {
        deleteConfiguration(myProject_, (RunnerAndConfigurationSettingsImpl) ((ItemWrapper)o).getValue());
        getListModel().deleteItem(o);
        final List<Object> values = getListStep().getValues();
        values.remove(o);

        if (index < values.size()){
          onChildSelectedFor(values.get(index));
        } else if (index - 1 >= 0) {
          onChildSelectedFor(values.get(index - 1));
        }
      }
    }
  }
}