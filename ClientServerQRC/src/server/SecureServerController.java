/**
 *  @author GROUP 2, CS544-900-SPRING12, DREXEL UNIVERSITY
 *  Members: Jeremy Glesner, Dustin Overmiller, Yiqi Ju, Lei Yuan
 *  Project: Advanced Game Message Protocol Implementation
 *  
 *  This is an example of a server side application using the Advanced Game Management Protocol to send messages   
 * 
 *  This application framework originally drew heavily from the following resource:
 *  1. Saleem, Usman. "A Pattern/Framework for Client/Server Programming in Java". Year accessed: 2012, Month accessed: 05, Day accessed: 2.
 *  http://www.developer.com/java/ent/article.php/10933_1356891_2/A-PatternFramework-for-ClientServer-Programming-in-Java.htm
 *  
 *  However, the code has changed significantly since that time. Other contributing resources: 
 *  
 *  2. Oracle Corporation. "Java™ Secure Socket Extension (JSSE) Reference Guide". Java SE Documentation. Year accessed: 2012,
 *  Month accessed: 05, Day accessed: 2. http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html
 *   
 *  3. StackOverflow. "How to get a path to a resource in a Java JAR file". Year accessed: 2012, Month accessed: 05, Day accessed: 2.
 *  http://stackoverflow.com/questions/941754/how-to-get-a-path-to-a-resource-in-a-java-jar-file
 *  
 *  4. IBM. "Custom SSL for advanced JSSE developers". Year accessed: 2012, Month accessed: 05, Day accessed: 2.
 *  http://www.ibm.com/developerworks/java/library/j-customssl/
 *  
 */

package server;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;
import java.util.Observer;
import java.io.*;
import javax.net.ssl.*;
import common.*;
import java.security.*;
import java.security.cert.*;


/**
 *  The SecureServerController Class used to instantiate the server
 *  
 *  @author GROUP 2, CS544-900-SPRING12, DREXEL UNIVERSITY
 *  Members: Jeremy Glesner, Dustin Overmiller, Yiqi Ju, Lei Yuan
 *  Project: Advanced Game Message Protocol Implementation
 */
public class SecureServerController implements Observer {
	private String trustStore=null;
	private String trustStorePassword=null;	
	private String keyStore=null;
	private String keyStorePassword=null;  
	
	/** This vector holds all connected clients. */
	private Vector<ClientModel> clients;
	
	private SSLSocket socket;
	private SSLServerSocket ssocket;  //ServerController Socket
	
	private StartSecureServerControllerThread sst; //inner class
	private SSLServerSocketFactory ssf;
	private ClientModel ClientModel;
	private final XmlParser xmlParser;
	/* logging utility */
	private LogAndPublish logAndPublish;
	

	/** Port number of ServerController. */
	private int port = 0;
	
	/** status for listening */
	private boolean listening;	
	
	/**
   * start - start the server controller
   * @param none
   * @return none
   */
	public void start()
	{
      logAndPublish.write("Starting Server...", true, true);
		try
		{
			startServerController();
         logAndPublish.write("Server started successfully", true, true);
		}
		catch(Exception e)
		{
         logAndPublish.write(e, true, false);		
		}	
	}	
	/**
   * Constructor for SecureServerController
   */
	public SecureServerController(XmlParser xmlParser, LogAndPublish logAndPublish) {
		this.xmlParser = xmlParser;
      this.logAndPublish = logAndPublish;
		this.clients = new Vector<ClientModel>();
	    this.port = Integer.parseInt(this.xmlParser.getServerTagValue("PORT_NUMBER"));
	    this.listening = false;
		this.trustStore=this.xmlParser.getServerTagValue("DEFAULT_TRUSTSTORE");
		this.trustStorePassword=this.xmlParser.getServerTagValue("DEFAULT_TRUSTSTORE_PASSWORD");
		this.keyStore=this.xmlParser.getServerTagValue("DEFAULT_KEYSTORE");
		this.keyStorePassword=this.xmlParser.getServerTagValue("DEFAULT_KEYSTORE_PASSWORD");
		this.logAndPublish.write("Setting port number to:" + this.port, true, false);
		this.logAndPublish.write("Using TrustStore: " + this.trustStore, true, false);
		this.logAndPublish.write("Setting KeyStore: " + this.keyStore, true, false);
	}

   /**
   * startServerController - start the server controller thread
   * @param none
   * @return none
   */
	public void startServerController() {
		if (!listening) {
			this.sst = new StartSecureServerControllerThread();
	        this.sst.start();
	        this.listening = true;
	    }
	}
	/**
   * stpServerController - stop the server controller
   * @param none
   * @return none
   */
	public void stopServerController() {
	    if (this.listening) {
	        this.sst.stopServerControllerThread();
	        //close all connected clients//

	        Enumeration<ClientModel> e = this.clients.elements();
	        while(e.hasMoreElements())
	        {
			  ClientModel ct = (ClientModel)e.nextElement();
	          ct.stopClient();
	        }
	        this.listening = false;
	    }
	}

	/**
   * update - Observable interface
   * @param observable
   * @param object
   * @return none
   */
	public void update(Observable observable, Object object) {
	    //notified by observables, do cleanup here//
	    this.clients.removeElement(observable);
	}
   /**
   * getPort - get the port number
   * @param none
   * @return int
   */
	public int getPort() {
	    return port;
	}
   /**
   * setPort - set the port number
   * @param port
   * @return none
   */
	public void setPort(int port) {
	    this.port = port;
	}

	 
	/**
	 * Utility HandshakeCompletedListener which simply displays the
	 * certificate presented by the connecting peer.
	 */
	class SimpleHandshakeListener implements HandshakeCompletedListener
	{
		String ident;
		private final LogAndPublish logAndPublish;

	    /**
	     * Constructs a SimpleHandshakeListener with the given
	     * identifier.
	     * @param ident Used to identify output from this Listener.
	     * @param logAndPublish Used to manage log and console publishing
	     */
	    public SimpleHandshakeListener(String ident, LogAndPublish logAndPublish)
	    {
	      this.ident=ident;
	      this.logAndPublish = logAndPublish;
	    }


	    /**
	     * Used to capture and publish X509 certificate information 
	     */
	    public void handshakeCompleted(HandshakeCompletedEvent event)
	    {
	      // Display the peer specified in the certificate.
	      try {
	        X509Certificate cert=(X509Certificate)event.getPeerCertificates()[0];
	        String peer=cert.getSubjectDN().getName();
	        this.logAndPublish.write(""+ident+": Request from "+peer+"\n", true, false);
	      }
	      catch (SSLPeerUnverifiedException pue) {
	    	  this.logAndPublish.write(""+ident+": Peer unverified\n", true, false);
	      }
	    }	
	  }

	/** This inner class will keep listening to incoming connections,
	 *  and initiating a ClientModel object for each connection. 
	 *  
	 */
	private class StartSecureServerControllerThread extends Thread {
		private boolean listen;
		
	    public StartSecureServerControllerThread() {
	        this.listen = false;
	    	
	        try 
	        {	        
	        	SecureServerController.this.ssf= getSSLServerSocketFactory();
		    }
		    catch(Exception e)
		    {
		    	System.err.println("Error " + e);
		    }
	    }

	    public void run() {
	        this.listen = true;
	        try 
	        {

	        	SecureServerController.this.ssocket = (SSLServerSocket)SecureServerController.this.ssf.createServerSocket(SecureServerController.this.port);
	        	ssocket.setNeedClientAuth(true);
	        	
	        	
	        	/* CLIENT MANAGEMENT */
	            while (this.listen) {
				//wait for client to connect//

	            	SecureServerController.this.socket = (SSLSocket)SecureServerController.this.ssocket.accept();
	            	String uniqueID = socket.getInetAddress() + ":" + socket.getPort();
	          
    	            HandshakeCompletedListener hcl=new SimpleHandshakeListener(uniqueID, SecureServerController.this.logAndPublish);
    	            SecureServerController.this.socket.addHandshakeCompletedListener(hcl);      
	            	
	            	SecureServerController.this.logAndPublish.write("Client " + uniqueID + " connected using ", true, false);
	                try 
	                {
	                	SSLSession clientSession = socket.getSession();
	                	SecureServerController.this.logAndPublish.write("protocol: " + clientSession.getProtocol() + ", ", true, false);
	                	SecureServerController.this.logAndPublish.write("cipher: " + clientSession.getCipherSuite() + "\n", true, false);
	                	
	                	SecureServerController.this.ClientModel = new ClientModel(SecureServerController.this.socket, SecureServerController.this.xmlParser, SecureServerController.this.logAndPublish);
	                    Thread t = new Thread(SecureServerController.this.ClientModel);
	                    SecureServerController.this.ClientModel.addObserver(SecureServerController.this);
	                    SecureServerController.this.clients.addElement(SecureServerController.this.ClientModel);
	    	            
	                    t.start();
	                } catch (IOException ioe) {
	                    System.err.println("Error " + ioe);
	                }
	            }
	        } catch (IOException ioe) {
	            //I/O error in ServerControllerSocket//
	            this.stopServerControllerThread();
	        }
	    }
   /**
   * stopServerControllerThread - stop the server controller thread
   * @param none
   * @return none
   */
    public void stopServerControllerThread() {
    	try {
    		SecureServerController.this.logAndPublish.write("Stopping the server thread", true, false);
    		SecureServerController.this.ssocket.close();
    	}
    	catch (IOException ioe) {
    		//unable to close ServerControllerSocket
    	}
    	this.listen = false;
    }

		  /**
		   * Provides a SSLSocketFactory which ignores JSSE's choice of truststore,
		   * and instead uses either the hard-coded filename and password, or those
		   * passed in on the command-line.
		   * This method calls out to getTrustManagers() to do most of the
		   * grunt-work. It actually just needs to set up a SSLContext and obtain
		   * the SSLSocketFactory from there.
		   * @return SSLSocketFactory SSLSocketFactory to use
		   */
		  protected SSLServerSocketFactory getSSLServerSocketFactory() throws IOException, GeneralSecurityException
		  {
		    // Call getTrustManagers to get suitable trust managers
		    TrustManager[] tms=getTrustManagers();
		    
		    // Call getKeyManagers (from CustomKeyStoreClient) to get suitable
		    // key managers
		    KeyManager[] kms=getKeyManagers();

		    // Next construct and initialize a SSLContext with the KeyStore and
		    // the TrustStore. We use the default SecureRandom.
		    SSLContext context=SSLContext.getInstance("TLS");
		    context.init(kms, tms, null);

		    // Finally, we get a SocketFactory, and pass it to SimpleSSLClient.
		    SSLServerSocketFactory ssf=context.getServerSocketFactory(); //.getSocketFactory();
		    return ssf;
		  }	

	  /**
	   * Returns an array of TrustManagers, set up to use the required
	   * trustStore. This is pulled out separately so that later  
	   * examples can call it.
	   * This method does the bulk of the work of setting up the custom
	   * trust managers.
	   * @return an array of TrustManagers set up accordingly.
	   */
	  protected TrustManager[] getTrustManagers() throws IOException, GeneralSecurityException
	  {
	    // First, get the default TrustManagerFactory.
	    String alg=TrustManagerFactory.getDefaultAlgorithm();
	    TrustManagerFactory tmFact=TrustManagerFactory.getInstance(alg);
	    
	    // Next, set up the TrustStore to use. We need to load the file into
	    // a KeyStore instance.
	    KeyStore ks=KeyStore.getInstance("jks");
	    
	    ClassLoader classLoader = getClass().getClassLoader();
	    InputStream keystoreStream = classLoader.getResourceAsStream(trustStore); // note, not getSYSTEMResourceAsStream  
	    ks.load(keystoreStream, trustStorePassword.toCharArray());

	    // Now we initialize the TrustManagerFactory with this KeyStore
	    tmFact.init(ks);

	    // And now get the TrustManagers
	    TrustManager[] tms=tmFact.getTrustManagers();
	    return tms;
	  }  

	  /**
	   * Returns an array of KeyManagers, set up to use the required
	   * keyStore. This is pulled out separately so that later  
	   * examples can call it.
	   * This method does the bulk of the work of setting up the custom
	   * trust managers.
	   * @return an array of KeyManagers set up accordingly.
	   */
	  protected KeyManager[] getKeyManagers() throws IOException, GeneralSecurityException
	  {
	    // First, get the default KeyManagerFactory.
	    String alg=KeyManagerFactory.getDefaultAlgorithm();
	    KeyManagerFactory kmFact=KeyManagerFactory.getInstance(alg);
	    
	    // Next, set up the KeyStore to use. We need to load the file into
	    // a KeyStore instance.
	    KeyStore ks=KeyStore.getInstance("jks");

	    ClassLoader classLoader = getClass().getClassLoader();
	    InputStream keystoreStream = classLoader.getResourceAsStream(keyStore); // note, not getSYSTEMResourceAsStream  
	    ks.load(keystoreStream, keyStorePassword.toCharArray()); 

	    // Now we initialize the KeyManagerFactory with this KeyStore
	    kmFact.init(ks, keyStorePassword.toCharArray());

	    // And now get the KeyManagers
	    KeyManager[] kms=kmFact.getKeyManagers();
	    return kms;
	  }
	}
}


