java -cp "lib/*" ^
-Dserver.port=8280 ^
-Dspring.config.location=application.properties ^
-Dspring.cloud.bootstrap.location=bootstrap.properties ^
-Dbootstrap.servers=%KAFKA_SERVERS% ^
-Dlogging.config=logback-spring.xml ^
-Dfeign.atp.executor.url=http://localhost:8180 ^
-Dmessage-broker.url=tcp://localhost:61616?wireFormat.maxInactivityDuration=0 ^
-Dcom.sun.management.jmxremote=true ^
-Dcom.sun.management.jmxremote.port=9090 ^
-Dcom.sun.management.jmxremote.ssl=false ^
-Dcom.sun.management.jmxremote.authenticate=false ^
-Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl ^
-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl ^
-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl ^
org.qubership.automation.itf.Main
