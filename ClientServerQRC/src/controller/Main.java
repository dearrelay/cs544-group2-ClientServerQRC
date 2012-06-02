/**
 * 
 */
package controller;

import common.XmlParser;

import client.*;
import server.*;


/**
 * @author Jeremy Glesner
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try
		{
			final XmlParser xmlParser = new XmlParser();
			
//			if (args[0].equalsIgnoreCase("client"))
//			{
//				ClientController cc = new ClientController(xmlParser);
//				cc.start();
//			}
//			else if (args[0].equalsIgnoreCase("server"))
//			{
//				ServerController sc = new ServerController();
//				sc.start();
//			}
			if (args[0].equalsIgnoreCase("secureClient"))
			{
				SecureClientController scc = new SecureClientController(xmlParser, null);
				scc.start();
			}
			else if (args[0].equalsIgnoreCase("secureServer"))
			{
				SecureServerController ssc = new SecureServerController(xmlParser);
				ssc.start();
			}
//			else if (args[0].equalsIgnoreCase("sslTestClient"))
//			{
//				SimpleSSLClient stc = new SimpleSSLClient();
//				stc.begin();
//			}
//			else if (args[0].equalsIgnoreCase("sslTestServer"))
//			{
//				SimpleSSLServer sts = new SimpleSSLServer();
//				sts.begin();
//			}				
		}
		catch(Exception e)
		{
			System.out.println("Please pass in an argument of \"secureClient\" or \"secureServer\"");
			
		}
	}

}
