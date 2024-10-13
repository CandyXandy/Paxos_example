# Distributed Systems
## Assignment 3
### Paxos Algorithm
#### Alexander Robertson - a1884188

## Introduction
This assignment is an implementation of the Paxos algorithm. We use the Paxos algorithm
in this case to elect a president for the Adelaide Suburbs Council. The council is made up
of 9 members, any of which can be elected president.
However, the council members are prone to acting arbitrarily and may not be reliable.

## Implementation
The implementation of this algorithm is done primarily in the member package. The member
package contains an interface as well as a class that implements the interface.
We have two roles within our implementation of the algorithm. Acceptors and proposer.
Proposer make proposals to all the other nodes, and acceptors vote on the proposals.
However, the roles are dynamic, if any acceptor hasn't received a message in a long
time, it will become a proposer.
To avoid the situation where too many proposers exist to form a majority, proposers
will become acceptors if they have made 10 failed proposals.


To simulate the unreliable nature of the council members, I have implemented a
quirk package that exists within the member package. This package contains a Quirk 
interface and 4 classes that implement it.
The classes represent the different profiles as specified in the assignment description.


## Testing
A number of tests can be found in the test folder inside the main folder.

```
./src/test/java/*
```

They test the different scenarios as described in the assignment description.

## Compiling and Running
Please build the project using IntelliJ IDEA.
The project can be run by running the main method in 
```
./src/main/java/member.CouncilVote.java
```
