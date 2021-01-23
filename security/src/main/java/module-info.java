module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires guava;
    requires com.google.gson;
    requires java.prefs;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.application;

    opens com.udacity.catpoint.security.service;
}