FROM tomcat:8.0.43-jre8
ADD ./target/odata2sparql.v4.war /usr/local/tomcat/webapps/
ADD server.xml /usr/local/tomcat/conf/
EXPOSE 8080
CMD chmod +x /usr/local/tomcat/bin/catalina.sh
CMD ["catalina.sh", "run"]
rain pipes as it 'leaks' 
# http://www.agiletrailblazers.com/blog/how-to-dockerize-an-web-application
# docker build -t odata2sparql.v4 .
# docker images 
# docker tag odata2sparql.v4  myusername/odata2sparql.v4 
# docker push myusername/odata2sparql.v4 
# docker pull myusername/odata2sparql.v4 
# docker run -it -d -p 8080:8080 myusername/odata2sparql.v4 