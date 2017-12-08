@echo off
echo Setting JAVA_HOME

setx JAVA_HOME "C:\Program Files\Java\jdk1.8.0_91"

echo JAVA_HOME: %JAVA_HOME%

echo setting PATH

setx PATH "%JAVA_HOME%\bin;%PATH%"
echo PATH: %PATH%

setx M2_HOME "C:\Users\gaian\apache-maven-3.3.9"
echo M2_HOME : %M2_HOME%
setx MAVEN_OPTS -Xms256m -Xmx512m
echo setting PATH
setx PATH "%M2_HOME%\bin;%PATH%"
echo Display java version
java -version

mvn -e test -Denv.USER=pnarayan@snaplogic.com -Denv.PASSWORD=9241980! -Denv.FILEPATH=E:/Reports/output/SnapsSnaplexreport -Denv.URL=https://elastic.snaplogic.com