# derived from http://geekyplatypus.com/packaging-and-serving-your-java-application-with-docker/

docker run --rm -it -v  "$(pwd):/external" -w /external maven mvn archetype:generate "-DgroupId=com.mikechernev.docker.example" "-DartifactId=DockerExample"  "-DarchetypeArtifactId=maven-archetype-webapp" "-DinteractiveMode=false"

docker run --rm -it -v "$(pwd):/project" -w /project maven mvn package
#docker run --rm -it -v "$(pwd):/project"  maven mvn package -f /project


#docker run -it  -p 8083:8080 -v "$(pwd)/target/DockerExample.war:/usr/local/tomcat/webapps/docker-example.war"   tomcat
docker run -it -p 8083:8080  -v "$(pwd)/target/DockerExample.war:/usr/local/tomcat/webapps/ROOT.war"   -v "$(pwd)/target/DockerExample:/usr/local/tomcat/webapps/ROOT" tomcat

docker run --rm -it -v "$(pwd):/project" -v "~/.m2:/root/.m2" -w /project maven mvn package ; docker-compose up



