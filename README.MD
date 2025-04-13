# Distributed Systems
### Paxos Algorithm
#### Alexander Robertson

## Introduction
This is an implementation of the Paxos algorithm. We use the Paxos algorithm
in this case to elect a president for the Adelaide Suburbs Council. The council is made up
of 9 members, any of which can be elected president.
However, the council members are prone to acting arbitrarily and may not be reliable.

## Implementation
The implementation of this algorithm is done primarily in the member package. The member
package contains an interface as well as a class that implements the interface.
We have two roles within our implementation of the algorithm. Acceptors and proposer.
Proposer make proposals to all the other nodes, and acceptors vote on the proposals.
Once a value has been decided, and the value was committed to by a majority of the nodes,
a proposer will eventually receive a set of promises from the acceptors with the decided value,
forming a majority and the proposer will then broadcast a 'terminate' message to all nodes with
the decided value, and the process will terminate.

To simulate the unreliable nature of the council members, I have implemented a
quirk package that exists within the member package. This package contains a Quirk 
interface and 4 classes that implement it.
The classes represent a number of different profiles.


## Testing
A number of tests can be found in the test folder inside the main folder.

```
./src/test/java/*
```

They test the different scenarios as described in the assignment description.
**Please be aware that a single run of the paxos algorithm may take as long as 10 minutes.
Especially when a node is behaving arbitrarily and repeatedly drops connection to other nodes**
Please be patient! Even if it seems like nothing is happening, the nodes are still running the
algorithm. I guarantee that the algorithm will eventually terminate, and the president will be
decided. I have seen a run take as long as 25 minutes in an incredibly unlucky situation 


## Compiling and Running
Please build the project using IntelliJ IDEA or another IDE.
The project can be run by running the main method in 
```
./src/main/CouncilVote.java
```
The input arguments are as follows:
```
args[0] is the member number (1-9)
args[1] determines if this process is a proposer or an acceptor.
args[2] is if this process is to run in test mode, that is, without each process having quirks.
Any value passed in args[2] will be considered true.
quirks: quirks are where the process will behave according to a set of pre-defined quirks.
For example, a process may be incredibly responsive, unreliable, or slow. This is to allow
for testing of scenarios such as processes acting according to their quirks, or acting as normal.
```
