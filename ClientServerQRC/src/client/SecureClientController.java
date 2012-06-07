/**
 *  @author GROUP 2, CS544-900-SPRING12, DREXEL UNIVERSITY
 *  Members: Jeremy Glesner, Dustin Overmiller, Yiqi Ju, Lei Yuan
 *	Project: Advanced Game Message Protocol Implementation
 *
 *  This is an example of a client side application using the Advanced Game Management Protocol to send messages   
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
package client;

import java.io.*;
import java.security.*;
import java.util.ArrayList;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import client.findServer.EchoFinder;
import common.card_game.*;
import common.*;


/**
 *  SecureClientController class manages the logic for the client side
 *  within the AGMP example application.
 *   
 *  @author GROUP 2, CS544-900-SPRING12, DREXEL UNIVERSITY
 *  Members: Jeremy Glesner, Dustin Overmiller, Yiqi Ju, Lei Yuan
 *  Project: Advanced Game Message Protocol Implementation
 *  
 */
public class SecureClientController implements Runnable {

	/* PKI references */
	private String trustStore=null;
	private String trustStorePassword=null;	
	private String keyStore=null;
	private String keyStorePassword=null; 
	
	/* management mechanisms */
    private GameState gameState;
    private MessageParser messageParser;
	private boolean connected;
	private LogAndPublish logAndPublish;
	private long bankAmount;
	private long betAmount;
	
	/* connectivity mechanisms */    
	private SecureClientController c = null;
	private SSLSocketFactory ssf = null;	
    private SSLSocket socket;
	private final XmlParser xmlParser;
	private BufferedReader br;
	private InputStreamReader isr;
    private InputStream oInputStream;
    private OutputStream oOutputStream;	
	private ObjectOutputStream outputStream; 
	private ObjectInputStream inputstream;     
    private int port = 0; 
    private String hostName= null;
    private int m_iVersion = -1;
    private int m_iMinorVersion = -1;
    private int gamePhase = -1;
    
    /**
     * Constructor for this class.
     * @param xmlParser Incoming XML Parser
     * @param fLogger Incoming Logger
     */
    public SecureClientController(XmlParser xmlParser, LogAndPublish logAndPublish) 
    {
        
		/* setup parser */
    	this.xmlParser = xmlParser;
    	this.logAndPublish = logAndPublish;
    	
    	/* instantiate remaining variables */
    	this.gameState = new GameState();
        this.gameState.setState(GameState.LISTENING);
        this.messageParser = new MessageParser();
        this.isr = new InputStreamReader(System.in);
        this.br = new BufferedReader(isr);
	    this.port = Integer.parseInt(this.xmlParser.getClientTagValue("PORT_NUMBER"));
	    this.connected = false;
		this.trustStore=this.xmlParser.getClientTagValue("DEFAULT_TRUSTSTORE");
		this.trustStorePassword=this.xmlParser.getClientTagValue("DEFAULT_TRUSTSTORE_PASSWORD");
		this.keyStore=this.xmlParser.getClientTagValue("DEFAULT_KEYSTORE");
		this.keyStorePassword=this.xmlParser.getClientTagValue("DEFAULT_KEYSTORE_PASSWORD");    
		this.hostName = this.xmlParser.getClientTagValue("HOSTNAME");
		this.m_iVersion = Integer.parseInt(this.xmlParser.getClientTagValue("VERSION"));
		this.m_iMinorVersion = Integer.parseInt(this.xmlParser.getClientTagValue("MINOR_VERSION"));
		this.bankAmount = 0;
		this.gamePhase = -1;
		
		logAndPublish.write("Setting port number to:" + this.port, true, false);
		logAndPublish.write("Using TrustStore: " + this.trustStore, true, false);
		logAndPublish.write("Setting KeyStore: " + this.keyStore, true, false);
	
    }    
    
    /**
     * Accessor method to verify connection state
	 * 
     */       
    public boolean isConnected() {
		return connected;
    }

    /**
     * Accessor method to get port
	 * 
     */    
    public int getPort(){
            return port;
        }

    /**
     * Mutator method to set port
     * 
     * @param int port
	 * 
     */      
    public void setPort(int port){
            this.port = port;
        }

    /**
     * Accessor method to get hostname
	 * 
     */      
    public String getHostName(){
            return hostName;
        }

    /**
     * Mutator method to set hostname
     * 
     * @param String hostName
	 * 
     */       
    public void setHostName(String hostName){
            this.hostName = hostName;
        }   
    
    
    /**
     * start method to initiate Secure Client Connection to an AGMP Server
	 * 
     */   
	public void start()
	{
		
		/* Log and Publish */
		logAndPublish.write("Client connecting to server...", true, true);

		/* instantiate a new SecureClientController */
		try {
			c = new SecureClientController(this.xmlParser, logAndPublish);
	        ssf=getSSLSocketFactory();
		}
		catch(Exception e)
		{
			/* Log and Publish */
			logAndPublish.write(e, true, true);			
		}		
        
		/* Connect to server identified in configuration settings */
		try
		{
	        c.connect(ssf,c.hostName,c.port);
		} catch (IOException e) 
		{
			/* Log and Publish */
			logAndPublish.write(e, true, false);
		}
		
		/* find server using ICMP/TCP methods */
		if (!c.connected) {

			
			try {
				c.findConnect(ssf, port);
			} catch (Exception e) 
			{
				/* Log and Publish */
				logAndPublish.write(e, true, true);				
			}
		}
		
		/* Log and Publish */
		logAndPublish.write("Client connected.", true, true);

	}

    /**
     * run method executes when a new SecureClientController thread, 
     * complete with socket is started 
     */
    public void run() {
       try 
       {
    	   /* Game loop */
    	   while(connected && this.gameState.getState() != GameState.CLOSED) 
    	   {
    		   if (this.gameState.getState() == GameState.LISTENING)
    		   {
    			   logAndPublish.write("Enter Listening State.", false, false);
    			   GameListeningState();
    		   }
    		   else if (this.gameState.getState() == GameState.AUTHENTICATE)
    		   {
    				/* Log and Publish */
    				logAndPublish.write("Enter Authentication State.", false, false);
    				GameAuthenticateState();
    		   	}
 
    		   	else if (this.gameState.getState() == GameState.GAMELIST)
	       		{
    				/* Log and Publish */
    				logAndPublish.write("Enter List State.", false, false);
    				
	       			GameListState();
	       		}    	        		
    		   	else if (this.gameState.getState() == GameState.GAMESET)
        		{
    				/* Log and Publish */
    				logAndPublish.write("Enter Set State.", false, false);
    				
        			GameSetState();
        		}        		
        		
        		else if (this.gameState.getState() == GameState.GAMEPLAY)
        		{
    				/* Log and Publish */
    				logAndPublish.write("Enter Play State.", false, false);
    				
        			GamePlayState();
        		}
				/* Log and Publish */
				logAndPublish.write("Connection status: " + connected, false, false);
				
				/* Log and Publish */
				logAndPublish.write("Game state: " + this.gameState.getState(), false, false);				

        	}
    	   connected = false;
    	   System.exit(0);
		}
		catch(Exception e)
		{
      		/* Log and Publish */
      		logAndPublish.write(e, true, true); 
      		this.disconnect();
      		System.exit(0);
		}	    		   
        finally { connected = false; }
    }
	
	/**
	 * GamePlayState method contains all the logic to interface with the server
	 * during game play
	 */
	private void GamePlayState() 
	{

		/* set variables */
		int command = 0;
		while(this.gameState.getState() == GameState.GAMEPLAY)
		{
			try
			{
				/* minimum ante allowable */
				ServerResponse sr = this.receiveMessage();
				if (this.messageParser.GetVersion(sr.getMessage(), sr.getSize()) != this.m_iVersion)
				{
					logAndPublish.write("Ignoring message with incorrect version number", true, false);
					break;
				}
				
				if (this.messageParser.GetTypeIndicator(sr.getMessage(), sr.getSize()) == MessageParser.TYPE_INDICATOR_GAME)
				{
					if (this.messageParser.GetGameIndicator(sr.getMessage(), sr.getSize()) == MessageParser.GAME_INDICATOR_PLAY_GAME)
					{
						MessageParser.ServerPlayGameMessage svrPlayMsg = this.messageParser.GetServerPlayGameMessage(sr.getMessage(), sr.getSize());			
						if (this.gamePhase == GamePhase.INIT)
						{
							if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_INIT_ACK)
							{
								int min_ante = svrPlayMsg.getAnte();
								this.bankAmount = svrPlayMsg.getBankAmount();
								/* sub-state: Verify player has enough money to play and send NOT_SET*/
								if (this.bankAmount < min_ante)
								{
									logAndPublish.write("You are too poor to play this game.  Returning to game list...", false, true);
									MessageParser.ClientGetGameMessage oMsg1 = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
									this.sendMessage(this.messageParser.CreateClientGetGameMessage(oMsg1));
									this.gameState.setState(GameState.GAMELIST); 
									break;
								}	
							
								// print the server response
								PrintGamePlayMessage(sr);
								// get the ante from the client
								logAndPublish.write("Place your bet.", false, true);
								// make sure the client wants to play
								command = UserSelection("Enter 1 to play or 2 to go to the list of games", 1, 2);
								if (command == 2)
								{
									logAndPublish.write("Sending Get GameList message.", true, false);
									MessageParser.ClientGetGameMessage oMsg1 = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
									this.sendMessage(this.messageParser.CreateClientGetGameMessage(oMsg1));
									this.gameState.setState(GameState.GAMELIST); 
									break;
								}
								/* loop to handle user command line input */
								command = UserSelection("Enter any amount greater than or equal to " + min_ante + " to begin:", min_ante, (int)this.bankAmount);
							
								// send GET_HOLE message and transition to the next phase
								MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_HOLE, (long)command);  
								this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
								logAndPublish.write("Sending GET_HOLE request", true, false);
								this.gamePhase = GamePhase.HOLE;
							}
							else
							{
								logAndPublish.write("Ignoring invalid gameplay response from server", true, false);
							}
						}
						else if (this.gamePhase == GamePhase.HOLE)
						{
							if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_GET_HOLE_ACK)
							{
								PrintGamePlayMessage(sr);
								this.bankAmount = svrPlayMsg.getBankAmount();
								logAndPublish.write("Do you wish to play this hand or fold?", false, true);
								logAndPublish.write("To Play, you must bet twice your ante amount.", false, true);
								/* loop to handle user command line input */
								command = UserSelection("Enter 1 to continue, and 2 to fold.", 1, 2);
								int orig_ante = svrPlayMsg.getAnte();
					
								/* handle user selection */
								if (command == 1)
								{
									if ((orig_ante*2) > bankAmount)
									{
										logAndPublish.write("You do not have enough money to keep playing.  Goodbye.", false, true);
										this.gameState.setState(GameState.GAMELIST);
										MessageParser.ClientGetGameMessage getMsg = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
										this.sendMessage(this.messageParser.CreateClientGetGameMessage(getMsg));
										break;
									}		
									else
									{
										/* send GET_FLOP message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_FLOP, (long)(orig_ante*2));
										this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
										logAndPublish.write("Getting the Flop Cards", true, false);
										this.gamePhase = GamePhase.FLOP;
									}
								}
								else
								{
									// send fold message and go to fold phase
									logAndPublish.write("Sending a fold message", true, false);
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_FOLD, (long)(orig_ante));
									this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
									this.gamePhase = GamePhase.FOLD;
								}
							}
							else if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_INVALID_ANTE_BET)
							{
								int min_ante = svrPlayMsg.getAnte();
								this.bankAmount = svrPlayMsg.getBankAmount();
								/* sub-state: Verify player has enough money to play and send NOT_SET*/
								if (this.bankAmount < min_ante)
								{
									logAndPublish.write("You are too poor to play this game.  Returning to game list...", false, true);
									MessageParser.ClientGetGameMessage oMsg1 = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
									this.sendMessage(this.messageParser.CreateClientGetGameMessage(oMsg1));
									this.gameState.setState(GameState.GAMELIST); 
									break;
								}	
							
								// print the server response
								PrintGamePlayMessage(sr);
								// get the ante from the client
								logAndPublish.write("ERROR: Invalid Ante Bet.", false, true);
								// make sure the client wants to play
								command = UserSelection("Enter 1 to play or 2 to go to the list of games", 1, 2);
								if (command == 2)
								{
									logAndPublish.write("Sending Get GameList message.", true, false);
									MessageParser.ClientGetGameMessage oMsg1 = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
									this.sendMessage(this.messageParser.CreateClientGetGameMessage(oMsg1));
									this.gameState.setState(GameState.GAMELIST); 
									break;
								}
								/* loop to handle user command line input */
								command = UserSelection("Enter any amount greater than or equal to " + min_ante + " to begin:", min_ante, (int)this.bankAmount);
							
								// send GET_HOLE message and transition to the next phase
								MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_HOLE, (long)command);  
								this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
								logAndPublish.write("Sending GET_HOLE request", true, false);
							}
							else
							{
								logAndPublish.write("Ignoring invalid gameplay message", true, false);
							}
						}
						else if (this.gamePhase == GamePhase.FLOP)
						{
							if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_GET_FLOP_ACK)
							{
								int orig_ante = svrPlayMsg.getAnte();
								this.bankAmount = svrPlayMsg.getBankAmount();
								PrintGamePlayMessage(sr);
					            logAndPublish.write("Do you wish to bet, check or fold?", false, true);
					            logAndPublish.write("To bet, you may only bet the original ante amount.", false, true);
								/* loop to handle user command line input */
								command = UserSelection("Enter 1 to bet, 2 to check, and 3 to fold.", 1, 3);
								/* handle user selection */
								if (command == 1) 
								{								
									if ((orig_ante) > bankAmount)
									{
										logAndPublish.write("You do not have enough money to bet, checking.", false, true);
										logAndPublish.write("Sending Get Turn Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_TURN, (long)0);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.TURN;
									} 
									else
									{				
										/* send GET_TURN message */
										logAndPublish.write("Sending Get Turn Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_TURN, (long)orig_ante);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.TURN;
										}
									}
								else if (command == 2) 
								{	
									logAndPublish.write("Sending Get Turn Card message", true, false);
									/* send GET_TURN message */
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_TURN, (long)0);
							        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
									this.gamePhase = GamePhase.TURN;
								}	
								else
								{
									// send fold message and go to fold phase
									logAndPublish.write("Sending a fold message", true, false);
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_FOLD, (long)(orig_ante));
									this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
									this.gamePhase = GamePhase.FOLD;
								}
							}
							else if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_INVALID_HOLE_BET)
							{
								PrintGamePlayMessage(sr);
								this.bankAmount = svrPlayMsg.getBankAmount();
								logAndPublish.write("ERROR: Invalid Hole Bet", false, true);
								logAndPublish.write("Do you wish to play this hand or fold?", false, true);
								logAndPublish.write("To Play, you must bet twice your ante amount.", false, true);
								/* loop to handle user command line input */
								command = UserSelection("Enter 1 to continue, and 2 to return to fold.", 1, 2);
								int orig_ante = svrPlayMsg.getAnte();
					
								/* handle user selection */
								if (command == 1)
								{
									if ((orig_ante*2) > bankAmount)
									{
										logAndPublish.write("You do not have enough money to keep playing.  Goodbye.", false, true);
										this.gameState.setState(GameState.GAMELIST);
										MessageParser.ClientGetGameMessage getMsg = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
										this.sendMessage(this.messageParser.CreateClientGetGameMessage(getMsg));
										break;
									}		
									else
									{
										/* send GET_FLOP message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_FLOP, (long)(orig_ante*2));
										this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
										logAndPublish.write("Getting the Flop Cards", true, false);
										this.gamePhase = GamePhase.FLOP;
									}
								}
								else
								{
									// send fold message and go to fold phase
									logAndPublish.write("Sending a fold message", true, false);
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_FOLD, (long)(orig_ante));
									this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
									this.gamePhase = GamePhase.FOLD;
								}
							}
							else
							{
								logAndPublish.write("Ignoring invalid gameplay message", true, false);
							}
						}
						else if (this.gamePhase == GamePhase.TURN)
						{
							if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_GET_TURN_ACK)
							{
								int orig_ante = svrPlayMsg.getAnte();
								this.bankAmount = svrPlayMsg.getBankAmount();
								PrintGamePlayMessage(sr);
					            logAndPublish.write("Do you wish to bet, check or fold?", false, true);
					            logAndPublish.write("To bet, you may only bet the original ante amount.", false, true);
								/* loop to handle user command line input */
								command = UserSelection("Enter 1 to bet, 2 to check, and 3 to fold.", 1, 3);
								/* handle user selection */
								if (command == 1) 
								{								
									if ((orig_ante) > bankAmount)
									{
										logAndPublish.write("You do not have enough money to bet, checking.", false, true);
										logAndPublish.write("Sending Get River Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_RIVER, (long)0);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.RIVER;
									} 
									else
									{				
										/* send GET_RIVER message */
										logAndPublish.write("Sending Get River Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_RIVER, (long)orig_ante);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.RIVER;
										}
									}
								else if (command == 2) 
								{	
									logAndPublish.write("Sending Get River Card message", true, false);
									/* send GET_TURN message */
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_RIVER, (long)0);
							        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
									this.gamePhase = GamePhase.RIVER;
								}	
								else
								{
									// send fold message and go to fold phase
									logAndPublish.write("Sending a fold message", true, false);
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_FOLD, (long)(orig_ante));
									this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
									this.gamePhase = GamePhase.FOLD;
								}
							}
							else if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_INVALID_FLOP_BET)
							{
								int orig_ante = svrPlayMsg.getAnte();
								this.bankAmount = svrPlayMsg.getBankAmount();
								PrintGamePlayMessage(sr);
								logAndPublish.write("ERROR: Invalid Flop Bet.", false, true);
					            logAndPublish.write("Do you wish to bet, check or fold?", false, true);
					            logAndPublish.write("To bet, you may only bet the original ante amount.", false, true);
								/* loop to handle user command line input */
								command = UserSelection("Enter 1 to bet, 2 to check, and 3 to fold.", 1, 3);
								/* handle user selection */
								if (command == 1) 
								{								
									if ((orig_ante) > bankAmount)
									{
										logAndPublish.write("You do not have enough money to bet, checking.", false, true);
										logAndPublish.write("Sending Get Turn Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_TURN, (long)0);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.TURN;
									} 
									else
									{				
										/* send GET_TURN message */
										logAndPublish.write("Sending Get Turn Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_TURN, (long)orig_ante);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.TURN;
										}
									}
								else if (command == 2) 
								{	
									logAndPublish.write("Sending Get Turn Card message", true, false);
									/* send GET_TURN message */
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_TURN, (long)0);
							        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
									this.gamePhase = GamePhase.TURN;
								}	
								else
								{
									// send fold message and go to fold phase
									logAndPublish.write("Sending a fold message", true, false);
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_FOLD, (long)(orig_ante));
									this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
									this.gamePhase = GamePhase.FOLD;
								}
							}
							else
							{
								logAndPublish.write("Ignoring invalid gameplay message", true, false);
							}
						}
						else if (this.gamePhase == GamePhase.RIVER)
						{
							if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_GET_RIVER_ACK)
							{
								this.bankAmount = svrPlayMsg.getBankAmount();
								PrintGamePlayMessage(sr);
								logAndPublish.write("Do you want to play again?", false, true);
								command = UserSelection("Enter 1 to play again or 2 to go to the game list", 1, 2);
								if (command == 2)
								{
									logAndPublish.write("Sending GameList message", true, false);
									this.gameState.setState(GameState.GAMELIST);
									MessageParser.ClientGetGameMessage getMsg = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
									this.sendMessage(this.messageParser.CreateClientGetGameMessage(getMsg));
									break;
								}
								// send the init message and change to the init phase
								logAndPublish.write("Sending Play Game Init Message", true, false);
			        			MessageParser.ClientPlayGameMessage playMsg = this.messageParser.new ClientPlayGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_INIT,(long)0);
			        			this.sendMessage(this.messageParser.CreateClientPlayGameMessage(playMsg));
			        			this.gamePhase = GamePhase.INIT;
							}
							else if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_INVALID_TURN_BET)
							{
								int orig_ante = svrPlayMsg.getAnte();
								this.bankAmount = svrPlayMsg.getBankAmount();
								PrintGamePlayMessage(sr);
								logAndPublish.write("ERROR: Invalid Turn Bet.", false, true);
					            logAndPublish.write("Do you wish to bet, check or fold?", false, true);
					            logAndPublish.write("To bet, you may only bet the original ante amount.", false, true);
								/* loop to handle user command line input */
								command = UserSelection("Enter 1 to bet, 2 to check, and 3 to fold.", 1, 3);
								/* handle user selection */
								if (command == 1) 
								{								
									if ((orig_ante) > bankAmount)
									{
										logAndPublish.write("You do not have enough money to bet, checking.", false, true);
										logAndPublish.write("Sending Get River Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_RIVER, (long)0);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.RIVER;
									} 
									else
									{				
										/* send GET_RIVER message */
										logAndPublish.write("Sending Get River Card message", true, false);
										/* send GET_TURN message */
										MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_RIVER, (long)orig_ante);
								        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
										this.gamePhase = GamePhase.RIVER;
										}
									}
								else if (command == 2) 
								{	
									logAndPublish.write("Sending Get River Card message", true, false);
									/* send GET_TURN message */
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_GET_RIVER, (long)0);
							        this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));						
									this.gamePhase = GamePhase.RIVER;
								}	
								else
								{
									// send fold message and go to fold phase
									logAndPublish.write("Sending a fold message", true, false);
									MessageParser.ClientPlayGameMessage msg = this.messageParser.new ClientPlayGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_FOLD, (long)(orig_ante));
									this.sendMessage(this.messageParser.CreateClientPlayGameMessage(msg));	
									this.gamePhase = GamePhase.FOLD;
								}
							}
							else
							{
								logAndPublish.write("Ignoring invalid gameplay message", true, false);
							}
						}
						else if (this.gamePhase == GamePhase.FOLD)
						{
							if (svrPlayMsg.getGamePlayResponse() == MessageParser.GAME_PLAY_RESPONSE_FOLD_ACK)
							{
								this.bankAmount = svrPlayMsg.getBankAmount();
								PrintGamePlayMessage(sr);
								logAndPublish.write("Do you want to play again?", false, true);
								command = UserSelection("Enter 1 to play again or 2 to go to the game list", 1, 2);
								if (command == 2)
								{
									logAndPublish.write("Sending GameList message", true, false);
									this.gameState.setState(GameState.GAMELIST);
									MessageParser.ClientGetGameMessage getMsg = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
									this.sendMessage(this.messageParser.CreateClientGetGameMessage(getMsg));
									break;
								}
								// send the init message and change to the init phase
								logAndPublish.write("Sending Play Game Init Message", true, false);
			        			MessageParser.ClientPlayGameMessage playMsg = this.messageParser.new ClientPlayGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_INIT,(long)0);
			        			this.sendMessage(this.messageParser.CreateClientPlayGameMessage(playMsg));
			        			this.gamePhase = GamePhase.INIT;
							}
							else
							{
								logAndPublish.write("Ignoring invalid gameplay message", true, false);
							}
						}
					}
					else
					{
						logAndPublish.write("Ignoring invalid game message", true, false);
					}
				}
				else
				{
					logAndPublish.write("Ignoring invalid type message", true, false);
				}
			} catch(Exception e) {
				logAndPublish.write(e,true,true);
				this.disconnect();
				System.exit(0);
			}
		}
	}
					
	/**
	 * Format and Print ServerGamePlayMessage to the screen
	 * @param msg
	 */
	private void PrintGamePlayMessage (ServerResponse sr) 
	{
	    try
	    {
	        /* verify type as GAME */
	    	if (this.messageParser.GetVersion(sr.getMessage(), sr.getSize()) != this.m_iVersion)
	    	{
	    		logAndPublish.write("Ignoring message with incorrect version", true, false);
	    		return;
	    	}
	        if (this.messageParser.GetTypeIndicator(sr.getMessage(), sr.getSize()) == MessageParser.TYPE_INDICATOR_GAME)
	        {
	        	if (this.messageParser.GetGameIndicator(sr.getMessage(), sr.getSize()) == MessageParser.GAME_INDICATOR_PLAY_GAME)
	        	{
	        		MessageParser.ServerPlayGameMessage msg = this.messageParser.GetServerPlayGameMessage(sr.getMessage(), sr.getSize());
	        		bankAmount = msg.getBankAmount();
	        	   	betAmount = msg.getBetAmount();
	        	   	int gamePlayResponse = msg.getGamePlayResponse();
	        	   	int ante = msg.getAnte();
	        	   	int potSize = (int)msg.getPotSize();      	   	
	        	   	Card pcard1 = msg.getPlayerCard1();
	        	   	Card pcard2 = msg.getPlayerCard2();
	        	   	Card dcard1 = msg.getDealerCard1();
	        	   	Card dcard2 = msg.getDealerCard2();	        
	        	   	Card fcard1 = msg.getFlopCard1();
	        	   	Card fcard2 = msg.getFlopCard2();
	        	   	Card fcard3 = msg.getFlopCard3();		          
	        	   	Card tcard = msg.getTurnCard();
	        	   	Card rcard = msg.getRiverCard();
		          
	        	   	int winner = msg.getWinner();
	        	   	String sWinner = "ERROR";
	        	   	if (winner == 1)
	        	   	{
	        	   		sWinner = "DEALER";
	        	   	}
	        	   	else if (winner == 2)
	        	   	{
	        	   		sWinner = "PLAYER";
	        	   	}
	        	   	else if (winner == 3)
	        	   	{
	        	   		sWinner = "DRAW";
	        	   	}
	        	   	else if (winner == 0)
	        	   	{
	        	   		sWinner = "NOT_SET";
	        	   	}
	        	   	
	        	   	String message = "";
	        	   	message += "\n";
	        	   	message += "Your Bank Amount: " + bankAmount + "\n";
	        	   	message += "Current Ante: " + ante + "\n";
	        	   	message += "Current Bet Amount: " + betAmount + "\n";
	        	   	message += "Current Pot Size: " + potSize + "\n";
	        	   	message += "\n";
	        	   	message += "Table Status:----------------------------------------------------------\n";
	        	   	message += "Player Card 1: " + pcard1.toString() + "\n";
	        	   	message += "Player Card 2: " + pcard2.toString() + "\n";
	        	   	message += "\n";
	        	   	message += "Dealer Card 1: " + dcard1.toString() + "\n";
	        	   	message += "Dealer Card 2: " + dcard2.toString() + "\n";
	        	   	message += "\n";	    	 
	        	   	message += "Flop Card 1: " + fcard1.toString() + "\n";
	        	   	message += "Flop Card 2: " + fcard2.toString() + "\n";
	        	   	message += "Flop Card 3: " + fcard3.toString() + "\n";
	        	   	message += "\n";
	        	   	message += "Turn Card: " + tcard.toString() + "\n";
	        	   	message += "River Card: " + rcard.toString() + "\n";
	        	   	message += "-----------------------------------------------------------------------\n";
	        	   	message += "Winner: " + sWinner + "\n";
	        	
	        	   	/* Log and Publish */
	        	   	logAndPublish.write(message, false, true);  
	        	}
	        	else
	        	{
	        		logAndPublish.write("Ignoring invalid game message", true, false);
	        	}
	        }
	        else
	        {
	        	logAndPublish.write("Ignoring invalid type message", true, false);
	        }
	    } catch (Exception e) {
	    	logAndPublish.write(e,true,true);
	    	this.disconnect();
	    }
	    
	}
	
	
	/**
	 * GameSetState method simply informs the client which game has been set
	 * @throws IOException
	 */
	private void GameSetState() throws IOException 
	{
        /* get server response */
        ServerResponse sr = this.receiveMessage();
        if (this.messageParser.GetVersion(sr.getMessage(), sr.getSize()) != this.m_iVersion)
        {
        	logAndPublish.write("Ignoring message with an incorrect Version", true, false);
        	return;
        }   
		
        /* verify type as SET */
        if (this.messageParser.GetTypeIndicator(sr.getMessage(), sr.getSize()) == MessageParser.TYPE_INDICATOR_GAME)
        {
        	if (this.messageParser.GetGameIndicator(sr.getMessage(), sr.getSize()) == MessageParser.GAME_INDICATOR_SET_GAME)
        	{
        		MessageParser.ServerSetGameMessage msg = this.messageParser.GetServerSetGameMessage(sr.getMessage(), sr.getSize());
        		if (msg.getGameTypeResponse() == MessageParser.GAME_TYPE_RESPONSE_ACK)
        		{
        			// got a valid response
        			// Send the game play init message and switch to the game play state
        			logAndPublish.write("Sending Play Game Init Message", true, false);
        			MessageParser.ClientPlayGameMessage playMsg = this.messageParser.new ClientPlayGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_PLAY_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM, MessageParser.GAME_PLAY_REQUEST_INIT,(long)0);
        			this.sendMessage(this.messageParser.CreateClientPlayGameMessage(playMsg));
        			this.gameState.setState(GameState.GAMEPLAY);
        			this.gamePhase = GamePhase.INIT;
        		}
        		else if (msg.getGameTypeCode() == MessageParser.GAME_TYPE_RESPONSE_INVALID)
        		{
        			// need to resend the get games message and transition to games list state
        			MessageParser.ClientGetGameMessage oMsg1 = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
        			this.sendMessage(this.messageParser.CreateClientGetGameMessage(oMsg1));
        			this.gameState.setState(GameState.GAMELIST); 
        		}
        		else
        		{
        			/* Log and Publish */
        			logAndPublish.write("Ignoring invalid Server Set Game Message", true, false);
        		}
        	}
        	else
        	{
        		/* Log and Publish */
    			logAndPublish.write("Ignoring invalid Server Game Message", true, false);
        	}
        }
        else
        {
        	/* Log and Publish */
			logAndPublish.write("Ignoring invalid Server Type Message", true, false);
        }
	}
	
	/**
	 * GameListState method enables the user to select which game they 
	 * would like to play or to exit, disconnect and close the client application.
	 * @throws IOException
	 */
	private void GameListState() throws IOException 
	{
		
		/* set variables */
		int command = 0;
		
        /* get server response */
        ServerResponse sr = this.receiveMessage();
        
        if (this.messageParser.GetVersion(sr.getMessage(), sr.getSize()) != this.m_iVersion)
        {
        	logAndPublish.write("Ignoring message with an incorrect Version", true, false);
        	return;
        }
        if (this.messageParser.GetTypeIndicator(sr.getMessage(), sr.getSize()) == MessageParser.TYPE_INDICATOR_GAME)
        {
        	if (this.messageParser.GetGameIndicator(sr.getMessage(), sr.getSize()) == MessageParser.GAME_INDICATOR_GET_GAME)
        	{
        		MessageParser.ServerGetGameMessage svrMsg = this.messageParser.GetServerGetGameMessage(sr.getMessage(), sr.getSize());
        		ArrayList<Integer> gameList = svrMsg.getGameTypeCodeList();
        		/* Log and Publish */
        		logAndPublish.write(svrMsg.toString(), false, false);   
        		logAndPublish.write("Game Options:", false, true);
        		command = UserSelection("1: Play Texas Hold'em\n2: Close Connection", 1, 2);
		
        		/* handle user selection */
        		if (command == 1)
        		{
        			/* inform server that user has selected Texas Hold'em */
        			logAndPublish.write("Sending Game Selection Message", true, false);
        			MessageParser.ClientSetGameMessage msg = this.messageParser.new ClientSetGameMessage(1, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_SET_GAME, MessageParser.GAME_TYPE_TEXAS_HOLDEM);  
        			this.sendMessage(this.messageParser.CreateClientSetGameMessage(msg));
        			this.gameState.setState(GameState.GAMESET);
        		} 
        		else 
        		{
        			// send a close connection message and close the connection
        			logAndPublish.write("Sending Close Connection Message", true, false);
        			MessageParser.ConnectionMessage msg = this.messageParser.new ConnectionMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_CLOSE_CONNECTION, MessageParser.CONNECTION_INDICATOR_CLOSE_CONNECTION);
        			this.sendMessage(this.messageParser.CreateConnectionMessage(msg));
        			this.gameState.setState(GameState.CLOSED);
        		}
        	}
        	else
        	{
        		logAndPublish.write("Ignoring wrong Game Message", true, false);
        	}
		}
        else
        {
        	logAndPublish.write("Ignoring wrong Type Indicator", true, false);
        }		
	} 
	/**
	 * GameAuthenticateState client has sent the version message and is waiting for an acknowledgment
	 * If the client gets acknowledged then it will request to get the game list
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void GameAuthenticateState() throws IOException, InterruptedException 
	{

		/* Log and Publish */
		logAndPublish.write("Entered Connection Negotiation State", true, false);
        
        /* get server response */
        ServerResponse sr = this.receiveMessage();
        
		/* verify type as VERSION */
        if (this.messageParser.GetTypeIndicator(sr.getMessage(), sr.getSize()) == MessageParser.TYPE_INDICATOR_VERSION) 
        {
        	MessageParser.VersionMessage iMsg = this.messageParser.GetVersionMessage(sr.getMessage(), sr.getSize());
        	
        	if (iMsg.getVersionType() == MessageParser.VERSION_INDICATOR_VERSION_ACK)
        	{
        		if (iMsg.getVersion() == this.m_iVersion)
        		{
        			logAndPublish.write("Successfully Authenticated with the server\n", true, true);
        			this.bankAmount = iMsg.getBankAmount();
        			/* Log and Publish */
        			logAndPublish.write(iMsg.toString(), false, false); 
        			// get the list of games and transition to the game listed state
        			MessageParser.ClientGetGameMessage oMsg1 = this.messageParser.new ClientGetGameMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_GAME, MessageParser.GAME_INDICATOR_GET_GAME);
        			this.sendMessage(this.messageParser.CreateClientGetGameMessage(oMsg1));
        			this.gameState.setState(GameState.GAMELIST); 
        		}
        		else
        		{
        			logAndPublish.write("Invalid message from the server, incorrect version number", true, true);
        		}
        	}
        	else if (iMsg.getVersionType() == MessageParser.VERSION_INDICATOR_VERSION_UPGRADE)
        	{
        		logAndPublish.write("Upgrade Required", true, true);
        		this.gameState.setState(GameState.CLOSED);
        	}
        	else if (iMsg.getVersionType() == MessageParser.VERSION_INDICATOR_VERSION_REQUIREMENT)
        	{
        		logAndPublish.write("Version Required message received from the server", true, false);
        		this.gameState.setState(GameState.LISTENING);
        	}
        	else
        	{
        		logAndPublish.write("Invalid version message received from the server", true, false);
        	}    		       	
        }
        else
        {
        	logAndPublish.write("Invalid message received from the server", true, false);
        }
	}	
	
	/**
	 * GameListeningState() client has just connected and needs to send the Client Version Message
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void GameListeningState() throws IOException, InterruptedException 
	{

		/* Log and Publish */
		logAndPublish.write("Sending Version Message", true, true);
        
		/*
		 * Send the client version message and then transition to the authentication state
		 */
        MessageParser.VersionMessage oMsg1 = this.messageParser.new VersionMessage(this.m_iVersion, MessageParser.TYPE_INDICATOR_VERSION, MessageParser.VERSION_INDICATOR_CLIENT_VERSION, (short)this.m_iMinorVersion, (long)0);
        this.sendMessage(this.messageParser.CreateVersionMessage(oMsg1));
        
        this.gameState.setState(GameState.AUTHENTICATE);
        logAndPublish.write("Changing State to Connection Negotiation", true, false);
	}	
    
	/**
	 * getSSLSocketFactory method uses the KeyManagers and TrustManagers packaged with
	 * this JAR to instantiate a new SSL Context using the TLS protocol.
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
    protected SSLSocketFactory getSSLSocketFactory() throws IOException, GeneralSecurityException
    {
      /* load TrustManagers packaged with this JAR */
      TrustManager[] tms=getTrustManagers();
      
      /* load KeyManagers packaged with this JAR */
      KeyManager[] kms=getKeyManagers();

      /* Create TLS Protocol SSL Context using the  KeyManagers and TrustManagers packaged with this JAR */
      SSLContext context=SSLContext.getInstance("TLS");
      context.init(kms, tms, null);

      /* Create an SSL Socket Factory and return */
      SSLSocketFactory ssf=context.getSocketFactory();
      return ssf;
    }  
    

    /**
     * getTrustManagers method loads the key manager packaged with this JAR, 
     * initializes and returns the TrustManager array
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    protected TrustManager[] getTrustManagers() throws IOException, GeneralSecurityException
    {
    	/* get default trust manager factory */
    	String alg=TrustManagerFactory.getDefaultAlgorithm();
    	TrustManagerFactory tmFact=TrustManagerFactory.getInstance(alg);
      
    	/* get KeyStore instance and type */
    	KeyStore ks=KeyStore.getInstance("jks");
      
  	  	/* initiate class loader to retrieve jks file from this JAR */      
      	ClassLoader classLoader = getClass().getClassLoader();
      	InputStream keystoreStream = classLoader.getResourceAsStream(trustStore); // note, not getSYSTEMResourceAsStream  
      	ks.load(keystoreStream, trustStorePassword.toCharArray());

      	/* initialize the KeyManagerFactory with this KeyStore */
      	tmFact.init(ks);

      	/* retrieve the key manager(s) and return */
      	TrustManager[] tms=tmFact.getTrustManagers();
      	return tms;
    }  

    /**
     * getKeyManagers method loads the key manager packaged with this JAR, 
     * initializes and returns the KeyManager array
     * @return returns an array of key managers
     * @throws IOException
     * @throws GeneralSecurityException
     */
    protected KeyManager[] getKeyManagers() throws IOException, GeneralSecurityException
    {

    	/* get default key manager factory */
    	String alg=KeyManagerFactory.getDefaultAlgorithm();
    	KeyManagerFactory kmFact=KeyManagerFactory.getInstance(alg);
      
    	/* construct KeyStore instance and type */
    	KeyStore ks=KeyStore.getInstance("jks");

    	/* initiate class loader to retrieve jks file from this JAR */
    	ClassLoader classLoader = getClass().getClassLoader();
    	InputStream keystoreStream = classLoader.getResourceAsStream(keyStore); // note, not getSYSTEMResourceAsStream  
    	ks.load(keystoreStream, keyStorePassword.toCharArray()); 

    	/* initialize the KeyManagerFactory with this KeyStore */
    	kmFact.init(ks, keyStorePassword.toCharArray());

    	/* retrieve the key manager(s) and return */
    	KeyManager[] kms=kmFact.getKeyManagers();
    	return kms;
    }  
    
	/**
	 * connect method forms a connection to the server defined by the configuration
	 * @param ssf SSL Socket Factory instance
	 * @param hostName Host name of the Server
	 * @param port Port of the AGMP Server
	 * @throws IOException
	 */
    public void connect(SSLSocketFactory ssf, String hostName, int port) throws IOException 
    {
        
		/* Log and Publish */
		logAndPublish.write("Open secure connection to " + hostName + ".", true, true);    	
    	
    	if(!connected)
        {
    		/* set variables */
        	this.hostName = hostName;
            this.port = port;
            
            /* open secure socket */
            socket = (SSLSocket)ssf.createSocket(hostName, port); 
            
            /* get input and output screens */            
            oInputStream = socket.getInputStream();
            oOutputStream = socket.getOutputStream();
   		 	outputStream = new ObjectOutputStream(oOutputStream); 
   		 	inputstream = new ObjectInputStream(oInputStream); 
   		 	this.connected = true;
    		 	
   		 	
   		 	/* Spawn thread with 'this' instance.  Initiate reading from server. */
   		 	Thread t = new Thread(this);
   		 	t.start(); 
        }
    }
    
    /**
     * findConnect method will use the Java InetAddress .isReachable method, 
     * which uses ICMP if logged in as root in linux or OSX, or uses a TCP RST,ACK 
     * ping to Port 7 Echo Request.  
     * @param ssf SSL Socket Factory instance
     * @param port Port of the AGMP Server
     * @throws IOException
     */
    public void findConnect(SSLSocketFactory ssf, int port) throws IOException 
    {
        
		/* Log and Publish */
		logAndPublish.write("Attempting to find server using ICMP and/or TCP.", true, true); 
    	
    	if(!connected)
        { 	
    		
    		/* set variables */
        	this.port = port;
        	
        	/* open secure socket */
        	EchoFinder ef = new EchoFinder(ssf, port);
	        socket = ef.findAGMPServer(port);
	        
	        /* get input and output screens */ 
	        if (socket != null) {
		        oInputStream = socket.getInputStream();
		        oOutputStream = socket.getOutputStream();
				outputStream = new ObjectOutputStream(oOutputStream); 
				inputstream = new ObjectInputStream(oInputStream); 
				connected = true;

	  			/* Log and Publish */
	   			logAndPublish.write("'findConnect' status: " + connected, false, true);   		 	
				
				/* Spawn thread with 'this' instance.  Initiate reading from server. */
		        Thread t = new Thread(this);
		        t.start();	
	        }
        }
    }
    
	/**
	 * UserSelection method to capture user input from command line to questions
	 * 
	 * @param choice String summarizing the choice to be made
	 * @param greaterThan Integer of a value to be greater than as first number in the choice sequence
	 * @param lessThan Integer of a value to be less than and equal to the last number in the choice sequence
	 * @return
	 */
	public int UserSelection(String choice, int greaterThan, int lessThan)
	{
		int command = -1;
		
		reset:
		while(true) {
			try {
				logAndPublish.write(choice, false, true);
				command = Integer.parseInt(br.readLine());
			} catch (NumberFormatException e) {
			
	    		/* Log and Publish */
	    		logAndPublish.write(e, true, false);
	    		logAndPublish.write("Invalid Selection.  Try again.", false, true);
				continue reset;

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.disconnect();
			}
			
			/* command contains a valid number */
			if ((command >= greaterThan) && (command <=lessThan))
				break;
			else 
			{
				logAndPublish.write("Invalid Selection.  Try again.", false, true);
				continue reset;
			}
		}	
		
		return command;
	
	}    
    
    /**
     * receiveMessage method reads server response from ObjectInputStream 
     * @return 
     * @throws IOException
     */
    public ServerResponse receiveMessage () throws IOException
    {
		byte iByteCount = inputstream.readByte();
		byte [] inputBuffer = new byte[iByteCount];
		inputstream.readFully(inputBuffer);   
		
		ServerResponse sr = new ServerResponse(inputBuffer, (int)iByteCount);
		return sr;
    }    
    
    /**
     * sendMessage method dispatches messages to the server
     * @param msg MessageParser assembled byte message
     * @throws IOException
     */
    public void sendMessage (byte[] msg) throws IOException
    {
		outputStream.writeByte((byte)msg.length);
        outputStream.write(msg); 
        outputStream.flush();
    }

    /**
     * Disconnect this client from the server
     */
    public void disconnect() 
    {
    	
    	/* Disconnect from server */
		if(socket != null && connected)
        {
          try {
			this.socket.close();
          }catch(IOException ioe) {
        	  
      		/* Log and Publish */
      		logAndPublish.write(ioe, true, true); 
          }
          finally {
			this.connected = false;
          }
        }
    }
}


/**
 * ServerResponse class used to store a response message and length from an AGMP Server
 * @author root
 *
 */
class ServerResponse {
	private byte[] message = null;
	private int size = -1;
	
	/**
	 * constructor method to set byte array and size
	 * @param msg
	 * @param size
	 */
	public ServerResponse (byte[] msg, int size) 
	{
		this.message = msg;
		this.size = size;
	}
	
	/**
	 * setMessage method to set byte array
	 * @param msg
	 */
	public void setMessage (byte[] msg) 
	{
		this.message = msg;
	}

	/**
	 * setSize method to set array size
	 * @param size
	 */
	public void setSize (int size)
	{
		this.size = size;
	}
	
	/**
	 * getSize method returns array size
	 * @return
	 */
	public int getSize ()
	{
		return size;
	}
	
	/**
	 * getMessage method returns byte array
	 * @return
	 */
	public byte[] getMessage ()
	{
		return message;
	}
}    

	

