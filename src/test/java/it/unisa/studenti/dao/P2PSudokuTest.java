package it.unisa.studenti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import de.ad.sudoku.Grid;
import it.unisa.studenti.MessageListener;
import it.unisa.studenti.P2PSudoku;
import net.tomp2p.dht.PeerDHT;

public class P2PSudokuTest{
    private PeerDHT peer = null;
    private P2PSudoku sudoku;
    private String gameName = "game";
    private String nickname = "nickname";
    private Integer score;
    private Grid board = null;

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
        this.board = sudoku.getSudoku(gameName);
    }

    @After
    public void leaveNetwork(){
        this.peer.peer().announceShutdown().start().awaitUninterruptibly();
    }


    @Test
    public void getPeerTest(){
        assertEquals("Correct retrieve of the peer of the sudoku game", this.peer, sudoku.getPeer());
    }

    @Test
    public void getScore(){
        assertEquals("Correct retrieve of the peer's score of the sudoku game", this.score, sudoku.getScore());
    }

    @Test
    public void generateNewSudokuTest() throws Exception{
        assertNotEquals("Correct sudoku game generation when anothe game with same name does not exist", null, sudoku.generateNewSudoku(gameName, 1));
        sudoku.generateNewSudoku(gameName, 1);
        assertEquals("Incorrect sudoku game generation when anothe game with same name is present", null, sudoku.generateNewSudoku(gameName, 1));
    }

    @Test
    public void joinTest() throws Exception{
        String mockNickname = "flash96";
        assertFalse("Incorrect join when game with name <gameName> does not exist", sudoku.join(gameName, nickname));
        sudoku.generateNewSudoku(gameName, 1);
        assertTrue("Correct join when player with name <nickname> is not present in game with name <gameName> and the game does exist", sudoku.join(gameName, nickname));
        sudoku.join(gameName, mockNickname);
        assertFalse("Incorrect join when player with name <nickname> is already present in game with name <gameName> does not exist", sudoku.join(gameName, mockNickname));
    }

    @Test
    public void getSudokuTest(){
        assertEquals("Correct sudoku board retrivial of the local board of the player", this.board, sudoku.getSudoku(gameName));
    }

    @Test
    public void placeNumberTest()throws Exception{
        String mockGameName = "partita20";
        assertEquals("Incorrect number placing when no game with <mockGameName> exists", null,sudoku.placeNumber(mockGameName, nickname, 0, 0, 1));
        Integer high = 1, low = -1;
        sudoku.generateNewSudoku(gameName, 1);
        sudoku.join(gameName, nickname);
        assertTrue("Correct number placing", high >= sudoku.placeNumber(gameName, nickname, 0, 0, 1));
        assertTrue("Correct number placing", low <= sudoku.placeNumber(gameName, nickname, 0, 0, 1));
    }

    @Test
    public void leaveGameTest() throws Exception{
        assertFalse("Correct leaving a game that does not exist", sudoku.leaveGame(gameName, nickname));
        sudoku.generateNewSudoku(gameName, 1);
        sudoku.join(gameName, nickname);
        assertTrue("Correct leaving a game", sudoku.leaveGame(gameName, nickname));
    }

    @Test
    public void getLeaderboardTest() throws Exception{
        assertEquals("Incorrect Leaderboard retrieval of a game that does not exist", null, sudoku.getLeaderboard(gameName));
        sudoku.generateNewSudoku(gameName, 1);
        sudoku.join(gameName, nickname);
        assertNotEquals("Correct Leaderboard retrieval", null, sudoku.getLeaderboard(gameName));
    }

}