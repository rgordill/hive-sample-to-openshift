package com.mycompany.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

public class Example {
    private static final Logger logger = LoggerFactory.getLogger(Example.class);

    public static void main(String[] args) throws Throwable {

        Properties props = new Properties();
        props.load(Example.class.getClassLoader().getResourceAsStream("application.properties"));

        String host = props.getProperty("host");
        int port = Integer.parseInt(props.getProperty("port"));
        String clientId = props.getProperty("clientId");
        String protocol = props.getProperty("protocol");
        String keyStoreFile = props.getProperty("keystoreFile");
        String keyStorePass = props.getProperty("keystorePass");

        Mqtt3AsyncClient client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .sslConfig(getClientSslConfig(keyStoreFile, keyStorePass, protocol))
                .buildAsync();

        String username = props.getProperty("username");
        String password = props.getProperty("password");

        client.connectWith()
                .simpleAuth()
                .username(username)
                .password(password.getBytes())
                .applySimpleAuth()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        logger.error("Connection Exception: ", throwable);
                    } else {
                        logger.info("Client connected");

                        publish(client, props);

                        client.disconnect();
                    }
                });

    }

    private static void publish(Mqtt3AsyncClient client, Properties props) {
        String topic = props.getProperty("topic");
        String content = props.getProperty("content");

        client.publishWith()
                .topic(topic)
                .payload(content.getBytes())
                .send()
                .whenComplete((publish, throwable2) -> {
                    if (throwable2 != null) {
                        logger.error("Publishing Exception: ", throwable2);
                    } else {
                        logger.info("Message published.");
                    }
                });

    }

    private static MqttClientSslConfig getClientSslConfig(
            String keystoreFile,
            String keystorePass,
            String protocol)
            throws GeneralSecurityException, IOException {

        // System.setProperty("jsse.enableSNIExtension", "true");
        // System.setProperty("org.bouncycastle.jsse.client.assumeOriginalHostName",
        // "true");

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (InputStream in = new FileInputStream(keystoreFile)) {
            keystore.load(in, keystorePass.toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePass.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keystore);

        MqttClientSslConfig sslConfig = MqttClientSslConfig.builder()
                .keyManagerFactory(keyManagerFactory)
                .trustManagerFactory(trustManagerFactory)
                .protocols(Arrays.asList(protocol))
                .build();

        return sslConfig;
    }

}
