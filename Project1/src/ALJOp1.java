/**
 *  CS 3345.005
 *  Project 1
 * 
 * @author Alex Johnston
 * 
 * date 2/17/2017
 * 
 * 
 */

import java.util.Scanner;

public class ALJOp1 {
	
	// Debug values that help solve issues
	private static boolean debug = false; // If true debugging is displayed, if false the program acts as if there is no debugging.
	private static char  disname = '0'; // Initial segment name in display (character used to represent first block)
	private static int displayScale = 5; // change the width of the display by reducing it by a factor of displayScale
	
	
	// Main function, program loop found here.
	public static void main(String[] args){
		
		
		Scanner in = new Scanner(System.in);

		int[] data = new int[8]; // All data about the simulation, see below VVV
		/* data: This variable is used to keep track of statistic data during the simulation
		 * 0 = time - Global time of the simulation
		 * 1 = numSegs - Number of segments in the simulation summed over time
		 * 2 = numHoles - Number of holes in the simulation summed over time
		 * 3 = numProbes - Number of probes done upon successful placement
		 * 4 = freeSpaceOnFail - Sum of all free-space upon failure to place a segment
		 * 5 = spaceTimeOccupation - Sum of all space * lifetime of each data segment
		 * 6 = numPlace - Number of segments placed
		 * 7 = numFail - Number of segments that failed to place
		 */

		boolean run = true; // Read lines from the input until run = false
		Node memory = new Node(0,0,false); // The initial segment linked list head
		boolean firstFit = false; // The type of fitting currently set
	
		// Main operating loop
		while(run){
			String[] line =  in.nextLine().toUpperCase().trim().split(" "); // Command array split into sanitized tokens, e.g { "P", "100", "20" }
			switch(line[0]){ //Switch on the command token [0]
				case "FF": // Set for first fit
					System.out.println("FIRST FIT");
					firstFit = true;
					break;
				case "BF": // Set for best fit
					System.out.println("BEST FIT");
					firstFit = false;
					break;
				case "C": // Initialize the memory segment size to the 2nd token [1]
					memory.setSize(Integer.parseInt(line[1]));
					break;
				case "P": // Place a segment into memory using the correct fitting and return data
					data = place(data, line, memory, firstFit);
					break;
				case "R": // Run a report to display what's in the file
					System.out.println("Report at time: " + data[0]);
					reportMem(memory);
					break;
				case "S": // Run the statistics from data[] and display them
					
					data = wait(data, memory); // Run simulation until all memory has departed
					
					/* data:
					 * 0 = time - Global time of the simulation
					 * 1 = numSegs - Number of segments in the simulation summed over time
					 * 2 = numHoles - Number of holes in the simulation summed over time
					 * 3 = numProbes - Number of probes done upon successful placement
					 * 4 = freeSpaceOnFail - Sum of all free-space upon failure to place a segment
					 * 5 = spaceTimeOccupation - Sum of all space * lifetime of each data segment
					 * 6 = numPlace - Number of segments placed
					 * 7 = numFail - Number of segments that failed to place
					 */
					System.out.format("Average number of segments in memory: %.2f\n", 	 				(data[1] + 0.0) / data[0]);
					System.out.format("Average number of holes in memory: %.2f\n", 						(data[2] +0.0)  / data[0]);
					System.out.format("Average number of probes in a successful placement: %.2f\n",		(0.0 + data[3]) / data[6]);
					System.out.format("Average amount of memory in use: %.2f\n", 						(data[5] + 0.0) / data[0]);
					if (data[7] > 0){
						System.out.format("Average amount of total free space when a placement fails: %.2f\n", (0.0 + data[4]) / data[7]);
					}
					System.out.format("Average percentage failure rate: %.2f\n", 						(100.0*data[7]) / data[6]);
					
					break;
				case "E": // End the simulation
					run = false;
					break;
			}
			
		}
		in.close(); // Close the input stream
		
		if (debug){ // Output some debug data if debug is set
			System.out.println("Segments: " + data[1]);
			System.out.println("Holes:    " + data[2]);
			System.out.println("MemUsage: " + data[5]);
			System.out.println("Place:    " + data[6]);
			System.out.println("Fails:    " + data[7]);
		}
		
		
	}
	
	
	
	// Run the simulation until all memory has departed
	public static int[] wait(int[] data, Node memory){
		while(memory.getNext() != null){ // While there is more than one memory segment
			data[0]++; // Increase time
			cullSegments(memory, data[0]);  // Remove expired segments
			data = countSegments(memory, data); // Gather data on remaining segments
		}
		return data; // Return gathered data
	}

	// Place a new segment into memory, if there is no space wait until there is then place
	public static int[] place(int[] data, String[] item, Node memory, boolean firstFit){
		boolean failed = false; // If there is a failure to place trigger this flag (used so that each failure is only counted once)
		
		int segSize = Integer.parseInt(item[1]); // Get the segment size-
		int segLife = Integer.parseInt(item[2]); // -and lifespan of the new segment to be placed
		
		boolean placed = false; // Loop until the segment is placed
		while (!placed){
			
			data[0]++; // Increment time
			
			cullSegments(memory, data[0]); // Clear out any segments that expire at this new time
			
			Node insertionSegment; // This Node holds the empty segment that the new segment will occupy
			
			if (firstFit){ // Run the correct fitting algorithm
				insertionSegment = getFirstFit(segSize, memory); // FF
			}else{
				insertionSegment = getBestFit(segSize, memory); // BF
			}

			
			if (insertionSegment != null){ // If the fitting algorithms produced a non-null Node it must be able to contain the new segment
				placed = true; // The segment has found a place
				data[3] += insertionSegment.getNumProbes();  // Add to the number of probes the amount done by the algorithms (stored into the Node)
				int holeSize = insertionSegment.getSize() - segSize; // Calculate the hole's new size after insertion (could be 0)
				
				// Change the properties of the hole such that it is full.
				// This program doesn't generate new nodes for each inserted segment, but rather over writes holes.
				insertionSegment.setOccupied(true); // Set hole to now be occupied
				insertionSegment.setSize(segSize); // Set the occupied hole's size to be that of the new segment
				insertionSegment.setTime(data[0] + segLife); // Set the life span of the new segment

				data[5] += segSize * segLife; // Add the total segment size * segment life span to the total memory Impulse
				
				if (holeSize > 0){ // If new segment failed to fill the hole entirely create a new hole and insert it after the segment
					Node newHole = new Node(holeSize, 0, false);
					newHole.setNext(insertionSegment.getNext());
					insertionSegment.setNext(newHole);
				}
				
			}else{ // If there was a failure to place, set failed to true and collect data.
				if (!failed){
					data[7]++; // Add to the number of fails
					data[4] += countFreeSpace(memory); // Add to the free space available upon fail
				}
				failed = true; // This ensures that the data will only be collected once per segment even if it fails several times
			}
			
			data = countSegments(memory, data); // Collect data on the current state of the segments

		}
		data[6]++; // The segment must be placed at this point, so add to the number of placements
		return data;
	}
	
	// Count the amount of empty space in the memory, only used for updating the freeSpaceOnFail metric
	public static int countFreeSpace(Node memory){
		int space = 0;
		
		while (memory != null){ // For each element in the linked list,
			
			if (!memory.isOccupied()){ 
				space += memory.getSize(); // If it's empty add it's size to space
			}
			
			memory = memory.getNext();
		}
		return space; // Return the sum of all free space
	}
	
	// Collect data on the current time. Updates number of segments and holes. Contains majority of debugging code.
	public static int[] countSegments(Node memory, int[] data){
		
		int holes = 0; // Contains the number of holes at this time
		int segs = 0;  // Contains the number of segments at this time
		
		while (memory != null){ // For each element in the linked list
			
			// Count if it's a segment or a hole
			if (memory.isOccupied()){
				segs++;
				
				if (memory.name == '.'){ // Debug code, if the segment isn't a hole but has a hole name, set it's name to the next one available
					memory.name = disname++;
				}
			}else{
				holes++;
				
				memory.name = '.'; // Debug code, if the segment is a hole then change its name to the default hole name
			}
			
			// Debug code, display the current segment (width divided by displayScale)
			for (int i = 0; debug && (i < memory.getSize() / displayScale); i++){
				System.out.print(memory.name);
			}
			
			memory = memory.getNext(); // Increment along the list
		}
		
		
		if (debug){ // Output more debug info about the current slice of time (" [time]: [#holes]s [#segments]s")
			System.out.println(" " + data[0] + ": " + holes + "h " + segs + "s ");
		}
		
		
		data[1] += segs;  // Add the current number of segments to the global count
		data[2] += holes; // Same for holes
		return data;
	}
	
	// Remove segments that have expired from the memory.
	public static void cullSegments(Node memory, int time){
		
		
		Node lagReference = null; // references the last visited node in the list, used for combining holes after removal
		
		while (memory != null){ // For each Node in the linked list
			
			// If the node isn't empty and its time has come remove it
			if (memory.isOccupied() && memory.getTime() <= time){
				
				// Remove the data from the segment
				memory.setOccupied(false);
				memory.setTime(0);
				
				memory.name = '.'; // Debug code, set the segment's name to the default empty
				
				// If the Node before this one is empty, merge them.
				if (lagReference != null && !lagReference.isOccupied()){
					
					// Merge the current node into lagReference
					lagReference.setSize(lagReference.getSize() + memory.getSize());
					lagReference.setNext(memory.getNext());
					memory = lagReference;
				}
				
				// If the Node after this one is empty, merge them.
				if (memory.getNext() != null && !memory.getNext().isOccupied()){
					
					// Merge the next Node into the current one.
					memory.setSize(memory.getNext().getSize() + memory.getSize());
					memory.setNext(memory.getNext().getNext());

				}
			}
	
			lagReference = memory; // Set the lag reference to the current node
			memory = memory.getNext(); // Set the current node to the next node
		}


	}
	
	// Returns the first empty segment of memory that is at least the size of some amount
	public static Node getFirstFit(int segSize, Node memory){
		int numProbesTest = 0;
		while(memory != null){ // For every segment in the linked list
			
			// If it is empty and of the correct size, return it.
			if (!memory.isOccupied()) {
				numProbesTest++; // Keep track of the number of probes done for statistical reasons.
				if (memory.getSize() >= segSize){
					memory.setNumProbes(numProbesTest); // Set the segment's probe counter before returning it
					return memory;
				}
			}
			memory = memory.getNext();
		}
		return null; // If no such segment exists, return null to indicate it
	}
	
	// Returns the smallest empty segment that is at least the size of some amount
	public static Node getBestFit(int segSize, Node memory){
		int numProbesTest = 0;
		Node bestFit = null; // Keep track of the best node
		while(memory != null){ // For each segment in the linked list
			
			// If it is empty and fits the data and is smaller than the current best, then make it the current best
			if (!memory.isOccupied()) {
				numProbesTest++; // Keep track of the number of probes needed to find the segment
				if (memory.getSize() >= segSize && (bestFit == null || bestFit.getSize() > memory.getSize())){
					bestFit = memory;
				}
			}
			memory = memory.getNext(); // Increment the list
		}
		if (bestFit != null){ // If a fit was found set it's probe count to the found number
			bestFit.setNumProbes(numProbesTest);
		}
		return bestFit; // Return null of no fit was found
	}
	
	// Prints a report of the current state of the memory by printing each segment's properties onto a new line
	public static void reportMem(Node memory){
		int segAddress = 0; // Keep track of the address of each segment (only the size is recorded for each node)
		while (memory != null){ // For each element of the linked list,
			
			
			// Print if the segment has data or is a hole, it's address and size, and the time it will depart if it's a segment.
			String common = segAddress + " " +  memory.getSize();
			if (memory.isOccupied()){
				System.out.println("Segment " + common + " " + memory.getTime() );
			}else{
				System.out.println("Hole " + common);
			}
			segAddress += memory.getSize(); // Update to the next address
			
			memory = memory.getNext(); // Increment the list...
			
		}
	}
	
}

/*
 * This is the Node class for the linked list.
 * It has three values: size, time, and occupied
 * Each has a getter and a setter
 */
class Node{
	private int size; // The size of the segment in memory
	private int time; // The time the segment will depart
	private boolean occupied; // Whether or not the segment has data or is empty (empty = false)
	private Node next; // Link to the next node

	private int numProbes; // The number of probes used to place data into this segment (only used when a placement is made into this segment)
	
	public char name = '.'; // Debug variable, what the segment will use as it's symbol ( . = empty )
	
	// Constructor
	public Node(int segmentSize, int departureTime, boolean type){
		size = segmentSize;
		time = departureTime;
		occupied = type;
	}
	
	// Getters and Setters for all data values
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public boolean isOccupied() {
		return occupied;
	}

	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	public int getNumProbes() {
		return numProbes;
	}

	public void setNumProbes(int numProbes) {
		this.numProbes = numProbes;
	}
	
	// Getter and Setter for next node value
	public Node getNext() {
		return next;
	}

	public void setNext(Node next) {
		this.next = next;
	}



}