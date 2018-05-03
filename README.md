# COMP3331-ComputerNetworksAndApplications

## Assignment 1 Overview
As part of this assignment, I implemented a Simple Transport Protocol (STP), a piece of software that consists of a sender and receiver component that allows reliable unidirectional data transfer. STP includes some of the features of TCP protocols. STP protocol was used to transfer simple text (ASCII) files from the sender to the receiver. STP is implemented as two separate programs: Sender and Receiver. I was required to implement a unidirectional transfer of data from the Sender to the Receiver. Data segments will flow from Sender to Receiver while ACK segments will flow from Receiver to Sender. STP was implemented on top of UDP.

## Assignment 2 Overview
For each virtual circuit request in the above workload, the program must use a specified routing algorithm to determine if the circuit can be established. To be more specific, your program must select the “best” route depending on the routing protocol in use (Shortest Hop Path, Shortest Delay Path, Least Loaded Path) from the source to the destination of the circuit and then determine if there is sufficient capacity along each link of this end-to-end path to accommodate the circuit.
