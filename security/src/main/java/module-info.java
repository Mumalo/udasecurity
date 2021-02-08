module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires guava;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.application;

    opens com.udacity.catpoint.security.service;
}