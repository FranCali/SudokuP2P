package it.unisa.studenti;

import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;
import it.unisa.studenti.utils.*;
import it.unisa.studenti.dao.*;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.net.InetAddress;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class P2PSudoku implements SudokuGame {

    private Grid solution;
    private Grid globalSudoku;
    private Grid localSudoku;
    private static Integer score = 0;
    private Generator generator = new Generator();
    private Solver solver = new Solver();
    final private PeerDHT peer;

    public P2PSudoku(int peerId, String masterPeer, final MessageListener listener) throws Exception{
        peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerId)).ports(4000 + peerId).start()).start();
        FutureBootstrap fb;
        fb = this.peer.peer().bootstrap().inetAddress(InetAddress.getByName(masterPeer)).ports(4001).start();
        fb.awaitUninterruptibly();
        
        if(fb.isSuccess()){
            peer.peer().discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }
        else
            throw new Exception("Error connecting to master peer");
        
        peer.peer().objectDataReply(new ObjectDataReply() {
            public Object reply(PeerAddress sender, Object request) throws Exception {
                return listener.parseMessage(request);
            }
        });
    }

    public PeerDHT getPeer(){
        return this.peer;
    }
    public Integer getScore(){
        return score;
    }

    public Grid generateNewSudoku(String gameName, Integer difficulty) throws Exception{
        boolean isValidSolution = false;
        Grid grid = generator.generate(0);
        solution = generator.generate(0);

        while(!isValidSolution){
            switch(difficulty){
                case 1: 
                    grid = generator.generate(42); //Easy
                    break;
                case 2: 
                    grid = generator.generate(50); //Medium
                    break;
                case 3: 
                    grid = generator.generate(56); //Hard
                    break;
            }

            for(int i=0; i<grid.getSize(); i++){ //Copying the generated sudoku
                for(int j=0; j<grid.getSize(); j++){
                    solution.getCell(i,j).setValue(grid.getCell(i, j).getValue());
                }
            }

            solver.solve(solution);
            if(solution.isFull())
                isValidSolution = true;
        }
        peer.put(Number160.createHash(gameName + "_solution")).data(new Data(solution)).start().awaitUninterruptibly(); //Save solution in DHT
        
        if(SudokuDAO.initGlobalSudoku(peer, gameName + "_empty", grid)){ //Store the initiated version
            SudokuDAO.initGlobalSudoku(peer, gameName, grid);
            this.localSudoku = grid;
         
            return this.localSudoku;
        } 
        else{
            return null;
        }
    }

    public boolean join(String gameName, String nickname) {
        HashMap<String, Object[]> players = new HashMap<>();

        if(SudokuDAO.getGlobalSudoku(peer, gameName)!= null){
            try{
                players = PlayerDAO.getGamePlayers(peer, gameName);
                if(players == null){
                    PlayerDAO.initPlayers(peer, gameName, nickname, score);
                }else{
                    if(players.containsKey(nickname)){
                        return false;
                    }
                    else{
                        PlayerDAO.storePlayer(peer, gameName, nickname, score, false);
                    }
                }
                localSudoku = SudokuDAO.getGlobalSudoku(peer, gameName + "_empty");
                solution = getSolution(gameName);
                return true;
                
            }catch(Exception e) {
                e.printStackTrace();
            }
        } 
        else{
            return false;
        }
        return false;
    }

    public Grid getSudoku(String gameName) {
        return this.localSudoku;
    }

    public Integer placeNumber(String gameName, String nickname, int row, int col, int number) throws ClassNotFoundException, InterruptedException, IOException {
        int points = 0;
        globalSudoku = SudokuDAO.getGlobalSudoku(peer, gameName);
        if(globalSudoku == null)
            return null;
        Cell globalCell = globalSudoku.getCell(row, col);
        Cell solutionCell = solution.getCell(row, col);

        if(globalCell.isEmpty()){
            if(number == solutionCell.getValue()){
                points = 1;
                try{
                    SudokuDAO.storeGlobalSudoku(peer, gameName, row, col, number);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            else{
                points = -1;
            }    
        }
        else {
            if(number == solutionCell.getValue()){
                points = 0;
            }
            else{
                System.out.println("valore non valido");
                points = -1;
            }
        }
        
        score += points;
        if(score < 0)
            score = 0;
        PlayerDAO.storePlayer(peer, gameName, nickname, score, false);

        CmdLineUtils.clearScreen();
        System.out.println("My points: " + points + " New Score: " + score);
        String message = "Player " + nickname + " scored " + points + " points - New Score: " + Integer.toString(score);
        this.broadcastMessage(gameName, message);

        Cell localCell = localSudoku.getCell(row, col);
        if(localCell.isEmpty()){
            if(number == solutionCell.getValue()){
                localCell.setValue(number);

                if(localSudoku.isFull()){ //Termination condition
                    this.endGame(gameName);
                }
            }
            else{
                System.out.println("Not valid value");
            }
        }
        else{
            System.out.println("Cell not empty");
        }

        return points;
    }
    
    public boolean leaveGame(String gameName, String nickname) throws ClassNotFoundException, InterruptedException, IOException {
        
        if(PlayerDAO.storePlayer(peer, gameName, nickname, score, true)){//true flag for isDeletion
            System.out.println("Player removed");
            peer.peer().announceShutdown().start().awaitUninterruptibly();
            
            return true;
        }
        return false;
    }
    
    private Grid getSolution(String gameName){
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName + "_solution")).getLatest().start();
            futureGet.awaitUninterruptibly();
            
            if(futureGet.isSuccess()){
                if(futureGet.isEmpty()) return null;
                Grid solution = (Grid) futureGet.data().object();
                return solution;
            }  
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
   
    private void endGame(String gameName){
        final HashMap<String, Object[]> players = PlayerDAO.getGamePlayers(peer, gameName);
        HashMap<String, Integer> playerScores = new HashMap<>();

        for(String nickname: players.keySet()){
            playerScores.put(nickname, (Integer) players.get(nickname)[1]);
        }

        MapUtils.sortByValue(playerScores);
        for(String nickname: playerScores.keySet()){
            playerScores.put(nickname, (Integer) players.get(nickname)[1]);
        }

        Map.Entry<String,Integer> entry = playerScores.entrySet().iterator().next();
        String nickname = entry.getKey();
        Integer score = entry.getValue();

        String message = "Game Finished!, " + "player " + nickname + " won with score: " + score;

        System.out.println(message);
        this.broadcastMessage(gameName, message);
        System.exit(0);
    }

    public HashMap<String, Integer> getLeaderboard(String gameName) { 
		final HashMap<String, Object[]> players = PlayerDAO.getGamePlayers(peer, gameName);
        
        if(players == null)
            return null;

        HashMap<String, Integer> playerScores = new HashMap<>();

		for(String nickname: players.keySet()){
			playerScores.put(nickname, (Integer) players.get(nickname)[1]);
		}

        MapUtils.sortByValue(playerScores);
        
        return playerScores;
	}

    @SuppressWarnings("unchecked")
    private void broadcastMessage(String gameName, Object message){
        HashMap<String, Object[]> players;
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName + "_players")).getLatest().start();
            futureGet.awaitUninterruptibly();
            
            if(futureGet.isSuccess() && !futureGet.isEmpty()){
                players =  (HashMap<String,Object[]>) futureGet.dataMap().values().iterator().next().object();
            
                for(Object[] player:players.values()){
                    if(!this.peer.peer().peerAddress().equals((PeerAddress) player[0])){
                        FutureDirect futureDirect = peer.peer().sendDirect((PeerAddress) player[0]).object(message).start();
                        futureDirect.awaitUninterruptibly();
                        
                    }
                }
            }  
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}