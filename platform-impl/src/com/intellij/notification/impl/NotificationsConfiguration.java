package com.intellij.notification.impl;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.hash.LinkedHashMap;
import com.intellij.util.messages.MessageBus;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author spleaner
 */
@State(name = "NotificationConfiguration", storages = {@Storage(id = "other", file = "$APP_CONFIG$/notifications.xml")})
public class NotificationsConfiguration implements ApplicationComponent, Notifications, PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.notification.impl.NotificationsConfiguration");

  private Map<String, NotificationSettings> myIdToSettingsMap = new LinkedHashMap<String, NotificationSettings>();
  private MessageBus myMessageBus;

  private List<String> mySessionIdList = new ArrayList<String>();

  public NotificationsConfiguration(@NotNull final MessageBus bus) {
    myMessageBus = bus;
  }

  public static NotificationsConfiguration getNotificationsConfiguration() {
    return ApplicationManager.getApplication().getComponent(NotificationsConfiguration.class);
  }

  public static NotificationSettings[] getAllSettings() {
    return getNotificationsConfiguration()._getAllSettings();
  }

  private NotificationSettings[] _getAllSettings() {
    final Collection<NotificationSettings> collection = myIdToSettingsMap.values();
    return collection.toArray(new NotificationSettings[collection.size()]);
  }

  @Nullable
  public static NotificationSettings getSettings(@NotNull final NotificationImpl notification) {
    final NotificationsConfiguration configuration = getNotificationsConfiguration();
    return configuration.myIdToSettingsMap.get(notification.getComponentName());
  }

  @NotNull
  public String getComponentName() {
    return "NotificationsConfiguration";
  }

  public void initComponent() {
    myMessageBus.connect().subscribe(TOPIC, this);
  }

  public void disposeComponent() {
    myIdToSettingsMap.clear();
    mySessionIdList.clear();
  }

  public void register(@NotNull final String componentName, @NotNull final NotificationDisplayType displayType, final boolean canDisable) {
    if (!myIdToSettingsMap.containsKey(componentName)) {
      myIdToSettingsMap.put(componentName, new NotificationSettings(componentName, displayType, canDisable));
      mySessionIdList.add(componentName);
    }
  }

  public void notify(@NotNull final String componentName, @NotNull final String name, @NotNull final String description, @NotNull final NotificationType type, @NotNull final NotificationListener handler) {
    // do nothing
  }

  public void notify(@NotNull final String componentName, @NotNull final String name, @NotNull final String description, @NotNull final NotificationType type, @NotNull final NotificationListener handler, @Nullable final Icon icon) {
    // do nothing
  }

  public Element getState() {
    @NonNls Element element = new Element("NotificationsConfiguration");
    for (NotificationSettings settings : myIdToSettingsMap.values()) {
      element.addContent(settings.save());
    }

    return element;
  }

  public void loadState(final Element state) {
    for (@NonNls Element child : (Iterable<? extends Element>)state.getChildren("notification")) {
      final NotificationSettings settings = NotificationSettings.load(child);
      final String id = settings.getComponentName();
      LOG.assertTrue(!myIdToSettingsMap.containsKey(id), String.format("Settings for '%s' already loaded!", id));

      myIdToSettingsMap.put(id, settings);
    }
  }
}