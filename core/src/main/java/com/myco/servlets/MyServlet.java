package com.myco.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;

/**
 * @author Code &amp; Theory
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = Servlet.class,
           property = { ServletResolverConstants.SLING_SERVLET_SELECTORS + "=feign",
                        ServletResolverConstants.SLING_SERVLET_SELECTORS + "=httpclient",
                        ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json",
                        ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/list-items",
                        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET })
@Designate(ocd = MyServletConfiguration.class)
public class MyServlet extends SlingSafeMethodsServlet {

    @Reference
    private CryptoSupport cryptoSupport;

    private String host;
    private String username;
    private String password;

    @Activate
    public void activate(final MyServletConfiguration configuration)
            throws
            CryptoException {

        this.host = configuration.host();
        this.username = configuration.username();
        if (this.cryptoSupport.isProtected(configuration.password())) {
            this.password = this.cryptoSupport.unprotect(this.password);
        } else {
            this.password = configuration.password();
        }
    }

    @Override
    protected void doGet(
            @Nonnull
            final SlingHttpServletRequest request,
            @Nonnull
            final SlingHttpServletResponse response)
            throws
            IOException {

        final List<Item> items = StringUtils.equals(request.getRequestPathInfo()
                                                           .getSelectorString(), "feign")
                ? this.getItemsViaFeign()
                : this.getItemsViaHttpClient();

        // output
        final JsonFactory factory = new JsonFactory();
        try (final JsonGenerator generator = factory.createGenerator(response.getOutputStream(), JsonEncoding.UTF8)) {

            generator.writeStartArray();

            for (final Item item : items) {
                generator.writeString(item.getName());
            }

            generator.writeEndArray();
        }

        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(CharEncoding.UTF_8);
    }

    private List<Item> getItemsViaFeign() {

        final ItemService api = Feign.builder()
                                     .decoder(new JacksonDecoder())
                                     .requestInterceptor(new BasicAuthRequestInterceptor(this.username, this.password))
                                     .target(ItemService.class, this.host);
        return api.listItems();
    }

    private List<Item> getItemsViaHttpClient()
            throws
            IOException {

        final CredentialsProvider credentialProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        credentialProvider.setCredentials(AuthScope.ANY, credentials);

        final HttpHost targetHost = HttpHost.create(this.host);

        final AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        final HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialProvider);
        context.setAuthCache(authCache);

        final HttpClient client = HttpClientBuilder.create()
                                                   .build();
        final HttpResponse httpResponse = client.execute(new HttpGet(this.host + "/list-items"), context);
        final int statusCode = httpResponse.getStatusLine()
                                           .getStatusCode();

        List<Item> listItems = Collections.emptyList();
        if (statusCode == HttpStatus.SC_OK) {
            final InputStream content = httpResponse.getEntity()
                                                    .getContent();
            final Item[] items = new ObjectMapper().readValue(content, Item[].class);
            listItems = Arrays.asList(items);
        }
        return listItems;
    }
}
