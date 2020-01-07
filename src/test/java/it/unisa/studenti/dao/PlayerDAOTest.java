package it.unisa.studenti.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import it.unisa.studenti.MessageListener;
import it.unisa.studenti.P2PSudoku;
import it.unisa.studenti.dao.PlayerDAO;
import net.tomp2p.dht.PeerDHT;

public class PlayerDAOTest{
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
               return null;
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
    public void getGamePlayersTest() throws Exception{
        assertEquals("Players data structure retrieve when no game started", null, PlayerDAO.getGamePlayers(peer, gameName));
        
        sudoku.generateNewSudoku(gameName, 1);//creation of a mock sudoku game
        PlayerDAO.initPlayers(peer, gameName, nickname, score);

        HashMap<String, Object[]> players = new HashMap<>();
        Object[] playerInfo = new Object[2];
        playerInfo[0] = peer.peer().peerAddress();
        playerInfo[1] = score;
        players.put(nickname, playerInfo);
        HashMap<String, Object[]> playersCallResult = PlayerDAO.getGamePlayers(peer, gameName);

        assertEquals("Right player nickname in players data structure", players.entrySet().iterator().next().getValue()[0], playersCallResult.entrySet().iterator().next().getValue()[0]);
        assertEquals("Right player ip address in players data structure", players.entrySet().iterator().next().getValue()[1], playersCallResult.entrySet().iterator().next().getValue()[1]);
    }

    @Test
    public void initPlayersTest() throws Exception{
        boolean initPlayerResult1 = PlayerDAO.initPlayers(peer, gameName, nickname, score);
        assertFalse("Initialization with no game of Data Structure containing players data for a game", initPlayerResult1);

        sudoku.generateNewSudoku(gameName, 1);//creation of a mock sudoku game

        boolean initPlayerResult2 = PlayerDAO.initPlayers(peer, gameName, nickname, score);
        assertTrue("Game already existing, initialization of Data Structure containing players data for a game", initPlayerResult2);
    }

    @Test
    public void storePlayerTest() throws Exception{
        String nickname = "flash96";
        assertFalse("Incorrect Player store without a game with name gameName", PlayerDAO.storePlayer(peer, gameName, nickname, score, false));

        sudoku.generateNewSudoku(gameName, 1);//creation of a mock sudoku game
        PlayerDAO.initPlayers(peer, gameName, this.nickname, score);

        assertTrue("Correct Player store when the game with name gameName exists", PlayerDAO.storePlayer(peer, gameName, nickname, score, false));
    }

    @Test
    public void removePlayerTest() throws Exception{
        String nickname = "flash96";
        assertFalse("Incorrect Player remove without a game with name <gameName>", PlayerDAO.storePlayer(peer, gameName, nickname, score, true));
        
        sudoku.generateNewSudoku(gameName, 1);//creation of a mock sudoku game
        PlayerDAO.initPlayers(peer, gameName, this.nickname, score);
        
        assertFalse("Incorrect Player remove without player with nickname <nickname> in game with name <gameName>", PlayerDAO.storePlayer(peer, gameName, nickname, score, true));

        PlayerDAO.storePlayer(peer, gameName, nickname, score, false);

        assertTrue("Correct Player remove when the player with nickname <nickname> joined the game with name <gameName>", PlayerDAO.storePlayer(peer, gameName, nickname, score, false));
    }

    
}