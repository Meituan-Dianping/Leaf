FROM openjdk:8-jdk-alpine

ADD target/leaf.jar /
EXPOSE 8080

# you can put your own leaf.properties file into the '/' path
CMD ["java", "-Xbootclasspath/a:./", "-jar", "leaf.jar"]