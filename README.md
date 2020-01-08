# SudokuP2P

This project has been developed as final course work for university exam "Distributed Architectures for Cloud"

## Project Description

The project is a sudoku challenge game on a P2P network.
It has been developed in Java language, using the following technologies:
* TomP2P: P2P-based high performance key-value pair storage library, 
* Apache Maven: software project management and comprehension tool,
* JUnit: simple framework to write repeatable tests.

![sudoku](https://uploads.guim.co.uk/2019/10/13/SU-4572_P_E_copy.jpg)

The game exploits the following features:

 * Generate a sudoku game
 * Join an existing game (using a nickname)

 A player which joins a sudoku game can execute one of the following commands:
 * View sudoku board
 * Place a number
 * View the leaderboard
 * Exit the game

Players can choose the sudoku difficulty from the following:
1) Easy
2) Medium
3) Hard

The sudoku difficulty is based on the number of empty cells, the fewer the number of empty cells, the easier the game.

### Game Description
 Each user can place a number of the sudoku game, if it is not already placed takes 1 point, if it is already placed and it is rights takes 0 point, in other case receive -1 point. The game is based on 9 x 9 matrix. 

 The game provides notification features such that all users that play to a game are automatically informed when a user increment its score, and when the game is finished. 

 The game is based on the concept of global board and local board. Players who are in the same game have each one a local board. When a player places a number, it is compared to the number in the solution and the number is valis only if it matches the solution board number. The global board is used to evaluate the point to assign to the player as described before. A player only knows its local board.
 The game ends when the first player fills entirely its local board.
 The winner of a game is the player with the highest score. 

### Tests Description
All core functionalities of the sudoku P2P game have been tested, in particular, the test provides an example game with 4 peers which communicate and play together joining the same game. Functionalities of generating a new game, joining a game, placing a number, leaving a game have been tested. The tests have been written using JUnit and Java Threads.

## Build and run with Docker
This project supports Docker.
You can have the project up and runnning with the following steps:

1) Build the docker container with the following command:
``docker build --no-cache -t p2p-sudoku-client .``

2) Start the master peer with the following command:
``docker run -i --name MASTER-PEER -e ID=1 -e MASTERIP="127.0.0.1" p2p-sudoku-client``

ID and MASTERIP are Docker environment variables used to pass command line arguments to the program.

3) Start a generic peer, to do that you first have to check the ip address of your container:
    
    * Check the docker: ``docker ps``
    * Check the IP address: ``docker inspect <container ID>``

    Now you can start peers by executing the following command: ``docker run -i --name PEER-1 -e ID=2 -e MASTERIP="172.17.0.2" p2p-sudoku-client`` 
