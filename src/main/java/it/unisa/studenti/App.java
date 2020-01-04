package it.unisa.studenti;

import de.ad.sudoku.*;

import java.util.Scanner;


public class App {
    private static Grid localBoard;

    public App() throws Exception{
    }

    public static void main( String[] args ) throws NumberFormatException, Exception{
        
        Scanner scanner = new Scanner(System.in);
        int peerId = Integer.parseInt(args[0]);
        P2PSudoku sudoku = new P2PSudoku(peerId, new MessageListener(){
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

        while(!startGame){
            printStartMenu();
            int startOption = scanner.nextInt();
            clearScreen();
            System.out.print("Enter game name: ");
            gameName = scanner.next();
            clearScreen();
            
            if(startOption == 1){
                localBoard = sudoku.generateNewSudoku(gameName);
                System.out.print("Enter nickname: ");
                String nickname = scanner.next();
                if(sudoku.join(gameName, nickname))
                    startGame = true;
            }
            else if(startOption == 2){
                System.out.print("Enter nickname: ");
                String nickname = scanner.next();
                if(sudoku.join(gameName, nickname)){
                    System.out.println("Successfully joined");
                    startGame = true;
                }
                else{
                    clearScreen();
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
                    sudoku.placeNumber(gameName, row, col, number);
                    break;
                case 3:
                    System.out.println("Are you sure to leave the game?\n1)yes\n2)no");
                    int exitOption = scanner.nextInt();
                    if(exitOption == 1){
                        sudoku.leaveNetwork();
                        scanner.close();
                        System.exit(0);
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
        System.out.println("1) View Board");
        System.out.println("2) Place a Number");
        System.out.println("3) Exit");
    }

   
    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }

}

