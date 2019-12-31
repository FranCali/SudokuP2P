package it.unisa.studenti;
import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;

public class App 
{
    public static void main( String[] args )
    {
        P2PSudoku sudoku = new P2PSudoku();

        Grid grid = sudoku.generateNewSudoku("Partita1");
        
        


        System.out.print(grid.toString());
    }
}
