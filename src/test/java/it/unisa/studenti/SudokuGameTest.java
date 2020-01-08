package it.unisa.studenti;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SudokuGameTest {
    final static Integer PEERS_NUMBER = 4;
    final static Integer MASTERID = 1;
    final static String MASTERPEER = "127.0.0.1";
    Runnable[] peerRunnable = new Runnable[PEERS_NUMBER];
    static P2PSudoku[] peerSudoku = new P2PSudoku[PEERS_NUMBER];
    static Integer[] peerId = {1, 2, 3, 4 };
    String[] nicknames = {"master", "francesco", "michele", "giovanni" };
    final CountDownLatch[] cl = new CountDownLatch[4];
    private HashMap<String, Integer> leaderboard;
    private Boolean correctLeftGame;

    @BeforeAll
    static void startMasterPeer() throws Exception {

        class PeerMessageListener implements MessageListener {

            @Override
            public Object parseMessage(Object obj) {
                String message = (String) obj;
                if (message.contains("Game Finished")) {
                    System.out.println(message);
                    System.exit(0);
                }
                System.out.println(message);
                return "success";
            }
        }

        peerSudoku[0] = new P2PSudoku(MASTERID, MASTERPEER, new PeerMessageListener());

        for (int i = 1; i < 4; i++) {
            peerSudoku[i] = new P2PSudoku(peerId[i], MASTERPEER, new PeerMessageListener());
        }
    }

    @Test
    void startTestCase() throws Exception {
        Random random = new Random();
        String gameName = "game1";
        Integer difficulty = 1; // Easy Game
        String masterNickname = "master";

        // Master peer generates a new game
        peerSudoku[0].generateNewSudoku(gameName, difficulty);
        // Master peer joins its own game
        peerSudoku[0].join(gameName, masterNickname);

        assertNotEquals(null, peerSudoku[0], "sudoku peer correctly initialized");

        cl[0] = new CountDownLatch(PEERS_NUMBER);
        // peers join the same game
        for (int i = 0; i < 4; i++) {
            new Thread(new peerJoinRunnable(peerSudoku[i], gameName, nicknames[i])).start();
        }
        cl[0].await();

        // master peer places a number
        int randomRow = random.nextInt(9);
        int randomCol = random.nextInt(9);
        int randomNumber = random.nextInt(9) + 1;
        peerSudoku[0].placeNumber(gameName, masterNickname, randomRow, randomCol, randomNumber);

        // all peers place a number for a random number of rounds
        int rounds = random.nextInt(5)+1;
        cl[1] = new CountDownLatch(PEERS_NUMBER * rounds);
        do{
            for (int i = 0; i < 4; i++) {
                randomRow = random.nextInt(9);
                randomCol = random.nextInt(9);
                randomNumber = random.nextInt(9) + 1;
                new Thread(new peerPlaceNumberRunnable(peerSudoku[i], gameName, nicknames[i], randomRow, randomCol,
                        randomNumber)).start();
            }
            rounds--;
        }while(rounds > 0 );
        cl[1].await();

        cl[2] = new CountDownLatch(1);
        //Master peer gets the leaderboard
        new Thread(()->{
            leaderboard = peerSudoku[0].getLeaderboard(gameName);
            cl[2].countDown();
        }).start();

        cl[2].await();

        assertNotEquals(null, leaderboard, "Leaderboard not null");

        //One random peer gets its local sudoku board
        assertNotEquals(null, peerSudoku[random.nextInt(4)].getSudoku(gameName), "Sudoku board not null");
        
        cl[3] = new CountDownLatch(1);
        //one random peer leaves the game
        int index = random.nextInt(3) + 2;
        new Thread(new peerLeaveGameRunnable(peerSudoku[index], gameName, nicknames[index])).start();
        cl[3].await();

        assertTrue(correctLeftGame, "game left correctly by peer");
    }

    class peerJoinRunnable implements Runnable {
        P2PSudoku peerSudoku;
        String gameName;
        String nickname;

        peerJoinRunnable(P2PSudoku peerSudoku, String gameName, String nickname) {
            this.peerSudoku = peerSudoku;
            this.gameName = gameName;
            this.nickname = nickname;
        }

        @Override
        public void run() {
            peerSudoku.join(gameName, nickname);
            cl[0].countDown();
        }
    }

    class peerPlaceNumberRunnable implements Runnable {
        P2PSudoku peerSudoku;
        String gameName, nickname;
        Integer row, col, number;

        peerPlaceNumberRunnable(P2PSudoku peerSudoku, String gameName, String nickname, Integer row, Integer col,
                Integer number) {
            this.peerSudoku = peerSudoku;
            this.gameName = gameName;
            this.nickname = nickname;
            this.row = row;
            this.col = col;
            this.number = number;
        }

        @Override
        public void run() {
            try {
                peerSudoku.placeNumber(gameName, nickname, row, col, number);
                cl[1].countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
        
    }

    class peerLeaveGameRunnable implements Runnable {
        P2PSudoku peerSudoku;
        String gameName;
        String nickname;

        peerLeaveGameRunnable(P2PSudoku peerSudoku, String gameName, String nickname){
            this.peerSudoku = peerSudoku;
            this.gameName = gameName;
            this.nickname = nickname;
        }

        @Override
        public void run() {
            try{
                correctLeftGame=peerSudoku.leaveGame(gameName, nickname);
            }catch(Exception e){
                e.printStackTrace();
            }
            cl[3].countDown();
        }
        
    }

}