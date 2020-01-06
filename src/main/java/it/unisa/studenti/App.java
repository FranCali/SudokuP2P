package it.unisa.studenti;

import de.ad.sudoku.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;


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
            int startOption = -1;
            try{
                startOption = scanner.nextInt();
            
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
                    //clearScreen();
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
                        clearScreen();
                        System.out.println("Failed to join, nickname already present or game not existing");
                    } 
                }
            }catch(InputMismatchException e){
                clearScreen();
                System.out.println("Format Error, try again");
                scanner.nextLine();
            }
        }
        
        clearScreen();

        while(true){
            printMenu();
            try{
                int option = scanner.nextInt();

                switch(option) {
                    case 1: 
                        clearScreen();
                        localBoard = sudoku.getSudoku(gameName);
                        System.out.println(localBoard.toString());
                        break;
                    case 2: 
                        clearScreen();
                        localBoard = sudoku.getSudoku(gameName);
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
            }catch(InputMismatchException e){
                clearScreen();
                System.out.println("Format Error, use numeric options");
                scanner.nextLine();
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
        HashMap<String, Integer> playerScores = new HashMap<>();

        for(String nickname: players.keySet()){
            playerScores.put(nickname, (Integer) players.get(nickname)[1]);
        }

        sortByValue(playerScores);

        System.out.println("--------LEADERBOARD--------");
        for(String player: playerScores.keySet()){
            System.out.println("Player: " + player + " Score: " + playerScores.get(player));
        }
        System.out.println("----------------------------");

    }


    
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
    
}

