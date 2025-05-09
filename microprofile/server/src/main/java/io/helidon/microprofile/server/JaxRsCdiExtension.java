/*
 * Copyright (c) 2019, 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.microprofile.server;

import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.config.mp.MpConfig;
import io.helidon.jersey.webserver.JaxRsService;
import io.helidon.microprofile.server.HelidonHK2InjectionManagerFactory.InjectionManagerWrapper;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ResourceConfig;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * Configure Jersey related things.
 */
public class JaxRsCdiExtension implements Extension {
    private static final System.Logger LOGGER = System.getLogger(JaxRsCdiExtension.class.getName());

    private final List<JaxRsApplication> applicationMetas = new LinkedList<>();

    private final Set<Class<? extends Application>> applications = new LinkedHashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();
    private final Set<Class<?>> providers = new HashSet<>();
    private final AtomicBoolean setInStone = new AtomicBoolean(false);

    /**
     * Default constructor is required by {@link java.util.ServiceLoader}.
     */
    public JaxRsCdiExtension() {
    }

    private void collectApplications(@Observes ProcessManagedBean<? extends Application> processManagedBean) {
        applications.add(processManagedBean.getAnnotatedBeanClass().getJavaClass());
    }

    private void collectResourceClasses(@Observes ProcessManagedBean<?> processManagedBean) {
        Class<?> resourceClass = processManagedBean.getAnnotatedBeanClass().getJavaClass();
        if (hasAnnotation(resourceClass, Path.class)) {
            if (resourceClass.isInterface()) {
                // we are only interested in classes - interface is most likely a REST client API
                return;
            }
            LOGGER.log(Level.TRACE, () -> "Discovered resource class " + resourceClass.getName());
            resources.add(resourceClass);
        }
    }

    private void collectProviderClasses(@Observes ProcessManagedBean<?> processManagedBean) {
        if (processManagedBean.getAnnotated().isAnnotationPresent(Provider.class)) {
            Class<?> providerClass = processManagedBean.getAnnotatedBeanClass().getJavaClass();
            if (providerClass.isInterface()) {
                // we are only interested in classes
                LOGGER.log(Level.TRACE, () -> "Discovered @Provider interface " + providerClass
                        .getName() + ", ignored as we only support classes");
                return;
            }
            LOGGER.log(Level.TRACE, () -> "Discovered @Provider class " + providerClass.getName());
            providers.add(providerClass);
        }
    }

    // once application scoped starts, we do not allow modification of applications
    void fixApps(@Observes @Priority(PLATFORM_BEFORE) @Initialized(ApplicationScoped.class) Object event) {
        this.setInStone.set(true);
    }

    /**
     * List of applications including discovered and explicitly configured applications.
     * <p>
     * This method should only be called in {@code Initialized(ApplicationScoped.class)} observer methods,
     *  that have a higher priority than {@link io.helidon.microprofile.server.ServerCdiExtension} start server
     *  method.
     *
     * @return list of applications found by CDI
     * @throws java.lang.IllegalStateException in case the list of applications is not yet fixed
     */
    public List<JaxRsApplication> applicationsToRun() throws IllegalStateException {
        if (!setInStone.get()) {
            throw new IllegalStateException("Applications are not yet fixed. This method is only available in "
                                                    + "@Initialized(ApplicationScoped.class) event, before server is started");
        }

        // set of resource and provider classes that were discovered
        Set<Class<?>> allClasses = new HashSet<>();
        allClasses.addAll(resources);
        allClasses.addAll(providers);

        if (applications.isEmpty() && applicationMetas.isEmpty()) {
            // create a synthetic application from all resource classes
            if (!resources.isEmpty()) {
                addSyntheticApp(allClasses);
            }
        }

        // make sure the resources are used as a default if application does not define any
        applicationMetas.addAll(applications
                                        .stream()
                                        .map(appClass -> JaxRsApplication.builder()
                                                .applicationClass(appClass)
                                                .config(ResourceConfig.forApplicationClass(appClass, allClasses))
                                                .build())
                                        .toList());

        applications.clear();
        resources.clear();

        return applicationMetas;
    }

    /**
     * Remove all discovered applications (configured applications are not removed).
     *
     * @throws java.lang.IllegalStateException in case applications are already started
     */
    public void removeApplications() throws IllegalStateException {
        mutateApps();
        this.applications.clear();
    }

    /**
     * Remove all discovered and configured resource classes.
     *
     * @throws java.lang.IllegalStateException in case applications are already started
     */
    public void removeResourceClasses() throws IllegalStateException {
        mutateApps();
        this.resources.clear();
    }

    /**
     * Add all resource classes from the list to the list of resource classes discovered through CDI.
     * These may be added to an existing application, or may create a synthetic application, depending
     * on other configuration.
     *
     * @param resourceClasses resource classes to add
     * @throws java.lang.IllegalStateException in case applications are already started
     */
    public void addResourceClasses(List<Class<?>> resourceClasses) throws IllegalStateException {
        mutateApps();
        this.resources.addAll(resourceClasses);
    }

    /**
     * Add all application metadata from the provided list.
     *
     * @param applications application metadata
     * @throws java.lang.IllegalStateException in case applications are already started
     *
     * @see io.helidon.microprofile.server.JaxRsApplication
     */
    public void addApplications(List<JaxRsApplication> applications) throws IllegalStateException {
        mutateApps();
        this.applicationMetas.addAll(applications);
    }

    /**
     * Add a jersey application to the server. Context will be introspected from {@link jakarta.ws.rs.ApplicationPath} annotation.
     * You can also use {@link #addApplication(String, Application)}.
     *
     * @param application configured as needed
     * @throws java.lang.IllegalStateException in case applications are already started
     */
    public void addApplication(Application application) throws IllegalStateException {
        mutateApps();
        this.applicationMetas.add(JaxRsApplication.create(application));
    }

    /**
     * Add a jersey application to the server with an explicit context path.
     *
     * @param contextRoot Context root to use for this application ({@link jakarta.ws.rs.ApplicationPath} is ignored)
     * @param application configured as needed
     * @throws java.lang.IllegalStateException in case applications are already started
     */
    public void addApplication(String contextRoot, Application application) throws IllegalStateException {
        mutateApps();
        this.applicationMetas.add(JaxRsApplication.builder()
                                          .application(application)
                                          .contextRoot(contextRoot)
                                          .build());
    }

    /**
     * Makes an attempt to "guess" the service name.
     * <p>
     * Service name is determined as follows:
     * <ul>
     *     <li>A configuration key {@code service.name}</li>
     *     <li>A configuration key {@code tracing.service}</li>
     *     <li>Name of the first JAX-RS application, if any</li>
     *     <li>{@code helidon-mp}</li>
     * </ul>
     * @return name of this service
     */
    public String serviceName() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("service.name", String.class)
                .or(() -> config.getOptionalValue("tracing.service", String.class))
                .or(this::guessServiceName)
                .orElse("helidon-mp");
    }

    private Optional<String> guessServiceName() {
        if (applicationMetas.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(applicationMetas.get(0).appName());
    }

    /**
     * Create an application from the provided resource classes and add it to the list of applications.
     *
     * @param resourceClasses resource classes to create a synthetic application from
     * @throws java.lang.IllegalStateException in case applications are already started
     */
    public void addSyntheticApplication(List<Class<?>> resourceClasses) throws IllegalStateException {
        mutateApps();
        addSyntheticApp(resourceClasses);
    }

    // set-up synthetic application from resource classes
    private void addSyntheticApp(Collection<Class<?>> resourceClasses) {
        // the classes set must be created before the lambda, as the incoming collection may be mutable
        Set<Class<?>> classes = Set.copyOf(resourceClasses);
        this.applicationMetas.add(JaxRsApplication.builder()
                                          .synthetic(true)
                                          .applicationClass(Application.class)
                                          .config(ResourceConfig.forApplication(new Application() {
                                              @Override
                                              public Set<Class<?>> getClasses() {
                                                  return classes;
                                              }
                                          }))
                                          .appName("HelidonMP")
                                          .build());
    }

    JaxRsService toJerseySupport(JaxRsApplication jaxRsApplication,
                                 InjectionManager injectionManager) {

        ResourceConfig resourceConfig = jaxRsApplication.resourceConfig();

        if (injectionManager == null) {
            return JaxRsService.create(MpConfig.toHelidonConfig(ConfigProvider.getConfig()),
                                       resourceConfig);
        }

        InjectionManager wrappedIm = new InjectionManagerWrapper(injectionManager, resourceConfig);

        return JaxRsService.create(MpConfig.toHelidonConfig(ConfigProvider.getConfig()),
                                   resourceConfig,
                                   wrappedIm);
    }

    Optional<String> findContextRoot(io.helidon.config.Config config, JaxRsApplication jaxRsApplication) {
        return config.get(jaxRsApplication.appClassName() + "." + RoutingPath.CONFIG_KEY_PATH)
                .asString()
                .or(jaxRsApplication::contextRoot)
                .map(path -> (path.startsWith("/") ? path : ("/" + path)));
    }

    Optional<String> findNamedRouting(io.helidon.config.Config config, JaxRsApplication jaxRsApplication) {
        return config.get(jaxRsApplication.appClassName() + "." + RoutingName.CONFIG_KEY_NAME)
                .asString()
                .or(jaxRsApplication::routingName)
                .flatMap(it -> RoutingName.DEFAULT_NAME.equals(it) ? Optional.empty() : Optional.of(it));
    }

    boolean isNamedRoutingRequired(io.helidon.config.Config config, JaxRsApplication jaxRsApplication) {
        return config.get(jaxRsApplication.appClassName() + "." + RoutingName.CONFIG_KEY_REQUIRED)
                .asBoolean()
                .orElseGet(jaxRsApplication::routingNameRequired);
    }

    private void mutateApps() {
        if (setInStone.get()) {
            throw new IllegalStateException("You are attempting to modify applications in JAX-RS after they were registered "
                                                    + "with the server");
        }
    }

    /**
     * Checks presence of annotation on class or any of its supertypes.
     *
     * @param clazz the class
     * @param annotation the annotation
     * @return outcome of test
     */
    private static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz == null || clazz == Object.class) {
            return false;
        }
        if (clazz.isAnnotationPresent(annotation) || hasAnnotation(clazz.getSuperclass(), annotation)) {
            return true;
        }
        for (Class<?> type : clazz.getInterfaces()) {
            if (hasAnnotation(type, annotation)) {
                return true;
            }
        }
        return false;
    }
}
