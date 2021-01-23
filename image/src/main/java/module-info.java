module com.udacity.catpoint.image {
    exports com.udacity.catpoint.image.service;
    requires slf4j.api;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    requires java.desktop;
}