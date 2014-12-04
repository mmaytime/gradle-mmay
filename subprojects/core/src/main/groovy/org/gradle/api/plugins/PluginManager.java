/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Nullable;
import org.gradle.internal.HasInternalProtocol;

/**
 * Facilitates applying plugins and determining which plugins have been applied to a {@link PluginAware} object.
 *
 * @see PluginAware
 * @since 2.3
 */
@Incubating
@HasInternalProtocol
public interface PluginManager {

    /**
     * Applies the plugin with the given ID.
     * <p>
     * Plugins in the {@code "org.gradle"} namespace can be applied directly via name.
     * That is, the following two lines are equivalent…
     * <pre autoTested='true'>
     * apply "org.gradle.java"
     * apply "java"
     * </pre>
     *
     * @param pluginId the ID of the plugin to apply
     */
    @Incubating
    void apply(String pluginId);

    /**
     * Applies the given plugin.
     * <p>
     * The given class should implement the {@link org.gradle.api.Plugin} interface, and be parameterized for a compatible type of {@code this}.
     * <p>
     * The following two lines are equivalent…
     * <pre autoTested='true'>
     * apply org.gradle.api.plugins.JavaPlugin
     * apply "org.gradle.java"
     * </pre>
     *
     * @param type the plugin class to apply
     */
    @Incubating
    void apply(Class<?> type);

    /**
     * Returns the information about the plugin that has been applied with the given name or ID, or null if no plugin has been applied with the given name or ID.
     * <p>
     * Plugins in the {@code "org.gradle"} namespace (that is, core Gradle plugins) can be specified by either name (e.g. {@code "java"}) or ID {@code "org.gradle.java"}.
     * All other plugins must be queried for by their full ID (e.g. {@code "org.company.some-plugin"}).
     * <p>
     * Some Gradle plugins have not yet migrated to fully qualified plugin IDs.
     * Such plugins can be detected with this method by simply using the unqualified ID (e.g. {@code "some-third-party-plugin"}.
     *
     * @param id the plugin name (if in the {@code org.gradle} namespace) or ID
     * @return information about the applied plugin, or {@code null} if no plugin has been applied with the given ID
     * @since 2.3
     */
    @Nullable
    @Incubating
    AppliedPlugin findPlugin(String id);

    /**
     * Returns {@code true} if a plugin with the given ID has already been applied, otherwise {@code false}.
     * <p>
     * See {@link #findPlugin(String)} for information about the {@code nameOrId} parameter.
     *
     * @param id the plugin name (if in the {@code org.gradle} namespace) or ID
     * @return {@code true} if the plugin has been applied
     * @since 2.3
     */
    @Incubating
    boolean hasPlugin(String id);

    /**
     * Executes the given action, potentially in the future, if/when the plugin has been applied.
     * <p>
     * If a plugin with the given ID has already been applied, the given action will be executed immediately.
     * Otherwise, the action will be executed sometime in the future if a plugin with the given ID is applied.
     * <p>
     * See {@link #findPlugin(String)} for information about the {@code nameOrId} parameter.
     *
     * @param id the plugin name (if in the {@code org.gradle} namespace) or ID
     * @param action the action to execute if/when the plugin is applied
     * @since 2.3
     */
    @Incubating
    void withPlugin(String id, Action<? super AppliedPlugin> action);

}
