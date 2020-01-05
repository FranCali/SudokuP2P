package it.unisa.studenti;

import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

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

    private Grid globalSudoku;
    private Grid localSudoku;
    private Integer score = 0;
    private String nickname;

    Generator generator = new Generator();
    Random random = new Random();

    final private PeerDHT peer;
    ArrayList<String> players = new ArrayList<>();

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

    public Grid generateNewSudoku(String gameName) throws Exception{
        int command = -1;
        Scanner scanner = new Scanner(System.in);
        Grid grid = generator.generate(0);

        do{
            System.out.println("Chose difficulty: \n1)Easy \n2)Medium \n3)Hard");
            command = scanner.nextInt();
        }while(command!=1 && command!=2 && command!=3);
        
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
        HashMap<String, PeerAddress> players = new HashMap<>();

        if(getGlobalSudoku(gameName)!= null){
            try{
                if(getGamePlayers(gameName) == null){
                    peer.put(Number160.createHash(key)).data(new Data(players)).start().awaitUninterruptibly();   
                }
                players = getGamePlayers(gameName);
                if(players.containsKey(nickname))
                    return false;
                else{
                    this.nickname = nickname;
                    players.put(nickname, peer.peer().peerAddress());
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

    public HashMap<String, PeerAddress> getGamePlayers(String gameName){
        String key = gameName + "_players";
        
        try{
            FutureGet futureGet = peer.get(Number160.createHash(key)).start();
                futureGet.awaitUninterruptibly();
                if(futureGet.isSuccess() && !futureGet.isEmpty()){
                    return (HashMap<String, PeerAddress>) futureGet.dataMap().values().iterator().next().object();
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

    public Integer placeNumber(String gameName, int row, int col, int number){
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
        String message = "Player " + this.nickname + " scored " + points + " points - New Score: " + Integer.toString(score);
        this.broadcastMessage(gameName, message);

        Cell localCell = localSudoku.getCell(row, col);
        if(localCell.isEmpty()){
            if(localSudoku.isValidValueForCell(localCell, number)){
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

    private void broadcastMessage(String gameName, Object message){
        HashMap<String, PeerAddress> players;
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName + "_players")).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess() && !futureGet.isEmpty()){
                players =  (HashMap<String,PeerAddress>) futureGet.dataMap().values().iterator().next().object();
            
                for(PeerAddress player:players.values()){
                    if(!this.peer.peer().peerAddress().equals(player)){
                        FutureDirect futureDirect = peer.peer().sendDirect(player).object(message).start();
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

    public boolean leaveNetwork(){
        peer.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }

    @Override
    public Grid getSudoku(String gameName) {
        return this.localSudoku;
    }

    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }

    private void endGame(String gameName){
        String message = "Game Finished, " + "player " + nickname + " filled his board with score: " + score;
        this.broadcastMessage(gameName, message);
        System.exit(0);
    }
}