package it.unisa.studenti;

import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;
import it.unisa.studenti.utils.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.IOException;
import java.net.InetAddress;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Pair;

public class P2PSudoku implements SudokuGame {

    private Grid solution;
    private Grid globalSudoku;
    private Grid localSudoku;
    private static Integer score = 0;
    private static Random random = new Random();
    private Generator generator = new Generator();
    private Solver solver = new Solver();
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

        if(this.initGlobalSudoku(gameName + "_empty", grid)){ //Store the initiated version
            this.initGlobalSudoku(gameName, grid);
            this.localSudoku = grid;
         
            return this.localSudoku;
        } 
        else{
            return null;
        }
    }

    public boolean join(String gameName, String nickname) {
        HashMap<String, Object[]> players = new HashMap<>();

        if(getGlobalSudoku(gameName)!= null){
            try{
                players = getGamePlayers(gameName);
                if(players == null){
                    this.initPlayers(gameName, nickname);
                }else{
                    if(players.containsKey(nickname)){
                        return false;
                    }
                    else{
                        storePlayer(peer, gameName, nickname);
                    }
                }
                localSudoku = getGlobalSudoku(gameName + "_empty");
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

    private Grid getSolution(String gameName){
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName + "_solution")).getLatest().start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess()){
                if(futureGet.isEmpty()) return null;
                //grid = (Grid) futureGet.dataMap().values().iterator().next().object();
                Grid solution = (Grid) futureGet.data().object();
                return solution;
            }  
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public HashMap<String, Object[]> getGamePlayers(String gameName){
        String key = gameName + "_players";
        
        try{
            FutureGet futureGet = peer.get(Number160.createHash(key)).getLatest().start();
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
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).getLatest().start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess()){
                if(futureGet.isEmpty()) return null;
                //grid = (Grid) futureGet.dataMap().values().iterator().next().object();
                grid = (Grid) futureGet.data().object();
                return grid;
            }  
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer placeNumber(String gameName, String nickname, int row, int col, int number) throws ClassNotFoundException, InterruptedException, IOException {
        int points = 0;
        globalSudoku = this.getGlobalSudoku(gameName);
        Cell globalCell = globalSudoku.getCell(row, col);
        Cell solutionCell = solution.getCell(row, col);

        if(globalCell.isEmpty()){
            if(number == solutionCell.getValue()){
                points = 1;
                try{
                    storeGlobalSudoku(peer, gameName, row, col, number);
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
        storePlayer(peer, gameName, nickname);

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

    private void endGame(String gameName){
        final HashMap<String, Object[]> players = getGamePlayers(gameName);
        HashMap<String, Integer> playerScores = new HashMap<>();

        for(String nickname: players.keySet()){
            System.out.println(nickname + players.get(nickname)[1]);
            playerScores.put(nickname, (Integer) players.get(nickname)[1]);
        }

        MapUtils.sortByValue(playerScores);
        for(String nickname: playerScores.keySet()){
            System.out.println(nickname + playerScores.get(nickname));
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



    private static void storeGlobalSudoku(PeerDHT peerDHT, String gameName, int row, int col, int number) throws ClassNotFoundException, InterruptedException, IOException {
		Pair<Number640, Byte> pair2 = null;
		for (int i = 0; i < 5; i++) {
			Pair<Number160, Data> pair = getAndUpdateGrid(peerDHT, gameName, row, col, number);
			if (pair == null) {
				System.out.println("we cannot handle this kind of inconsistency automatically, handing over the the API dev");
				return;
			}
			FuturePut fp = peerDHT
					.put(Number160.createHash(gameName))
                    .data(pair.element1().prepareFlag(), pair.element0())
                    .start().awaitUninterruptibly();
            pair2 = checkVersions(fp.rawResult());
			// 1 is PutStatus.OK_PREPARED
			if (pair2 != null && pair2.element1() == 1) {
				break;
			}
			System.out.println("get delay or fork - put");
			// if not removed, a low ttl will eventually get rid of it
			peerDHT.remove(Number160.createHash(gameName)).versionKey(pair.element0()).start()
					.awaitUninterruptibly();
			Thread.sleep(random.nextInt(500));
		}
		if (pair2 != null && pair2.element1() == 1) {
			peerDHT.put(Number160.createHash(gameName))
					.versionKey(pair2.element0().versionKey()).putConfirm()
					.data(new Data()).start().awaitUninterruptibly();
		} else {
			System.out
					.println("we cannot handle this kind of inconsistency automatically, handing over the the API dev");
		}
    }
    
	private static Pair<Number160, Data> getAndUpdateGrid(PeerDHT peerDHT, String gameName, int row, int col, int number) throws InterruptedException, ClassNotFoundException, IOException {
		Pair<Number640, Data> pair = null;
		for (int i = 0; i < 5; i++) {
            FutureGet fg = peerDHT.get(Number160.createHash(gameName)).getLatest().start().awaitUninterruptibly();
			// check if all the peers agree on the same latest version, if not
			// wait a little and try again
            pair = checkVersions(fg.rawData());
			if (pair != null) {
				break;
			}
			System.out.println("get delay or fork - get");
			Thread.sleep(random.nextInt(500));
		}
		// we got the latest data
		if (pair != null) {
            // update operation is append
            Grid grid = (Grid) pair.element1().object();
            Cell globalCell = grid.getCell(row, col);
            globalCell.setValue(number);
			Data newData = new Data(grid);
			Number160 v = pair.element0().versionKey();
            long version = v.timestamp() + 1;
			newData.addBasedOn(v);
            //since we create a new version, we can access old versions as well
			return new Pair<Number160, Data>(new Number160(version, newData.hash()), newData);
		}
		return null;
    }
    

    private static void storePlayer(PeerDHT peerDHT, String gameName, String nickname) throws ClassNotFoundException, InterruptedException, IOException{
        Pair<Number640, Byte> pair2 = null;
		for (int i = 0; i < 5; i++) {
			Pair<Number160, Data> pair = getAndUpdatePlayer(peerDHT, gameName, nickname);
			if (pair == null) {
				System.out.println("we cannot handle this kind of inconsistency automatically, handing over the the API dev");
				return;
			}
			FuturePut fp = peerDHT
					.put(Number160.createHash(gameName + "_players"))
                    .data(pair.element1().prepareFlag(), pair.element0())
                    .start().awaitUninterruptibly();
            pair2 = checkVersions(fp.rawResult());
			// 1 is PutStatus.OK_PREPARED
			if (pair2 != null && pair2.element1() == 1) {
				break;
			}
			System.out.println("get delay or fork - put");
			// if not removed, a low ttl will eventually get rid of it
			peerDHT.remove(Number160.createHash(gameName + "_players")).versionKey(pair.element0()).start()
					.awaitUninterruptibly();
			Thread.sleep(random.nextInt(500));
		}
		if (pair2 != null && pair2.element1() == 1) {
			peerDHT.put(Number160.createHash(gameName + "_players"))
					.versionKey(pair2.element0().versionKey()).putConfirm()
					.data(new Data()).start().awaitUninterruptibly();
		} else {
			System.out
					.println("we cannot handle this kind of inconsistency automatically, handing over the the API dev");
		}
    }

    @SuppressWarnings("unchecked")
    private static Pair<Number160, Data> getAndUpdatePlayer(PeerDHT peerDHT, String gameName, String nickname) throws InterruptedException, ClassNotFoundException, IOException {

        Pair<Number640, Data> pair = null;
		for (int i = 0; i < 5; i++) {
            FutureGet fg = peerDHT.get(Number160.createHash(gameName + "_players")).getLatest().start().awaitUninterruptibly();
            // check if all the peers agree on the same latest version, if not
			// wait a little and try again
            pair = checkVersions(fg.rawData());
			if (pair != null) {
				break;
			}
			System.out.println("get delay or fork - get");
			Thread.sleep(random.nextInt(500));
		}
		// we got the latest data
		if (pair != null) {
            
            HashMap<String, Object[]> players = (HashMap<String, Object[]>) pair.element1().object();
            Object[] playerInfo = new Object[2]; //array containing player address and player score
            playerInfo[0] = peerDHT.peer().peerAddress();
            playerInfo[1] = score;
            players.put(nickname, playerInfo);
            
			Data newData = new Data(players);
			Number160 v = pair.element0().versionKey();
            long version = v.timestamp() + 1;
			newData.addBasedOn(v);
            //since we create a new version, we can access old versions as well
			return new Pair<Number160, Data>(new Number160(version, newData.hash()), newData);
		}
		return null;
    }


    private static <K> Pair<Number640, K> checkVersions(Map<PeerAddress, Map<Number640, K>> rawData) {
		Number640 latestKey = null;
		K latestData = null;
		for (Map.Entry<PeerAddress, Map<Number640, K>> entry : rawData.entrySet()) {
			if (latestData == null && latestKey == null) {
				latestData = entry.getValue().values().iterator().next();
                latestKey = entry.getValue().keySet().iterator().next();
			} else {
				if (!latestKey.equals(entry.getValue().keySet().iterator()
						.next())
						|| !latestData.equals(entry.getValue().values()
								.iterator().next())) {
					return null;
				}
			}
		}
		return new Pair<Number640, K>(latestKey, latestData);
	}


    private boolean initGlobalSudoku(String gameName, Grid grid) {
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

    private void initPlayers(String gameName, String nickname){
        HashMap<String, Object[]> players = new HashMap<>();
        String key = gameName + "_players";
        Object[] playerInfo = new Object[2];
        playerInfo[0] = peer.peer().peerAddress();
        playerInfo[1] = score;
        players.put(nickname, playerInfo);
        try{
            FutureGet futureGet = peer.get(Number160.createHash(key)).start();
            futureGet.awaitUninterruptibly();
            
            if(futureGet.isSuccess()) {
                peer.put(Number160.createHash(key)).data(new Data(players)).start().awaitUninterruptibly(); 
            }
        }catch(Exception e){
            e.printStackTrace();
        }
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