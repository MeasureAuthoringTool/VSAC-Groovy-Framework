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
import org.apache.commons.httpclient.util.EncodingUtil;
/**
 * @author MAT Team
 *
 */
class VSACGroovyClient {
	static final Logger LOG = Logger.getLogger(VSACGroovyClient.class.getName())
	String server
	String service
	String retieriveMultiOIDSService
	String profileService
	String retieriveVersionListForOidService
	HttpClient client
	static final String UTF8_BOM = "\uFEFF";
	static final int TIMEOUT_PERIOD = 5 * 60 * 1000
	static final int REQUEST_TIMEDOUT = 3
	static final int REQUEST_FAILED = 4
	
	/**
	 * Constructor
	 * */
	VSACGroovyClient(String proxyServer, int proxyPort,String vsacServerURL , String vsacServiceURL , String vsacReteriveServiceURL, String profileServiceURL, String versionServiceURL){
		HttpConnectionManager manager = new SimpleHttpConnectionManager()
		manager.setParams(new HttpConnectionManagerParams())
		HttpClientParams params = new HttpClientParams()
		params.setSoTimeout(TIMEOUT_PERIOD)
		HttpClient httpClient = new HttpClient(params, manager)
		if(proxyServer)
			httpClient.getHostConfiguration().setProxy(proxyServer, proxyPort)
		client = httpClient
		server = vsacServerURL
		service = vsacServiceURL
		retieriveMultiOIDSService = vsacReteriveServiceURL
		profileService = profileServiceURL
		retieriveVersionListForOidService = versionServiceURL
	}
	/**
	 *Eight hour Ticket granting service call.
	 * @param username
	 * @param password
	 * @return 8 Hours Ticket Granting Ticket in String.
	 */
	String getTicketGrantingTicket(String username, String password)  {
		def eightHourTicket = null
		PostMethod post = new PostMethod(server)
		post.setRequestBody([new NameValuePair("username", username),new NameValuePair("password", password)].toArray(new NameValuePair[2]))
		LOG.info "VSAC URL inside getTicketGrantingTicket method : " + post.getURI()
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
	 * @param ticketGrantingTicket
	 * @return Five Min Service Ticket in String.
	 */
	String getServiceTicket(String ticketGrantingTicket)
	{
		if (!ticketGrantingTicket)
			return null
	    def eightMinTicketURL = server +"/"+ticketGrantingTicket
		PostMethod post = new PostMethod(eightMinTicketURL)
		post.setRequestBody([new NameValuePair("service", service)].toArray(new NameValuePair[1]))
		LOG.info "VSAC URL inside getServiceTicket method : " + post.getURI()
		def srviceTicketFiveMin = null
		try    {
			client.executeMethod(post)
			switch (post.getStatusCode())      {
				case 200:
				srviceTicketFiveMin = post.getResponseBodyAsString()
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
	 * Retrieve All profile List.
	 * @param serviceTicket
	 * @return VSACResponseResult
	 */
	VSACResponseResult getProfileList(String serviceTicket){
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		GetMethod method = new GetMethod(profileService);
		method.setQueryString([new NameValuePair("ticket", serviceTicket)].toArray(new NameValuePair[1]))
		LOG.info "VSAC URL inside getProfileList method : " + method.getURI()
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
			LOG.warning("EXCEPTION IN VSAC JAR: getProfileList..")
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
	 * Reterival of Versions for given OID.
	 * @param oid
	 * @param serviceTicket
	 * @return VSACResponseResult
	 */
	VSACResponseResult reteriveVersionListForOid(String oid,String serviceTicket){
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		String versionServiceURL = retieriveVersionListForOidService +  oid+"/versions"
		GetMethod method = new GetMethod(versionServiceURL);
		method.setQueryString([new NameValuePair("ticket", serviceTicket)].toArray(new NameValuePair[1]))
		LOG.info "VSAC URL inside reteriveVersionListForOid method : " + method.getURI()
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
			LOG.warning("EXCEPTION IN VSAC JAR: reteriveVersionListForOid..")
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
	 * Multiple Value Set Retrieval based on oid.
	 * @param oid
	 * @param serviceTicket
	 * @return VSACResponseResult
	 */
	VSACResponseResult getMultipleValueSetsResponseByOID(String oid, String serviceTicket, String profile) {
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		GetMethod method = new GetMethod(retieriveMultiOIDSService)
		/*method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("ticket", serviceTicket),
			new NameValuePair("ReleaseType", "VSAC"),new NameValuePair("IncludeDraft", "yes")
				].toArray(new NameValuePair[4])))*/
		
		//method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("ticket", serviceTicket)].toArray(new NameValuePair[2])))
		method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("profile",profile)
			,new NameValuePair("ticket", serviceTicket),new NameValuePair("includeDraft", "yes")].toArray(new NameValuePair[4])))
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
	 * @param oid
	 * @param version
	 * @param serviceTicket
	 * @return VSACResponseResult
	 */
	VSACResponseResult getMultipleValueSetsResponseByOIDAndVersion( String oid, String version ,String serviceTicket) {
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		 GetMethod method = new GetMethod(retieriveMultiOIDSService)
		 /*method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("version", version),
			new NameValuePair("ticket", serviceTicket),
			new NameValuePair("ReleaseType", "VSAC"),new NameValuePair("IncludeDraft", "yes")
				].toArray(new NameValuePair[5])))*/
		 method.setQueryString(([new NameValuePair("id", oid),new NameValuePair("version", version),
			 new NameValuePair("ticket", serviceTicket)
				 ].toArray(new NameValuePair[3])))
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
	 * @param oid
	 * @param effectiveDate
	 * @param serviceTicket
	 * @return VSACResponseResult
	 */
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
	 * Multiple Value Set Retrieval based on oid and Profile Name.
	 * @param oid
	 * @param effectiveDate
	 * @param serviceTicket
	 * @return VSACResponseResult
	 */
	VSACResponseResult getMultipleValueSetsResponseByOIDAndProfile(String oid, String profile ,String serviceTicket) {
		VSACResponseResult vsacResponseResult = new VSACResponseResult()
		if(serviceTicket == null) {
			return null
		}
		GetMethod method = new GetMethod(retieriveMultiOIDSService)
		def queryString = EncodingUtil.formUrlEncode(([new NameValuePair("id", oid),new NameValuePair("profile", profile),
			new NameValuePair("ticket", serviceTicket),new NameValuePair("includeDraft", "yes")].toArray(new NameValuePair[4])), "UTF-8");
		method.setQueryString(queryString)
		LOG.info "VSAC URL inside getMultipleValueSetsResponseByOIDAndEffectiveDate method : " + queryString
		def responseString = null
		try      {
			URLEncoder.encode(method.getQueryString(), "UTF-8")
			LOG.info "VSAC URL inside getMultipleValueSetsResponseByOIDAndEffectiveDate method Encoding done : " + method.getURI()
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
