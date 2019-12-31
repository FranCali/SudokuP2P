package it.unisa.studenti;

import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;

import java.util.Scanner;


public class App {

    public App(int peerId) throws Exception{

        P2PSudoku sudoku = new P2PSudoku(peerId);
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("1) Generate new game\n2) Join an existing game");
        int option = scanner.nextInt();
        
        if(option == 1){//Generate and store a new game
            System.out.print("Game name: ");
            Grid grid = sudoku.generateNewSudoku(scanner.next());
            
            System.out.println("New Game Created!");
            System.out.println(grid.toString());
            
            scanner.close();
        }
        else if(option == 2){
            int empty = 0;
            System.out.print("Game name: ");
            Grid grid = sudoku.getSudoku(scanner.next());
            
            for (int row = 0; row < 9; row++) {
                for (int column = 0; column < 9; column++) {
                    Cell cell = grid.getCell(row, column);
                    if(cell.isEmpty())
                    empty++;
                }
            }

            if(empty != 81)
                System.out.print(grid.toString());
        }
        
    }

    public static void main( String[] args ) throws NumberFormatException, Exception{
     
        if(args.length == 0){
            System.out.println("Enter peer ID as command line argument");
            System.exit(-1);
        }
        
        new App(Integer.parseInt(args[0]));

    }

}
