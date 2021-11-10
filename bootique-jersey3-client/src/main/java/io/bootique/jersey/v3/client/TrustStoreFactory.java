/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey.v3.client;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.resource.ResourceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

@BQConfig
public class TrustStoreFactory {

    ResourceFactory location;
    String password;


    @BQConfigProperty
    public void setLocation(ResourceFactory location) {
        this.location = location;
    }

    @BQConfigProperty
    public void setPassword(String password) {
        this.password = password;
    }

    public KeyStore createTrustStore() {

        URL url = getLocationUrl();
        char[] passwordChars = getPasswordChars();

        try (InputStream in = url.openStream()) {

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(in, passwordChars);
            return trustStore;

        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            throw new RuntimeException("Error loading client trust store from " + url, e);
        }
    }

    private char[] getPasswordChars() {
        String password = this.password != null ? this.password : "changeit";
        return password.toCharArray();
    }

    private URL getLocationUrl() {
        Objects.requireNonNull(location, "TrustStore 'location' is not specified");
        return this.location.getUrl();
    }
}
