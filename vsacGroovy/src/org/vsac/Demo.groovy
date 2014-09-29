package org.vsac

import java.util.logging.Logger;

class Demo {
	static final Logger LOG = Logger.getLogger(Demo.class.getName())
	static void main(String[] args)  {
			String PROXY_HOSTNAME = "10.100.1.224";
			int PROXY_PORT = 8080;
			String SERVER_TICKET = "https://vsac.nlm.nih.gov/vsac/ws/Ticket";
			String SERVER_MULTIPLE_VALUESET = "https://vsac.nlm.nih.gov/vsac/ws/RetrieveMultipleValueSets?";
			String SERVICE = "http://umlsks.nlm.nih.gov";
		   VSACGroovyClient client = new VSACGroovyClient(PROXY_HOSTNAME,PROXY_PORT,SERVER_TICKET,SERVICE,SERVER_MULTIPLE_VALUESET)
		   def ticketGrantingTicket = client.getTicketGrantingTicket("jnarang","isthatme12#")
		   println "TicketGrantingTicket is $ticketGrantingTicket"
		   String serviceTicket = client.getServiceTicket(ticketGrantingTicket)
		   println "ServiceTicket is $serviceTicket"
		   def oid ="2.16.840.1.113883.3.464.1003.101.11.1080"
		   VSACResponseResult vsacXml = client.getMultipleValueSetsResponseByOID(oid,serviceTicket)
	   }
	

}
