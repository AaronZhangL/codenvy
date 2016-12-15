/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.onpremises.deploy;

import com.codenvy.api.license.SystemLicenseFilter;
import com.codenvy.api.permission.server.PermissionChecker;
import com.codenvy.auth.sso.client.TokenHandler;
import com.codenvy.onpremises.maintenance.MaintenanceStatusServlet;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import org.eclipse.che.inject.DynaModule;

import static com.codenvy.api.license.SystemLicenseFilter.NO_USER_INTERACTION;

/** Servlet module composer for user dashboard war. */
@DynaModule
public class DashboardServletModule extends ServletModule {

    @Override
    protected void configureServlets() {
        bind(PermissionChecker.class).to(com.codenvy.api.permission.server.HttpPermissionCheckerImpl.class);
        bind(TokenHandler.class).to(com.codenvy.api.permission.server.PermissionTokenHandler.class);
        bind(TokenHandler.class).annotatedWith(Names.named("delegated.handler"))
                                .to(com.codenvy.auth.sso.client.RecoverableTokenHandler.class);

        bindConstant().annotatedWith(Names.named("auth.sso.login_page_url")).to("/site/login");
        bindConstant().annotatedWith(Names.named("auth.sso.cookies_disabled_error_page_url"))
                      .to("/site/error/error-cookies-disabled");

        bindConstant().annotatedWith(Names.named(NO_USER_INTERACTION)).to(false);
        bindConstant().annotatedWith(Names.named("license.system.accept_fair_source_license_page_url"))
                      .to("/site/auth/accept-fair-source-license");
        bindConstant().annotatedWith(Names.named("license.system.fair_source_license_is_not_accepted_error_page_url"))
                      .to("/site/error/fair-source-license-is-not-accepted-error");

        filterRegex("/(?!_sso/).*$").through(com.codenvy.servlet.CacheDisablingFilter.class);

        filterRegex("/(?!_sso/).*$").through(com.codenvy.auth.sso.client.LoginFilter.class);

        filterRegex("/(?!_sso/).*$").through(SystemLicenseFilter.class);

        serve("/scheduled").with(MaintenanceStatusServlet.class);

        install(new com.codenvy.auth.sso.client.deploy.SsoClientServletModule());
    }
}
