@echo on
echo Setting JAVA_HOME

setx JAVA_HOME "C:\Program Files\Java\jdk1.8.0_144"

echo JAVA_HOME: %JAVA_HOME%

echo setting PATH

setx PATH "%JAVA_HOME%\bin;%PATH%"
echo PATH: %PATH%

setx M2_HOME "C:\Program Files\apache-maven-3.5.0\bin"
echo M2_HOME : %M2_HOME%
setx MAVEN_OPTS -Xms256m -Xmx512m
echo setting PATH
setx PATH "%M2_HOME%\bin;%PATH%"
echo Display java version
java -version

mvn -e test -Denv.USER=msangar@snaplogic.com -Denv.PASSWORD=Sn@p2015! -Denv.FILEPATH=E:/ -Denv.URL=https://elastic.snaplogic.com

cmd /k