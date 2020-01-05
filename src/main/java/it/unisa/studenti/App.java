package it.unisa.studenti;

import de.ad.sudoku.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


public class App {
    private static Grid localBoard;

    public App() throws Exception{
    }

    public static void main( String[] args ) throws NumberFormatException, Exception{
        
        Scanner scanner = new Scanner(System.in);
        int peerId = Integer.parseInt(args[0]);
        String masterPeer = args[1];
        P2PSudoku sudoku = new P2PSudoku(peerId, masterPeer, new MessageListener(){
            @Override
            public Object parseMessage(Object obj) {
                String message = (String) obj;
                if(message.contains("Game Finished")){
                    System.out.println(message);
                    System.exit(0);
                }
                System.out.println(message);
                return "success";
            }
        });

        clearScreen();

        boolean startGame = false;
        String gameName = null;
        String nickname = null;

        while(!startGame){
            printStartMenu();
            int startOption = scanner.nextInt();
            clearScreen();
            System.out.print("Enter game name: ");
            gameName = scanner.next();
            clearScreen();
            
            if(startOption == 1){ //Generate new game
                int difficulty = -1;
                do{
                    System.out.println("Chose difficulty: \n1)Easy \n2)Medium \n3)Hard");
                    difficulty = scanner.nextInt();
                }while(difficulty!=1 && difficulty!=2 && difficulty!=3);

                localBoard = sudoku.generateNewSudoku(gameName, difficulty);
                System.out.print("Enter nickname: ");
                nickname = scanner.next();
                if(sudoku.join(gameName, nickname))
                    startGame = true;
            }
            else if(startOption == 2){ //Join existing game
                System.out.print("Enter nickname: ");
                nickname = scanner.next();
                if(sudoku.join(gameName, nickname)){
                    System.out.println("Successfully joined");
                    startGame = true;
                }
                else{
                    //clearScreen();
                    System.out.println("Failed to join, nickname already present or game not existing");
                } 
            }

        }
        
        clearScreen();

        while(true){
            printMenu();
            int option = scanner.nextInt();

            switch(option) {
                case 1: 
                    clearScreen();
                    localBoard = sudoku.getSudoku(gameName); //DA AGGIUSTARE PERCHE' LA LOCAL BOARD CE L'HO GIA' COME VARIABILE LOCALE
                    System.out.println(localBoard.toString());
                    break;
                case 2: 
                    clearScreen();
                    System.out.println(localBoard.toString());
                    System.out.println("Enter Number: ");
                    int number = scanner.nextInt();
                    clearScreen();
                    System.out.println(localBoard.toString());
                    System.out.println("Enter row: ");
                    int row = scanner.nextInt();
                    clearScreen();
                    System.out.println(localBoard.toString());
                    System.out.println("Enter column: ");
                    int col = scanner.nextInt();
                    sudoku.placeNumber(gameName, nickname, row, col, number);
                    break;
                
                case 3: 
                    clearScreen();
                    printLeaderBoard(sudoku, gameName);
                    System.out.println("\n\n\n");
                    break;    
                case 4:
                    System.out.println("Are you sure to leave the game?\n1)yes\n2)no");
                    int exitOption = scanner.nextInt();
                    if(exitOption == 1){
                        if(sudoku.leaveGame(gameName, nickname)){
                            scanner.close();
                            System.exit(0); 
                        }
                        else{
                            System.out.println("Error leaving the game");
                        }
                    }
                    break;
                default:
                    break;
            }

        }

    }

    public static void printStartMenu(){
        System.out.println("1) Create New Game");
        System.out.println("2) Join Game");
    }

    public static void printMenu(){
        System.out.println("1) View Sudoku");
        System.out.println("2) Place a Number");
        System.out.println("3) View LeaderBoard");
        System.out.println("4) Exit");
    }

   
    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }

    public static void printLeaderBoard(P2PSudoku sudoku, String gameName){
        final HashMap<String, Object[]> players = sudoku.getGamePlayers(gameName);
        List<Integer> scores = new ArrayList<>();
        
        for(Object[] player: players.values())
            scores.add((Integer) player[1]);

        Collections.sort(scores);
        int i = 0;

        System.out.println("--------LEADERBOARD--------");
        for(String player: players.keySet()){
            System.out.println("Player: " + player + " Score: " + scores.get(i));
            i++;
        }
        System.out.println("----------------------------");

    }

}

