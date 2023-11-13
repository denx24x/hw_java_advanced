#!/bin/bash
javac -classpath ../../../shared/java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar ../java-solutions/info/kgeorgiy/ja/belozorov/implementor/Implementor.java
cd ../java-solutions
jar cfm ../scripts/implementor.jar ../scripts/MANIFEST.MF info/kgeorgiy/ja/belozorov/implementor/Implementor.class
