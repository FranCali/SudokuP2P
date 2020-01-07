package it.unisa.studenti.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import de.ad.sudoku.Generator;
import de.ad.sudoku.Grid;
import it.unisa.studenti.MessageListener;
import it.unisa.studenti.P2PSudoku;
import net.tomp2p.dht.PeerDHT;

public class SudokuDAOTest{
    private PeerDHT peer = null;
    private P2PSudoku sudoku;
    private String gameName = "game";
    private String nickname = "nickname";
    private Integer score;

    @Before
    public void startMasterPeer() throws Exception{
        sudoku = new P2PSudoku(1, "127.0.0.1", new MessageListener(){
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
        this.peer = sudoku.getPeer();
        this.score = sudoku.getScore();
    }

    @After
    public void leaveNetwork(){
        this.peer.peer().announceShutdown().start().awaitUninterruptibly();
    }

    @Test
    public void initGlobalSudokuTest() throws Exception{
        Generator generator = new Generator();
        Grid grid1 = generator.generate(0);
        assertTrue("Correct sudoku initialization when game with name <gameName> is not already created", SudokuDAO.initGlobalSudoku(peer, gameName, grid1));

        Grid grid2 = sudoku.generateNewSudoku(gameName, 1);
        assertFalse("Incorrect sudoku initialization when game with name <gameName> is already created", SudokuDAO.initGlobalSudoku(peer, gameName, grid2));
    }

    @Test
    public void getGlobalSudokuTest() throws Exception{
        
        assertEquals("Incorrect sudoku global board retrieval when game with name <gameName> does not exist", null, SudokuDAO.getGlobalSudoku(peer, gameName));

        Grid grid = sudoku.generateNewSudoku(gameName, 1);
        Grid gridCallResult2 = SudokuDAO.getGlobalSudoku(peer, gameName);

        for(int i=0; i<grid.getSize(); i++){
            for(int j=0; j<grid.getSize(); j++){
                assertEquals("Correct sudoku global board cell retrieval", grid.getCell(i, j).getValue(), gridCallResult2.getCell(i, j).getValue());
            }
        }

    }

    @Test
    public void storeGlobalSudokuTest() throws Exception{
        int row = 0;
        int col = 0;
        int number = 1;
        assertFalse("Incorrect global sudoku store when no game exists with <gameName>", SudokuDAO.storeGlobalSudoku(peer, gameName, row, col, number));

        sudoku.generateNewSudoku(gameName, 1);
        assertTrue("Correct global sudoku store when game with <gameName> exists", SudokuDAO.storeGlobalSudoku(peer, gameName, row, col, number));
    }

}