package org.vsac
import java.io.IOException
import java.sql.ResultSet;
import java.util.logging.Logger
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpConnectionManager
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.SimpleHttpConnectionManager
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.params.DefaultHttpParams
import org.apache.commons.httpclient.params.HttpClientParams
import org.apache.commons.httpclient.params.HttpConnectionManagerParams
import org.apache.commons.httpclient.params.HttpParams
import org.apache.commons.httpclient.params.HttpConnectionParams
class VSACGroovyClient {
	static final Logger LOG = Logger.getLogger(VSACGroovyClient.class.getName())
	String server
	String service
	String retieriveMultiOIDSService
	HttpClient client
	static final String UTF8_BOM = "\uFEFF";
	static final int TIMEOUT_PERIOD = 5 * 60 * 1000
	static final int REQUEST_TIMEDOUT = 3
	static final int REQUEST_FAILED = 4
	
	/**
	 * Constructor
	 * */
	VSACGroovyClient(String proxyServer, int proxyPort,String vsacServerURL , String vsacServiceURL , String vsacReteriveServiceURL){
		HttpConnectionManager manager = new SimpleHttpConnectionManager()
		manager.setParams(new HttpConnectionManagerParams())
		HttpClientParams params = new HttpClientParams()
		params.setSoTimeout(TIMEOUT_PERIOD)
		HttpClient httpClient = new HttpClient(params, manager)
		httpClient.getHostConfiguration().setProxy(proxyServer, proxyPort)
		client = httpClient
		server = vsacServerURL
		service = vsacServiceURL
		retieriveMultiOIDSService = vsacReteriveServiceURL
	}
	/**
	 *Eight hour Ticket granting service call.
	 *
	 * **/
	String getTicketGrantingTicket(String username, String password)  {
		def eightHourTicket = null
		PostMethod post = new PostMethod(server)
		post.setRequestBody([new NameValuePair("username", username),new NameValuePair("password", password)].toArray(new NameValuePair[2]))
		try    {
			client.executeMethod(post)
			switch (post.getStatusCode())      {
				case 200:
				eightHourTicket = post.getResponseBodyAsString()
				LOG.info("Eight Hours Ticket from VSAC === " + eightHourTicket)
				break
				default:
					LOG.warning("Invalid response code from VSAC server!")
					break
			}
		}    catch (final IOException e)    {
			LOG.warning(e.getMessage())
		}    finally    {
			post.releaseConnection()
		}
		return eightHourTicket
	}
	/**
	 *Five min Ticket granting service call.
	 * **/
	String getServiceTicket(String ticketGrantingTicket)
	{
		if (!ticketGrantingTicket)
			return null
		PostMethod post = new PostMethod("$server/$ticketGrantingTicket")
		post.setRequestBody([new NameValuePair("service", service)].toArray(new NameValuePair[1]))
		def srviceTicketFiveMin = null
		try    {
			client.executeMethod(post)
			switch (post.getStatusCode())      {
				case 200:
				String response = post.getResponseBodyAsString()
				srviceTicketFiveMin = response
				break
				default:
					LOG.warning("Invalid response code ( $post.getStatusCode() ) from VSAC server!")
					break
			}
		}    catch (final IOException e)    {
			LOG.warning(e.getMessage())
		}    finally    {
			post.releaseConnection()
		}
		return srviceTicketFiveMin
	}
	void notNull(Object object, String message)  {
		if (object == null)
			throw new IllegalArgumentException(message)
	}
	/**
	 * Multiple Value Set Retrieval based on oid.
	 * 
	 * **/
	VSACResponseResult getMultipleValueSetsResponseByOID(String oid, String serviceTicket) {
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		GetMethod method = new GetMethod(retieriveMultiOIDSService)
		method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("ticket", serviceTicket),
			new NameValuePair("ReleaseType", "VSAC"),new NameValuePair("IncludeDraft", "yes")
				].toArray(new NameValuePair[4])))
		LOG.info "VSAC URL inside getMultipleValueSetsResponseByOID method : " + method.getURI()
		def responseString = null
		try {
			client.executeMethod(method)
			switch (method.getStatusCode()) {
				case 200:
					InputStreamReader inputStreamReader = new InputStreamReader(method.getResponseBodyAsStream(),
						"UTF-8");
					BufferedReader r = new BufferedReader(inputStreamReader);
					StringBuilder stringBuilder = new StringBuilder();
					boolean firstLine = true;
					for (String s = ""; (s = r.readLine()) != null;) {
						if (firstLine) {
								s = removeUTF8BOM(s);
								firstLine = false;
						}
						stringBuilder.append(s);
					}
					responseString = stringBuilder.toString()
					LOG.info(responseString)
					vsacResponseResult.setXmlPayLoad(responseString)
					break
				default:
					LOG.warning("Invalid response code (" + method.getStatusCode() + ") from VSAC server!")
					break        }
		} catch (Exception e) {
			LOG.warning("EXCEPTION IN VSAC JAR: getTicketGrantingTicket..")
			if (e instanceof java.net.SocketTimeoutException) {
				vsacResponseResult.setFailReason(REQUEST_TIMEDOUT);
			} else {
				vsacResponseResult.setFailReason(REQUEST_FAILED);
			}
		} finally {
			method.releaseConnection()
		}
		return vsacResponseResult
	}
	/**
	 * Multiple Value Set Retrieval based on oid and version.
	 *
	 * **/
	VSACResponseResult getMultipleValueSetsResponseByOIDAndVersion( String oid, String version ,String serviceTicket) {
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		 GetMethod method = new GetMethod(retieriveMultiOIDSService)
		 method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("version", version),
			new NameValuePair("ticket", serviceTicket),
			new NameValuePair("ReleaseType", "VSAC"),new NameValuePair("IncludeDraft", "yes")
				].toArray(new NameValuePair[5])))
		 LOG.info "VSAC URL inside getMultipleValueSetsResponseByOIDAndVersion method : " + method.getURI()
		def responseString = null
		try      {
			client.executeMethod(method)
			switch (method.getStatusCode())        {
				case 200:
					InputStreamReader inputStreamReader = new InputStreamReader(method.getResponseBodyAsStream(),
						"UTF-8");
					BufferedReader r = new BufferedReader(inputStreamReader);
					StringBuilder stringBuilder = new StringBuilder();
					boolean firstLine = true;
					for (String s = ""; (s = r.readLine()) != null;) {
						if (firstLine) {
								s = removeUTF8BOM(s);
								firstLine = false;
						}
						stringBuilder.append(s);
					}
					responseString = stringBuilder.toString()
					LOG.info(responseString)
					vsacResponseResult.setXmlPayLoad(responseString)
					break
				default:
					LOG.warning("Invalid response code (" + method.getStatusCode() + ") from VSAC server!")
					break        }
		}catch (Exception e) {
			LOG.warning("EXCEPTION IN VSAC JAR: getTicketGrantingTicket..")
			if (e instanceof java.net.SocketTimeoutException) {
				vsacResponseResult.setFailReason(REQUEST_TIMEDOUT);
			} else {
				vsacResponseResult.setFailReason(REQUEST_FAILED);
			}
		} finally {
			method.releaseConnection()
		}
		return vsacResponseResult
	}
	/**
	 * Multiple Value Set Retrieval based on oid and effective date.
	 *
	 * **/
	VSACResponseResult getMultipleValueSetsResponseByOIDAndEffectiveDate(String oid, String effectiveDate ,String serviceTicket) {
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		GetMethod method = new GetMethod(retieriveMultiOIDSService)
		method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("effectiveDate", effectiveDate),
			new NameValuePair("ticket", serviceTicket),
			new NameValuePair("ReleaseType", "VSAC"),new NameValuePair("IncludeDraft", "yes")
				].toArray(new NameValuePair[5])))
		 LOG.info "VSAC URL inside getMultipleValueSetsResponseByOIDAndEffectiveDate method : " + method.getURI()
		def responseString = null
		try      {
			client.executeMethod(method)
			switch (method.getStatusCode())        {
				case 200:
					InputStreamReader inputStreamReader = new InputStreamReader(method.getResponseBodyAsStream(),
						"UTF-8");
					BufferedReader r = new BufferedReader(inputStreamReader);
					StringBuilder stringBuilder = new StringBuilder();
					boolean firstLine = true;
					for (String s = ""; (s = r.readLine()) != null;) {
						if (firstLine) {
								s = removeUTF8BOM(s);
								firstLine = false;
						}
						stringBuilder.append(s);
					}
					responseString = stringBuilder.toString()
					LOG.info(responseString)
					vsacResponseResult.setXmlPayLoad(responseString)
					break
				default:
					LOG.warning("Invalid response code (" + method.getStatusCode() + ") from VSAC server!")
					break        }
		}catch (Exception e) {
			LOG.warning("EXCEPTION IN VSAC JAR: getTicketGrantingTicket..")
			if (e instanceof java.net.SocketTimeoutException) {
				vsacResponseResult.setFailReason(REQUEST_TIMEDOUT);
			} else {
				vsacResponseResult.setFailReason(REQUEST_FAILED);
			}
		} finally {
			method.releaseConnection()
		}
		return vsacResponseResult
	}
	
	/**
	 * Method to remove UTF8BOM characters from retrieve xml from VSAC.
	 * */
	private String removeUTF8BOM(String s) {
		if (s.startsWith(UTF8_BOM)) {
			s = s.substring(1);
		}
		return s;
	}
}
