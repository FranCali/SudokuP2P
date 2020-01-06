package it.unisa.studenti;

import java.io.IOException;

import de.ad.sudoku.Grid;

/**
Copyright 2017 Universita' degli Studi di Salerno

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 A P2P Sudoku challege games. Each user can place a number of the
 sudoku game, if it is not already placed takes 1 point, if it is already placed and it is rights takes
 0 point, in other case receive -1 point. The games is based on 9 x 9 matrix.
 All users that play to a game are automatically informed when a users increment its score, and when
 the game is finished.
*/

public interface SudokuGame {
	/**
	 * Creates new games.
	 * @param gameName a String, the sudoku game name.
	 * @param difficulty an Integer, the sudoku game difficulty.
	 * @return
	 */
	public Grid generateNewSudoku(String gameName, Integer difficulty) throws Exception;

	/**
	 * Joins in a game.
	 * @param gameName a String, the sudoku game name.
	 * @param nickname a String, the name of the user.
	 * @return true if the join success, false otherwise.
	 */
	public boolean join(String gameName, String nickname);

	/**
	 * Gets the Sudoku matrix game, with only the number placed by the user.
	 * @param gameName a String, the sudoku game name.
	 * @return a Grid object representing the matrix of the sudoku game.
	 */
	public Grid getSudoku(String gameName);
	
	/**
	 * Places a new solution number in the game.
	 * @param gameName a String, the sudoku game name.
	 * @param nickname a String, the player nickname.
	 * @param row the position on the row.
	 * @param col the position on the column.
	 * @param number the solution number.
	 * @return the integer score of the placed number.
	 */
	public Integer placeNumber(String gameName, String nickname, int row, int col, int number) throws ClassNotFoundException, InterruptedException, IOException;
	
	
}
