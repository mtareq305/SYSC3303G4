import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FloorSubsystem {
	
	// Datagram sockets used to send and receive packets to the Scheduler
	private DatagramSocket sendReceive;
	
	// SEND_PORT is the port on the scheduler where data is sent and RECIEVE_PORT is where the floor subsystem listens for incoming data 
	private static final int SEND_PORT = 7000, RECEIVE_PORT = 7001;
	
	// Text file containing events to be sent to scheduler
	private static final String INPUT_PATH = "InputEvents.txt";
	
	// Current line in input file
	private int currentLine;
	
	// List of Events to be sent to the Scheduler
	private ArrayList<InputEvent> eventList;
	
	private static final int BYTE_SIZE = 6400;
	
	// Provides the floor number		
	public int floorNum;

	public boolean upButton, downButton;
	
	//to check if the elevator door is open or closed
	private boolean doorOpen, doorClosed; 
	
	//Up and Down lamps on the floor's elevator buttons
	private boolean upLamp, downLamp;
	
	// to check if elevator is currently present on the floor
	public boolean elevatorPresent;


	public FloorSubsystem() {

	      try 
	      {
	    	  this.sendReceive = new DatagramSocket();							
	      } 
	      catch (SocketException se) 													
	      {   
	         se.printStackTrace();
	         System.exit(1);
	      }

		
		this.currentLine = 0; 
		
		this.eventList = new ArrayList<InputEvent>();
		
	}
	

	
	public FloorSubsystem(int n) {
		
		this.currentLine = 0; 
		
		this.eventList = new ArrayList<InputEvent>();
		
		try {
			this.sendReceive = new DatagramSocket(RECEIVE_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		this.floorNum = n;
		
		this.upButton = false;
		
		this.downButton = false;
		
		this.elevatorPresent = false; 
		
	}
	
	/** get the current floor that the elevator is on */
	public int getCurrentFloor() {
		return floorNum;
		}

	/** true if the elevator is present on the current floor **/
	public boolean isElevatorPresent() {
		return elevatorPresent;
	}
	
	public void readInputEvent() {
		Path path = Paths.get(INPUT_PATH);
		
		List<String> inputEventList = null;
		 
		// Read the input file line by line into a list of strings
		try {
			inputEventList = Files.readAllLines(path);
		} catch (IOException e) { // Unable to read the input text file
			e.printStackTrace();
		}
		
		int i;
		
		// Starting from the current line saved in the floor subsystem read and parse the string
		for (i = this.currentLine; i < inputEventList.size(); i++ ) {
				
				String inputEvent = inputEventList.get(i);
				
				String[] inputEvents = inputEvent.split(" ");
				
				Integer currentFloor = Integer.parseInt(inputEvents[1]);
				
				if (this.floorNum == currentFloor) {
				
				// Input event starts with a string of the time log followed by whitespace
				String time = inputEvents[0];
				
				// True if the request was for an elevator going up and false otherwise
				Boolean up;
				
				if (inputEvents[2].equalsIgnoreCase("up")) {
					up = true;
				} else if (inputEvents[2].equalsIgnoreCase("down")) {
					up = false;
				} else {
					throw new IllegalArgumentException("Floor button read form input file is not valid");
				}
				
				// Finally an integer representing the requested destination
				Integer destinationFloor = Integer.parseInt(inputEvents[3]);
				
				// Create event object
				InputEvent event = new InputEvent(time, this.floorNum, up, destinationFloor);
				
				// Add to event object list
				eventList.add(event);
			}
		}
		
		this.currentLine = i;
		return;
		
	}
	
	public byte[] eventListToByteArray() {
		if (!this.eventList.isEmpty()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTE_SIZE);
			
			ObjectOutputStream oos = null;
			
			try {
				oos = new ObjectOutputStream(baos);
			} catch (IOException e1) {
				// Unable to create object output stream
				e1.printStackTrace();
			}
			
			try {
				oos.writeObject(this.eventList);
			} catch (IOException e) {
				// Unable to write eventList in bytes
				e.printStackTrace();
			}
			
			byte[] data = baos.toByteArray();
			
			this.eventList.clear();
			
			return data;
		} else {
			throw new IllegalArgumentException("The eventlist must not be empty before being converted to byte array");
		}
	}
	
	public void sendEventList() {
		DatagramPacket sendPacket = null;

		if (!eventList.isEmpty()) {
			
			byte[] data = eventListToByteArray();
			
			// Create Datagram packet containing byte array of event list information
			try {
			     sendPacket = new DatagramPacket(data,
			                                     data.length, InetAddress.getLocalHost(), SEND_PORT);
			  } catch (UnknownHostException e) {
			     e.printStackTrace();
			     System.exit(1);
			  }
			
			// Send event list to scheduler
			try {
		         sendReceive.send(sendPacket);
		      } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
		}
	}
	
}
