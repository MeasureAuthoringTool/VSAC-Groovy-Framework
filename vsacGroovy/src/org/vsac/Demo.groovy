package org.vsac

import java.util.logging.Logger;

class Demo {
	static final Logger LOG = Logger.getLogger(Demo.class.getName())
	static void main(String[] args)  {
			/*String PROXY_HOSTNAME = "10.100.1.224";
			int PROXY_PORT = 8080;*/
			String PROXY_HOSTNAME = null;
			int PROXY_PORT = 0;
			String SERVER_TICKET = "https://vsac.nlm.nih.gov/vsac/ws/Ticket";
			//String SERVER_MULTIPLE_VALUESET = "https://vsac.nlm.nih.gov/vsac/ws/RetrieveMultipleValueSets?";
			String SERVER_MULTIPLE_VALUESET ="https://vsac.nlm.nih.gov/vsac/svs/RetrieveMultipleValueSets?";
			String SERVER_SINGLE_VALUE_SET ="https://vsac.nlm.nih.gov/vsac/svs/RetrieveValueSet?"
			String SERVICE = "http://umlsks.nlm.nih.gov";
			String PROFILE_SERVICE = "https://vsac.nlm.nih.gov/vsac/profiles"
			String VERSION_SERVICE = "https://vsac.nlm.nih.gov/vsac/oid/"
		    VSACGroovyClient client = new VSACGroovyClient(PROXY_HOSTNAME,PROXY_PORT,SERVER_TICKET,SERVICE,SERVER_MULTIPLE_VALUESET, PROFILE_SERVICE, VERSION_SERVICE)
		    def ticketGrantingTicket = client.getTicketGrantingTicket("jnarang","isthatme12#")
		    println "TicketGrantingTicket is $ticketGrantingTicket"
		    String serviceTicket = client.getServiceTicket(ticketGrantingTicket)
		   println "ServiceTicket is $serviceTicket"
		   /*def oid ="2.16.840.1.113883.3.464.1003.101.11.1080"*/
		  def oid ="2.16.840.1.113883.3.526.2.39"
		  
		 /* VSACResponseResult vsacOnlyOidXml = client.getMultipleValueSetsResponseByOID(oid,serviceTicket)
		  print vsacOnlyOidXml.getXmlPayLoad()
		  serviceTicket = client.getServiceTicket(ticketGrantingTicket)
		   println "ServiceTicket is $serviceTicket"
		   VSACResponseResult vsacOnlyOidAndVersionXml = client.getMultipleValueSetsResponseByOIDAndVersion(oid,"20150127",serviceTicket)
		  print vsacOnlyOidAndVersionXml.getXmlPayLoad()
		  serviceTicket = client.getServiceTicket(ticketGrantingTicket)
		   println "ServiceTicket is $serviceTicket"*/
		  VSACResponseResult vsacXml = client.getMultipleValueSetsResponseByOIDAndProfile(oid,"MU2 Update 2015-05-01",serviceTicket)
		  print vsacXml.getXmlPayLoad()
		   //  VSACResponseResult abc = client.reteriveVersionListForOid(oid,serviceTicket);
			//	  print abc.getXmlPayLoad()
		 // VSACResponseResult vsacXml = client.getProfileList(serviceTicket)
		  //print vsacXml.getXmlPayLoad()
		  
	   }
	

}
