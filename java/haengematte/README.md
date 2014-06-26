How to run
==========

1. Install Maven 3
2. $ mvn -version
3. Note the Java version used with Maven
4. $ cd /haengematte/java/haengematte
5. Open pom.xml and ensure the "source" and "target" versions for the "maven-compiler-plugin" match the Java version of Maven
6. $ mvn compile
7. $ mvn exec:java -Dexec.mainClass="com.cloudant.Crud"
