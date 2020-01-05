package it.unisa.studenti;

import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;

import java.util.HashMap;
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

    private Grid globalSudoku;
    private Grid localSudoku;
    private Integer score = 0;

    private Generator generator = new Generator();

    final private PeerDHT peer;

    public P2PSudoku(int peerId, String masterPeer, final MessageListener listener) throws Exception{
        peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerId)).ports(4000 + peerId).start()).start();
        FutureBootstrap fb;
        fb = this.peer.peer().bootstrap().inetAddress(InetAddress.getByName(masterPeer)).ports(4001).start();
        fb.awaitUninterruptibly();
        if(fb.isSuccess())
            peer.peer().discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        else
            throw new Exception("Error connecting to master peer");
        
        peer.peer().objectDataReply(new ObjectDataReply() {
            public Object reply(PeerAddress sender, Object request) throws Exception {
                return listener.parseMessage(request);
            }
        });
    }

    public Grid generateNewSudoku(String gameName, Integer difficulty) throws Exception{
        Grid grid = generator.generate(0);

        switch(difficulty){
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

        
        if(this.storeGlobalSudoku(gameName + "_empty", grid)){ //Store the initiated version
            this.storeGlobalSudoku(gameName, grid);
            this.localSudoku = grid;

            return this.localSudoku;
        } 
        else{
            return null;
        }
    }

    public boolean join(String gameName, String nickname) {
        String key = gameName + "_players";
        HashMap<String, Object[]> players = new HashMap<>();

        if(getGlobalSudoku(gameName)!= null){
            try{
                if(getGamePlayers(gameName) == null){ //First Player to join game
                    peer.put(Number160.createHash(key)).data(new Data(players)).start().awaitUninterruptibly();   
                }
                players = getGamePlayers(gameName);
                if(players.containsKey(nickname)){
                    return false;
                }
                else{
                    Object[] playerInfo = new Object[2]; //array containing player address and player score
                    playerInfo[0] = peer.peer().peerAddress();
                    playerInfo[1] = score;
                    players.put(nickname, playerInfo);
                    peer.put(Number160.createHash(key)).data(new Data(players)).start().awaitUninterruptibly();   
                    this.localSudoku = this.getGlobalSudoku(gameName + "_empty");
                    return true;
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
        } 
        else{
            return false;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object[]> getGamePlayers(String gameName){
        String key = gameName + "_players";
        
        try{
            FutureGet futureGet = peer.get(Number160.createHash(key)).start();
                futureGet.awaitUninterruptibly();
                if(futureGet.isSuccess() && !futureGet.isEmpty()){
                    return (HashMap<String, Object[]>) futureGet.dataMap().values().iterator().next().object();
                }
                else{
                    return null;
                }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Grid getGlobalSudoku(String gameName){
        Grid grid = Grid.emptyGrid();
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess()){
                if(futureGet.isEmpty()) return null;
                grid = (Grid) futureGet.dataMap().values().iterator().next().object();
                return grid;
            }  
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer placeNumber(String gameName, String nickname, int row, int col, int number){
        int points = 0;
        globalSudoku = this.getGlobalSudoku(gameName);
        Cell globalCell = globalSudoku.getCell(row, col);
        
        if(globalCell.isEmpty()){
            if(globalSudoku.isValidValueForCell(globalCell, number)){
                globalCell.setValue(number);
                points = 1;
                this.storeGlobalSudoku(gameName, globalSudoku);
            }
            else{
                points = -1;
            }    
        }
        else {
            if(globalSudoku.isValidValueForCell(globalCell, number)){
                points = 0;
            }
            else{
                points = -1;
            }
        }
        score += points;

        clearScreen();
        System.out.println("My points: " + points + " New Score: " + this.score);
        String message = "Player " + nickname + " scored " + points + " points - New Score: " + Integer.toString(score);
        this.broadcastMessage(gameName, message);

        Cell localCell = localSudoku.getCell(row, col);
        if(localCell.isEmpty()){
            if(localSudoku.isValidValueForCell(localCell, number)){
                localCell.setValue(number);
                if(localSudoku.isFull()){ //Termination condition
                    this.endGame(gameName, nickname);
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

    @SuppressWarnings("unchecked")
    private void broadcastMessage(String gameName, Object message){
        HashMap<String, Object[]> players;
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName + "_players")).start();
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

    private boolean storeGlobalSudoku(String gameName, Grid grid) {
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess()) {
                peer.put(Number160.createHash(gameName)).data(new Data(grid)).start().awaitUninterruptibly();   
                return true; 
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean leaveGame(String gameName, String nickname){

        if(removePlayer(gameName, nickname)){
            peer.peer().announceShutdown().start().awaitUninterruptibly();
            return true;
        }

        return false;
    }

    @Override
    public Grid getSudoku(String gameName) {
        return this.localSudoku;
    }

    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }

    private void endGame(String gameName, String nickname){
        String message = "Game Finished, " + "player " + nickname + " filled his board with score: " + score;
        this.broadcastMessage(gameName, message);
        System.exit(0);
    }

    private boolean removePlayer(String gameName, String nickname){
        String key = gameName + "_players";

        HashMap<String, Object[]> players = this.getGamePlayers(gameName);
        if(players != null){
            if(players.containsKey(nickname)){
                try{
                    players.remove(nickname);

                    peer.put(Number160.createHash(key)).data(new Data(players)).start().awaitUninterruptibly();   
                }catch(Exception e){
                    e.printStackTrace();
                }
                
                return true;
            }
            else
                return false;
        }
        
        return false;
    }
}