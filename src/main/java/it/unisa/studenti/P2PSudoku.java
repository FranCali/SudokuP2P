package it.unisa.studenti;
import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;

import java.util.Random;
import java.util.Scanner;


public class P2PSudoku implements SudokuGame {

    Integer[][] board = new Integer[9][9]; 
    Generator generator = new Generator();
    Random random = new Random();

    public P2PSudoku(){
        System.out.println("Created new Sudoku");
    }

    public Grid generateNewSudoku(String _game_name){
        int command = -1;
        Scanner scanner = new Scanner(System.in);
        Grid grid = generator.generate(0);
        do{
            System.out.println("Chose difficulty: \n1)Easy \n2)Medium \n3)Hard");
            command = scanner.nextInt();
        }while(command!=1 && command!=2 && command!=3);
        scanner.close();
        
        switch(command){
            case 1: 
                grid = generator.generate(43); //Easy
                break;
            case 2: 
                grid = generator.generate(51); //Medium
                break;
            case 3: 
                grid = generator.generate(56); //Hard
                break;
        }
        return grid;
    }

    public boolean join(String _game_name, String _nickname){
        
        return false;
    }

    public Integer[][] getSudoku(String _game_name){
        return board;
    }

    public Integer placeNumber(String _game_name, int _i, int _j, int _number){
        int point = 0;

        return point;
    }

}