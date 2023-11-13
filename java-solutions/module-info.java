module info.kgeorgiy.ja.belozorov {
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    requires java.compiler;
    requires java.rmi;
    requires jdk.httpserver;
    exports info.kgeorgiy.ja.belozorov.implementor;
    exports info.kgeorgiy.ja.belozorov.concurrent;
    exports info.kgeorgiy.ja.belozorov.crawler;
    exports info.kgeorgiy.ja.belozorov.hello;
}